package dev.noahpn.litetype.app;

import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The settings page, shown where the text was. Each setting is a name, a row of choices drawn
 * like the bar's, and a note on what it does. Picking a choice applies it at once, and the page
 * stays open. Text size is the first setting, and the page is where later ones go.
 */
final class SettingsPage extends VBox {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    /**
     * Creates the page.
     *
     * @param textSize   the text size in use, shown as picked
     * @param onTextSize called with a text size when its choice is clicked
     */
    SettingsPage(TextSize textSize, Consumer<TextSize> onTextSize) {
        getStyleClass().add("settings-page");

        HBox sizes = new HBox();
        sizes.getStyleClass().add("choice-group");

        for (TextSize size : TextSize.values()) {
            Button choice = new Button(size.name().toLowerCase(Locale.ROOT));
            choice.getStyleClass().add("choice");
            // Like the bar's buttons: no keyboard focus, so Space can never press one.
            choice.setFocusTraversable(false);
            choice.setUserData(size);
            choice.setOnAction(event -> {
                sizes.getChildren().forEach(other ->
                    other.pseudoClassStateChanged(SELECTED, other == choice));
                onTextSize.accept(size);
            });
            choice.pseudoClassStateChanged(SELECTED, size == textSize);
            sizes.getChildren().add(choice);
        }

        getChildren().addAll(
            label("text size", "setting-name"),
            sizes,
            label("fit grows with the window; the others stay the same size", "setting-note"),
            label("esc back to typing", "hint"));
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
