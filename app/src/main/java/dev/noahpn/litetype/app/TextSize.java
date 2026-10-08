package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.SnippetParser;

/**
 * How big the typing text is. Fit follows the window: the column takes a share of the window's
 * width, and the code font is whatever fits a full line of code across it, so a bigger window
 * gives bigger text. The others are fixed sizes that stay put when the window changes. Words are
 * always larger than code by the same ratio.
 */
enum TextSize {

    FIT(0),
    SMALL(16),
    MEDIUM(20),
    LARGE(24),
    HUGE(30);

    /**
     * How wide one character of JetBrains Mono is, as a share of the font size. Read from the
     * font file: every character's advance is 600 of the font's 1,000 units.
     */
    static final double CHARACTER_WIDTH = 0.6;

    /**
     * How much larger words are than code, since they're short and only fill 3 lines.
     */
    static final double WORDS_SCALE = 1.4;

    private static final double FIT_SHARE = 0.8;
    private static final double SMALLEST_FIT = 12;
    private static final double LARGEST_FIT = 32;

    private final double codeSize;

    TextSize(double codeSize) {
        this.codeSize = codeSize;
    }

    /**
     * Returns the code font size in pixels, given how wide the window's content area is. Fit
     * rounds to a whole pixel, for crisp text, and stays between 12 and 32.
     *
     * @param available the content area's width in pixels
     * @return the code font size
     */
    double codeSize(double available) {
        if (this != FIT) {
            return codeSize;
        }

        double fitted = available * FIT_SHARE / lineWidth(1);
        return Math.clamp(Math.round(fitted), SMALLEST_FIT, LARGEST_FIT);
    }

    /**
     * Returns how wide the column is: a full line of code at {@code codeSize}, but never wider
     * than the content area.
     *
     * @param codeSize  the code font size in pixels
     * @param available the content area's width in pixels
     * @return the column's width in pixels
     */
    static double columnWidth(double codeSize, double available) {
        return Math.min(lineWidth(codeSize), available);
    }

    /**
     * Returns how wide the longest line a snippet may have is, at a font size.
     */
    private static double lineWidth(double fontSize) {
        return SnippetParser.MAX_LINE_LENGTH * CHARACTER_WIDTH * fontSize;
    }
}
