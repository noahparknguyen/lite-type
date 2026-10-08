package dev.noahpn.litetype.app;

import dev.noahpn.litetype.app.Choices.Mode;
import dev.noahpn.litetype.app.Choices.SnippetSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class ChoicesTest {

    // The build points the test run's saved settings under target/, so this never touches the
    // user's own.
    private Preferences preferences;

    @BeforeEach
    void freshSettings() {
        preferences = Preferences.userRoot().node("lite-type-test");
    }

    @AfterEach
    void removeSettings() throws BackingStoreException {
        preferences.removeNode();
    }

    @Test
    void describesWhatTheRunWas() {
        assertEquals("words · 30 s", Choices.DEFAULT.describe());
        assertEquals("words · 50", Choices.DEFAULT.withMode(Mode.LENGTH).withWords(50).describe());
        Choices code = Choices.DEFAULT.withKind(TextKind.CODE).withMode(Mode.LENGTH);
        assertEquals("code · long", code.withSize(SnippetSize.LONG).describe());
    }

    @Test
    void savedChoicesComeBack() {
        Choices picked = new Choices(TextKind.CODE, Mode.LENGTH, 60, 100, SnippetSize.SHORT);
        picked.save(preferences);
        assertEquals(picked, Choices.load(preferences));
    }

    @Test
    void nothingSavedGivesTheDefaults() {
        assertEquals(Choices.DEFAULT, Choices.load(preferences));
    }

    @Test
    void anythingNoLongerOfferedFallsBackToItsDefault() {
        // An old version's choices, or a hand-edited settings file.
        preferences.put("kind", "QUOTES");
        preferences.put("mode", "ZEN");
        preferences.putInt("seconds", 45);
        preferences.putInt("words", 7);
        preferences.put("size", "HUGE");
        assertEquals(Choices.DEFAULT, Choices.load(preferences));
    }
}
