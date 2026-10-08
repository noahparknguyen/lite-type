package dev.noahpn.litetype.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextSizeTest {

    // Room in one direction too big to matter, to test the other.
    private static final double WIDE = 10_000;
    private static final double TALL = 10_000;
    // Nothing above or below the text, to test the text on its own.
    private static final double NO_SURROUNDS = 0;

    @Test
    void fixedSizesIgnoreAWindowBigEnough() {
        // 960 is a full 80-character line at 20px.
        assertEquals(20, TextSize.MEDIUM.codeSize(960, TALL, NO_SURROUNDS));
        assertEquals(20, TextSize.MEDIUM.codeSize(3000, TALL, NO_SURROUNDS));
    }

    @Test
    void fixedSizesShrinkToFitANarrowWindow() {
        // A full line at 30px needs 1,440. 744 fits 15.5px, rounded down to a whole pixel.
        assertEquals(15, TextSize.HUGE.codeSize(744, TALL, NO_SURROUNDS));
    }

    @Test
    void fixedSizesShrinkToFitAShortWindow() {
        // Code's 8 lines at 20px take 8 × 1.32 × 20 = 211.2, so 220 fits 20px but not 21.
        assertEquals(20, TextSize.HUGE.codeSize(WIDE, 220, NO_SURROUNDS));
    }

    @Test
    void theSurroundsCountAgainstTheHeight() {
        // At 18px, the text takes 190.08 and the surrounds 9 × 13 = 117, over 303 together. At
        // 17px, the text takes 179.52 and the surrounds 9 × 12 = 108, which fits.
        assertEquals(17, TextSize.HUGE.codeSize(WIDE, 303, 9));
    }

    @Test
    void fitGrowsWithTheWindow() {
        // 80% of 1,200 is 960, a full 80-character line at 20px.
        assertEquals(20, TextSize.FIT.codeSize(1200, TALL, NO_SURROUNDS));
        assertTrue(TextSize.FIT.codeSize(1600, TALL, NO_SURROUNDS)
            > TextSize.FIT.codeSize(1200, TALL, NO_SURROUNDS));
    }

    @Test
    void fitShrinksToFitAShortWindow() {
        // Code's 8 lines at 25px take 264, so 270 fits 25px but not 26.
        assertEquals(25, TextSize.FIT.codeSize(WIDE, 270, NO_SURROUNDS));
    }

    @Test
    void fitStaysBetweenItsSmallestAndLargest() {
        assertEquals(12, TextSize.FIT.codeSize(300, TALL, NO_SURROUNDS));
        assertEquals(32, TextSize.FIT.codeSize(WIDE, TALL, NO_SURROUNDS));
    }

    @Test
    void theColumnHoldsAFullLineOfCodeButNeverOverflows() {
        assertEquals(960, TextSize.columnWidth(20, 2000), 0.001);
        assertEquals(700, TextSize.columnWidth(20, 700), 0.001);
    }

    @Test
    void theSurroundsAreSevenTenthsOfTheCode() {
        assertEquals(14, TextSize.surroundsSize(20));
        assertEquals(21, TextSize.surroundsSize(30));
    }

    @Test
    void theSurroundsStayReadable() {
        // 0.7 of 12 is 8.4, and of 16 is 11.2.
        assertEquals(12, TextSize.surroundsSize(12));
        assertEquals(12, TextSize.surroundsSize(16));
    }
}
