package dev.noahpn.litetype.core;

import java.util.Objects;

/**
 * A {@link Word} and what the typist has done to it so far: the letters typed into it, and
 * whether they've moved on from it. Each character's {@link Look} is worked out from those when
 * asked, never stored, so it can't disagree with what was typed.
 *
 * <p>The public methods only read, for the display. The methods that change a typed word are
 * package-private, so only the run, in this package, can call them.
 */
public final class TypedWord {

    /**
     * How many letters can be typed past the end of a word. Holding a key down repeats it, so
     * without a cap one held key would grow a word without limit.
     */
    static final int MAX_EXTRA_LETTERS = 20;

    private final Word word;
    private final StringBuilder input = new StringBuilder();
    private boolean ended;
    private boolean endedByRightKey;

    /**
     * Creates a typed word with nothing typed into it yet.
     *
     * @param word the word to type
     * @throws NullPointerException if {@code word} is null
     */
    TypedWord(Word word) {
        this.word = Objects.requireNonNull(word, "word must not be null");
    }

    /**
     * Returns the word being typed.
     *
     * @return the word
     */
    public Word word() {
        return word;
    }

    /**
     * Returns how many characters the word shows: its own letters, plus any extra letters typed
     * past its end.
     *
     * @return the number of characters shown
     */
    public int length() {
        return Math.max(word.text().length(), input.length());
    }

    /**
     * Returns how many letters have been typed into the word, extra letters included. The cursor
     * sits right after them.
     *
     * @return the number of letters typed
     */
    public int typedLength() {
        return input.length();
    }

    /**
     * Returns the character shown at {@code index}. Within the word it's the word's own letter,
     * even where a wrong one was typed. Past the end it's the extra letter that was typed.
     *
     * @param index the position, from zero up to {@link #length()}, exclusive
     * @return the character shown there
     * @throws IndexOutOfBoundsException if {@code index} is outside the word
     */
    public char charAt(int index) {
        Objects.checkIndex(index, length());
        String text = word.text();
        return index < text.length() ? text.charAt(index) : input.charAt(index);
    }

    /**
     * Returns what the character at {@code index} looks like now. A letter not typed yet is
     * untyped while the typist is still on or before the word, and missed once they've moved on.
     *
     * @param index the position, from zero up to {@link #length()}, exclusive
     * @return the look of the character there
     * @throws IndexOutOfBoundsException if {@code index} is outside the word
     */
    public Look lookAt(int index) {
        Objects.checkIndex(index, length());
        String text = word.text();

        if (index >= input.length()) {
            return ended ? Look.MISSED : Look.UNTYPED;
        }

        if (index >= text.length()) {
            return Look.EXTRA;
        }

        return input.charAt(index) == text.charAt(index) ? Look.CORRECT : Look.WRONG;
    }

    /**
     * Returns what the separator after the word looks like now: untyped until the typist moves
     * on, then correct or wrong depending on the key they moved on with.
     *
     * @return the look of the separator
     */
    public Look separatorLook() {
        if (!ended) {
            return Look.UNTYPED;
        }

        return endedByRightKey ? Look.CORRECT : Look.WRONG;
    }

    /**
     * Returns whether nothing has been typed into the word.
     *
     * @return whether the input is empty
     */
    boolean isEmpty() {
        return input.isEmpty();
    }

    /**
     * Returns whether the typist has moved on from the word.
     *
     * @return whether the word has ended
     */
    boolean isEnded() {
        return ended;
    }

    /**
     * Returns whether the word has taken all the extra letters it can.
     *
     * @return whether another letter would pass the cap
     */
    boolean isFull() {
        return input.length() >= word.text().length() + MAX_EXTRA_LETTERS;
    }

    /**
     * Adds a letter typed into the word. A letter typed past the end never matches.
     *
     * @param letter the letter typed
     * @return whether it matches the word's letter at that position
     * @throws IllegalStateException if the word has ended or is full
     */
    boolean type(char letter) {
        requireOpen();

        if (isFull()) {
            throw new IllegalStateException("word is full: " + word.text());
        }

        int index = input.length();
        input.append(letter);
        String text = word.text();
        return index < text.length() && text.charAt(index) == letter;
    }

    /**
     * Removes the last letter typed.
     *
     * @throws IllegalStateException if the word has ended or nothing has been typed
     */
    void backspace() {
        requireOpen();

        if (input.isEmpty()) {
            throw new IllegalStateException("nothing typed in word: " + word.text());
        }

        input.setLength(input.length() - 1);
    }

    /**
     * Removes every letter typed.
     *
     * @throws IllegalStateException if the word has ended
     */
    void clear() {
        requireOpen();
        input.setLength(0);
    }

    /**
     * Ends the word, because the typist pressed Space or Enter to move on. The key is right only
     * when it's this word's separator and exactly as many letters as the word has were typed. A
     * wrong letter inside the word doesn't make the key wrong: it was pressed in the right place.
     *
     * @param key the key pressed: {@code ' '} for Space or {@code '\n'} for Enter
     * @return whether the key was right
     * @throws IllegalStateException if the word has already ended
     */
    boolean end(char key) {
        requireOpen();
        boolean atWordEnd = input.length() == word.text().length();
        ended = true;
        endedByRightKey = atWordEnd && word.separator().isTypedWith(key);
        return endedByRightKey;
    }

    /**
     * Reopens an ended word, because Backspace stepped back into it. This undoes the key that
     * ended it.
     *
     * @throws IllegalStateException if the word hasn't ended
     */
    void reopen() {
        if (!ended) {
            throw new IllegalStateException("word has not ended: " + word.text());
        }

        ended = false;
        endedByRightKey = false;
    }

    private void requireOpen() {
        if (ended) {
            throw new IllegalStateException("word has ended: " + word.text());
        }
    }
}
