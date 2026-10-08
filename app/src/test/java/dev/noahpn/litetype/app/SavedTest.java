package dev.noahpn.litetype.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class SavedTest {

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
    void readsWhatWasSaved() {
        preferences.put("textSize", "HUGE");
        assertEquals(TextSize.HUGE, Saved.read(preferences, "textSize", TextSize.FIT));
    }

    @Test
    void nothingSavedGivesTheDefault() {
        assertEquals(TextSize.FIT, Saved.read(preferences, "textSize", TextSize.FIT));
    }

    @Test
    void aValueTheAppNoLongerHasGivesTheDefault() {
        preferences.put("textSize", "ENORMOUS");
        assertEquals(TextSize.FIT, Saved.read(preferences, "textSize", TextSize.FIT));
    }
}
