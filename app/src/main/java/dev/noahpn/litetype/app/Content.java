package dev.noahpn.litetype.app;

import dev.noahpn.litetype.app.Choices.SnippetSize;
import dev.noahpn.litetype.core.SnippetParser;
import dev.noahpn.litetype.core.Word;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The text that ships with the app, read from its resources. The app reads it once, at startup,
 * so no run ever waits on a file.
 *
 * <p>In every content file, a line starting with {@code #} is a note, not content. In a snippet
 * file, a line holding only {@code ---} ends one snippet and starts the next.
 */
final class Content {

    private static final String WORDS = "/content/words.txt";
    private static final String SNIPPETS = "/content/snippets/";
    private static final String NEXT_SNIPPET = "---";

    private Content() {
    }

    /**
     * Returns the word list, one word per line, skipping blank lines and notes.
     *
     * @return the words, in the file's order
     * @throws IOException           if the file can't be read
     * @throws IllegalStateException if the file is missing
     */
    static List<String> words() throws IOException {
        return read(WORDS)
            .lines()
            .map(String::strip)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .toList();
    }

    /**
     * Returns every snippet, by size, each turned into words to type.
     *
     * @return the snippets of each size
     * @throws IOException              if a file can't be read
     * @throws IllegalStateException    if a file is missing
     * @throws IllegalArgumentException if a snippet breaks the typing rules
     */
    static Map<SnippetSize, List<List<Word>>> snippets() throws IOException {
        Map<SnippetSize, List<List<Word>>> snippets = new EnumMap<>(SnippetSize.class);

        for (SnippetSize size : SnippetSize.values()) {
            snippets.put(size, snippets(size));
        }

        return snippets;
    }

    /**
     * Returns the snippets of one size, each turned into words to type.
     *
     * @param size which snippets
     * @return the snippets, in the file's order
     * @throws IOException              if the file can't be read
     * @throws IllegalStateException    if the file is missing
     * @throws IllegalArgumentException if a snippet breaks the typing rules, such as a line over
     *                                  80 characters
     */
    static List<List<Word>> snippets(SnippetSize size) throws IOException {
        return snippetSources(size).stream().map(SnippetParser::parse).toList();
    }

    /**
     * Returns the source of each snippet of one size, as written, without its notes.
     * Package-private for the test that compiles every snippet.
     *
     * @param size which snippets
     * @return each snippet's source, in the file's order
     * @throws IOException           if the file can't be read
     * @throws IllegalStateException if the file is missing
     */
    static List<String> snippetSources(SnippetSize size) throws IOException {
        String file = SNIPPETS + size.name().toLowerCase(Locale.ROOT) + ".txt";
        List<String> sources = new ArrayList<>();
        StringBuilder snippet = new StringBuilder();

        for (String line : read(file).lines().toList()) {
            if (line.equals(NEXT_SNIPPET)) {
                sources.add(snippet.toString());
                snippet.setLength(0);
            } else if (!line.startsWith("#")) {
                snippet.append(line).append('\n');
            }
        }

        sources.add(snippet.toString());
        return sources;
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = Content.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing content: " + resource);
            }

            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
