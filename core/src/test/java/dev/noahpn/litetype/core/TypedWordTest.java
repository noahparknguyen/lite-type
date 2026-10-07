package dev.noahpn.litetype.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedWordTest {

    private static TypedWord typed(String text, Separator separator, String letters) {
        TypedWord word = new TypedWord(new Word(text, separator, 0));
        for (char letter : letters.toCharArray()) {
            word.type(letter);
        }
        return word;
    }

    private static TypedWord typed(String text, String letters) {
        return typed(text, Separator.SPACE, letters);
    }

    @Test
    void typedLettersAreCorrectOrWrongAndTheRestUntyped() {
        TypedWord word = typed("the", "tx");
        assertEquals(2, word.typedLength());
        assertEquals(Look.CORRECT, word.lookAt(0));
        assertEquals(Look.WRONG, word.lookAt(1));
        assertEquals(Look.UNTYPED, word.lookAt(2));
    }

    @Test
    void typeReportsWhetherTheLetterMatches() {
        TypedWord word = typed("ab", "");
        assertTrue(word.type('a'));
        assertFalse(word.type('x'));
        assertFalse(word.type('b'));
    }

    @Test
    void wrongLetterStillShowsTheWordsOwnLetter() {
        TypedWord word = typed("the", "tx");
        assertEquals('h', word.charAt(1));
    }

    @Test
    void lettersPastTheEndAreExtraAndShowWhatWasTyped() {
        TypedWord word = typed("the", "thex");
        assertEquals(4, word.length());
        assertEquals(Look.EXTRA, word.lookAt(3));
        assertEquals('x', word.charAt(3));
    }

    @Test
    void fixedMistakeLooksCorrect() {
        TypedWord word = typed("the", "tx");
        word.backspace();
        word.type('h');
        assertEquals(Look.CORRECT, word.lookAt(1));
    }

    @Test
    void clearRemovesEveryLetter() {
        TypedWord word = typed("the", "thex");
        word.clear();
        assertTrue(word.isEmpty());
        assertEquals(3, word.length());
        assertEquals(Look.UNTYPED, word.lookAt(0));
    }

    @Test
    void indexOutsideTheWordThrows() {
        TypedWord word = typed("the", "");
        assertThrows(IndexOutOfBoundsException.class, () -> word.lookAt(3));
        assertThrows(IndexOutOfBoundsException.class, () -> word.charAt(-1));
    }

    @Test
    void separatorIsUntypedUntilTheWordEnds() {
        TypedWord word = typed("the", "the");
        assertEquals(Look.UNTYPED, word.separatorLook());
    }

    @Test
    void separatorAfterEveryLetterIsRight() {
        TypedWord word = typed("the", "the");
        assertTrue(word.end(' '));
        assertEquals(Look.CORRECT, word.separatorLook());
    }

    @Test
    void separatorAfterAWrongLetterIsStillRight() {
        TypedWord word = typed("the", "tha");
        assertTrue(word.end(' '));
    }

    @Test
    void earlySeparatorIsWrongAndLeavesTheRestMissed() {
        TypedWord word = typed("quick", "qu");
        assertFalse(word.end(' '));
        assertEquals(Look.WRONG, word.separatorLook());
        assertEquals(Look.MISSED, word.lookAt(2));
        assertEquals(Look.MISSED, word.lookAt(4));
    }

    @Test
    void separatorAfterExtraLettersIsWrong() {
        TypedWord word = typed("the", "thee");
        assertFalse(word.end(' '));
    }

    @Test
    void onlyTheExpectedSeparatorIsRight() {
        assertFalse(typed("{", Separator.LINE_BREAK, "{").end(' '));
        assertTrue(typed("{", Separator.LINE_BREAK, "{").end('\n'));
        assertFalse(typed("the", Separator.SPACE, "the").end('\n'));
    }

    @Test
    void reopenUndoesTheEnding() {
        TypedWord word = typed("quick", "qu");
        word.end(' ');
        word.reopen();
        assertFalse(word.isEnded());
        assertEquals(Look.UNTYPED, word.separatorLook());
        assertEquals(Look.UNTYPED, word.lookAt(2));
    }

    @Test
    void wordIsFullAtTwentyExtraLetters() {
        TypedWord word = typed("a", "a" + "x".repeat(TypedWord.MAX_EXTRA_LETTERS - 1));
        assertFalse(word.isFull());
        word.type('x');
        assertTrue(word.isFull());
        assertThrows(IllegalStateException.class, () -> word.type('x'));
    }

    @Test
    void changingAnEndedWordThrows() {
        TypedWord word = typed("the", "the");
        word.end(' ');
        assertThrows(IllegalStateException.class, () -> word.type('x'));
        assertThrows(IllegalStateException.class, word::backspace);
        assertThrows(IllegalStateException.class, () -> word.end(' '));
    }

    @Test
    void backspaceWithNothingTypedThrows() {
        TypedWord word = typed("the", "");
        assertThrows(IllegalStateException.class, word::backspace);
    }

    @Test
    void reopeningAWordThatHasNotEndedThrows() {
        TypedWord word = typed("the", "th");
        assertThrows(IllegalStateException.class, word::reopen);
    }
}
