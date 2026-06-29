package com.unifurniture.mobile.util;

import android.content.Context;

import com.unifurniture.mobile.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client-side fallback localizer for product color / variant strings that do NOT (yet) carry a
 * {@code color_i18n} object from the backend — e.g. legacy variants or order snapshots that store a
 * plain string.
 *
 * <p>It localizes every recognized color <em>word</em> in place using the {@code R.string.color_*}
 * resources and the current app locale, preserves descriptive tokens (sizes like {@code 1m6},
 * product types like {@code Giường}/{@code Sofa}) untouched, and drops the semantically-empty filler
 * words {@code màu}/{@code phối}. {@link FormatUtil#stripDiacritics(String)} is used ONLY to build
 * the lookup key — never to produce displayed text, so "Trắng"/"trắng"/"Trang" all resolve to the
 * same color while the output is always a clean localized string.
 *
 * <p>Examples (en / vi): {@code Màu nâu}/{@code Nau}/{@code Nâu} → "Brown"/"Nâu";
 * {@code Trắng - Xám} → "White - Gray"; {@code Giường Màu Trắng 1m6} → "Giường White 1m6".
 */
public final class ColorUi {

    private ColorUi() {}

    /** Multi-word color phrases, matched before single words. Keys are diacritics-stripped lowercase. */
    private static final Map<String, Integer> PHRASES = new LinkedHashMap<>();
    /** Single color words. Keys are diacritics-stripped lowercase. */
    private static final Map<String, Integer> WORDS = new LinkedHashMap<>();
    /** Filler words with no product meaning — dropped from the output. */
    private static final Set<String> FILLER = new HashSet<>(Arrays.asList("mau", "phoi"));

    static {
        PHRASES.put("tu nhien", R.string.color_natural);
        PHRASES.put("xanh duong", R.string.color_blue);
        PHRASES.put("xanh la", R.string.color_green);

        WORDS.put("trang", R.string.color_white);
        WORDS.put("den", R.string.color_black);
        WORDS.put("xam", R.string.color_gray);
        WORDS.put("ghi", R.string.color_gray);
        WORDS.put("nau", R.string.color_brown);
        WORDS.put("be", R.string.color_beige);
        WORDS.put("beige", R.string.color_beige);
        WORDS.put("kem", R.string.color_cream);
        WORDS.put("cam", R.string.color_orange);
        WORDS.put("vang", R.string.color_yellow);
        WORDS.put("xanh", R.string.color_blue);
        WORDS.put("do", R.string.color_red);
        WORDS.put("hong", R.string.color_pink);
        WORDS.put("tim", R.string.color_purple);
        WORDS.put("camel", R.string.color_camel);
        WORDS.put("olive", R.string.color_olive);
    }

    private static String key(String token) {
        return FormatUtil.stripDiacritics(token).toLowerCase().trim();
    }

    /**
     * Returns {@code raw} with recognized color words localized to the current app language.
     * Unrecognized / descriptive tokens are preserved in place; {@code null}/blank returns "".
     */
    public static String label(Context ctx, String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return "";

        // Normalize the compound delimiters ('/', '-') to a standalone " - " token, then split on
        // whitespace so every word/size token (incl. "1m6") and separator becomes its own token.
        String normalizedDelims = trimmed
                .replaceAll("\\s*/\\s*", " - ")
                .replaceAll("\\s*-\\s*", " - ");
        String[] tokens = normalizedDelims.trim().split("\\s+");

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String tok = tokens[i];
            if ("-".equals(tok)) {
                append(out, "-");
                continue;
            }
            String k = key(tok);
            if (FILLER.contains(k)) {
                continue; // drop "màu"/"phối"
            }
            // Two-word phrase lookahead (only across adjacent word tokens, not delimiters).
            if (i + 1 < tokens.length && !"-".equals(tokens[i + 1])) {
                Integer phraseRes = PHRASES.get(k + " " + key(tokens[i + 1]));
                if (phraseRes != null) {
                    append(out, ctx.getString(phraseRes));
                    i++;
                    continue;
                }
            }
            Integer wordRes = WORDS.get(k);
            append(out, wordRes != null ? ctx.getString(wordRes) : tok);
        }

        // Tidy up: drop dangling separators left by removed fillers and collapse duplicate spaces.
        return out.toString()
                .replaceAll("(?:\\s*-\\s*)+", " - ")
                .replaceAll("^\\s*-\\s*", "")
                .replaceAll("\\s*-\\s*$", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static void append(StringBuilder out, String piece) {
        if (out.length() > 0) out.append(' ');
        out.append(piece);
    }
}
