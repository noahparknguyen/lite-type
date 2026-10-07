package dev.noahpn.litetype.core;

import java.time.Duration;
import java.util.Objects;

/**
 * What a finished run scored: its speed, and two accuracies. Accuracy counts every keypress, so
 * a mistake still costs something after it's fixed. Text accuracy looks only at the text as it
 * ended, so fixing every mistake gives 100%.
 *
 * @param wpm          words per minute, counting only fully correct words, unrounded
 * @param accuracy     right keypresses as a share of all keypresses, from 0 to 1
 * @param textAccuracy characters that ended up right, as a share of the characters reached,
 *                     from 0 to 1
 */
public record Results(double wpm, double accuracy, double textAccuracy) {

    /**
     * Works out the results of a finished run. A run that took no time at all, a single letter
     * typed, scores zero WPM rather than an infinite one.
     *
     * @param run the finished run
     * @return its results
     * @throws NullPointerException  if {@code run} is null
     * @throws IllegalStateException if the run hasn't finished
     */
    public static Results of(Run run) {
        Objects.requireNonNull(run, "run must not be null");
        Duration elapsed = run.elapsed();

        int wpmCharacters = 0;
        int rightCharacters = 0;
        int wrongCharacters = 0;

        for (int i = 0; i <= run.currentWordIndex(); i++) {
            TypedWord word = run.word(i);
            wpmCharacters += wpmCharacters(word);

            // Untyped characters weren't reached, so they count neither way. Missed ones were
            // passed over, so they count as wrong.
            for (int j = 0; j < word.length(); j++) {
                Look look = word.lookAt(j);
                if (look == Look.CORRECT) {
                    rightCharacters++;
                } else if (look != Look.UNTYPED) {
                    wrongCharacters++;
                }
            }

            Look separator = word.separatorLook();
            if (separator == Look.CORRECT) {
                rightCharacters++;
            } else if (separator == Look.WRONG) {
                wrongCharacters++;
            }
        }

        // A run can only finish after a counted key, so there's always at least one.
        int keys = run.correctKeys() + run.wrongKeys();
        double accuracy = (double) run.correctKeys() / keys;

        // Every character typed can be erased before a timed run ends, leaving none.
        int characters = rightCharacters + wrongCharacters;
        double textAccuracy = characters == 0 ? 0 : (double) rightCharacters / characters;

        double wpm = elapsed.isZero() ? 0 : WpmCalculator.calculate(wpmCharacters, elapsed);

        return new Results(wpm, accuracy, textAccuracy);
    }

    /**
     * Returns the characters a word earns toward WPM. A word the typist moved on from earns its
     * letters and the key after it, but only when all of them are right. The word still open at
     * the end earns the letters typed so far, if they're all right: the whole word when an untimed
     * run ends on it, or the part typed before a timed run's deadline.
     */
    private static int wpmCharacters(TypedWord word) {
        int typed = word.typedLength();

        for (int i = 0; i < typed; i++) {
            if (word.lookAt(i) != Look.CORRECT) {
                return 0;
            }
        }

        if (!word.isEnded()) {
            return typed;
        }

        return word.separatorLook() == Look.CORRECT ? typed + 1 : 0;
    }
}
