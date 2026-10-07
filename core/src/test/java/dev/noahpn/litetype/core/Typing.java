package dev.noahpn.litetype.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helpers that let a test build text and type into a run in a line.
 */
final class Typing {

    static final long SECOND = 1_000_000_000L;

    private Typing() {
    }

    /**
     * Splits text into words the way content will: a space or line break ends each word, and the
     * last word has nothing after it.
     */
    static Iterator<Word> text(String text) {
        List<Word> words = new ArrayList<>();
        StringBuilder letters = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c == ' ' || c == '\n') {
                Separator separator = c == ' ' ? Separator.SPACE : Separator.LINE_BREAK;
                words.add(new Word(letters.toString(), separator, 0));
                letters.setLength(0);
            } else {
                letters.append(c);
            }
        }

        words.add(new Word(letters.toString(), Separator.NONE, 0));
        return words.iterator();
    }

    /**
     * Returns text that never runs out: the word "go", over and over.
     */
    static Iterator<Word> endless() {
        return Stream.generate(() -> new Word("go", Separator.SPACE, 0)).iterator();
    }

    /**
     * Presses each key at time zero, reading {@code '\b'} as Backspace.
     */
    static void press(Run run, String keys) {
        for (char key : keys.toCharArray()) {
            if (key == '\b') {
                run.backspace(0);
            } else {
                run.type(key, 0);
            }
        }
    }
}
