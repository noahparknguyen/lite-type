package dev.noahpn.litetype.app;

import dev.noahpn.litetype.app.Choices.Mode;
import dev.noahpn.litetype.app.Choices.SnippetSize;
import dev.noahpn.litetype.core.Advance;
import dev.noahpn.litetype.core.Results;
import dev.noahpn.litetype.core.Run;
import dev.noahpn.litetype.core.Separator;
import dev.noahpn.litetype.core.Texts;
import dev.noahpn.litetype.core.Word;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.random.RandomGenerator;

/**
 * The window: the choices bar, then one of three things: the text being typed, with a small
 * number above it, the results of the run that just ended, or the theme page. Picking a choice or
 * pressing Escape starts new text; Shift+Escape starts the same text again.
 */
public class LiteTypeApp extends Application {

    private static final String FONT = "/fonts/JetBrainsMono-Regular.ttf";
    private static final String THEME_KEY = "theme";
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    // What some systems type for Ctrl+Backspace or Delete. Neither is a letter.
    private static final char DELETE = 127;

    /**
     * What fills the window below the bar.
     */
    private enum Showing {
        TEXT, RESULTS, THEMES
    }

    private final Preferences preferences = Preferences.userNodeForPackage(LiteTypeApp.class);
    private final BorderPane root = new BorderPane();
    private final Label progress = new Label();
    private ChoicesBar bar;
    private Choices choices;
    private List<String> themes;
    private String theme;
    private List<String> words;
    private Map<SnippetSize, List<List<Word>>> snippets;
    // Every snippet, for Timed mode, which mixes all sizes.
    private List<List<Word>> allSnippets;
    private long seed;
    private Run run;
    private TypingView view;
    private Showing showing = Showing.TEXT;

    // In Length mode: how many words or lines the text has, and for code, the line each word is
    // on, worked out once per run so the progress number costs nothing per key.
    private int total;
    private int[] lineOfWord;
    private int shownProgress;

