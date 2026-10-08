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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
 * The window: its own title bar, the choices bar, then one of four things: the text being typed,
 * with its progress above it, the results of the run that just ended, the theme page, or the
 * settings page. All of it below the title bar sits in one column, centred in the window. Picking
 * a choice or pressing Escape starts new text; Shift+Escape starts the same text again.
 */
public class LiteTypeApp extends Application {

    private static final String FONT = "/fonts/JetBrainsMono-Regular.ttf";
    // The window's icon sizes. The system picks what it needs: 16 and 32 at normal scaling, 24 and
    // 48 at 150% and 200%. WSLg scales the largest for the taskbar, so 96 keeps it sharp there.
    static final int[] ICON_SIZES = {16, 24, 32, 48, 96};
    private static final String THEME_KEY = "theme";
    private static final String TEXT_SIZE_KEY = "textSize";
    private static final String PROGRESS_KEY = "progress";
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    // What some systems type for Ctrl+Backspace or Delete. Neither is a letter.
    private static final char DELETE = 127;
    // Small enough for half a laptop screen, big enough that the bar always fits in one row.
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 480;

    /**
     * What fills the window below the bar.
     */
    private enum Showing {
        TEXT, RESULTS, THEMES, SETTINGS
    }

    private final Preferences preferences = Preferences.userNodeForPackage(LiteTypeApp.class);
    // The scene's root: the title bar on top, the content below. It carries the theme's class, so
    // both are in the theme's colours.
    private final BorderPane window = new BorderPane();
    // Everything below the title bar: the choices bar, the page, and the spacer, padded in from
    // the window's edges.
    private final BorderPane content = new BorderPane();
    private final Label progressNumber = new Label();
    private ChoicesBar bar;
    private Choices choices;
    private List<String> themes;
    private String theme;
    private TextSize textSize;
    private ProgressStyle progressStyle;
    private double surroundsSize;
    private List<String> words;
    private Map<SnippetSize, List<List<Word>>> snippets;
    // Every snippet, for Timed mode, which mixes all sizes.
    private List<List<Word>> allSnippets;
    private long seed;
    private Run run;
    private TypingView view;
    private ProgressLine progressLine;
    private Showing showing = Showing.TEXT;

    // In Length mode: how many words or lines the text has, and for code, the line each word is
    // on, worked out once per run so showing progress costs nothing per key.
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

        textSize = loadSetting(TEXT_SIZE_KEY, TextSize.FIT);
        progressStyle = loadSetting(PROGRESS_KEY, ProgressStyle.LINE);

        choices = Choices.load(preferences);
        bar = new ChoicesBar(choices, this::choose, theme, this::openThemes, this::openSettings);
        BorderPane.setAlignment(bar, Pos.TOP_CENTER);
        content.setTop(bar);
        content.getStyleClass().add("content");
        window.getStyleClass().add(Themes.styleClass(theme));
        progressNumber.getStyleClass().add("counter");

        // As tall as the bar, at the bottom, so the space between them is centred below the title
        // bar, and the test centred in that space sits in the middle of the content.
        Region spacer = new Region();
        spacer.prefHeightProperty().bind(bar.heightProperty());
        content.setBottom(spacer);

        window.setTop(new TitleBar());
        window.setCenter(content);

        // The window's own size is set by WindowPlace, so the scene takes whatever it's given. The
        // content's size is only known once the window lays it out below the title bar, so sizes
        // are worked out when it changes, not when the scene does.
        Scene scene = new Scene(window);
        scene.getStylesheets().add(stylesheet());
        content.widthProperty().addListener((property, oldWidth, newWidth) -> applySizes());
        content.heightProperty().addListener((property, oldHeight, newHeight) -> applySizes());

        // Every theme is loaded, though only the window's class picks which one colours it.
        // The theme page needs them all, to draw each box in its own theme.
        for (String name : themes) {
            scene.getStylesheets().add(Themes.stylesheet(name));
        }

