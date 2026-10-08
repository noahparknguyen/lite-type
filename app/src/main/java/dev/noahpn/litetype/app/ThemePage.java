package dev.noahpn.litetype.app;

import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * The theme page, shown where the text was: a box per theme, each drawn in its own theme's
 * colours with its name, in a grid that wraps to the window. Clicking a box picks that theme,
 * and the page stays open so the typist can try another.
 */
final class ThemePage extends VBox {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final FlowPane boxes = new FlowPane();

    /**
     * Creates the page.
     *
     * @param names    every theme, in the order to show them
     * @param current  the theme in use, shown as picked
     * @param onChoose called with a theme's name when its box is clicked
     */
    ThemePage(List<String> names, String current, Consumer<String> onChoose) {
        getStyleClass().add("theme-page");
        boxes.getStyleClass().add("theme-boxes");

        for (String name : names) {
            // A label, not a button: a label never takes keyboard focus, so no key is lost to it.
            Label box = new Label(name);
            box.getStyleClass().addAll("theme-box", Themes.styleClass(name));
            box.setUserData(name);
            box.setOnMouseClicked(event -> {
                show(name);
                onChoose.accept(name);
            });
            boxes.getChildren().add(box);
        }

        Label hint = new Label("esc back to typing");
        hint.getStyleClass().add("hint");
        getChildren().addAll(boxes, hint);
        show(current);
    }

    /**
     * Marks {@code current} as the theme in use.
     */
    private void show(String current) {
        boxes.getChildren().forEach(box ->
            box.pseudoClassStateChanged(SELECTED, current.equals(box.getUserData())));
    }
}
