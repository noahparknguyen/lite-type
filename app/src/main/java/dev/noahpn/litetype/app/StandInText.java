package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.SnippetParser;
import dev.noahpn.litetype.core.Texts;
import dev.noahpn.litetype.core.Word;

import java.util.Iterator;
import java.util.List;

/**
 * Code to type until the real snippets exist: one Java method written for the project. Replaced
 * once the snippets are written.
 */
final class StandInText {

    // Package-private so the measurement can repeat it into a longer text.
    static final String SNIPPET = """
        static int countWords(String text) {
            int count = 0;
            boolean inWord = false;

            for (char c : text.toCharArray()) {
                if (Character.isWhitespace(c)) {
                    inWord = false;
                } else if (!inWord) {
                    inWord = true;
                    count++;
                }
            }

            return count;
        }
        """;

    private StandInText() {
    }

    /**
     * Returns one snippet's words. The stand-in has a single snippet, so every size and seed
     * gives it.
     *
     * @param size the snippet size picked
     * @param seed where the choice of snippet starts
     * @return the snippet's words, in order
     */
    static Iterator<Word> snippet(Choices.SnippetSize size, long seed) {
        return SnippetParser.parse(SNIPPET).iterator();
    }

    /**
     * Returns snippets back to back, never running out, for Timed mode.
     *
     * @param seed where the shuffling starts; the same seed gives the same order
     * @return an endless supply of words
     */
    static Iterator<Word> endlessSnippets(long seed) {
        return Texts.endlessSnippets(List.of(SnippetParser.parse(SNIPPET)), seed);
    }
}
