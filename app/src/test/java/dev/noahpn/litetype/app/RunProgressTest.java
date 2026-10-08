package dev.noahpn.litetype.app;

import dev.noahpn.litetype.app.Choices.Mode;
import dev.noahpn.litetype.app.Choices.SnippetSize;
import dev.noahpn.litetype.core.Advance;
import dev.noahpn.litetype.core.Run;
import dev.noahpn.litetype.core.Separator;
import dev.noahpn.litetype.core.Word;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RunProgressTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    void timedRunShowsTheFullTimeBeforeTheFirstKey() {
        Run run = Run.timed(words("go", "go"), Duration.ofSeconds(30));
        RunProgress progress = new RunProgress(run, timed(30));
        assertEquals("30", progress.text());
        assertEquals(0, progress.share(), 1e-9);
    }

    @Test
    void timedRunRoundsTheSecondsLeftUp() {
        Run run = Run.timed(words("go", "go"), Duration.ofSeconds(30));
        RunProgress progress = new RunProgress(run, timed(30));
        run.type('g', 0);
        // 10.5 seconds in, 19.5 are left, shown as 20, and the line is 35% full.
        run.tick(10 * SECOND + SECOND / 2);
        assertEquals("20", progress.text());
        assertEquals(0.35, progress.share(), 1e-9);
        // It reads 0 only when time is up.
        run.tick(29 * SECOND + SECOND / 2);
        assertEquals(1, progress.value());
        run.tick(30 * SECOND);
        assertEquals(0, progress.value());
    }

    @Test
    void wordsCountTheWordsDone() {
        Run run = Run.untimed(words("ab", "cd", "ef", "gh"));
        RunProgress progress = new RunProgress(run, length(TextKind.WORDS));
        assertEquals("0 / 4", progress.text());
        type(run, "ab cd ");
        assertEquals("2 / 4", progress.text());
        assertEquals(0.5, progress.share(), 1e-9);
    }

    @Test
    void codeCountsTheLinesDone() {
        // Three lines: "a b", "c", and "d".
        List<Word> code = List.of(
            new Word("a", Separator.SPACE, 0),
            new Word("b", Separator.LINE_BREAK, 0),
            new Word("c", Separator.LINE_BREAK, 0),
            new Word("d", Separator.NONE, 0));
        Run run = Run.untimed(code.iterator(), Advance.BY_CHARACTER);
        RunProgress progress = new RunProgress(run, length(TextKind.CODE));
        assertEquals("0 / 3", progress.text());
        type(run, "a b\n");
        assertEquals("1 / 3", progress.text());
        assertEquals(1.0 / 3, progress.share(), 1e-9);
    }

    private static Iterator<Word> words(String... texts) {
        List<Word> words = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            Separator after = i == texts.length - 1 ? Separator.NONE : Separator.SPACE;
            words.add(new Word(texts[i], after, 0));
        }
        return words.iterator();
    }

    private static void type(Run run, String keys) {
        for (char key : keys.toCharArray()) {
            run.type(key, 0);
        }
    }

    private static Choices timed(int seconds) {
        return new Choices(TextKind.WORDS, Mode.TIME, seconds, 25, SnippetSize.MEDIUM);
    }

    private static Choices length(TextKind kind) {
        return new Choices(kind, Mode.LENGTH, 30, 25, SnippetSize.MEDIUM);
    }
}
