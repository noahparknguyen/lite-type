package dev.noahpn.litetype.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Turns the source of a code snippet into words to type. A line's leading spaces become the
 * indentation of its first word, each single space inside a line ends a word, and the last word
 * on each line ends with a line break. Tabs become four spaces, trailing spaces are removed, and
 * blank lines are skipped.
 */
public final class SnippetParser {

    /**
     * The longest line a snippet may have, indentation included, so that code never wraps.
     */
    public static final int MAX_LINE_LENGTH = 80;

    private static final String TAB = "    ";

    private SnippetParser() {
    }

    /**
     * Returns the words of a snippet, in order. The last word has nothing after it.
     *
     * @param source the snippet's source, with any line endings
     * @return the words to type
     * @throws NullPointerException     if {@code source} is null
     * @throws IllegalArgumentException if the snippet has no code, a line longer than
     *                                  {@link #MAX_LINE_LENGTH}, two spaces in a row after a
     *                                  line's indentation, or a character not on a standard
     *                                  keyboard
     */
    public static List<Word> parse(String source) {
        Objects.requireNonNull(source, "source must not be null");

        List<String> lines = source.replace("\t", TAB)
            .lines()
            .map(String::stripTrailing)
            .filter(line -> !line.isEmpty())
            .toList();

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("snippet holds no code");
        }

        List<Word> words = new ArrayList<>();

        for (int n = 0; n < lines.size(); n++) {
            String line = lines.get(n);
            check(line);

            int indent = line.length() - line.stripLeading().length();
            String[] texts = line.substring(indent).split(" ");
            boolean lastLine = n == lines.size() - 1;

            for (int i = 0; i < texts.length; i++) {
                boolean lastInLine = i == texts.length - 1;
                Separator separator = !lastInLine ? Separator.SPACE
                    : lastLine ? Separator.NONE : Separator.LINE_BREAK;
                words.add(new Word(texts[i], separator, i == 0 ? indent : 0));
            }
        }

        return List.copyOf(words);
    }

    /**
     * Rejects a line the typing rules can't handle. Two spaces in a row can't be typed as
     * written, because Space on an empty word is ignored.
     */
    private static void check(String line) {
        if (line.length() > MAX_LINE_LENGTH) {
            throw new IllegalArgumentException(
                "line is longer than " + MAX_LINE_LENGTH + " characters: " + line);
        }

        if (line.stripLeading().contains("  ")) {
            throw new IllegalArgumentException("line has two spaces in a row: " + line);
        }

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c < ' ' || c > '~') {
                throw new IllegalArgumentException(
                    "line has a character not on a standard keyboard: " + line);
            }
        }
    }
}
