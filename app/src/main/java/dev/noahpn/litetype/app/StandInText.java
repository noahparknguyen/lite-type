package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.SnippetParser;
import dev.noahpn.litetype.core.Texts;
import dev.noahpn.litetype.core.Word;

import java.util.Iterator;
import java.util.List;

/**
 * Text to type until real content exists: common words, picked for this project, in a random
 * order, and one Java method written for it. Replaced at the content step.
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

    private static final List<String> WORDS = List.of(
        "about", "after", "again", "around", "because", "before", "between", "bring", "change",
        "close", "could", "early", "every", "first", "follow", "great", "group", "house", "large",
        "learn", "light", "little", "money", "never", "night", "number", "often", "open", "other",
        "people", "place", "point", "right", "river", "small", "sound", "still", "study", "think",
        "through", "under", "water", "where", "while", "world", "would", "write", "young");

    private StandInText() {
    }

    /**
     * Returns {@code count} random words, a space after each but the last.
     *
     * @param count how many words; must be positive
     * @param seed  where the random picks start; the same seed gives the same words
     * @return the words, in order
     */
    static Iterator<Word> words(int count, long seed) {
        return Texts.words(WORDS, count, seed);
    }

    /**
     * Returns random words that never run out, for Timed mode.
     *
     * @param seed where the random picks start; the same seed gives the same words
     * @return an endless supply of words
     */
    static Iterator<Word> endlessWords(long seed) {
        return Texts.endlessWords(WORDS, seed);
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
