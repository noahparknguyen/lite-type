package dev.noahpn.litetype.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds every theme to the names the main stylesheet looks up, so a half-finished theme fails
 * the build instead of drawing part of the window in JavaFX's fallback colour.
 */
class ThemesTest {

    // A name in use, after a property's colon, and a name being defined, before one.
    private static final Pattern USED = Pattern.compile(":\\s*(-lt-[a-z]+)");
    private static final Pattern DEFINED = Pattern.compile("(-lt-[a-z]+)\\s*:");

    @Test
    void thereIsAtLeastOneThemeAndNoneIsListedTwice() throws IOException {
        List<String> names = Themes.names();
        assertFalse(names.isEmpty());
        assertEquals(names.size(), new HashSet<>(names).size());
    }

    @Test
    void everyThemeHasAStylesheetUnderItsOwnClass() throws IOException {
        for (String name : Themes.names()) {
            String css = read("/themes/" + name + ".css");
            assertTrue(css.contains("." + Themes.styleClass(name) + " {"),
                name + ".css doesn't define ." + Themes.styleClass(name));
        }
    }

    @Test
    void everyThemeDefinesEveryColourTheStylesheetUses() throws IOException {
        Set<String> used = names(USED, read("/dev/noahpn/litetype/app/lite-type.css"));
        assertFalse(used.isEmpty());

        for (String name : Themes.names()) {
            Set<String> missing = new TreeSet<>(used);
            missing.removeAll(names(DEFINED, read("/themes/" + name + ".css")));
            assertTrue(missing.isEmpty(), name + " is missing " + missing);
        }
    }

    private static Set<String> names(Pattern pattern, String css) {
        Set<String> names = new TreeSet<>();
        Matcher matcher = pattern.matcher(css);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = ThemesTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
