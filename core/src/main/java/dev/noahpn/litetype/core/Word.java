package dev.noahpn.litetype.core;

import java.util.Objects;

/**
 * One word of the text to type: its letters, what comes after it, and the indentation shown
 * before it. A word never changes once made. What the typist does to it is kept in
 * {@link TypedWord}.
 *
 * @param text      the letters to type; never empty, and never holding a space or line break
 * @param separator what the typist presses after the word
 * @param indent    the spaces shown before the word when it starts a line of code, which the
 *                  typist skips; zero everywhere else
 */
public record Word(String text, Separator separator, int indent) {

    /**
     * Checks each part of the word.
     *
     * @throws NullPointerException     if {@code text} or {@code separator} is null
     * @throws IllegalArgumentException if {@code text} is empty or holds a space or line break,
     *                                  or {@code indent} is negative
     */
    public Word {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(separator, "separator must not be null");

        if (text.isEmpty()) {
            throw new IllegalArgumentException("text must not be empty");
        }

        if (text.contains(" ") || text.contains("\n")) {
            throw new IllegalArgumentException(
                "text must not hold a space or line break: " + text);
        }

        if (indent < 0) {
            throw new IllegalArgumentException("indent must not be negative: " + indent);
        }
    }
}
