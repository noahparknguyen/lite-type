package dev.noahpn.litetype.app;

import dev.noahpn.litetype.app.Choices.Mode;
import dev.noahpn.litetype.app.Choices.SnippetSize;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The bar above the text: words or code, Timed or Length, and the length. Picking anything hands
 * the new choices to the app, which starts a fresh run with them. At the end, apart from those,
 * the current theme's name opens the theme page.
 *
 * <p>Its buttons never take keyboard focus. A focused button fires on Space, so after a click,
 * the typist's first Space would press it again. A button asks for focus on a mouse press only
 * when it's focus-traversable, so turning that off keeps every key for the text.
 */
final class ChoicesBar extends HBox {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass TYPING = PseudoClass.getPseudoClass("typing");

    private final Consumer<Choices> onChange;
    private final HBox kinds = group();
    private final HBox modes = group();
    private final HBox lengths = group();
    private final Button theme = new Button();
    private Choices choices;

    /**
     * Creates the bar.
     *
     * @param choices  the choices to show as picked
     * @param onChange called with the new choices whenever the typist picks one
     * @param theme    the theme in use, whose name opens the theme page
     * @param onThemes called when the typist opens the theme page
     */
    ChoicesBar(Choices choices, Consumer<Choices> onChange, String theme, Runnable onThemes) {
        this.onChange = onChange;
        getStyleClass().add("choices-bar");

        this.theme.getStyleClass().add("choice");
        this.theme.setFocusTraversable(false);
        this.theme.setOnAction(event -> onThemes.run());
        HBox themes = group();
        themes.getChildren().add(this.theme);

        getChildren().addAll(kinds, modes, lengths, themes);
        show(choices);
        setTheme(theme);
    }

    /**
     * Shows the name of the theme in use.
     *
     * @param name the theme
     */
    void setTheme(String name) {
        theme.setText(name);
    }

    /**
     * Marks the theme entry while the theme page is open.
     *
     * @param open whether the theme page is showing
     */
    void setThemesOpen(boolean open) {
        theme.pseudoClassStateChanged(SELECTED, open);
    }

    /**
     * Fades the bar out while the typist is typing, and lets it take clicks again otherwise.
     *
     * @param typing whether a run is under way
     */
    void setTyping(boolean typing) {
        pseudoClassStateChanged(TYPING, typing);
        setMouseTransparent(typing);
    }

    /**
     * Shows {@code choices} as picked. The third group's buttons depend on the first two, so all
     * three are made again. This happens on a click, never on a key.
     */
    private void show(Choices choices) {
        this.choices = choices;

        kinds.getChildren().setAll(
            choice("words", choices.kind() == TextKind.WORDS, c -> c.withKind(TextKind.WORDS)),
            choice("code", choices.kind() == TextKind.CODE, c -> c.withKind(TextKind.CODE)));

        modes.getChildren().setAll(
            choice("time", choices.mode() == Mode.TIME, c -> c.withMode(Mode.TIME)),
            choice("length", choices.mode() == Mode.LENGTH, c -> c.withMode(Mode.LENGTH)));

        List<Button> options = new ArrayList<>();

        if (choices.mode() == Mode.TIME) {
            for (int seconds : Choices.SECONDS) {
                boolean picked = seconds == choices.seconds();
                options.add(choice(String.valueOf(seconds), picked, c -> c.withSeconds(seconds)));
            }
        } else if (choices.kind() == TextKind.WORDS) {
            for (int words : Choices.WORD_COUNTS) {
                boolean picked = words == choices.words();
                options.add(choice(String.valueOf(words), picked, c -> c.withWords(words)));
            }
        } else {
            for (SnippetSize size : SnippetSize.values()) {
                boolean picked = size == choices.size();
                String label = size.name().toLowerCase(Locale.ROOT);
                options.add(choice(label, picked, c -> c.withSize(size)));
            }
        }

        lengths.getChildren().setAll(options);
    }

    private Button choice(String label, boolean selected, UnaryOperator<Choices> change) {
        Button button = new Button(label);
        button.getStyleClass().add("choice");
        button.setFocusTraversable(false);
        button.pseudoClassStateChanged(SELECTED, selected);
        button.setOnAction(event -> {
            Choices next = change.apply(choices);
            show(next);
            onChange.accept(next);
        });
        return button;
    }

    private static HBox group() {
        HBox group = new HBox();
        group.getStyleClass().add("choice-group");
        return group;
    }
}
