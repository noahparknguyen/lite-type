package dev.noahpn.litetype.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds the shipped content to its rules, so bad content fails the build instead of the app.
 */
class ContentTest {

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
}
