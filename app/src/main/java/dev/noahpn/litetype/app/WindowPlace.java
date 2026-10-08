package dev.noahpn.litetype.app;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

/**
 * Where the window sits. It first opens at three quarters of the screen's width, centred, and
 * after that it reopens at the size and place it was closed at, maximized if it was.
 */
final class WindowPlace {

    private static final double FIRST_WIDTH = 0.75;
    private static final double FIRST_HEIGHT = 0.6;
    private static final String X = "window.x";
    private static final String Y = "window.y";
    private static final String WIDTH = "window.width";
    private static final String HEIGHT = "window.height";
    private static final String MAXIMIZED = "window.maximized";

    private WindowPlace() {
    }

    /**
     * Puts the window where it was last closed. A place that's no longer on any screen, because
     * a monitor was unplugged, is ignored, so the window can't open out of sight.
     *
     * @param stage       the window, not yet shown
     * @param preferences where the place was saved
     */
    static void restore(Stage stage, Preferences preferences) {
        double x = preferences.getDouble(X, 0);
        double y = preferences.getDouble(Y, 0);
        double width = preferences.getDouble(WIDTH, 0);
        double height = preferences.getDouble(HEIGHT, 0);
        boolean saved = width > 0 && height > 0;

        if (saved && !Screen.getScreensForRectangle(x, y, width, height).isEmpty()) {
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(width);
            stage.setHeight(height);
        } else {
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setWidth(screen.getWidth() * FIRST_WIDTH);
            stage.setHeight(screen.getHeight() * FIRST_HEIGHT);
            stage.centerOnScreen();
        }

        stage.setMaximized(preferences.getBoolean(MAXIMIZED, false));
    }

    /**
     * Saves where the window is. A maximized window's size is just the screen's, so only the
     * flag is saved then, and the last normal size is kept for when it's restored.
     *
     * @param stage       the window
     * @param preferences where to save the place
     */
    static void save(Stage stage, Preferences preferences) {
        preferences.putBoolean(MAXIMIZED, stage.isMaximized());

        if (!stage.isMaximized()) {
            preferences.putDouble(X, stage.getX());
            preferences.putDouble(Y, stage.getY());
            preferences.putDouble(WIDTH, stage.getWidth());
            preferences.putDouble(HEIGHT, stage.getHeight());
        }
    }
}
