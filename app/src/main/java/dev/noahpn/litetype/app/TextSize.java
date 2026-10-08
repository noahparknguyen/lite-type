package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.SnippetParser;

/**
 * How big the typing text is. Fit follows the window: a full line of code takes 80% of its width,
 * so a bigger window gives bigger text. The others are fixed sizes that stay put when the window
 * changes. Either way, the text never outgrows the window: a line of code never wraps and the text
 * never runs off the bottom, so a size too big shrinks to the largest that fits. Words are always
 * larger than code by the same ratio.
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
     * How tall one line of JetBrains Mono is, as a share of the font size. Read from the font
     * file: it rises 1,020 of the font's 1,000 units above the baseline and drops 300 below.
     */
    private static final double LINE_HEIGHT = 1.32;

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
     * Returns the code font size in pixels, given the room in the window. Fit takes 80% of the
     * width, rounded to a whole pixel for crisp text, and stays between 12 and 32. A fixed size is
     * a ceiling. Either way, the size drops a pixel at a time until the text fits the width, and
     * fits the height together with everything around it, which grows with the text.
     *
     * @param width          the content area's width in pixels
     * @param height         the content area's height in pixels
     * @param surroundsShare how tall everything above and below the text is, in pixels for each
     *                       pixel of the base size from {@link #surroundsSize}
     * @return the code font size
     */
    double codeSize(double width, double height, double surroundsShare) {
        double size = this == FIT
            ? Math.clamp(Math.round(width * FIT_SHARE / lineWidth(1)), SMALLEST_FIT, LARGEST_FIT)
            : Math.min(codeSize, Math.floor(width / lineWidth(1)));

        while (size > 1 && textHeight(size) + surroundsShare * surroundsSize(size) > height) {
            size--;
        }

        return size;
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

    /**
     * Returns how tall the text's lines are at a font size. Code's lines decide for both kinds:
     * they take more height than words' (8 lines, against 4 at 1.4 times the size), and one size
     * for both means changing the mode never resizes the bar.
     */
    private static double textHeight(double fontSize) {
        return TextKind.CODE.linesShown() * LINE_HEIGHT * fontSize;
    }
}
