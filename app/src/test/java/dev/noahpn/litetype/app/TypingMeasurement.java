package dev.noahpn.litetype.app;

import dev.noahpn.litetype.core.Run;
import dev.noahpn.litetype.core.Separator;
import dev.noahpn.litetype.core.Texts;
import dev.noahpn.litetype.core.Word;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures the hot path: a scripted typist presses one key per frame, about 720 WPM, into a real
 * {@link TypingView}, and every key's time in our code and every gap between frames is recorded.
 * It prints a report and saves it, with screenshots, to {@code app/target/measure/}.
 *
 * <p>It opens a window, so its name doesn't end in "Test" and a normal build skips it. Run it by
 * name:
 *
 * <pre>
 * ./mvnw -pl app -am test -Dtest=TypingMeasurement -Dsurefire.failIfNoSpecifiedTests=false
 * </pre>
 *
 * <p>Add {@code -DargLine=-Xlog:gc} to see garbage-collection pauses. Both kinds of text run in one
 * JVM, so the second runs on code the JIT has already compiled: compare runs in the same order.
 * It fails only if a run doesn't finish or the median key breaks the 1 ms budget, since the
 * slowest keys depend on the machine as much as the code.
 */
class TypingMeasurement {

    private static final Path OUT = Path.of("target", "measure");
    // A fixed seed types the same words every time, so one measurement compares with the next.
    private static final long SEED = 1;
    private static final int CODE_WORDS = 400;
    private static final double WORDS_SIZE = 28;
    private static final double CODE_SIZE = 20;
    private static final int SETTLE_FRAMES = 30;
    private static final int WARM_UP_KEYS = 100;
    private static final long LATE_FRAME_NANOS = 20_000_000;
    private static final long KEY_BUDGET_NANOS = 1_000_000;

    /**
     * The keys to press, with {@code '\b'} as Backspace, and the key after which to take the
     * mid-run screenshot.
     */
    private record Script(List<Character> keys, int screenshotAfter) {
    }

