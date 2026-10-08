package dev.noahpn.litetype.app;

import dev.noahpn.litetype.app.Choices.Mode;
import dev.noahpn.litetype.core.Run;
import dev.noahpn.litetype.core.Separator;

/**
 * How far one run has gone, as the progress above the text shows it: seconds left in Timed mode,
 * or words or lines done in Length mode. Plain Java, with no JavaFX, so it's tested on its own.
 *
 * <p>A text that ends is counted once, when the run starts, so asking costs nothing per key.
 */
final class RunProgress {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final Run run;
    private final Choices choices;
    // In Length mode: how many words or lines the text has, and for code, the line each word is
    // on. Unused in Timed mode, whose text never ends.
    private final int total;
    private final int[] lineOfWord;

    /**
     * Starts following a run, counting its text if it ends.
     *
     * @param run     the run, before its first key
     * @param choices the choices it was made with
     */
    RunProgress(Run run, Choices choices) {
        this.run = run;
        this.choices = choices;

        if (choices.mode() == Mode.TIME) {
            total = 0;
            lineOfWord = new int[0];
            return;
        }

        int count = 0;
        while (run.hasWord(count)) {
            count++;
        }

        lineOfWord = new int[count];
        int line = 0;

        for (int i = 0; i < count; i++) {
            lineOfWord[i] = line;
            if (run.word(i).word().separator() == Separator.LINE_BREAK) {
                line++;
            }
        }

        total = choices.kind() == TextKind.WORDS ? count : line + 1;
    }

    /**
     * Returns the number to show: whole seconds left in Timed mode, rounded up so it reads 0 only
     * when time is up, or the words or lines done in Length mode.
     *
     * @return the number
     */
    int value() {
        if (choices.mode() == Mode.TIME) {
            long nanosLeft = run.timeLeft().orElseThrow().toNanos();
            return (int) ((nanosLeft + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
        }

        int current = run.currentWordIndex();
        return choices.kind() == TextKind.WORDS ? current : lineOfWord[current];
    }

    /**
     * Returns the number as text: the seconds alone in Timed mode, such as {@code 29}, and done
     * out of the total in Length mode, such as {@code 12 / 25}.
     *
     * @return the text to show
     */
    String text() {
        int value = value();
        return choices.mode() == Mode.TIME ? String.valueOf(value) : value + " / " + total;
    }

    /**
     * Returns how much of the line to fill, from 0 to 1: the time used in Timed mode, to the
     * nanosecond so it fills smoothly, or the share of words or lines done in Length mode.
     *
     * @return the share done
     */
    double share() {
        if (choices.mode() == Mode.TIME) {
            double limitNanos = choices.seconds() * (double) NANOS_PER_SECOND;
            return 1 - run.timeLeft().orElseThrow().toNanos() / limitNanos;
        }

        return (double) value() / total;
    }
}
