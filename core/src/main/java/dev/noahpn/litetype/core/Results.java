package dev.noahpn.litetype.core;

import java.time.Duration;
import java.util.Objects;

/**
 * What a finished run scored: its speed, its accuracy, and its mistakes as counts. Every wrong
 * keypress ends up either fixed or left in, so the two counts always add up to the wrong
 * keypresses.
 *
 * @param wpm      words per minute, counting only fully correct words, unrounded
 * @param accuracy right keypresses as a share of all keypresses, from 0 to 1
 * @param fixed    mistakes made and then undone
 * @param leftIn   mistakes still in the text at the end
 */
public record Results(double wpm, double accuracy, int fixed, int leftIn) {

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
        int leftIn = 0;

        for (int i = 0; i <= run.currentWordIndex(); i++) {
            TypedWord word = run.word(i);
            wpmCharacters += wpmCharacters(word);
            leftIn += mistakesLeftIn(word);
        }

        // A run can only finish after a counted key, so there's always at least one.
        int keys = run.correctKeys() + run.wrongKeys();
        double accuracy = (double) run.correctKeys() / keys;
        double wpm = elapsed.isZero() ? 0 : WpmCalculator.calculate(wpmCharacters, elapsed);

        return new Results(wpm, accuracy, run.wrongKeys() - leftIn, leftIn);
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

    /**
     * Returns the mistakes still showing in a word: each wrong or extra letter, and the key that
     * ended it if that was wrong. Letters skipped by an early Space or Enter aren't counted one by
     * one, because the early key is the one mistake that skipped them.
     */
    private static int mistakesLeftIn(TypedWord word) {
        int mistakes = 0;

        for (int i = 0; i < word.length(); i++) {
            Look look = word.lookAt(i);
            if (look == Look.WRONG || look == Look.EXTRA) {
                mistakes++;
            }
        }

        if (word.separatorLook() == Look.WRONG) {
            mistakes++;
        }

        return mistakes;
    }
}
