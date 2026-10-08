package dev.noahpn.litetype.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The files bundled with the app: content, themes, and the stylesheet. A missing one means a
 * broken build, so it fails at once, naming the file.
 */
final class Resources {

    private Resources() {
    }

    /**
     * Returns a bundled text file's contents, read as UTF-8.
     *
     * @param path the file's path among the resources, starting with {@code /}
     * @return the file's text
     * @throws IOException           if the file can't be read
     * @throws IllegalStateException if the file is missing
     */
    static String text(String path) throws IOException {
        try (InputStream in = Resources.class.getResourceAsStream(path)) {
            if (in == null) {
                throw missing(path);
            }

            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Returns a bundled file's address, as a scene loads a stylesheet from.
     *
     * @param path the file's path among the resources, starting with {@code /}
     * @return the file's URL, as a string
     * @throws IllegalStateException if the file is missing
     */
    static String address(String path) {
        URL url = Resources.class.getResource(path);

        if (url == null) {
            throw missing(path);
        }

        return url.toExternalForm();
    }

    private static IllegalStateException missing(String path) {
        return new IllegalStateException("missing a bundled file: " + path);
    }
}
