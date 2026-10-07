package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Results;
import dev.noahpn.litetype.core.Run;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * The window: one untimed run of stand-in words at a time, typed into a {@link TypingView}.
 * Escape starts a new run. A line of results shows when the run ends, until the results screen
 * exists.
 */
public class LiteTypeApp extends Application {

    private static final String FONT = "/fonts/JetBrainsMono-Regular.ttf";
    private static final int WORD_COUNT = 25;
    private static final int WORD_LINES = 3;
    // What some systems type for Ctrl+Backspace or Delete. Neither is a letter.
    private static final char DELETE = 127;

    private final BorderPane root = new BorderPane();
    private final Label status = new Label();
    private Run run;
    private TypingView view;

    @Override
    public void start(Stage stage) throws IOException {
        loadFont();

        Scene scene = new Scene(root, 1000, 320);
        URL stylesheet = Objects.requireNonNull(
            LiteTypeApp.class.getResource("lite-type.css"),
            "the stylesheet is missing");
        scene.getStylesheets().add(stylesheet.toExternalForm());

        status.getStyleClass().add("status");
        root.setBottom(status);
        newRun();

        scene.setOnKeyTyped(this::keyTyped);
        scene.setOnKeyPressed(this::keyPressed);

        stage.setTitle("lite-type");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Takes every key that makes a character. Control characters, which Enter and Backspace also
     * make, are ignored here and taken from {@link #keyPressed} instead.
     */
    private void keyTyped(KeyEvent event) {
        long now = System.nanoTime();
        String typed = event.getCharacter();

        if (typed.length() != 1 || typed.charAt(0) < ' ' || typed.charAt(0) == DELETE) {
            return;
        }

        run.type(typed.charAt(0), now);
        afterKey();
    }

    /**
     * Takes the keys that make no character: Enter, Backspace, Ctrl+Backspace, and Escape.
     */
    private void keyPressed(KeyEvent event) {
        long now = System.nanoTime();
        KeyCode code = event.getCode();

        if (code == KeyCode.ESCAPE) {
            newRun();
        } else if (code == KeyCode.ENTER) {
            run.type('\n', now);
            afterKey();
        } else if (code == KeyCode.BACK_SPACE) {
            if (event.isControlDown()) {
                run.deleteWord(now);
            } else {
                run.backspace(now);
            }
            afterKey();
        }
    }

    private void afterKey() {
        view.refresh();

        if (run.isFinished() && status.getText().isEmpty()) {
            Results results = Results.of(run);
            status.setText(String.format(
                "%.0f wpm   %.0f%% accuracy   %.0f%% text accuracy",
                results.wpm(),
                results.accuracy() * 100,
                results.textAccuracy() * 100));
        }
    }

    private void newRun() {
        run = Run.untimed(StandInText.words(WORD_COUNT));
        view = new TypingView(run, WORD_LINES);
        root.setCenter(view);
        status.setText("");
    }

    private static void loadFont() throws IOException {
        try (InputStream font = LiteTypeApp.class.getResourceAsStream(FONT)) {
            if (font == null || Font.loadFont(font, 0) == null) {
                throw new IllegalStateException("could not load the font " + FONT);
            }
        }
    }
}
