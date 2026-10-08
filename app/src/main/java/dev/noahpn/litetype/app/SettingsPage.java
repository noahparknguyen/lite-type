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
 * like the bar's, and a note on what it does. Picking a choice applies it, and the page stays
 * open. A text size shows at once, and a progress style from the next run.
 */
final class SettingsPage extends VBox {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    /**
     * Creates the page.
     *
     * @param textSize   the text size in use, shown as picked
     * @param onTextSize called with a text size when its choice is clicked
     * @param progress   the progress style in use, shown as picked
     * @param onProgress called with a progress style when its choice is clicked
     */
    SettingsPage(TextSize textSize, Consumer<TextSize> onTextSize,
                 ProgressStyle progress, Consumer<ProgressStyle> onProgress) {
        getStyleClass().add("settings-page");

        getChildren().addAll(
            setting("text size",
                choices(TextSize.values(), textSize, onTextSize),
                "fit grows with the window; the others stay put while they fit"),
            setting("progress",
                choices(ProgressStyle.values(), progress, onProgress),
                "the line fills as you go; the number counts seconds left, or words or lines done"),
            label("esc back to typing", "hint"));
    }

    private static VBox setting(String name, HBox choices, String note) {
        VBox setting = new VBox(label(name, "setting-name"), choices, label(note, "setting-note"));
        setting.getStyleClass().add("setting");
        return setting;
    }

    /**
     * Makes a row with a choice for each of a setting's values, with the one in use picked.
     * Generic over the setting's enum, so every setting's row is built the same way.
     */
    private static <E extends Enum<E>> HBox choices(E[] values, E current, Consumer<E> onPick) {
        HBox row = new HBox();
        row.getStyleClass().add("choice-group");

        for (E value : values) {
            Button choice = new Button(value.name().toLowerCase(Locale.ROOT));
            choice.getStyleClass().add("choice");
            // Like the bar's buttons: no keyboard focus, so Space can never press one.
            choice.setFocusTraversable(false);
            choice.setOnAction(event -> {
                row.getChildren().forEach(other ->
                    other.pseudoClassStateChanged(SELECTED, other == choice));
                onPick.accept(value);
            });
            choice.pseudoClassStateChanged(SELECTED, value == current);
            row.getChildren().add(choice);
        }

        return row;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
