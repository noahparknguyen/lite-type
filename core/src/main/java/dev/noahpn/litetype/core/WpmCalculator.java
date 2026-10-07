package dev.noahpn.litetype.core;

import java.time.Duration;
import java.util.Objects;

/**
 * Calculates typing speed in words per minute. A word is five characters, spaces included, so
 * the result doesn't depend on how long the words in a text happen to be.
 */
public final class WpmCalculator {

    private static final int CHARACTERS_PER_WORD = 5;
    private static final double NANOS_PER_MINUTE = 60_000_000_000.0;

    private WpmCalculator() {
    }

    /**
     * Returns the typing speed in words per minute, unrounded.
     *
     * @param characterCount the characters that count toward WPM: those in fully correct words,
     *                       plus the space after each; must not be negative
     * @param elapsed        how long the test took; must be positive
     * @return the words per minute
     * @throws NullPointerException     if {@code elapsed} is null
     * @throws IllegalArgumentException if {@code characterCount} is negative or {@code elapsed} is
     *                                  not positive
     */
    public static double calculate(int characterCount, Duration elapsed) {
        Objects.requireNonNull(elapsed, "elapsed must not be null");

        if (characterCount < 0) {
            throw new IllegalArgumentException(
                "characterCount must not be negative: " + characterCount);
        }

        if (!elapsed.isPositive()) {
            throw new IllegalArgumentException("elapsed must be positive: " + elapsed);
        }

        double words = (double) characterCount / CHARACTERS_PER_WORD;
        double minutes = elapsed.toNanos() / NANOS_PER_MINUTE;

        return words / minutes;
    }
}
