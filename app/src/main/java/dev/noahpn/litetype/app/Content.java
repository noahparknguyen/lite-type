package dev.noahpn.litetype.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The text that ships with the app, read from its resources. The app reads it once, at startup,
 * so no run ever waits on a file.
 */
final class Content {

    private static final String WORDS = "/content/words.txt";

    private Content() {
    }

    /**
     * Returns the word list: one word per line, skipping blank lines and notes, which start with
     * {@code #}.
     *
     * @return the words, in the file's order
     * @throws IOException           if the file can't be read
     * @throws IllegalStateException if the file is missing
     */
    static List<String> words() throws IOException {
        return lines(WORDS);
    }

    private static List<String> lines(String resource) throws IOException {
        try (InputStream in = Content.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing content: " + resource);
            }

            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        }
    }
}
