package dev.noahpn.litetype.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static dev.noahpn.litetype.core.Typing.*;
import static org.junit.jupiter.api.Assertions.*;

class ResultsTest {

    @Test
    void perfectRunCountsEveryWordAndTheSpaceAfterIt() {
        Run run = Run.untimed(text("ab cd"));
        press(run, "ab c");
        run.type('d', 6 * SECOND);
        Results results = Results.of(run);
        // "ab " and "cd" are 5 characters, one word, in a tenth of a minute.
        assertEquals(10.0, results.wpm(), 0.001);
        assertEquals(1.0, results.accuracy(), 0.001);
        assertEquals(1.0, results.textAccuracy(), 0.001);
    }

    @Test
    void fixedMistakeLowersAccuracyButNotTextAccuracy() {
        Run run = Run.untimed(text("the fox"));
        press(run, "tx\bhe fox");
        Results results = Results.of(run);
        assertEquals(7.0 / 8, results.accuracy(), 0.001);
        assertEquals(1.0, results.textAccuracy(), 0.001);
    }

    @Test
    void textAccuracyCountsEveryCharacterAnEarlySpaceSkipped() {
        Run run = Run.untimed(text("the quick brown fox"));
        press(run, "the qx\buick br fo");
        run.type('x', 60 * SECOND);
        Results results = Results.of(run);
        // "the ", "quick ", and "fox" earn 13 characters; "brown" earns nothing.
        assertEquals(13.0 / 5, results.wpm(), 0.001);
        assertEquals(15.0 / 17, results.accuracy(), 0.001);
        // The early space was one key, but it left "own" and the space after it wrong.
        assertEquals(15.0 / 19, results.textAccuracy(), 0.001);
    }

    @Test
    void wordEndedByTheWrongKeyEarnsNothingAndCountsAgainstTheText() {
        Run run = Run.untimed(text("a\nb"));
        press(run, "a ");
        run.type('b', 60 * SECOND);
        Results results = Results.of(run);
        assertEquals(1.0 / 5, results.wpm(), 0.001);
        assertEquals(2.0 / 3, results.textAccuracy(), 0.001);
    }

    @Test
    void extraLettersCountAgainstTheText() {
        Run run = Run.untimed(text("the fox"));
        press(run, "thee fox");
        // The extra e and the space after it are wrong; the six letters are right.
        assertEquals(6.0 / 8, Results.of(run).textAccuracy(), 0.001);
    }

    @Test
    void timedRunCreditsTheRightPartOfTheWordInProgress() {
        Run run = Run.timed(endless(), Duration.ofSeconds(12));
        press(run, "go g");
        run.tick(12 * SECOND);
        Results results = Results.of(run);
        // "go " and the "g" in progress are 4 characters in a fifth of a minute.
        assertEquals(4.0, results.wpm(), 0.001);
        // The "o" not yet typed when time ran out isn't counted against the text.
        assertEquals(1.0, results.textAccuracy(), 0.001);
    }

    @Test
    void timedRunGivesTheWordInProgressNothingWhenItHasAMistake() {
        Run run = Run.timed(endless(), Duration.ofSeconds(12));
        press(run, "go x");
        run.tick(12 * SECOND);
        Results results = Results.of(run);
        assertEquals(3.0, results.wpm(), 0.001);
        assertEquals(3.0 / 4, results.textAccuracy(), 0.001);
    }

    @Test
    void timedRunWithEverythingErasedHasNoTextAccuracy() {
        Run run = Run.timed(endless(), Duration.ofSeconds(12));
        press(run, "g\b");
        run.tick(12 * SECOND);
        assertEquals(0.0, Results.of(run).textAccuracy());
    }

    @Test
    void byCharacterAWrongLetterStaysInTheText() {
        Run run = Run.untimed(text("int x"), Advance.BY_CHARACTER);
        press(run, "inr x");
        Results results = Results.of(run);
        // The r replaced the t: one wrong key, and one wrong character of five.
        assertEquals(4.0 / 5, results.accuracy(), 0.001);
        assertEquals(4.0 / 5, results.textAccuracy(), 0.001);
    }

    @Test
    void untimedRunTakesFromTheFirstKeyToTheLast() {
        Run run = Run.untimed(text("ab cd"));
        press(run, "ab c");
        run.type('d', 6 * SECOND);
        assertEquals(Duration.ofSeconds(6), Results.of(run).elapsed());
    }

    @Test
    void timedRunTakesExactlyItsLimit() {
        Run run = Run.timed(endless(), Duration.ofSeconds(12));
        press(run, "go");
        // The first tick after the deadline ends the run, but the time stops at the deadline.
        run.tick(13 * SECOND);
        assertEquals(Duration.ofSeconds(12), Results.of(run).elapsed());
    }

    @Test
    void runThatTookNoTimeScoresZeroWpm() {
        Run run = Run.untimed(text("a"));
        press(run, "a");
        assertEquals(0.0, Results.of(run).wpm());
    }

    @Test
    void resultsOfAnUnfinishedRunThrow() {
        Run run = Run.untimed(text("the fox"));
        assertThrows(IllegalStateException.class, () -> Results.of(run));
    }
}
