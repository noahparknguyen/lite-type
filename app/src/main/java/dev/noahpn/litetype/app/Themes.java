package dev.noahpn.litetype.app;

import java.io.IOException;
import java.util.List;

/**
 * The themes that ship with the app. Each is a stylesheet in {@code /themes/} defining the
 * colour names the main stylesheet looks up, under a class named for the theme. The index file
 * lists them in the order the theme page shows them, and the first is the default. Adding a
 * theme takes one stylesheet and one line in the index, and no Java.
 */
final class Themes {

    private static final String FOLDER = "/themes/";
    private static final String INDEX = FOLDER + "themes.txt";

    private Themes() {
    }

    /**
     * Returns the names of the themes, in the index's order, skipping notes, which start with
     * {@code #}.
     *
     * @return the theme names; the first is the default
     * @throws IOException           if the index can't be read
     * @throws IllegalStateException if the index is missing
     */
    static List<String> names() throws IOException {
        return Resources.text(INDEX)
            .lines()
            .map(String::strip)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .toList();
    }

    /**
     * Returns a theme's stylesheet, as an address a scene can load.
     *
     * @param name the theme
     * @return the stylesheet's URL, as a string
     * @throws IllegalStateException if the theme has no stylesheet
     */
    static String stylesheet(String name) {
        return Resources.address(FOLDER + name + ".css");
    }

    /**
     * Returns the style class that applies a theme's colours to a node and everything inside it.
     *
     * @param name the theme
     * @return the theme's style class
     */
    static String styleClass(String name) {
        return "theme-" + name;
    }
}
