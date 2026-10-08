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

        /**
         * Timed: the run ends on its deadline, and the text never runs out.
         */
        TIME,

        /**
         * Length: the run ends at the end of its text.
         */
        LENGTH
    }

    /**
     * How long a code snippet is, in Length mode.
     */
    enum SnippetSize {

        /**
         * 4 to 8 lines.
         */
        SHORT,

        /**
         * 10 to 18 lines.
         */
        MEDIUM,

        /**
         * 20 to 35 lines.
         */
        LONG
    }

    /**
     * The time limits offered in Timed mode, in seconds.
     */
    static final List<Integer> SECONDS = List.of(15, 30, 60, 120);

    /**
     * The word counts offered for words in Length mode.
     */
    static final List<Integer> WORD_COUNTS = List.of(10, 25, 50, 100);

    /**
     * The choices on first launch: words, for 30 seconds.
     */
    static final Choices DEFAULT =
        new Choices(TextKind.WORDS, Mode.TIME, 30, 25, SnippetSize.MEDIUM);

    /**
     * Returns these choices with words or code changed. A record's fields can't change, so each
     * {@code with} method returns a copy with one choice changed.
     *
     * @param kind words or code
     * @return the new choices
     */
    Choices withKind(TextKind kind) {
        return new Choices(kind, mode, seconds, words, size);
    }

    /**
     * Returns these choices with the mode changed.
     *
     * @param mode Timed or Length
     * @return the new choices
     */
    Choices withMode(Mode mode) {
        return new Choices(kind, mode, seconds, words, size);
    }

    /**
     * Returns these choices with the time limit changed.
     *
     * @param seconds the time limit, one of {@link #SECONDS}
     * @return the new choices
     */
    Choices withSeconds(int seconds) {
        return new Choices(kind, mode, seconds, words, size);
    }

    /**
     * Returns these choices with the word count changed.
     *
     * @param words the word count, one of {@link #WORD_COUNTS}
     * @return the new choices
     */
    Choices withWords(int words) {
        return new Choices(kind, mode, seconds, words, size);
    }

    /**
     * Returns these choices with the snippet size changed.
     *
     * @param size the snippet size
     * @return the new choices
     */
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
        TextKind kind = Saved.read(preferences, "kind", DEFAULT.kind);
        Mode mode = Saved.read(preferences, "mode", DEFAULT.mode);
        int seconds = preferences.getInt("seconds", DEFAULT.seconds);
        int words = preferences.getInt("words", DEFAULT.words);
        SnippetSize size = Saved.read(preferences, "size", DEFAULT.size);

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
}
