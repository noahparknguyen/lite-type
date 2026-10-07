package dev.noahpn.litetype.core;

/**
 * What a character looks like on screen right now. It says nothing about the past: a mistake
 * that was fixed looks {@link #CORRECT}.
 */
public enum Look {

    /**
     * Not reached yet.
     */
    UNTYPED,

    /**
     * Typed, and matches.
     */
    CORRECT,

    /**
     * Typed, and doesn't match.
     */
    WRONG,

    /**
     * Skipped: the typist moved on from the word before reaching it.
     */
    MISSED,

    /**
     * Typed past the end of the word.
     */
    EXTRA
}
