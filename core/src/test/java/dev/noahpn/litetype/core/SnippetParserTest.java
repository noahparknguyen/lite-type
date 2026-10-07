package dev.noahpn.litetype.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnippetParserTest {

    @Test
    void wordsEndWithSpacesAndEachLineWithALineBreak() {
        List<Word> words = SnippetParser.parse("int x = 1;\nx++;");
        assertEquals(List.of(
            new Word("int", Separator.SPACE, 0),
            new Word("x", Separator.SPACE, 0),
            new Word("=", Separator.SPACE, 0),
            new Word("1;", Separator.LINE_BREAK, 0),
            new Word("x++;", Separator.NONE, 0)), words);
    }

    @Test
    void leadingSpacesBecomeTheFirstWordsIndent() {
        List<Word> words = SnippetParser.parse("if (on) {\n    run();\n}");
        assertEquals(new Word("run();", Separator.LINE_BREAK, 4), words.get(3));
    }

    @Test
    void tabsBecomeFourSpaces() {
        List<Word> words = SnippetParser.parse("{\n\treturn;\n}");
        assertEquals(4, words.get(1).indent());
    }

    @Test
    void trailingSpacesAndBlankLinesAreDropped() {
        List<Word> words = SnippetParser.parse("a();   \n\n   \nb();\n");
        assertEquals(List.of(
            new Word("a();", Separator.LINE_BREAK, 0),
            new Word("b();", Separator.NONE, 0)), words);
    }

    @Test
    void windowsLineEndingsWork() {
        List<Word> words = SnippetParser.parse("a();\r\nb();");
        assertEquals(2, words.size());
        assertEquals("a();", words.get(0).text());
    }

    @Test
    void lineOfExactlyTheLimitIsAllowed() {
        String line = "x".repeat(SnippetParser.MAX_LINE_LENGTH);
        assertEquals(1, SnippetParser.parse(line).size());
    }

    @Test
    void lineOverTheLimitThrows() {
        String line = "x".repeat(SnippetParser.MAX_LINE_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> SnippetParser.parse(line));
    }

    @Test
    void indentCountsTowardTheLimit() {
        String line = "  " + "x".repeat(SnippetParser.MAX_LINE_LENGTH - 1);
        assertThrows(IllegalArgumentException.class, () -> SnippetParser.parse(line));
    }

    @Test
    void twoSpacesInARowThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> SnippetParser.parse("int x  = 1;"));
    }

    @Test
    void characterNotOnAKeyboardThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> SnippetParser.parse("String s = “hi”;"));
    }

    @Test
    void snippetWithNoCodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> SnippetParser.parse("\n  \n"));
    }
}
