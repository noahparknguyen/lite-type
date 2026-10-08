package dev.noahpn.litetype.app;

/**
 * How a run shows how far it has gone, above the text. A setting: the line suits typing without
 * looking away, and the number suits wanting the exact count.
 */
enum ProgressStyle {

    /**
     * A thin line that fills from the left: time used in Timed mode, or words or lines done in
     * Length mode.
     */
    LINE,

    /**
     * A number: seconds left in Timed mode, or words or lines done out of the total in Length
     * mode.
     */
    NUMBER
}
