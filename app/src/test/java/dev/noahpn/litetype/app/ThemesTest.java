package dev.noahpn.litetype.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds every theme to the names the main stylesheet looks up, so a half-finished theme fails
 * the build instead of drawing part of the window in JavaFX's fallback colour, and to the
 * contrast the app depends on, so a new theme can't make text hard to read or a mistake hard to
 * see. Each rule says what it protects.
 */
class ThemesTest {

    // A name in use, after a property's colon, and a name being defined, before one.
    private static final Pattern USED = Pattern.compile(":\\s*(-lt-[a-z]+)");
    private static final Pattern DEFINED = Pattern.compile("(-lt-[a-z]+)\\s*:");
    // A name being defined, with its colour.
    private static final Pattern COLOUR = Pattern.compile("(-lt-[a-z]+)\\s*:\\s*#([0-9a-fA-F]{6})");

    /**
     * A contrast a theme must keep between two of its colours, by WCAG's formula, where 1 is no
     * contrast and 21 is black on white.
     */
    private record Rule(String colour, String against, double minimum, String soThat) {
    }

    private static final List<Rule> RULES = List.of(
        new Rule("correct", "bg", 7, "typed text reads at full strength"),
        new Rule("untyped", "bg", 3, "text ahead is easy to read: WCAG's minimum for large text"),
        new Rule("wrong", "bg", 3, "a mistake is easy to see"),
        new Rule("cursor", "bg", 3, "the cursor is easy to find"),
        new Rule("accent", "bg", 3, "the progress is easy to see"),
        new Rule("soft", "bg", 4.5, "the accuracies read as text: WCAG's minimum for small text"),
        new Rule("dim", "bg", 3, "hints and labels stay readable"),
        new Rule("missed", "bg", 2, "a skipped letter still shows"),
        new Rule("mark", "bg", 1.25, "the line-end mark and quiet outlines are visible"),
        new Rule("wrong", "untyped", 1.8, "a mistake differs from untyped text by brightness too"),
        new Rule("wrong", "correct", 2, "a mistake differs from typed text by brightness too"),
        new Rule("extra", "wrong", 1.5, "an extra letter differs from a wrong one"));

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

    @Test
    void everyThemeKeepsItsContrast() throws IOException {
        // Every break in every theme, so whoever fixes a theme sees them all at once.
        List<String> broken = new ArrayList<>();

        for (String name : Themes.names()) {
            Map<String, int[]> colours = colours(read("/themes/" + name + ".css"));

            for (Rule rule : RULES) {
                double contrast = contrast(
                    colours.get("-lt-" + rule.colour()), colours.get("-lt-" + rule.against()));
                if (contrast < rule.minimum()) {
                    broken.add(String.format(
                        "%s: %s against %s is %.2f:1, under the %.2f:1 needed so that %s",
                        name, rule.colour(), rule.against(), contrast, rule.minimum(),
                        rule.soThat()));
                }
            }
        }

        assertTrue(broken.isEmpty(), String.join("\n", broken));
    }

    @Test
    void everyThemesQuietLinesStayQuiet() throws IOException {
        for (String name : Themes.names()) {
            Map<String, int[]> colours = colours(read("/themes/" + name + ".css"));
            double contrast = contrast(colours.get("-lt-mark"), colours.get("-lt-bg"));
            assertTrue(contrast <= 2, String.format(
                "%s: mark against bg is %.2f:1; over 2:1 it stops reading as quiet", name, contrast));
        }
    }

    @Test
    void noThemeShowsItsProgressInItsMistakeColour() throws IOException {
        for (String name : Themes.names()) {
            Map<String, int[]> colours = colours(read("/themes/" + name + ".css"));
            assertFalse(Arrays.equals(colours.get("-lt-accent"), colours.get("-lt-wrong")),
                name + ": the accent is the mistake colour, so the progress looks like an error");
        }
    }

    /**
     * Returns the contrast between two colours, by WCAG 2's formula: the lighter's relative
     * luminance plus 0.05, over the darker's plus 0.05.
     */
    private static double contrast(int[] a, int[] b) {
        assertNotNull(a, "a colour the rule needs is missing");
        assertNotNull(b, "a colour the rule needs is missing");
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /**
     * Returns a colour's relative luminance: each channel turned back into linear light, then
     * weighted by how bright the eye finds it.
     */
    private static double luminance(int[] rgb) {
        double[] weights = {0.2126, 0.7152, 0.0722};
        double sum = 0;
        for (int i = 0; i < 3; i++) {
            double c = rgb[i] / 255.0;
            c = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
            sum += weights[i] * c;
        }
        return sum;
    }

    private static Map<String, int[]> colours(String css) {
        Map<String, int[]> colours = new HashMap<>();
        Matcher matcher = COLOUR.matcher(css);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(2), 16);
            colours.put(matcher.group(1), new int[] {value >> 16, (value >> 8) & 0xff, value & 0xff});
        }
        return colours;
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
