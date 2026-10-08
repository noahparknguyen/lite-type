package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Advance;

/**
 * The two ways text is typed and shown: words, which wrap to the window, and code, whose lines
 * keep the shape they were written in.
 */
enum TextKind {

    /**
     * Random words: 3 full lines, moving by word, as on Monkeytype.
     */
    WORDS(3, Advance.BY_WORD),

    /**
     * Code: 7 full lines, since code is read in blocks, moving by character, so every key fills
     * one position and a line never changes shape.
     */
    CODE(7, Advance.BY_CHARACTER);

    private final int fullLines;
    private final Advance advance;

    TextKind(int fullLines, Advance advance) {
        this.fullLines = fullLines;
        this.advance = advance;
    }

    /**
     * Returns how many lines show in full. The cursor stays within them.
     *
     * @return the number of full lines
     */
    int fullLines() {
        return fullLines;
    }

    /**
     * Returns how many lines the text takes on screen: the full lines, and the faded preview
     * line below them.
     *
     * @return the number of lines on screen
     */
    int linesShown() {
        return fullLines + 1;
    }

    /**
     * Returns how the cursor moves through this kind of text.
     *
     * @return the run's way of advancing
     */
    Advance advance() {
        return advance;
    }
}
