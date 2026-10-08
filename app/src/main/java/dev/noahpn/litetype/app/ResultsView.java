package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Results;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * A finished run's results, shown where the text was: WPM large, then the two accuracies, then
 * what the run was, and the keys that start another. Every number is a whole number.
 */
final class ResultsView extends VBox {

    /**
     * Creates the view.
     *
     * @param results     the run's results
     * @param description what the run was, such as {@code words · 25}
     */
    ResultsView(Results results, String description) {
        getStyleClass().add("results");

        HBox headline = new HBox(
            label(String.valueOf(Math.round(results.wpm())), "results-wpm"),
            label("wpm", "results-unit"));
        headline.getStyleClass().add("results-headline");

        getChildren().addAll(
            headline,
            label("accuracy " + percent(results.accuracy()), "results-detail"),
            label("text accuracy " + percent(results.textAccuracy()), "results-detail"),
            label(description, "results-context"),
            label("esc new text · shift+esc same text", "hint"));
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private static String percent(double fraction) {
        return Math.round(fraction * 100) + "%";
    }
}
