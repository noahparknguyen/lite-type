package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Look;
import dev.noahpn.litetype.core.Separator;
import dev.noahpn.litetype.core.TypedWord;
import javafx.css.PseudoClass;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.text.Text;

import java.util.Locale;

/**
 * One word on screen: a {@link Text} per letter, then a mark for the separator after it. Each
 * letter is placed one cell after the last, and its look is a CSS pseudo-class, such as
 * {@code :correct}, which the stylesheet colours.
 */
final class WordNode extends Group {

    private static final Look[] LOOKS = Look.values();
    private static final PseudoClass[] LOOK_CLASSES = new PseudoClass[LOOKS.length];
    private static final PseudoClass WRONG = PseudoClass.getPseudoClass("wrong");

    static {
        for (Look look : LOOKS) {
            String name = look.name().toLowerCase(Locale.ROOT);
            LOOK_CLASSES[look.ordinal()] = PseudoClass.getPseudoClass(name);
        }
    }

    private final TypedWord word;
    private final double cellWidth;
    private final Text separator;

    /**
     * Creates the node for a word, with every letter in its current look.
     *
     * @param word      the word to show
     * @param cellWidth the width of one character
     */
    WordNode(TypedWord word, double cellWidth) {
        this.word = word;
        this.cellWidth = cellWidth;

        Separator kind = word.word().separator();
        String mark = switch (kind) {
            case SPACE -> "_";
            case LINE_BREAK -> "⏎";
            case NONE -> "";
        };
        separator = new Text(mark);
        separator.getStyleClass().add("separator");
        if (kind == Separator.LINE_BREAK) {
            separator.getStyleClass().add("line-break");
        }
        separator.setTextOrigin(VPos.TOP);
        getChildren().add(separator);

        sync();
    }

    /**
     * Returns how many cells the word's letters take, extra letters included.
     *
     * @return the word's width in characters
     */
    int cells() {
        return word.length();
    }

    /**
     * Brings the node in line with the word: adds or removes letter nodes when extra letters
     * came or went, and sets every letter's look.
     */
    void sync() {
        int length = word.length();
        int shown = getChildren().size() - 1;

        while (shown < length) {
            getChildren().add(shown, letter(shown));
            shown++;
        }

        while (shown > length) {
            shown--;
            getChildren().remove(shown);
        }

        int textLength = word.word().text().length();

        for (int i = 0; i < length; i++) {
            Text letter = (Text) getChildren().get(i);

            // An extra letter can be replaced by a different one between two syncs.
            if (i >= textLength && letter.getText().charAt(0) != word.charAt(i)) {
                letter.setText(String.valueOf(word.charAt(i)));
            }

            setLook(letter, word.lookAt(i));
        }

        separator.setLayoutX(length * cellWidth);
        separator.pseudoClassStateChanged(WRONG, word.separatorLook() == Look.WRONG);
    }

    private Text letter(int index) {
        Text letter = new Text(String.valueOf(word.charAt(index)));
        letter.getStyleClass().add("letter");
        letter.setTextOrigin(VPos.TOP);
        letter.setLayoutX(index * cellWidth);
        return letter;
    }

    private static void setLook(Text letter, Look look) {
        for (Look each : LOOKS) {
            letter.pseudoClassStateChanged(LOOK_CLASSES[each.ordinal()], each == look);
        }
    }
}
