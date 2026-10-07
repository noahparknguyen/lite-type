package dev.noahpn.litetype.core;

/**
 * How the cursor moves through the text as keys are pressed.
 */
public enum Advance {

    /**
     * By word, as on Monkeytype. Space or Enter ends the current word wherever the cursor is,
     * skipping any letters left, and letters typed past a word's end are added after it. One
     * slip stays one mistake. Used for words.
     */
    BY_WORD,

    /**
     * By character. Every key fills exactly the next position, right or wrong, and the cursor
     * moves one: Space or Enter inside a word is just a wrong letter, and any key at a word's end
     * ends it. A line never changes shape. Used for code.
     */
    BY_CHARACTER
}
