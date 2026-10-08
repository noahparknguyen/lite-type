package dev.noahpn.litetype.app;

import javafx.geometry.Insets;
import javafx.scene.layout.Region;

/**
 * A thin line across the column that fills from the left as a run goes. Its full length is drawn
 * faintly, and the part done in the accent colour. The stylesheet sets the thickness, the
 * colours, and the gap below.
 *
 * <p>The filled part eases toward its share rather than jumping, a quarter of the remaining way
 * each frame, so a word's step glides in about 150 ms. When the system asks for reduced motion,
 * it jumps. Both parts are placed by hand rather than laid out, so moving the line never asks the
 * window to lay itself out again.
 *
 * <p>Each run makes its own line, so a new run starts empty rather than easing back from the
 * last one.
 */
final class ProgressLine extends Region {

    // The share of the remaining distance the filled part covers each frame.
    private static final double EASE = 0.25;

    private final Region track = new Region();
    private final Region fill = new Region();
    private double share;
    private double shown;

    /**
     * Creates an empty line.
     */
    ProgressLine() {
        getStyleClass().add("progress-line");
        track.getStyleClass().add("progress-track");
        fill.getStyleClass().add("progress-fill");
        track.setManaged(false);
        fill.setManaged(false);
        getChildren().addAll(track, fill);
    }

    /**
     * Sets how much of the line should be filled. The filled part moves there over the next
     * frames, through {@link #step}.
     *
     * @param share the part done, from 0 to 1
     */
    void setShare(double share) {
        this.share = share;
    }

    /**
     * Moves the filled part toward its share, once a frame: a quarter of the remaining way, or
     * all of it within half a pixel or under reduced motion. Does nothing once it has arrived.
     */
    void step() {
        if (shown == share) {
            return;
        }

        double width = track.getWidth();
        boolean jump = getScene() == null || getScene().getPreferences().isReducedMotion();
        boolean close = Math.abs(share - shown) * width < 0.5;
        shown = jump || close ? share : shown + (share - shown) * EASE;
        fill.resize(width * shown, track.getHeight());
    }

    @Override
    protected void layoutChildren() {
        Insets insets = getInsets();
        double width = getWidth() - insets.getLeft() - insets.getRight();
        double thickness = track.prefHeight(-1);
        track.resizeRelocate(insets.getLeft(), insets.getTop(), width, thickness);
        fill.resizeRelocate(insets.getLeft(), insets.getTop(), width * shown, thickness);
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        return insets.getTop() + track.prefHeight(-1) + insets.getBottom();
    }
}
