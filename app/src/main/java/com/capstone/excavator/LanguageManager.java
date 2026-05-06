package com.capstone.excavator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Central authority for the app's language preference.
 *
 * Reads / writes the same SharedPreferences key that SettingsActivity uses,
 * so both the first-run picker and the Settings page stay in sync
 * automatically.
 *
 * Supported language codes:
 *   "zh-Hans"  – Simplified Chinese  (default)
 *   "zh-Hant"  – Traditional Chinese
 *   "en"       – English
 */
public class LanguageManager {

    static final String PREFS_NAME = "general_ui_prefs";
    static final String KEY_LANGUAGE = "language";
    static final String KEY_LANGUAGE_CHOSEN = "language_chosen";

    static final String LANG_ZH_HANS = "zh-Hans";
    static final String LANG_ZH_HANT = "zh-Hant";
    static final String LANG_EN      = "en";

    static final String DEFAULT_LANGUAGE = LANG_ZH_HANS;

    private LanguageManager() {}

    // ── SharedPreferences helpers ────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns the stored language code, or {@link #DEFAULT_LANGUAGE} when
     * the user has never explicitly picked one.
     */
    public static String getLanguage(Context ctx) {
        return prefs(ctx).getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    /**
     * Persists the selected language code.
     * Call this from the first-run picker AND from SettingsActivity so both
     * paths write to the same slot.
     */
    public static void setLanguage(Context ctx, String langCode) {
        prefs(ctx).edit()
                .putString(KEY_LANGUAGE, langCode)
                .putBoolean(KEY_LANGUAGE_CHOSEN, true)
                .apply();
    }

    /**
     * Returns {@code true} after the user has explicitly picked a language
     * at least once (either via the first-run dialog or Settings).
     */
    public static boolean isLanguageChosen(Context ctx) {
        return prefs(ctx).getBoolean(KEY_LANGUAGE_CHOSEN, false);
    }
}
