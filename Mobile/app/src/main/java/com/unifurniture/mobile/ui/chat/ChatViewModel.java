package com.unifurniture.mobile.ui.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.unifurniture.mobile.data.repository.ChatRepository;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository = new ChatRepository();

    private final MutableLiveData<List<ChatMessage>> messages =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    @Nullable private String conversationId;

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean> getSending() { return sending; }
    public LiveData<String> getError() { return error; }

    /** Send a user message and stream the assistant's reply. */
    public void sendMessage(@NonNull String text, @NonNull String userId) {
        if (Boolean.TRUE.equals(sending.getValue())) return; // one at a time
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        List<ChatMessage> list = currentList();
        list.add(ChatMessage.user(trimmed));
        final ChatMessage botMsg = ChatMessage.botPending();
        list.add(botMsg);
        messages.setValue(list);
        sending.setValue(true);

        repository.send(trimmed, conversationId, userId, new ChatRepository.ChatStreamListener() {
            @Override
            public void onChunk(@NonNull String fullAnswer) {
                botMsg.retrying = false; // real answer arrived
                botMsg.text = fullAnswer;
                messages.postValue(currentList());
            }

            @Override
            public void onConversationId(@NonNull String id) {
                conversationId = id;
            }

            @Override
            public void onRetry(int attempt, int max) {
                botMsg.retrying = true;
                botMsg.retryAttempt = attempt;
                botMsg.retryMax = max;
                messages.postValue(currentList());
            }

            @Override
            public void onComplete() {
                botMsg.pending = false;
                // If nothing streamed back, leave a friendly empty marker rather than a blank bubble.
                messages.postValue(currentList());
                sending.postValue(false);
            }

            @Override
            public void onError(@NonNull String message) {
                botMsg.pending = false;
                messages.postValue(currentList());
                sending.postValue(false);
                error.postValue(message);
            }
        });
    }

    /** Defensive copy so observers always get a fresh list reference. */
    private List<ChatMessage> currentList() {
        List<ChatMessage> cur = messages.getValue();
        return cur != null ? new ArrayList<>(cur) : new ArrayList<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cancel();
    }
}
