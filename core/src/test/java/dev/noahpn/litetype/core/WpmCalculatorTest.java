package dev.noahpn.litetype.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WpmCalculatorTest {

    @Test
    void wpmCountsFiveCharactersAsAWord() {
        double wpm = WpmCalculator.calculate(50, Duration.ofSeconds(30));
        assertEquals(20.0, wpm, 0.001);
    }

    @Test
    void noCharactersGiveZeroWpm() {
        double wpm = WpmCalculator.calculate(0, Duration.ofSeconds(30));
        assertEquals(0.0, wpm, 0.001);
    }

    @Test
    void unevenResultKeepsItsFraction() {
        double wpm = WpmCalculator.calculate(50, Duration.ofSeconds(90));
        assertEquals(20.0 / 3.0, wpm, 0.001);
    }

    @Test
    void elapsedCountsItsFractionOfASecond() {
        double wpm = WpmCalculator.calculate(50, Duration.ofMillis(500));
        assertEquals(1200.0, wpm, 0.001);
    }

    @Test
    void nullElapsedThrows() {
        assertThrows(NullPointerException.class,
            () -> WpmCalculator.calculate(50, null));
    }

    @Test
    void negativeCharacterCountThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> WpmCalculator.calculate(-1, Duration.ofSeconds(30)));
    }

    @Test
    void zeroElapsedThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> WpmCalculator.calculate(50, Duration.ZERO));
    }

    @Test
    void negativeElapsedThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> WpmCalculator.calculate(50, Duration.ofSeconds(-1)));
    }
}
