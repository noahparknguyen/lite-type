package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Run;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Shows a run's text a few lines at a time, each letter coloured by its look, with a cursor at
 * the typist's position.
 *
 * <p>The font is monospace, so the view is a grid of cells: a letter's place is its column times
 * the character width, and nothing is measured per key. After each key, {@link #refresh()}
 * restyles only the words a key can touch and places the words in the window again, so its cost
 * depends on the size of the window, never on the length of the text.
 */
final class TypingView extends Pane {

    private static final Duration BLINK = Duration.millis(530);

    private final Run run;
    private final int visibleLines;
    // The words on screen, in order: the first is word firstWord in the run.
    private final List<WordNode> shown = new ArrayList<>();
    // Where each line that scrolled off the top began, latest first, for Backspace to return to.
    private final Deque<Integer> linesAbove = new ArrayDeque<>();
    private final Group words = new Group();
    private final Region cursor = new Region();
    private final Timeline blink;

    private double cellWidth;
    private double lineHeight;
    private int firstWord;
    private int secondLineStart;
    private int cursorLine;
    private int cursorColumn;

    /**
     * Creates a view of a run.
     *
     * @param run          the run to show
     * @param visibleLines how many lines of text show at once
     */
    TypingView(Run run, int visibleLines) {
        this.run = run;
        this.visibleLines = visibleLines;
        getStyleClass().add("typing-view");

        // Positions are set by hand, so the pane never needs to lay these out. The cursor is
        // added last to draw on top.
        words.setManaged(false);
        cursor.getStyleClass().add("cursor");
        cursor.setManaged(false);
        getChildren().addAll(words, cursor);

        blink = new Timeline(new KeyFrame(BLINK, event -> cursor.setVisible(!cursor.isVisible())));
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();

        // A view replaced before its first key would otherwise blink forever, off screen.
        sceneProperty().addListener((property, oldScene, newScene) -> {
            if (newScene == null) {
                blink.stop();
            }
        });

        // Lines wrap at the width, so a new width means new lines. Lines above keep their old
        // breaks, so the trail back to them is dropped.
        widthProperty().addListener((property, oldWidth, newWidth) -> {
            linesAbove.clear();
            refresh();
        });
    }

    /**
     * Brings the view in line with the run. Called after every key.
     */
    void refresh() {
        if (getWidth() <= 0) {
            return;
        }

        if (cellWidth == 0) {
            measure();
        }

        if (run.isStarted() && blink.getStatus() == Animation.Status.RUNNING) {
            blink.stop();
            cursor.setVisible(true);
        }

        int current = run.currentWordIndex();

        // Backspace can step back into a line that has scrolled off the top.
        while (current < firstWord) {
            moveWindowTo(linesAbove.isEmpty() ? current : linesAbove.pop());
        }

        // A key changes only the current word and the words either side of it.
        for (int i = current - 1; i <= current + 1; i++) {
            if (i >= firstWord && i < firstWord + shown.size()) {
                shown.get(i - firstWord).sync();
            }
        }

        placeWords();

        // Reaching the bottom line moves everything up one, dropping the top line.
        while (visibleLines > 1 && (cursorLine < 0 || cursorLine == visibleLines - 1)
            && secondLineStart > firstWord) {
            linesAbove.push(firstWord);
            moveWindowTo(secondLineStart);
            placeWords();
        }

        int typed = run.word(current).typedLength();
        cursor.relocate((cursorColumn + typed) * cellWidth, cursorLine * lineHeight);
    }

    /**
     * Reads the size of one character cell from a letter styled the way the stylesheet says.
     */
    private void measure() {
        Text probe = new Text("M");
        probe.getStyleClass().add("letter");
        getChildren().add(probe);
        probe.applyCss();
        Bounds bounds = probe.getLayoutBounds();
        getChildren().remove(probe);

        cellWidth = bounds.getWidth();
        lineHeight = bounds.getHeight();
        cursor.applyCss();
        cursor.resize(cursor.prefWidth(-1), lineHeight);
    }

    /**
     * Places the words from firstWord on, line by line, until the window is full. Words that no
     * longer fit are taken off screen, and words coming into view are made. Records the cursor's
     * line and the column where its word starts, or line -1 if the word is out of view.
     */
    private void placeWords() {
        int columns = Math.max(1, (int) (getWidth() / cellWidth));
        int current = run.currentWordIndex();
        int line = 0;
        int column = 0;
        int count = 0;

        cursorLine = -1;
        secondLineStart = firstWord;

        for (int i = firstWord; run.hasWord(i); i++, count++) {
            WordNode node = count < shown.size() ? shown.get(count) : add(i);
            int width = node.cells();

            if (column > 0 && column + width > columns) {
                line++;
                column = 0;

                if (line == 1) {
                    secondLineStart = i;
                }
            }

            if (line == visibleLines) {
                break;
            }

            node.setLayoutX(column * cellWidth);
            node.setLayoutY(line * lineHeight);

            if (i == current) {
                cursorLine = line;
                cursorColumn = column;
            }

            // One cell after the letters is the gap, where a wrong separator shows.
            column += width + 1;
        }

        while (shown.size() > count) {
            words.getChildren().remove(shown.removeLast());
        }
    }

    /**
     * Makes word {@code start} the first on screen, taking words off the front or adding them
     * back as needed.
     */
    private void moveWindowTo(int start) {
        while (firstWord < start && !shown.isEmpty()) {
            words.getChildren().remove(shown.removeFirst());
            firstWord++;
        }

        firstWord = Math.max(firstWord, start);

        while (firstWord > start) {
            firstWord--;
            WordNode node = new WordNode(run.word(firstWord), cellWidth);
            shown.addFirst(node);
            words.getChildren().add(node);
        }
    }

    private WordNode add(int index) {
        WordNode node = new WordNode(run.word(index), cellWidth);
        shown.add(node);
        words.getChildren().add(node);
        return node;
    }
}
