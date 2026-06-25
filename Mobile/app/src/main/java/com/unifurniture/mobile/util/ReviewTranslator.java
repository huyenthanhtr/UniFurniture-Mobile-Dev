package com.unifurniture.mobile.util;

import androidx.annotation.NonNull;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * On-device translation for user-generated review text (Google ML Kit). Free, runs offline once
 * the small per-language model has been downloaded. Used by the review list's "Translate /
 * See Original" toggle so each review keeps its original text but can be translated on demand.
 */
public final class ReviewTranslator {

    public interface Callback {
        void onResult(String translatedText);
        void onError();
    }

    // Reuse Translator instances per source→target pair (they hold native resources).
    private static final Map<String, Translator> TRANSLATORS = new HashMap<>();

    private ReviewTranslator() {}

    /**
     * Translate {@code text} into {@code targetLang} (an app language code: en/vi/zh/fr). The source
     * language is auto-detected. If detection fails or the source already matches the target, the
     * original text is returned via onResult.
     */
    public static void translate(String text, String targetLang, @NonNull Callback callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onResult(text);
            return;
        }
        String target = toTranslateLanguage(targetLang);
        if (target == null) {
            callback.onError();
            return;
        }

        LanguageIdentifier identifier = LanguageIdentification.getClient();
        identifier.identifyLanguage(text)
                .addOnSuccessListener(langTag -> {
                    String source = "und".equals(langTag) ? null : toTranslateLanguage(langTag);
                    if (source == null || source.equals(target)) {
                        // Unknown source or already in the target language → nothing to translate.
                        callback.onResult(text);
                        return;
                    }
                    runTranslate(source, target, text, callback);
                })
                .addOnFailureListener(e -> callback.onError());
    }

    private static void runTranslate(String source, String target, String text, Callback callback) {
        String key = source + "-" + target;
        Translator translator = TRANSLATORS.get(key);
        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build();
            translator = Translation.getClient(options);
            TRANSLATORS.put(key, translator);
        }
        final Translator t = translator;
        // Download model if needed (any network), then translate.
        t.downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnSuccessListener(unused -> t.translate(text)
                        .addOnSuccessListener(callback::onResult)
                        .addOnFailureListener(e -> callback.onError()))
                .addOnFailureListener(e -> callback.onError());
    }

    /** Map an app language code (en/vi/zh/fr) to an ML Kit TranslateLanguage constant. */
    private static String toTranslateLanguage(String code) {
        if (code == null) return null;
        String c = code.toLowerCase();
        if (c.startsWith("zh")) return TranslateLanguage.CHINESE;
        if (c.startsWith("en")) return TranslateLanguage.ENGLISH;
        if (c.startsWith("vi")) return TranslateLanguage.VIETNAMESE;
        if (c.startsWith("fr")) return TranslateLanguage.FRENCH;
        return TranslateLanguage.fromLanguageTag(c); // best-effort for any other detected source
    }
}
