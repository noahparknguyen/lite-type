package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Results;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * A finished run's results, shown where the text was and centred: WPM large, then the two
 * accuracies, then what the run was, and the keys that start another. Every number is a whole
 * number: WPM rounded to the nearest, the accuracies rounded down.
 */
final class ResultsView extends VBox {

    /**
     * Creates the view.
     *
     * @param results     the run's results
     * @param description what the run was, such as {@code words · 25 · 18 s}
     */
    ResultsView(Results results, String description) {
        getStyleClass().add("results");

        HBox headline = new HBox(
            label(String.valueOf(Math.round(results.wpm())), "results-wpm"),
            label("wpm", "results-unit"));
        headline.getStyleClass().add("results-headline");

        getChildren().addAll(
            headline,
            label("accuracy " + results.accuracyPercent() + "%", "results-detail"),
            label("text accuracy " + results.textAccuracyPercent() + "%", "results-detail"),
            label(description, "results-context"),
            label("esc new text · shift+esc same text", "hint"));
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
