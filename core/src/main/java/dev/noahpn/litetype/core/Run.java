package dev.noahpn.litetype.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One run of a typing test: it takes the typist's keys, applies the typing rules, and counts
 * right and wrong keypresses.
 *
 * <p>The run never reads the clock. Every key arrives with the time it was pressed, as a
 * {@link System#nanoTime()} reading, and a timed run also hears the time through {@link #tick}.
 * The clock starts on the first key that changes anything. A timed run ends exactly on its
 * deadline. An untimed run ends on the last letter of its text, right or wrong.
 *
 * <p>The cursor moves through the text by word or by character, as the run's {@link Advance}
 * says.
 *
 * <p>Words are taken from the text only as they're needed, so the text can be endless. The run
 * isn't safe to share between threads; it's built to be called from one.
 */
public final class Run {

    private final Iterator<Word> source;
    private final List<TypedWord> words = new ArrayList<>();
    // Null for an untimed run.
    private final Duration limit;
    private final Advance advance;

    private int current;
    private int correctKeys;
    private int wrongKeys;

    private boolean started;
    private long startNanos;
    private long latestNanos;
    private boolean finished;
    private long endNanos;

    private Run(Iterator<Word> source, Duration limit, Advance advance) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.limit = limit;
        this.advance = Objects.requireNonNull(advance, "advance must not be null");

        if (!hasWord(0)) {
            throw new IllegalArgumentException("source must hold at least one word");
        }
    }

    /**
     * Creates a run that ends on the last letter of its text, moving by word.
     *
     * @param source the words to type, in order; must hold at least one
     * @return the run, waiting for its first key
     * @throws NullPointerException     if {@code source} is null
     * @throws IllegalArgumentException if {@code source} holds no words
     */
    public static Run untimed(Iterator<Word> source) {
        return untimed(source, Advance.BY_WORD);
    }

    /**
     * Creates a run that ends on the last letter of its text.
     *
     * @param source  the words to type, in order; must hold at least one
     * @param advance how the cursor moves through the text
     * @return the run, waiting for its first key
     * @throws NullPointerException     if {@code source} or {@code advance} is null
     * @throws IllegalArgumentException if {@code source} holds no words
     */
    public static Run untimed(Iterator<Word> source, Advance advance) {
        return new Run(source, null, advance);
    }

    /**
     * Creates a run that ends when {@code limit} has passed since its first key, moving by word.
     *
     * @param source the words to type, in order; normally endless
     * @param limit  how long the run lasts; must be positive
     * @return the run, waiting for its first key
     * @throws NullPointerException     if {@code source} or {@code limit} is null
     * @throws IllegalArgumentException if {@code source} holds no words or {@code limit} is not
     *                                  positive
     */
    public static Run timed(Iterator<Word> source, Duration limit) {
        return timed(source, limit, Advance.BY_WORD);
    }

    /**
     * Creates a run that ends when {@code limit} has passed since its first key.
     *
     * @param source  the words to type, in order; normally endless
     * @param limit   how long the run lasts; must be positive
     * @param advance how the cursor moves through the text
     * @return the run, waiting for its first key
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if {@code source} holds no words or {@code limit} is not
     *                                  positive
     */
    public static Run timed(Iterator<Word> source, Duration limit, Advance advance) {
        Objects.requireNonNull(limit, "limit must not be null");

        if (!limit.isPositive()) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }

        return new Run(source, limit, advance);
    }

    /**
     * Takes a key that makes a character, with Enter given as {@code '\n'}. Space or Enter on an
     * empty word is ignored, and so is Tab.
     *
     * <p>Moving by word, Space ends the current word, and so does Enter. In the last word they're
     * ordinary letters, since there's no next word to move to. A letter past the extra-letter cap
     * is ignored. Moving by character, every key fills exactly the next position, right or
     * wrong.
     *
     * @param key       the character the key made
     * @param timeNanos when it was pressed
     */
    public void type(char key, long timeNanos) {
        advanceTo(timeNanos);

        if (finished || key == '\t') {
            return;
        }

        TypedWord word = words.get(current);
        boolean separator = key == ' ' || key == '\n';

        // Neither key can be a word's first character, so this never ignores a right key.
        if (separator && word.isEmpty()) {
            return;
        }

        if (advance == Advance.BY_CHARACTER) {
            typeByCharacter(word, key, timeNanos);
            return;
        }

        if (separator && hasWord(current + 1)) {
            startAt(timeNanos);
            count(word.end(key));
            current++;
            return;
        }

        if (word.isFull()) {
            return;
        }

        startAt(timeNanos);
        count(word.type(key));
        finishIfLastLetter(word, timeNanos);
    }

    /**
     * Takes Backspace: removes the last letter of the current word, or, if it's empty, steps back
     * into the previous word, undoing the key that ended it. Backspace is never counted.
     *
     * @param timeNanos when it was pressed
     */
    public void backspace(long timeNanos) {
        advanceTo(timeNanos);

        if (finished) {
            return;
        }

        TypedWord word = words.get(current);

        if (!word.isEmpty()) {
            word.backspace();
        } else if (current > 0) {
            current--;
            words.get(current).reopen();
        }
    }

    /**
     * Takes Ctrl+Backspace: clears the current word, or, if it's empty, steps back into the
     * previous word and clears that. Like Backspace, it's never counted.
     *
     * @param timeNanos when it was pressed
     */
    public void deleteWord(long timeNanos) {
        advanceTo(timeNanos);

        if (finished) {
            return;
        }

        TypedWord word = words.get(current);

        if (!word.isEmpty()) {
            word.clear();
        } else if (current > 0) {
            current--;
            TypedWord previous = words.get(current);
            previous.reopen();
            previous.clear();
        }
    }

    /**
     * Tells the run the time without a key, so a timed run can end on its deadline while the
     * typist isn't typing.
     *
     * @param timeNanos the time now
     */
    public void tick(long timeNanos) {
        advanceTo(timeNanos);
    }

    /**
     * Returns whether the text has a word at {@code index}, taking words from it as needed.
     *
     * @param index the position of the word
     * @return whether there's a word there
     */
    public boolean hasWord(int index) {
        while (words.size() <= index && source.hasNext()) {
            words.add(new TypedWord(source.next()));
        }

        return index >= 0 && index < words.size();
    }

    /**
     * Returns the word at {@code index}, taking words from the text as needed.
     *
     * @param index the position of the word
     * @return the word
     * @throws IndexOutOfBoundsException if the text has no word there
     */
    public TypedWord word(int index) {
        if (!hasWord(index)) {
            throw new IndexOutOfBoundsException("no word at index " + index);
        }

        return words.get(index);
    }

    /**
     * Returns the position of the word the typist is on.
     *
     * @return the current word's index
     */
    public int currentWordIndex() {
        return current;
    }

    /**
     * Returns whether the clock has started: whether a key has changed anything yet.
     *
     * @return whether the run has started
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Returns whether the run has ended.
     *
     * @return whether the run is finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Returns how long a timed run has left, as of the latest time it was given. That's the whole
     * limit before the first key, and zero once the run has ended.
     *
     * @return the time left, or empty for an untimed run
     */
    public Optional<Duration> timeLeft() {
        if (limit == null) {
            return Optional.empty();
        }

        if (!started) {
            return Optional.of(limit);
        }

        if (finished) {
            return Optional.of(Duration.ZERO);
        }

        return Optional.of(limit.minusNanos(latestNanos - startNanos));
    }

    /**
     * Returns how long the finished run took: exactly the limit for a timed run that reached its
     * deadline, otherwise the time from the first key to the last.
     *
     * @return the time the run took
     * @throws IllegalStateException if the run hasn't finished
     */
    Duration elapsed() {
        if (!finished) {
            throw new IllegalStateException("run has not finished");
        }

        return Duration.ofNanos(endNanos - startNanos);
    }

    /**
     * Returns how many keypresses were right.
     *
     * @return the count of right keypresses
     */
    int correctKeys() {
        return correctKeys;
    }

    /**
     * Returns how many keypresses were wrong.
     *
     * @return the count of wrong keypresses
     */
    int wrongKeys() {
        return wrongKeys;
    }

    /**
     * Applies a key moving by character: inside a word it's typed in, right or wrong, and at a
     * word's end it ends the word, right only if it's that word's separator. Either way the
     * cursor moves one position, so letters can never pile up past a word's end.
     */
    private void typeByCharacter(TypedWord word, char key, long timeNanos) {
        startAt(timeNanos);

        if (word.typedLength() < word.word().text().length()) {
            count(word.type(key));
            finishIfLastLetter(word, timeNanos);
        } else if (hasWord(current + 1)) {
            count(word.end(key));
            current++;
        }
    }

    private void finishIfLastLetter(TypedWord word, long timeNanos) {
        boolean lastWord = !hasWord(current + 1);
        if (lastWord && word.typedLength() == word.word().text().length()) {
            finish(timeNanos);
        }
    }

    private void advanceTo(long timeNanos) {
        if (finished) {
            return;
        }

        latestNanos = timeNanos;

        // nanoTime readings are compared by subtracting, as its documentation requires.
        if (started && limit != null && timeNanos - startNanos >= limit.toNanos()) {
            finish(startNanos + limit.toNanos());
        }
    }

    private void startAt(long timeNanos) {
        if (!started) {
            started = true;
            startNanos = timeNanos;
        }
    }

    private void count(boolean right) {
        if (right) {
            correctKeys++;
        } else {
            wrongKeys++;
        }
    }

    private void finish(long timeNanos) {
        finished = true;
        endNanos = timeNanos;
    }
}
