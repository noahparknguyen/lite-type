package dev.noahpn.litetype.app;

import dev.noahpn.litetype.app.Choices.SnippetSize;
import dev.noahpn.litetype.core.Separator;
import dev.noahpn.litetype.core.Word;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds the shipped content to its rules, so bad content fails the build instead of the app.
 */
class ContentTest {

    // What a snippet may use without importing it, since each compiles inside a class of its own.
    private static final String IMPORTS = """
        import java.io.*;
        import java.nio.file.*;
        import java.time.*;
        import java.util.*;
        import java.util.function.*;
        import java.util.stream.*;
        """;

    @Test
    void thereAreTwoHundredWords() throws IOException {
        assertEquals(200, Content.words().size());
    }

    @Test
    void everyWordIsLowercaseLettersOnly() throws IOException {
        for (String word : Content.words()) {
            assertTrue(word.matches("[a-z]+"), "not lowercase letters only: " + word);
        }
    }

    @Test
    void noWordIsListedTwice() throws IOException {
        List<String> words = Content.words();
        assertEquals(words.size(), new HashSet<>(words).size());
    }

    @Test
    void everyLetterAppearsSoEveryKeyGetsPractised() throws IOException {
        Set<Character> letters = new HashSet<>();
        for (String word : Content.words()) {
            for (char letter : word.toCharArray()) {
                letters.add(letter);
            }
        }
        assertEquals(26, letters.size());
    }

    /**
     * Reading the snippets parses every one, and parsing rejects a line over 80 characters, two
     * spaces in a row, or a character not on a standard keyboard.
     */
    @Test
    void everySizeHasSnippetsThatFollowTheTypingRules() throws IOException {
        Map<SnippetSize, List<List<Word>>> snippets = Content.snippets();

        for (SnippetSize size : SnippetSize.values()) {
            assertFalse(snippets.get(size).isEmpty(), "no " + size + " snippets");
        }
    }

    @Test
    void everySnippetIsTheLengthOfItsSize() throws IOException {
        Map<SnippetSize, int[]> lines = Map.of(
            SnippetSize.SHORT, new int[] {4, 8},
            SnippetSize.MEDIUM, new int[] {10, 18},
            SnippetSize.LONG, new int[] {20, 35});

        for (SnippetSize size : SnippetSize.values()) {
            int[] range = lines.get(size);

            for (List<Word> snippet : Content.snippets(size)) {
                int typed = typedLines(snippet);
                assertTrue(typed >= range[0] && typed <= range[1],
                    size + " snippet starting \"" + snippet.getFirst().text() + "\" has "
                        + typed + " lines, not " + range[0] + " to " + range[1]);
            }
        }
    }

    @Test
    void everySnippetCompiles(@TempDir Path classes) throws IOException {
        List<JavaFileObject> sources = new ArrayList<>();

        for (SnippetSize size : SnippetSize.values()) {
            for (String snippet : Content.snippetSources(size)) {
                String name = "Snippet" + sources.size();
                sources.add(source(name, IMPORTS + "class " + name + " {\n" + snippet + "}\n"));
            }
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> problems = new DiagnosticCollector<>();
        List<String> options = List.of("-d", classes.toString(), "-proc:none");
        boolean compiled = compiler.getTask(null, null, problems, options, null, sources).call();

        assertTrue(compiled, problems.getDiagnostics().toString());
    }

    private static int typedLines(List<Word> snippet) {
        int lines = 1;
        for (Word word : snippet) {
            if (word.separator() == Separator.LINE_BREAK) {
                lines++;
            }
        }
        return lines;
    }

    /**
     * Wraps source code held in a string as a file the compiler can read.
     */
    private static JavaFileObject source(String className, String code) {
        URI uri = URI.create("string:///" + className + ".java");
        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }
}
