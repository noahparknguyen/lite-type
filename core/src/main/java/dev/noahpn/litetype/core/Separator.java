package dev.noahpn.litetype.core;

/**
 * What the typist presses after a word to move on to the next one.
 */
public enum Separator {

    /**
     * A space, between words on the same line.
     */
    SPACE,

    /**
     * Enter, at the end of a line of code.
     */
    LINE_BREAK,

    /**
     * Nothing: the word is the last one in the text.
     */
    NONE;

    /**
     * Returns whether pressing {@code key} types this separator. {@link #NONE} is never typed.
     *
     * @param key the character the key made, with Enter as {@code '\n'}
     * @return whether the key types this separator
     */
    public boolean isTypedWith(char key) {
        return switch (this) {
            case SPACE -> key == ' ';
            case LINE_BREAK -> key == '\n';
            case NONE -> false;
        };
    }
}
