package dev.noahpn.litetype.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunTest {

    private static final long SECOND = 1_000_000_000L;

    /**
     * Splits text into words the way content will: a space or line break ends each word, and the
     * last word has nothing after it.
     */
    private static Iterator<Word> text(String text) {
        List<Word> words = new ArrayList<>();
        StringBuilder letters = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c == ' ' || c == '\n') {
                Separator separator = c == ' ' ? Separator.SPACE : Separator.LINE_BREAK;
                words.add(new Word(letters.toString(), separator, 0));
                letters.setLength(0);
            } else {
                letters.append(c);
            }
        }

        words.add(new Word(letters.toString(), Separator.NONE, 0));
        return words.iterator();
    }

    private static Iterator<Word> endless() {
        return Stream.generate(() -> new Word("go", Separator.SPACE, 0)).iterator();
    }

    /**
     * Presses each key at time zero, reading {@code '\b'} as Backspace.
     */
    private static void press(Run run, String keys) {
        for (char key : keys.toCharArray()) {
            if (key == '\b') {
                run.backspace(0);
            } else {
                run.type(key, 0);
            }
        }
    }

    @Test
    void lettersGoIntoTheCurrentWord() {
        Run run = Run.untimed(text("the fox"));
        press(run, "th");
        assertEquals(0, run.currentWordIndex());
        assertEquals(2, run.word(0).typedLength());
    }

    @Test
    void spaceAtTheEndOfAWordMovesOnAndIsRight() {
        Run run = Run.untimed(text("the fox"));
        press(run, "the ");
        assertEquals(1, run.currentWordIndex());
        assertEquals(Look.CORRECT, run.word(0).separatorLook());
        assertEquals(4, run.correctKeys());
        assertEquals(0, run.wrongKeys());
    }

    @Test
    void earlySpaceSkipsTheRestOfTheWordAsOneWrongKey() {
        Run run = Run.untimed(text("quick fox"));
        press(run, "qu ");
        assertEquals(1, run.currentWordIndex());
        assertEquals(Look.MISSED, run.word(0).lookAt(2));
        assertEquals(1, run.wrongKeys());
    }

    @Test
    void spaceOrEnterOnAnEmptyWordIsIgnored() {
        Run run = Run.untimed(text("a b\nc"));
        press(run, " a  b\n\n");
        assertEquals(2, run.currentWordIndex());
        assertEquals(4, run.correctKeys());
        assertEquals(0, run.wrongKeys());
    }

    @Test
    void enterInTheMiddleOfALineMovesOneWordAsAWrongKey() {
        Run run = Run.untimed(text("int x\n}"));
        press(run, "int\n");
        assertEquals(1, run.currentWordIndex());
        assertEquals(Look.WRONG, run.word(0).separatorLook());
        assertEquals(1, run.wrongKeys());
    }

    @Test
    void spaceAtTheEndOfALineMovesToTheNextLineAsAWrongKey() {
        Run run = Run.untimed(text("x\n}"));
        press(run, "x ");
        assertEquals(1, run.currentWordIndex());
        assertEquals(1, run.wrongKeys());
    }

    @Test
    void tabIsIgnored() {
        Run run = Run.untimed(text("the fox"));
        press(run, "\t");
        assertEquals(0, run.word(0).typedLength());
        assertEquals(0, run.wrongKeys());
    }

    @Test
    void backspaceRemovesALetterAndIsNeverCounted() {
        Run run = Run.untimed(text("the fox"));
        press(run, "tx\bh");
        assertEquals(Look.CORRECT, run.word(0).lookAt(1));
        assertEquals(2, run.correctKeys());
        assertEquals(1, run.wrongKeys());
    }

    @Test
    void backspaceOnAnEmptyWordStepsBackIntoThePreviousOne() {
        Run run = Run.untimed(text("quick fox"));
        press(run, "qu \b");
        assertEquals(0, run.currentWordIndex());
        assertEquals(Look.UNTYPED, run.word(0).lookAt(2));
        assertEquals(Look.UNTYPED, run.word(0).separatorLook());
    }

    @Test
    void backspaceAtTheVeryStartDoesNothing() {
        Run run = Run.untimed(text("the fox"));
        press(run, "\b");
        assertEquals(0, run.currentWordIndex());
    }

    @Test
    void deleteWordClearsTheCurrentWord() {
        Run run = Run.untimed(text("the fox"));
        press(run, "the f");
        run.deleteWord(0);
        assertEquals(1, run.currentWordIndex());
        assertEquals(0, run.word(1).typedLength());
    }

    @Test
    void deleteWordOnAnEmptyWordClearsThePreviousOne() {
        Run run = Run.untimed(text("the fox"));
        press(run, "the ");
        run.deleteWord(0);
        assertEquals(0, run.currentWordIndex());
        assertEquals(0, run.word(0).typedLength());
        assertEquals(Look.UNTYPED, run.word(0).separatorLook());
    }

    @Test
    void lettersPastTheCapAreIgnoredAndNotCounted() {
        Run run = Run.untimed(text("a b"));
        press(run, "a" + "x".repeat(TypedWord.MAX_EXTRA_LETTERS + 5));
        assertEquals(1 + TypedWord.MAX_EXTRA_LETTERS, run.word(0).typedLength());
        assertEquals(TypedWord.MAX_EXTRA_LETTERS, run.wrongKeys());
    }

    @Test
    void untimedRunEndsOnTheLastLetterEvenWhenWrong() {
        Run run = Run.untimed(text("the fox"));
        press(run, "the fo");
        assertFalse(run.isFinished());
        press(run, "y");
        assertTrue(run.isFinished());
    }

    @Test
    void spaceInsideTheLastWordIsAnOrdinaryLetter() {
        Run run = Run.untimed(text("the fox"));
        press(run, "the f x");
        assertEquals(1, run.currentWordIndex());
        assertEquals(Look.WRONG, run.word(1).lookAt(1));
        assertTrue(run.isFinished());
    }

    @Test
    void keysAfterTheEndAreIgnored() {
        Run run = Run.untimed(text("ab"));
        press(run, "ab");
        press(run, "c\b");
        assertEquals(2, run.correctKeys());
        assertEquals(2, run.word(0).typedLength());
    }

    @Test
    void untimedRunTakesTheTimeFromTheFirstKeyToTheLast() {
        Run run = Run.untimed(text("ab"));
        run.type('a', 1 * SECOND);
        run.type('b', 3 * SECOND);
        assertEquals(Duration.ofSeconds(2), run.elapsed());
    }

    @Test
    void ignoredKeysDoNotStartTheClock() {
        Run run = Run.timed(endless(), Duration.ofSeconds(30));
        run.type(' ', 0);
        run.backspace(0);
        run.tick(40 * SECOND);
        assertFalse(run.isFinished());
    }

    @Test
    void timedRunEndsExactlyOnItsDeadline() {
        Run run = Run.timed(endless(), Duration.ofSeconds(30));
        run.type('g', 5 * SECOND);
        run.tick(34 * SECOND);
        assertFalse(run.isFinished());
        run.tick(36 * SECOND);
        assertTrue(run.isFinished());
        assertEquals(Duration.ofSeconds(30), run.elapsed());
    }

    @Test
    void keyAtTheDeadlineEndsTheRunAndIsNotCounted() {
        Run run = Run.timed(endless(), Duration.ofSeconds(30));
        run.type('g', 0);
        run.type('o', 30 * SECOND);
        assertTrue(run.isFinished());
        assertEquals(1, run.correctKeys());
        assertEquals(1, run.word(0).typedLength());
    }

    @Test
    void timeLeftCountsDownFromTheFirstKey() {
        Run run = Run.timed(endless(), Duration.ofSeconds(30));
        assertEquals(Optional.of(Duration.ofSeconds(30)), run.timeLeft());
        run.type('g', 2 * SECOND);
        run.tick(12 * SECOND);
        assertEquals(Optional.of(Duration.ofSeconds(20)), run.timeLeft());
        run.tick(40 * SECOND);
        assertEquals(Optional.of(Duration.ZERO), run.timeLeft());
    }

    @Test
    void untimedRunHasNoTimeLeft() {
        Run run = Run.untimed(text("the fox"));
        assertEquals(Optional.empty(), run.timeLeft());
    }

    @Test
    void elapsedBeforeTheEndThrows() {
        Run run = Run.untimed(text("the fox"));
        assertThrows(IllegalStateException.class, run::elapsed);
    }

    @Test
    void wordsAreTakenFromTheTextAsNeeded() {
        Run finite = Run.untimed(text("the fox"));
        assertTrue(finite.hasWord(1));
        assertFalse(finite.hasWord(2));
        assertFalse(finite.hasWord(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> finite.word(2));

        Run endless = Run.timed(endless(), Duration.ofSeconds(30));
        assertTrue(endless.hasWord(1_000));
    }

    @Test
    void textWithNoWordsThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> Run.untimed(Collections.emptyIterator()));
    }

    @Test
    void limitThatIsNotPositiveThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> Run.timed(endless(), Duration.ZERO));
    }
}
