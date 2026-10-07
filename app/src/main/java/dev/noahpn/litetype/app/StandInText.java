package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Separator;
import dev.noahpn.litetype.core.SnippetParser;
import dev.noahpn.litetype.core.Word;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Text to type until real content exists: common words, picked for this project, in a random
 * order, and one Java method written for it. Replaced at the content step.
 */
final class StandInText {

    private static final String SNIPPET = """
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
     * @return the words, in order
     */
    static Iterator<Word> words(int count) {
        RandomGenerator random = RandomGenerator.getDefault();
        List<Word> words = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String text = WORDS.get(random.nextInt(WORDS.size()));
            Separator separator = i == count - 1 ? Separator.NONE : Separator.SPACE;
            words.add(new Word(text, separator, 0));
        }

        return words.iterator();
    }

    /**
     * Returns the words of the stand-in snippet.
     *
     * @return the snippet's words, in order
     */
    static Iterator<Word> snippet() {
        return SnippetParser.parse(SNIPPET).iterator();
    }
}
