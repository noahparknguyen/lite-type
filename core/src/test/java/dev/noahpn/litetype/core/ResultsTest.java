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
        assertEquals(0, results.fixed());
        assertEquals(0, results.leftIn());
    }

    @Test
    void oneSlipFixedAndOneLeftIn() {
        Run run = Run.untimed(text("the quick brown fox"));
        press(run, "the qx\buick br fo");
        run.type('x', 60 * SECOND);
        Results results = Results.of(run);
        // "the ", "quick ", and "fox" earn 13 characters; "brown" earns nothing.
        assertEquals(13.0 / 5, results.wpm(), 0.001);
        assertEquals(15.0 / 17, results.accuracy(), 0.001);
        assertEquals(1, results.fixed());
        assertEquals(1, results.leftIn());
    }

    @Test
    void wordEndedByTheWrongKeyEarnsNothingAndIsLeftIn() {
        Run run = Run.untimed(text("a\nb"));
        press(run, "a ");
        run.type('b', 60 * SECOND);
        Results results = Results.of(run);
        assertEquals(1.0 / 5, results.wpm(), 0.001);
        assertEquals(1, results.leftIn());
    }

    @Test
    void timedRunCreditsTheRightPartOfTheWordInProgress() {
        Run run = Run.timed(endless(), Duration.ofSeconds(12));
        press(run, "go g");
        run.tick(12 * SECOND);
        Results results = Results.of(run);
        // "go " and the "g" in progress are 4 characters in a fifth of a minute.
        assertEquals(4.0, results.wpm(), 0.001);
        assertEquals(0, results.leftIn());
    }

    @Test
    void timedRunGivesTheWordInProgressNothingWhenItHasAMistake() {
        Run run = Run.timed(endless(), Duration.ofSeconds(12));
        press(run, "go x");
        run.tick(12 * SECOND);
        Results results = Results.of(run);
        assertEquals(3.0, results.wpm(), 0.001);
        assertEquals(1, results.leftIn());
    }

    @Test
    void fixedPlusLeftInAlwaysEqualsWrongKeys() {
        Run run = Run.untimed(text("the quick fox"));
        press(run, "tgx\b\bhe ");
        press(run, "qu \bick");
        run.deleteWord(0);
        press(run, "quiet fox");
        Results results = Results.of(run);
        // Fixed: g and x, then the early space. Left in: the e and t of "quiet".
        assertEquals(3, results.fixed());
        assertEquals(2, results.leftIn());
        assertEquals(run.wrongKeys(), results.fixed() + results.leftIn());
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
