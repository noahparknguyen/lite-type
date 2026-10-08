package dev.noahpn.litetype.app;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderButtonType;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

/**
 * The window's title bar, drawn by the app so it follows the theme: the logo on the left, and
 * minimize, maximize, and close on the right. The window keeps the system's frame, so moving,
 * resizing, snapping, and double-clicking to maximize work as in any other window.
 *
 * <p>Built on JavaFX's {@link HeaderBar}, for a stage with the {@code EXTENDED} style. Its
 * background drags the window. Each button is marked with its {@link HeaderButtonType}, which
 * gives it the button's behaviour and tells the system what it is: hovering the maximize button
 * on Windows brings up snap layouts.
 */
final class TitleBar extends HeaderBar {

    // The glyphs, drawn in a 10 by 10 box like the system's: a line, a square, two overlapping
    // squares for restore, and a cross. The half pixels put a 1px line on whole pixels.
    private static final String MINIMIZE = "M0 5.5 H10";
    private static final String MAXIMIZE = "M0.5 0.5 H9.5 V9.5 H0.5 Z";
    private static final String RESTORE = "M2.5 2.5 V0.5 H9.5 V7.5 H7.5 M0.5 2.5 H7.5 V9.5 H0.5 Z";
    private static final String CLOSE = "M0 0 L10 10 M10 0 L0 10";

    /**
     * Creates the title bar.
     */
    TitleBar() {
        getStyleClass().add("title-bar");

        Node logo = logo();
        HeaderBar.setAlignment(logo, Pos.CENTER_LEFT);
        // Content doesn't drag the window by default, but the logo is part of the bar.
        HeaderBar.setDragType(logo, HeaderDragType.DRAGGABLE);
        setLeft(logo);

        setRight(new HBox(
            button(HeaderButtonType.ICONIFY, "minimize", glyph(MINIMIZE, "minimize-glyph")),
            button(HeaderButtonType.MAXIMIZE, "maximize",
                glyph(MAXIMIZE, "maximize-glyph"), glyph(RESTORE, "restore-glyph")),
            button(HeaderButtonType.CLOSE, "close", glyph(CLOSE, "close-glyph"))));
    }

    /**
     * The logo in the theme's colours: "lt" over a short progress line, the line as wide as the
     * letters and just over half filled.
     */
    private static Node logo() {
        Text letters = new Text("lt");
        letters.getStyleClass().add("title-logo-letters");

        Region track = new Region();
        track.getStyleClass().add("title-logo-track");
        Region fill = new Region();
        fill.getStyleClass().add("title-logo-fill");
        fill.maxWidthProperty().bind(track.widthProperty().multiply(0.55));
        StackPane line = new StackPane(track, fill);
        line.setAlignment(Pos.CENTER_LEFT);

        // A VBox makes the line as wide as the widest child, the letters.
        VBox logo = new VBox(letters, line);
        logo.getStyleClass().add("title-logo");
        logo.setMaxHeight(Region.USE_PREF_SIZE);
        return logo;
    }

    /**
     * A window button: a box that lights up on hover, holding its glyph. The maximize button
     * holds two, and the stylesheet shows restore's while the window is maximized.
     */
    private static Node button(HeaderButtonType type, String name, Node... glyphs) {
        StackPane button = new StackPane(glyphs);
        button.getStyleClass().addAll("window-button", name);
        HeaderBar.setButtonType(button, type);
        return button;
    }

    private static Node glyph(String path, String styleClass) {
        SVGPath glyph = new SVGPath();
        glyph.setContent(path);
        glyph.getStyleClass().addAll("window-glyph", styleClass);
        return glyph;
    }
}