        scene.setOnKeyTyped(this::keyTyped);
        scene.setOnKeyPressed(this::keyPressed);

        newRun(newSeed());

        // Every frame tells the run the time, so a timed run ends on its deadline even while no
        // key is pressed, and the countdown moves. It also moves the progress line a frame's
        // worth, which does nothing when the number is in use or the line has arrived.
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (showing == Showing.TEXT) {
                    run.tick(System.nanoTime());
                    updateStatus();
                    progressLine.step();
                }
            }
        }.start();

        stage.setTitle("lite-type");
        addIcons(stage);
        // The app draws its own title bar, so the system's buttons are switched off. The system
        // keeps the frame: resizing, snapping, and the shadow.
        stage.initStyle(StageStyle.EXTENDED);
        HeaderBar.setSystemButtonHeight(stage, 0);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        WindowPlace.restore(stage, preferences);
        stage.setOnHiding(event -> WindowPlace.save(stage, preferences));
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
     * the progress above the text current: the line by its share, or the number's text, set only
     * when the number changes.
     */
    private void updateStatus() {
        if (run.isFinished()) {
            showResults();
            return;
        }

        bar.setTyping(run.isStarted());

        if (progressStyle == ProgressStyle.LINE) {
            progressLine.setShare(progressShare());
            return;
        }

        int value = progressValue();
        if (value != shownProgress) {
            shownProgress = value;
            String text = choices.mode() == Mode.TIME
                ? String.valueOf(value)
                : value + " / " + total;
            progressNumber.setText(text);
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

    /**
     * Returns how much of the line is filled, from 0 to 1: the time used in Timed mode, to the
     * nanosecond so it fills smoothly, or the share of words or lines typed in Length mode.
     */
    private double progressShare() {
        if (choices.mode() == Mode.TIME) {
            double limitNanos = choices.seconds() * (double) NANOS_PER_SECOND;
            return 1 - run.timeLeft().orElseThrow().toNanos() / limitNanos;
        }

        return (double) progressValue() / total;
    }

    /**
     * Returns what shows the progress above the text, by the setting: the line or the number.
     */
    private Region progressView() {
        return progressStyle == ProgressStyle.LINE ? progressLine : progressNumber;
    }

    /**
     * Shows the results where the text was. A timed run's description already says its time, so
     * only a run that ends with its text adds how long it took, in whole seconds.
     */
    private void showResults() {
        showing = Showing.RESULTS;
        bar.setTyping(false);
        Results results = Results.of(run);
        String description = choices.describe();

        if (choices.mode() == Mode.LENGTH) {
            description += " · " + Math.round(results.elapsed().toMillis() / 1000.0) + " s";
        }

        showCentre(new ResultsView(results, description));
    }

    private void openThemes() {
        showing = Showing.THEMES;
        bar.setTyping(false);
        bar.setThemesOpen(true);
        showCentre(new ThemePage(themes, theme, this::pickTheme));
    }

    private void openSettings() {
        showing = Showing.SETTINGS;
        bar.setTyping(false);
        bar.setSettingsOpen(true);
        showCentre(new SettingsPage(
            textSize, this::pickTextSize, progressStyle, this::pickProgress));
    }

    private void pickTextSize(TextSize size) {
        textSize = size;
        preferences.put(TEXT_SIZE_KEY, size.name());
        applySizes();
    }

    /**
     * Saves the progress style. It shows from the next run, which leaving the page starts.
     */
    private void pickProgress(ProgressStyle style) {
        progressStyle = style;
        preferences.put(PROGRESS_KEY, style.name());
    }

    /**
     * Reads a setting saved by name. A missing one, or one whose value no longer exists, falls
     * back to the default.
     */
    private <E extends Enum<E>> E loadSetting(String key, E fallback) {
        try {
            String saved = preferences.get(key, fallback.name());
            return Enum.valueOf(fallback.getDeclaringClass(), saved);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Puts a page in the column, below the bar.
     */
    private void showCentre(Region page) {
        BorderPane.setAlignment(page, Pos.TOP_CENTER);
        content.setCenter(page);
        applySizes();
    }

    /**
     * Sizes everything from the window and the text size setting: the typing text, the bar and
     * everything else around it, and the column. The column holds a full line of code, but is
     * never narrower than the bar. Runs when the window, the page, or the setting changes, never
     * on a key.
     */
    private void applySizes() {
        Insets padding = content.getInsets();
        double width = content.getWidth() - padding.getLeft() - padding.getRight();
        double height = content.getHeight() - padding.getTop() - padding.getBottom();

        if (width <= 0 || height <= 0) {
            return;
        }

        // The bar, the spacer as tall as it, and the progress above the text take height from the
        // text. They're sized in em, so together they're a fixed number of pixels tall for each
        // pixel of the base size, measured here at the current one. The first time, there's no
        // base size yet, so it starts from what the text alone would allow. Off screen, the
        // progress keeps the size it last had, which is close enough: showing the text runs this
        // again.
        if (surroundsSize == 0) {
            setSurrounds(textSize.codeSize(width, height, 0));
        }

        // Styles normally apply just before a frame is drawn. They're applied now, so the bar and
        // the progress are measured as they'll be drawn: at the current base size, and with any
        // buttons a choice just rebuilt, which have no styles yet.
        bar.applyCss();
        Region progress = progressView();
        progress.applyCss();
        double surroundsShare = (2 * bar.prefHeight(-1) + progress.prefHeight(-1)) / surroundsSize;
        double code = textSize.codeSize(width, height, surroundsShare);
        setSurrounds(code);
        bar.applyCss();

        double barWidth = Math.min(bar.prefWidth(-1), width);
        double column = Math.max(TextSize.columnWidth(code, width), barWidth);

        bar.setMaxWidth(column);
        if (content.getCenter() instanceof Region page) {
            page.setMaxWidth(column);
        }

        // Only the text on screen is resized. A page replaces it, and the next run makes a new
        // view, which gets the current size when it's shown.
        if (showing == Showing.TEXT) {
            double size = choices.kind() == TextKind.WORDS
                ? Math.round(code * TextSize.WORDS_SCALE)
                : code;
            view.setFontSize(size);
        }
    }

    /**
     * Sets the size the bar, the counter, the results, and the pages are sized from in em, from
     * the code size. Only set when it changes, since it restyles the whole window.
     */
    private void setSurrounds(double code) {
        double surrounds = TextSize.surroundsSize(code);
        if (surrounds != surroundsSize) {
            surroundsSize = surrounds;
            content.setStyle("-fx-font-size: " + surrounds + "px;");
        }
    }

    /**
     * Switches the window to another theme by swapping its theme class, which restyles
     * everything once, the title bar included. Happens on a click, never on a key.
     */
    private void pickTheme(String name) {
        window.getStyleClass().remove(Themes.styleClass(theme));
        theme = name;
        window.getStyleClass().add(Themes.styleClass(theme));
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
        progressLine = new ProgressLine();
        showing = Showing.TEXT;
        bar.setThemesOpen(false);
        bar.setSettingsOpen(false);
        shownProgress = -1;

        if (choices.mode() == Mode.LENGTH) {
            countText();
        }

        VBox text = new VBox(progressView(), view);
        text.getStyleClass().add("typing-area");
        showCentre(text);
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
     * Gives the window its icon in every size. A missing one is skipped rather than stopping the
     * app over an icon: {@code IconsTest} fails the build if any is missing.
     */
    private static void addIcons(Stage stage) {
        for (int size : ICON_SIZES) {
            URL icon = LiteTypeApp.class.getResource(iconPath(size));
            if (icon != null) {
                stage.getIcons().add(new Image(icon.toExternalForm()));
            }
        }
    }

    /**
     * Returns where the icon of one size is, among the resources. Package-private for the test.
     *
     * @param size the icon's width and height in pixels
     * @return the icon's resource path
     */
    static String iconPath(int size) {
        return "/icons/lite-type-" + size + ".png";
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
