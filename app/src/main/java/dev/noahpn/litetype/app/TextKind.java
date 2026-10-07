package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Advance;

/**
 * The two ways text is typed and shown: words, which wrap to the window, and code, whose lines
 * keep the shape they were written in.
 */
enum TextKind {

    /**
     * Random words: 3 lines, moving by word, as on Monkeytype.
     */
    WORDS(3, Advance.BY_WORD, "words"),

    /**
     * Code: 7 lines, since code is read in blocks, moving by character, so every key fills one
     * position and a line never changes shape.
     */
    CODE(7, Advance.BY_CHARACTER, "code");

    private final int visibleLines;
    private final Advance advance;
    private final String styleClass;

    TextKind(int visibleLines, Advance advance, String styleClass) {
        this.visibleLines = visibleLines;
        this.advance = advance;
        this.styleClass = styleClass;
    }

    /**
     * Returns how many lines of text show at once.
     *
     * @return the number of lines in the window
     */
    int visibleLines() {
        return visibleLines;
    }

    /**
     * Returns how the cursor moves through this kind of text.
     *
     * @return the run's way of advancing
     */
    Advance advance() {
        return advance;
    }

    /**
     * Returns the style class the stylesheet sizes this kind of text by.
     *
     * @return the style class
     */
    String styleClass() {
        return styleClass;
    }
}
