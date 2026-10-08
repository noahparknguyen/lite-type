package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Run;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * The progress above the text: a line or a number, by the setting, kept up to date with the run.
 * The working out is {@link RunProgress}'s; this only shows it.
 */
final class ProgressView extends StackPane {

    private final Label number = new Label();
    private ProgressStyle style;
    private ProgressLine line;
    private RunProgress progress;
    // The number on screen, so its text is set only when it changes, never every frame.
    private int shown;

    /**
     * Creates the view, showing nothing until a run starts.
     *
     * @param style the progress style in use
     */
    ProgressView(ProgressStyle style) {
        this.style = style;
        number.getStyleClass().add("progress-number");
        setAlignment(Pos.CENTER_LEFT);
    }

    /**
     * Returns the progress style in use.
     *
     * @return the style
     */
    ProgressStyle style() {
        return style;
    }

    /**
     * Sets the progress style. It shows from the next run.
     *
     * @param style the style
     */
    void setStyle(ProgressStyle style) {
        this.style = style;
    }

    /**
     * Starts following a run. Each run gets a new line, so it starts empty rather than easing
     * back from the last run's.
     *
     * @param run     the run, before its first key
     * @param choices the choices it was made with
     */
    void start(Run run, Choices choices) {
        progress = new RunProgress(run, choices);
        line = new ProgressLine();
        shown = -1;
        getChildren().setAll(style == ProgressStyle.LINE ? line : number);
        update();
    }

    /**
     * Brings what's shown in line with the run: the line's share, or the number's text when the
     * number has changed. Called after every key and every frame.
     */
    void update() {
        if (style == ProgressStyle.LINE) {
            line.setShare(progress.share());
            return;
        }

        int value = progress.value();
        if (value != shown) {
            shown = value;
            number.setText(progress.text());
        }
    }

    /**
     * Moves the line a frame's worth toward its share. Does nothing when the number is shown or
     * the line has arrived.
     */
    void step() {
        line.step();
    }
}