    @BeforeAll
    static void startJavaFx() throws Exception {
        Files.createDirectories(OUT);
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();

        // Each measurement closes its window, and by default JavaFX shuts down with the last one.
        Platform.setImplicitExit(false);

        CompletableFuture<Void> loaded = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                LiteTypeApp.loadFont();
                loaded.complete(null);
            } catch (IOException | RuntimeException e) {
                loaded.completeExceptionally(e);
            }
        });
        loaded.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void words() throws Exception {
        Run run = Run.untimed(Texts.words(Content.words(), 150, SEED), TextKind.WORDS.advance());
        measure(TextKind.WORDS, run);
    }

    @Test
    void code() throws Exception {
        // The shipped snippets back to back, cut to a fixed length so the run has an end.
        List<List<Word>> snippets = Content.snippets().values().stream()
            .flatMap(List::stream)
            .toList();
        Iterator<Word> endless = Texts.endlessSnippets(snippets, SEED);
        List<Word> words = new ArrayList<>();

        for (int i = 0; i < CODE_WORDS; i++) {
            words.add(endless.next());
        }

        Word last = words.getLast();
        words.set(words.size() - 1, new Word(last.text(), Separator.NONE, last.indent()));
        measure(TextKind.CODE, Run.untimed(words.iterator(), TextKind.CODE.advance()));
    }

    private static void measure(TextKind kind, Run run) throws Exception {
        Script script = script(kind, run);
        List<Character> keys = script.keys();
        long[] keyNanos = new long[keys.size()];
        List<Long> frameGaps = new ArrayList<>();
        CompletableFuture<Void> done = new CompletableFuture<>();
        String theme = Themes.names().getFirst();

        Platform.runLater(() -> {
            // Fixed sizes and the default theme, so every measurement compares with the last.
            TypingView view = new TypingView(run, kind);
            view.setFontSize(kind == TextKind.WORDS ? WORDS_SIZE : CODE_SIZE);
            BorderPane root = new BorderPane(view);
            Scene scene = new Scene(root, 1080, 360);
            scene.getStylesheets().addAll(LiteTypeApp.stylesheet(), Themes.stylesheet(theme));
            root.getStyleClass().add(Themes.styleClass(theme));
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();

            new AnimationTimer() {
                private int frame;
                private int next;
                private long last;
                private boolean skipGap;

                @Override
                public void handle(long now) {
                    frame++;

                    // A screenshot slows the frame it's taken in, so that gap isn't counted.
                    if (frame > SETTLE_FRAMES && !skipGap) {
                        frameGaps.add(now - last);
                    }

                    last = now;
                    skipGap = false;

                    if (frame <= SETTLE_FRAMES) {
                        return;
                    }

                    if (next == keys.size()) {
                        stop();
                        save(scene, kind + "-end.png");
                        stage.close();
                        done.complete(null);
                        return;
                    }

                    char key = keys.get(next);
                    long start = System.nanoTime();

                    if (key == '\b') {
                        run.backspace(start);
                    } else {
                        run.type(key, start);
                    }

                    view.refresh();
                    keyNanos[next] = System.nanoTime() - start;

                    if (next == script.screenshotAfter()) {
                        save(scene, kind + "-middle.png");
                        skipGap = true;
                    }

                    next++;
                }
            }.start();
        });

        done.get(2, TimeUnit.MINUTES);
        long median = report(kind, run, keyNanos, frameGaps);

        assertTrue(run.isFinished(), kind + ": the scripted typist didn't finish the text");
        assertTrue(median < KEY_BUDGET_NANOS, kind + ": the median key took over 1 ms");
    }

    /**
     * Writes the keys a typist would press for the whole text, with mistakes of every kind the
     * typing rules handle, so each path through the code is measured.
     */
    private static Script script(TextKind kind, Run run) {
        List<Character> keys = new ArrayList<>();
        int screenshotAfter = -1;

        for (int w = 0; run.hasWord(w); w++) {
            Word word = run.word(w).word();
            String text = word.text();
            boolean wrongEnding = false;

            for (int i = 0; i < text.length(); i++) {
                if (kind == TextKind.WORDS && w % 13 == 5 && i == 2 && text.length() > 3) {
                    break; // An early space, skipping the rest of the word.
                }

                if (w % 5 == 2 && i == 0) {
                    keys.add('#');
                    keys.add('\b'); // A mistake fixed at once.
                }

                if (kind == TextKind.CODE && w % 7 == 3 && i == 1) {
                    keys.add('#'); // A mistake left in, in place of a letter.
                    continue;
                }

                keys.add(text.charAt(i));
            }

            if (kind == TextKind.WORDS && w % 11 == 4) {
                keys.add('x');
                keys.add('y');
                keys.add('\b');
                keys.add('\b'); // Extra letters, then removed.
            }

            if (kind == TextKind.WORDS && w % 17 == 8) {
                keys.add('q'); // An extra letter left in.
            }

            if (kind == TextKind.CODE && w % 11 == 4 && word.separator() != Separator.NONE) {
                keys.add('z'); // A wrong key in place of a space or line break.
                wrongEnding = true;
            }

            if (screenshotAfter < 0 && keys.size() > 60) {
                screenshotAfter = keys.size() - 1;
            }

            if (!wrongEnding && word.separator() == Separator.SPACE) {
                keys.add(' ');
            } else if (!wrongEnding && word.separator() == Separator.LINE_BREAK) {
                keys.add('\n');
            }
        }

        return new Script(keys, screenshotAfter);
    }

    /**
     * Prints the measurements and saves them beside the screenshots.
     *
     * @return the median time a key took in our code, after the warm-up keys
     */
    private static long report(TextKind kind, Run run, long[] keyNanos, List<Long> frameGaps)
        throws IOException {
        long[] keys = Arrays.copyOfRange(keyNanos, WARM_UP_KEYS, keyNanos.length);
        Arrays.sort(keys);
        long[] frames = frameGaps.stream().mapToLong(Long::longValue).sorted().toArray();

        long slowKeys = Arrays.stream(keys).filter(nanos -> nanos > KEY_BUDGET_NANOS).count();
        long lateFrames = Arrays.stream(frames).filter(nanos -> nanos > LATE_FRAME_NANOS).count();

        String report = String.format("""
                %s: %d keys, finished %s
                Our code per key, after the first %d: median %.3f ms, p90 %.3f, p99 %.3f, max %.3f
                Keys over 1 ms: %d of %d
                Frame gaps: median %.1f ms, p99 %.1f, max %.1f; over 20 ms: %d of %d
                """,
            kind,
            keyNanos.length,
            run.isFinished(),
            WARM_UP_KEYS,
            millis(percentile(keys, 0.5)),
            millis(percentile(keys, 0.9)),
            millis(percentile(keys, 0.99)),
            millis(keys[keys.length - 1]),
            slowKeys,
            keys.length,
            millis(percentile(frames, 0.5)),
            millis(percentile(frames, 0.99)),
            millis(frames[frames.length - 1]),
            lateFrames,
            frames.length);

        System.out.print(report);
        Files.writeString(OUT.resolve(kind + ".txt"), report);
        return percentile(keys, 0.5);
    }

    private static long percentile(long[] sorted, double fraction) {
        return sorted[(int) (fraction * (sorted.length - 1))];
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static void save(Scene scene, String name) {
        WritableImage image = scene.snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                picture.setRGB(x, y, reader.getArgb(x, y));
            }
        }

        try {
            ImageIO.write(picture, "png", OUT.resolve(name).toFile());
        } catch (IOException e) {
            throw new IllegalStateException("could not save the screenshot " + name, e);
        }
    }
}
