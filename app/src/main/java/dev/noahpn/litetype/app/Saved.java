package dev.noahpn.litetype.app;

import java.util.prefs.Preferences;

/**
 * Reads what the app saved between launches. A value that's missing, or that names something the
 * app no longer has, gives the default instead, so an old or hand-edited setting can't stop the
 * app from starting.
 */
final class Saved {

    private Saved() {
    }

    /**
     * Returns a saved setting that's one of an enum's values, such as a text size.
     *
     * @param preferences where it was saved
     * @param key         its name
     * @param fallback    the default, returned when nothing usable was saved
     * @param <E>         the enum the setting is one of
     * @return the saved value, or the default
     */
    static <E extends Enum<E>> E read(Preferences preferences, String key, E fallback) {
        try {
            String saved = preferences.get(key, fallback.name());
            return Enum.valueOf(fallback.getDeclaringClass(), saved);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
