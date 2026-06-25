package com.unifurniture.mobile.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unifurniture.mobile.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * Streams replies from the "UniFurniture Assistant" chatbot.
 *
 * The app talks to our own backend ({@code /api/chat}), which proxies to Dify and keeps the
 * Dify API key server-side. The backend forwards Dify's Server-Sent Events stream, so we use
 * okhttp-sse to read each chunk as it arrives.
 *
 * NOTE: all listener callbacks run on a background thread — marshal to the main thread before
 * touching UI / non-thread-safe LiveData (the ViewModel uses {@code postValue}).
 */
public class ChatRepository {

    /** Callbacks for one streamed reply. */
    public interface ChatStreamListener {
        /** The full answer accumulated so far (re-sent on every chunk). */
        void onChunk(@NonNull String fullAnswer);
        /** Dify's conversation id — persist it to keep the conversation going. */
        void onConversationId(@NonNull String conversationId);
        /** The backend is retrying a transient model error (e.g. Gemini 429/503). */
        void onRetry(int attempt, int max);
        /** Stream finished normally. */
        void onComplete();
        /** Stream failed or Dify returned an error. */
        void onError(@NonNull String message);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    @Nullable private EventSource activeSource;
    /** Set when we deliberately cancel (leaving the screen / sending a new message) — suppresses the late failure. */
    private volatile boolean canceled;

    public ChatRepository() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // tolerate Cloud Run cold starts
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // streaming: no read timeout
                .build();
    }

    /**
     * Send one message and stream the reply.
     *
     * @param query          the user's message
     * @param conversationId previous conversation id, or null/empty for a new conversation
     * @param user           stable end-user id (Dify uses it for per-user history)
     */
    public void send(@NonNull String query,
                     @Nullable String conversationId,
                     @NonNull String user,
                     @NonNull ChatStreamListener listener) {
        cancel(); // only one in-flight stream at a time
        canceled = false;

        JsonObject body = new JsonObject();
        body.addProperty("query", query);
        body.addProperty("user", user);
        if (conversationId != null && !conversationId.isEmpty()) {
            body.addProperty("conversation_id", conversationId);
        }

        Request request = new Request.Builder()
                .url(BuildConfig.API_BASE_URL + "chat")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        final StringBuilder answer = new StringBuilder();
        // [done] true once the stream reached a terminal state (message_end / error / close);
        // [gotAnswer] true once any reply text arrived. Guards against the SSE client reporting
        // the normal end-of-stream (or our own cancel) as a failure.
        final boolean[] done = {false};
        final boolean[] gotAnswer = {false};

        activeSource = EventSources.createFactory(client)
                .newEventSource(request, new EventSourceListener() {
                    @Override
                    public void onEvent(@NonNull EventSource es, @Nullable String id,
                                        @Nullable String type, @NonNull String data) {
                        // Each `data:` line is one JSON chunk from Dify.
                        JsonObject obj;
                        try {
                            obj = JsonParser.parseString(data).getAsJsonObject();
                        } catch (Exception ignored) {
                            return;
                        }

                        if (obj.has("conversation_id") && !obj.get("conversation_id").isJsonNull()) {
                            String cid = obj.get("conversation_id").getAsString();
                            if (!cid.isEmpty()) listener.onConversationId(cid);
                        }

                        String event = obj.has("event") && !obj.get("event").isJsonNull()
                                ? obj.get("event").getAsString() : "";

                        // Agent apps emit "agent_message"; basic apps emit "message".
                        if (("agent_message".equals(event) || "message".equals(event))
                                && obj.has("answer") && !obj.get("answer").isJsonNull()) {
                            answer.append(obj.get("answer").getAsString());
                            gotAnswer[0] = true;
                            listener.onChunk(answer.toString());
                        } else if ("retrying".equals(event)) {
                            int attempt = obj.has("attempt") && !obj.get("attempt").isJsonNull()
                                    ? obj.get("attempt").getAsInt() : 1;
                            int max = obj.has("max") && !obj.get("max").isJsonNull()
                                    ? obj.get("max").getAsInt() : 0;
                            listener.onRetry(attempt, max);
                        } else if ("message_end".equals(event)) {
                            if (!done[0]) {
                                done[0] = true;
                                listener.onComplete();
                            }
                        } else if ("error".equals(event)) {
                            if (!done[0]) {
                                done[0] = true;
                                String m = obj.has("message") && !obj.get("message").isJsonNull()
                                        ? obj.get("message").getAsString() : "error";
                                listener.onError(m);
                            }
                        }
                        // agent_thought / ping / tts_* are ignored.
                    }

                    @Override
                    public void onClosed(@NonNull EventSource es) {
                        if (!done[0]) {
                            done[0] = true;
                            listener.onComplete();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull EventSource es,
                                          @Nullable Throwable t, @Nullable Response response) {
                        // Ignore failures that happen after we already finished or that we caused
                        // by cancelling — these are the spurious "can't reach assistant" toasts.
                        if (done[0] || canceled) return;
                        done[0] = true;
                        if (gotAnswer[0]) {
                            // Got a reply, then the connection dropped at the tail — treat as done.
                            listener.onComplete();
                            return;
                        }
                        String msg;
                        if (t != null && t.getMessage() != null) {
                            msg = t.getMessage();
                        } else if (response != null) {
                            msg = "HTTP " + response.code();
                        } else {
                            msg = "stream failed";
                        }
                        android.util.Log.e("ChatRepository", "SSE onFailure: " + msg
                                + (response != null ? " (HTTP " + response.code() + ")" : ""), t);
                        listener.onError(msg);
                    }
                });
    }

    /** Cancel the in-flight stream, if any. Call when leaving the chat screen. */
    public void cancel() {
        canceled = true;
        if (activeSource != null) {
            activeSource.cancel();
            activeSource = null;
        }
    }
}
