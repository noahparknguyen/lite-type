package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.SnippetParser;

/**
 * How big the typing text is. Fit follows the window: the column takes a share of the window's
 * width, and the code font is whatever fits a full line of code across it, so a bigger window
 * gives bigger text. The others are fixed sizes that stay put when the window changes, unless it's
 * too narrow for a full line of code at that size: a line of code never wraps, so they shrink to
 * the largest size that fits. Words are always larger than code by the same ratio.
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

    /**
     * The base size of the bar, the counter, the results, and the pages, as a share of the code
     * font, so they grow with the text: 14px when code is 20px.
     */
    private static final double SURROUNDS_SCALE = 0.7;

    /**
     * The smallest the bar and the rest get, since 0.7 of a small code font is too small to read.
     */
    private static final double SMALLEST_SURROUNDS = 12;

    private static final double FIT_SHARE = 0.8;
    private static final double SMALLEST_FIT = 12;
    private static final double LARGEST_FIT = 32;

    private final double codeSize;

    TextSize(double codeSize) {
        this.codeSize = codeSize;
    }

    /**
     * Returns the code font size in pixels, given how wide the window's content area is. Fit
     * rounds to a whole pixel, for crisp text, and stays between 12 and 32. A fixed size is a
     * ceiling: in a window too narrow for a full line at that size, it drops to the largest whole
     * pixel that fits.
     *
     * @param available the content area's width in pixels
     * @return the code font size
     */
    double codeSize(double available) {
        if (this != FIT) {
            return Math.min(codeSize, Math.floor(available / lineWidth(1)));
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
     * Returns the base font size for the bar, the counter, the results, and the pages: 0.7 of the
     * code font, rounded to a whole pixel, and never below 12. The bar is about three quarters as
     * wide as a line of code, and a line of code always fits the window, so the bar does too.
     *
     * @param codeSize the code font size in pixels
     * @return the base font size in pixels
     */
    static double surroundsSize(double codeSize) {
        return Math.max(Math.round(codeSize * SURROUNDS_SCALE), SMALLEST_SURROUNDS);
    }

    /**
     * Returns how wide the longest line a snippet may have is, at a font size.
     */
    private static double lineWidth(double fontSize) {
        return SnippetParser.MAX_LINE_LENGTH * CHARACTER_WIDTH * fontSize;
    }
}
