package dev.noahpn.litetype.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Builds the text a run types from content: random words, or code snippets back to back. Each
 * source draws from a generator started from a seed, so the same seed always gives the same text.
 * That's how "the same text again" works, even for text that never runs out.
 *
 * <p>The generator is {@link Random}, whose algorithm Java specifies exactly, so a seed gives the
 * same text on every machine and every Java version.
 */
public final class Texts {

    private Texts() {
    }

    /**
     * Returns {@code count} words picked at random, a space after each but the last.
     *
     * @param words the words to pick from; must not be empty
     * @param count how many to pick; must be positive
     * @param seed  where the random picks start
     * @return the words, in order
     * @throws NullPointerException     if {@code words} is null
     * @throws IllegalArgumentException if {@code words} is empty or {@code count} isn't positive
     */
    public static Iterator<Word> words(List<String> words, int count, long seed) {
        requireSome(words, "words");

        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive: " + count);
        }

        Random random = new Random(seed);
        List<Word> picked = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Separator separator = i == count - 1 ? Separator.NONE : Separator.SPACE;
            picked.add(new Word(pick(words, random), separator, 0));
        }

        return picked.iterator();
    }

    /**
     * Returns words picked at random that never run out, a space after each.
     *
     * @param words the words to pick from; must not be empty
     * @param seed  where the random picks start
     * @return an endless supply of words
     * @throws NullPointerException     if {@code words} is null
     * @throws IllegalArgumentException if {@code words} is empty
     */
    public static Iterator<Word> endlessWords(List<String> words, long seed) {
        requireSome(words, "words");
        Random random = new Random(seed);
        return Stream.generate(() -> new Word(pick(words, random), Separator.SPACE, 0)).iterator();
    }

    /**
     * Returns one snippet picked at random, its last word with nothing after it.
     *
     * @param snippets the snippets to pick from, each as its words; must not be empty
     * @param seed     where the random pick starts
     * @return the picked snippet's words, in order
     * @throws NullPointerException     if {@code snippets} is null
     * @throws IllegalArgumentException if {@code snippets} is empty
     */
    public static Iterator<Word> snippet(List<List<Word>> snippets, long seed) {
        requireSome(snippets, "snippets");
        return snippets.get(new Random(seed).nextInt(snippets.size())).iterator();
    }

    /**
     * Returns code snippets back to back, never running out. Each snippet's last line ends with a
     * line break, so the next snippet starts on the next line. The order is shuffled, and no
     * snippet comes back until every one has been used.
     *
     * @param snippets the snippets, each as its words; none may be empty
     * @param seed     where the shuffling starts
     * @return an endless supply of words
     * @throws NullPointerException     if {@code snippets} or any snippet is null
     * @throws IllegalArgumentException if {@code snippets} or any snippet is empty
     */
    public static Iterator<Word> endlessSnippets(List<List<Word>> snippets, long seed) {
        requireSome(snippets, "snippets");
        List<List<Word>> joined = new ArrayList<>(snippets.size());

        for (List<Word> snippet : snippets) {
            requireSome(snippet, "snippet");
            List<Word> words = new ArrayList<>(snippet);
            Word last = words.getLast();
            words.set(words.size() - 1, new Word(last.text(), Separator.LINE_BREAK, last.indent()));
            joined.add(words);
        }

        return new ShuffledSnippets(joined, new Random(seed));
    }

    private static String pick(List<String> words, Random random) {
        return words.get(random.nextInt(words.size()));
    }

    private static void requireSome(List<?> list, String name) {
        Objects.requireNonNull(list, name + " must not be null");

        if (list.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    /**
     * Hands out the words of one snippet after another. The snippets are dealt from a shuffled
     * bag, refilled once empty, so each is used once per round.
     */
    private static final class ShuffledSnippets implements Iterator<Word> {

        private final List<List<Word>> snippets;
        private final Random random;
        private final Deque<Integer> bag = new ArrayDeque<>();
        private Iterator<Word> current = Collections.emptyIterator();
        private int lastSnippet = -1;

        ShuffledSnippets(List<List<Word>> snippets, Random random) {
            this.snippets = snippets;
            this.random = random;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Word next() {
            if (!current.hasNext()) {
                if (bag.isEmpty()) {
                    refill();
                }

                lastSnippet = bag.pop();
                current = snippets.get(lastSnippet).iterator();
            }

            return current.next();
        }

        /**
         * Shuffles every snippet back into the bag. If the new round would start with the snippet
         * that ended the last one, the first two swap, so no snippet comes twice in a row.
         */
        private void refill() {
            List<Integer> order = new ArrayList<>(snippets.size());

            for (int i = 0; i < snippets.size(); i++) {
                order.add(i);
            }

            Collections.shuffle(order, random);

            if (order.size() > 1 && order.getFirst() == lastSnippet) {
                Collections.swap(order, 0, 1);
            }

            bag.addAll(order);
        }
    }
}