    @Override
    public void start(Stage stage) throws IOException {
        loadFont();
        words = Content.words();
        snippets = Content.snippets();
        allSnippets = snippets.values().stream().flatMap(List::stream).toList();

        themes = Themes.names();
        // A saved theme that's since been removed falls back to the default, the first listed.
        theme = preferences.get(THEME_KEY, themes.getFirst());
        if (!themes.contains(theme)) {
            theme = themes.getFirst();
        }

        choices = Choices.load(preferences);
        bar = new ChoicesBar(choices, this::choose, theme, this::openThemes);
        root.setTop(bar);
        root.getStyleClass().add(Themes.styleClass(theme));
        progress.getStyleClass().add("counter");

        // Wide enough for 80 characters of code at the stylesheet's size, plus padding.
        Scene scene = new Scene(root, 1080, 420);
        scene.getStylesheets().add(stylesheet());

        // Every theme is loaded, though only the root's class picks which one colours the window.
        // The theme page needs them all, to draw each box in its own theme.
        for (String name : themes) {
            scene.getStylesheets().add(Themes.stylesheet(name));
        }

        scene.setOnKeyTyped(this::keyTyped);
        scene.setOnKeyPressed(this::keyPressed);

        newRun(newSeed());

        // Every frame tells the run the time, so a timed run ends on its deadline even while no
        // key is pressed, and the countdown moves.
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (showing == Showing.TEXT) {
                    run.tick(System.nanoTime());
                    updateStatus();
                }
            }
        }.start();

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

        if (showing != Showing.TEXT
            || typed.length() != 1
            || typed.charAt(0) < ' '
            || typed.charAt(0) == DELETE) {
            return;
        }

        run.type(typed.charAt(0), now);
        afterKey();
    }

    /**
     * Takes the keys that make no character: Escape, Enter, Backspace, and Ctrl+Backspace.
     */
    private void keyPressed(KeyEvent event) {
        long now = System.nanoTime();
        KeyCode code = event.getCode();

        if (code == KeyCode.ESCAPE) {
            newRun(event.isShiftDown() ? seed : newSeed());
        } else if (showing != Showing.TEXT) {
            return;
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
        updateStatus();
    }

    /**
     * Shows the results once the run has ended. Until then, fades the bar while typing and keeps
     * the number above the text current, setting its text only when the number changes.
     */
    private void updateStatus() {
        if (run.isFinished()) {
            showResults();
            return;
        }

        bar.setTyping(run.isStarted());

        int value = progressValue();
        if (value != shownProgress) {
            shownProgress = value;
            String text = choices.mode() == Mode.TIME
                ? String.valueOf(value)
                : value + " / " + total;
            progress.setText(text);
        }
    }

    /**
     * Returns the number above the text: whole seconds left in Timed mode, rounded up so it reads
     * 0 only when time is up, or the words or lines typed so far in Length mode.
     */
    private int progressValue() {
        if (choices.mode() == Mode.TIME) {
            long nanosLeft = run.timeLeft().orElseThrow().toNanos();
            return (int) ((nanosLeft + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
        }

        int current = run.currentWordIndex();
        return choices.kind() == TextKind.WORDS ? current : lineOfWord[current];
    }

    private void showResults() {
        showing = Showing.RESULTS;
        bar.setTyping(false);
        root.setCenter(new ResultsView(Results.of(run), choices.describe()));
    }

    private void openThemes() {
        showing = Showing.THEMES;
        bar.setTyping(false);
        bar.setThemesOpen(true);
        root.setCenter(new ThemePage(themes, theme, this::pickTheme));
    }

    /**
     * Switches the window to another theme by swapping the root's theme class, which restyles
     * everything once. Happens on a click, never on a key.
     */
    private void pickTheme(String name) {
        root.getStyleClass().remove(Themes.styleClass(theme));
        theme = name;
        root.getStyleClass().add(Themes.styleClass(theme));
        bar.setTheme(theme);
        preferences.put(THEME_KEY, theme);
    }

    private void choose(Choices next) {
        choices = next;
        choices.save(preferences);
        newRun(newSeed());
    }

    /**
     * Starts a run with the current choices. The seed decides the text, so the same seed gives
     * the same text again.
     */
    private void newRun(long seed) {
        this.seed = seed;
        run = makeRun(seed);
        view = new TypingView(run, choices.kind());
        showing = Showing.TEXT;
        bar.setThemesOpen(false);
        shownProgress = -1;

        if (choices.mode() == Mode.LENGTH) {
            countText();
        }

        VBox text = new VBox(progress, view);
        text.getStyleClass().add("typing-area");
        VBox.setVgrow(view, Priority.ALWAYS);
        root.setCenter(text);
        updateStatus();
    }

    private Run makeRun(long seed) {
        Advance advance = choices.kind().advance();
        Duration limit = Duration.ofSeconds(choices.seconds());
        boolean timed = choices.mode() == Mode.TIME;

        return switch (choices.kind()) {
            case WORDS -> timed
                ? Run.timed(Texts.endlessWords(words, seed), limit, advance)
                : Run.untimed(Texts.words(words, choices.words(), seed), advance);
            case CODE -> timed
                ? Run.timed(Texts.endlessSnippets(allSnippets, seed), limit, advance)
                : Run.untimed(Texts.snippet(snippets.get(choices.size()), seed), advance);
        };
    }

    /**
     * Counts a finite text once, at the start of a run: its words, and for code, the line each
     * word is on and how many lines there are.
     */
    private void countText() {
        int count = 0;
        while (run.hasWord(count)) {
            count++;
        }

        if (choices.kind() == TextKind.WORDS) {
            total = count;
            return;
        }

        lineOfWord = new int[count];
        int line = 0;

        for (int i = 0; i < count; i++) {
            lineOfWord[i] = line;
            if (run.word(i).word().separator() == Separator.LINE_BREAK) {
                line++;
            }
        }

        total = line + 1;
    }

    private static long newSeed() {
        return RandomGenerator.getDefault().nextLong();
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
