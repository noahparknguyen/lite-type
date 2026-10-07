package dev.noahpn.litetype.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WordTest {

    @Test
    void nullTextThrows() {
        assertThrows(NullPointerException.class,
            () -> new Word(null, Separator.SPACE, 0));
    }

    @Test
    void nullSeparatorThrows() {
        assertThrows(NullPointerException.class,
            () -> new Word("the", null, 0));
    }

    @Test
    void emptyTextThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Word("", Separator.SPACE, 0));
    }

    @Test
    void textWithASpaceThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Word("the quick", Separator.SPACE, 0));
    }

    @Test
    void textWithALineBreakThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Word("{\n", Separator.SPACE, 0));
    }

    @Test
    void negativeIndentThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Word("return", Separator.SPACE, -1));
    }
}
