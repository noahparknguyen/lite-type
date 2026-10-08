package dev.noahpn.litetype.app;

import javafx.geometry.Insets;
import javafx.scene.layout.Region;

/**
 * A thin line across the column that fills from the left as a run goes. Its full length is drawn
 * faintly, and the part done in the accent colour. The stylesheet sets the thickness, the
 * colours, and the gap below.
 *
 * <p>Both parts are placed by hand rather than laid out, so filling the line never asks the
 * window to lay itself out again. In Timed mode it fills a little every frame.
 */
final class ProgressLine extends Region {

    private final Region track = new Region();
    private final Region fill = new Region();
    private double share;

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
     * Sets how much of the line is filled. Resizes only the filled part, and only when the share
     * changed.
     *
     * @param share the part done, from 0 to 1
     */
    void setShare(double share) {
        if (share != this.share) {
            this.share = share;
            fill.resize(track.getWidth() * share, track.getHeight());
        }
    }

    @Override
    protected void layoutChildren() {
        Insets insets = getInsets();
        double width = getWidth() - insets.getLeft() - insets.getRight();
        double thickness = track.prefHeight(-1);
        track.resizeRelocate(insets.getLeft(), insets.getTop(), width, thickness);
        fill.resizeRelocate(insets.getLeft(), insets.getTop(), width * share, thickness);
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        return insets.getTop() + track.prefHeight(-1) + insets.getBottom();
    }
}
