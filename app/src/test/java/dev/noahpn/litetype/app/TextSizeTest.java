package dev.noahpn.litetype.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextSizeTest {

    @Test
    void fixedSizesIgnoreAWindowWideEnough() {
        // 960 is a full 80-character line at 20px.
        assertEquals(20, TextSize.MEDIUM.codeSize(960));
        assertEquals(20, TextSize.MEDIUM.codeSize(3000));
    }

    @Test
    void fixedSizesShrinkToFitANarrowWindow() {
        // A full line at 30px needs 1,440. 744 fits 15.5px, rounded down to a whole pixel.
        assertEquals(15, TextSize.HUGE.codeSize(744));
    }

    @Test
    void fitGrowsWithTheWindow() {
        // 80% of 1,200 is 960, a full 80-character line at 20px.
        assertEquals(20, TextSize.FIT.codeSize(1200));
        assertTrue(TextSize.FIT.codeSize(1600) > TextSize.FIT.codeSize(1200));
    }

    @Test
    void fitStaysBetweenItsSmallestAndLargest() {
        assertEquals(12, TextSize.FIT.codeSize(300));
        assertEquals(32, TextSize.FIT.codeSize(10_000));
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
