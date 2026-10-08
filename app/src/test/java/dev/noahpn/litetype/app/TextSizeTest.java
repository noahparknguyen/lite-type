package dev.noahpn.litetype.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextSizeTest {

    @Test
    void fixedSizesIgnoreTheWindow() {
        assertEquals(20, TextSize.MEDIUM.codeSize(800));
        assertEquals(20, TextSize.MEDIUM.codeSize(3000));
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
}
