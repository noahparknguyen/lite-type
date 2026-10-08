package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Results;
import dev.noahpn.litetype.core.Run;
import dev.noahpn.litetype.core.Word;
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
import java.util.Iterator;
import java.util.Objects;

/**
 * The window: one untimed run of stand-in text at a time, typed into a {@link TypingView}.
 * Escape starts a new run, and Tab switches between words and code. A line of results shows
 * when the run ends. Tab and the results line last until the screens exist.
 */
public class LiteTypeApp extends Application {

    private static final String FONT = "/fonts/JetBrainsMono-Regular.ttf";
    private static final int WORD_COUNT = 25;
    // What some systems type for Ctrl+Backspace or Delete. Neither is a letter.
    private static final char DELETE = 127;

    private final BorderPane root = new BorderPane();
    private final Label status = new Label();
    private TextKind kind = TextKind.WORDS;
    private Run run;
    private TypingView view;

    @Override
    public void start(Stage stage) throws IOException {
        loadFont();

        // Wide enough for 80 characters of code at the stylesheet's size, plus padding.
        Scene scene = new Scene(root, 1080, 360);
        scene.getStylesheets().add(stylesheet());

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
     * Takes the keys that make no character: Enter, Backspace, Ctrl+Backspace, Escape, and Tab.
     */
    private void keyPressed(KeyEvent event) {
        long now = System.nanoTime();
        KeyCode code = event.getCode();

        if (code == KeyCode.ESCAPE) {
            newRun();
        } else if (code == KeyCode.TAB) {
            kind = kind == TextKind.WORDS ? TextKind.CODE : TextKind.WORDS;
            newRun();
            event.consume();
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
        Iterator<Word> text = kind == TextKind.WORDS
            ? StandInText.words(WORD_COUNT)
            : StandInText.snippet();
        run = Run.untimed(text, kind.advance());
        view = new TypingView(run, kind);
        root.setCenter(view);
        status.setText("");
    }

    /**
     * Loads the bundled font, so the stylesheet can name it. Package-private for the
     * measurement, which sets up the display the same way.
     *
     * @throws IOException           if the font file can't be read
     * @throws IllegalStateException if the font is missing or won't load
     */
    static void loadFont() throws IOException {
        try (InputStream font = LiteTypeApp.class.getResourceAsStream(FONT)) {
            if (font == null || Font.loadFont(font, 0) == null) {
                throw new IllegalStateException("could not load the font " + FONT);
            }
        }
    }

    /**
     * Returns the stylesheet's address, for a scene to load. Package-private for the
     * measurement.
     *
     * @return the stylesheet's URL, as a string
     * @throws NullPointerException if the stylesheet is missing
     */
    static String stylesheet() {
        URL stylesheet = Objects.requireNonNull(
            LiteTypeApp.class.getResource("lite-type.css"),
            "the stylesheet is missing");
        return stylesheet.toExternalForm();
    }
}
