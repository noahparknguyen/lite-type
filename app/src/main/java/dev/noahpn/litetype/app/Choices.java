package dev.noahpn.litetype.app;

import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * What the typist has picked in the bar: words or code, Timed or Length, and the length. The
 * seconds, the word count, and the snippet size are each kept, so switching modes brings back the
 * last value picked for that mode.
 *
 * @param kind    words or code
 * @param mode    whether the run ends on a deadline or at the end of its text
 * @param seconds the time limit in Timed mode, one of {@link #SECONDS}
 * @param words   the word count for words in Length mode, one of {@link #WORD_COUNTS}
 * @param size    the snippet size for code in Length mode
 */
record Choices(TextKind kind, Mode mode, int seconds, int words, SnippetSize size) {

    /**
     * Whether the run ends on a deadline or at the end of its text.
     */
    enum Mode {
        TIME, LENGTH
    }

    /**
     * How long a code snippet is, in Length mode.
     */
    enum SnippetSize {
        SHORT, MEDIUM, LONG
    }

    static final List<Integer> SECONDS = List.of(15, 30, 60, 120);
    static final List<Integer> WORD_COUNTS = List.of(10, 25, 50, 100);
    static final Choices DEFAULT =
        new Choices(TextKind.WORDS, Mode.TIME, 30, 25, SnippetSize.MEDIUM);

    // A record's fields can't change, so each of these returns a copy with one choice changed.

    Choices withKind(TextKind kind) {
        return new Choices(kind, mode, seconds, words, size);
    }

    Choices withMode(Mode mode) {
        return new Choices(kind, mode, seconds, words, size);
    }

    Choices withSeconds(int seconds) {
        return new Choices(kind, mode, seconds, words, size);
    }

    Choices withWords(int words) {
        return new Choices(kind, mode, seconds, words, size);
    }

    Choices withSize(SnippetSize size) {
        return new Choices(kind, mode, seconds, words, size);
    }

    /**
     * Returns what a run with these choices is, for the results: {@code words · 25},
     * {@code code · 30 s}, or {@code code · medium}.
     *
     * @return a short description
     */
    String describe() {
        String what = kind == TextKind.WORDS ? "words" : "code";
        String length = switch (mode) {
            case TIME -> seconds + " s";
            case LENGTH -> kind == TextKind.WORDS
                ? String.valueOf(words)
                : size.name().toLowerCase(Locale.ROOT);
        };
        return what + " · " + length;
    }

    /**
     * Reads the choices saved by {@link #save}. Anything missing or no longer valid falls back to
     * its default, so an old or hand-edited setting can't stop the app from starting.
     *
     * @param preferences where they were saved
     * @return the saved choices
     */
    static Choices load(Preferences preferences) {
        TextKind kind = parse(TextKind.class, preferences.get("kind", null), DEFAULT.kind);
        Mode mode = parse(Mode.class, preferences.get("mode", null), DEFAULT.mode);
        int seconds = preferences.getInt("seconds", DEFAULT.seconds);
        int words = preferences.getInt("words", DEFAULT.words);
        SnippetSize size = parse(SnippetSize.class, preferences.get("size", null), DEFAULT.size);

        return new Choices(
            kind,
            mode,
            SECONDS.contains(seconds) ? seconds : DEFAULT.seconds,
            WORD_COUNTS.contains(words) ? words : DEFAULT.words,
            size);
    }

    /**
     * Saves the choices, so the next launch opens on them.
     *
     * @param preferences where to save them
     */
    void save(Preferences preferences) {
        preferences.put("kind", kind.name());
        preferences.put("mode", mode.name());
        preferences.putInt("seconds", seconds);
        preferences.putInt("words", words);
        preferences.put("size", size.name());
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        if (name == null) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
