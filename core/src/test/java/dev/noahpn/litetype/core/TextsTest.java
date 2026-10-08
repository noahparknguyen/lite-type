package dev.noahpn.litetype.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextsTest {

    private static final List<String> WORDS = List.of("one", "two", "three", "four", "five");

    private static List<Word> take(Iterator<Word> words, int count) {
        List<Word> taken = new ArrayList<>();
        for (int i = 0; i < count && words.hasNext(); i++) {
            taken.add(words.next());
        }
        return taken;
    }

    @Test
    void wordsGivesTheCountWithNothingAfterTheLast() {
        List<Word> words = take(Texts.words(WORDS, 10, 1), 20);
        assertEquals(10, words.size());
        assertEquals(Separator.SPACE, words.get(8).separator());
        assertEquals(Separator.NONE, words.get(9).separator());
    }

    @Test
    void theSameSeedGivesTheSameWords() {
        assertEquals(take(Texts.words(WORDS, 25, 7), 25), take(Texts.words(WORDS, 25, 7), 25));
        assertEquals(
            take(Texts.endlessWords(WORDS, 7), 100),
            take(Texts.endlessWords(WORDS, 7), 100));
    }

    @Test
    void aDifferentSeedGivesDifferentWords() {
        assertNotEquals(take(Texts.words(WORDS, 25, 7), 25), take(Texts.words(WORDS, 25, 8), 25));
    }

    @Test
    void endlessWordsNeverRunOut() {
        Iterator<Word> words = Texts.endlessWords(WORDS, 1);
        take(words, 10_000);
        assertTrue(words.hasNext());
        assertEquals(Separator.SPACE, words.next().separator());
    }

    @Test
    void snippetsJoinWithALineBreakAndKeepTheirIndent() {
        List<Word> a = SnippetParser.parse("if (a) {\n    go();\n}");
        Iterator<Word> words = Texts.endlessSnippets(List.of(a), 1);
        List<Word> twice = take(words, a.size() * 2);
        assertEquals(new Word("}", Separator.LINE_BREAK, 0), twice.get(a.size() - 1));
        assertEquals(a.getFirst(), twice.get(a.size()));
        assertEquals(new Word("go();", Separator.LINE_BREAK, 4), twice.get(3));
    }

    @Test
    void everySnippetIsUsedBeforeAnyComesBack() {
        List<List<Word>> snippets = List.of(
            SnippetParser.parse("a();"),
            SnippetParser.parse("b();"),
            SnippetParser.parse("c();"));
        List<Word> words = take(Texts.endlessSnippets(snippets, 3), 300);

        for (int round = 0; round < 100; round++) {
            HashSet<String> seen = new HashSet<>();
            for (Word word : words.subList(round * 3, round * 3 + 3)) {
                seen.add(word.text());
            }
            assertEquals(3, seen.size(), "round " + round + " repeated a snippet");
        }

        for (int i = 1; i < words.size(); i++) {
            assertNotEquals(words.get(i - 1).text(), words.get(i).text(), "repeat at " + i);
        }
    }

    @Test
    void theSameSeedGivesTheSameSnippets() {
        List<List<Word>> snippets = List.of(
            SnippetParser.parse("a();"),
            SnippetParser.parse("b();"),
            SnippetParser.parse("c();"));
        assertEquals(
            take(Texts.endlessSnippets(snippets, 5), 60),
            take(Texts.endlessSnippets(snippets, 5), 60));
    }

    @Test
    void emptyContentThrows() {
        assertThrows(IllegalArgumentException.class, () -> Texts.words(List.of(), 10, 1));
        assertThrows(IllegalArgumentException.class, () -> Texts.endlessWords(List.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> Texts.endlessSnippets(List.of(), 1));
        assertThrows(IllegalArgumentException.class,
            () -> Texts.endlessSnippets(List.of(List.of()), 1));
    }

    @Test
    void countThatIsNotPositiveThrows() {
        assertThrows(IllegalArgumentException.class, () -> Texts.words(WORDS, 0, 1));
    }
}
