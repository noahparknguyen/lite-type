package dev.noahpn.litetype.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class LiteTypeApp extends Application {

    private long lastEventNanos;

    @Override
    public void start(Stage stage) {
        Pane root = new Pane();
        Scene scene = new Scene(root, 800, 500);

        scene.setOnKeyPressed(event ->
            IO.println(
                "pressed: " + event.getCode() + "  (" + elapsedSinceLastEvent() + "ms)"));

        scene.setOnKeyTyped(event -> {
            String ch = event.getCharacter();
            int codePoint = ch.codePointAt(0);
            IO.println("typed:   '" + printable(ch) + "' (" + codePoint + ")  (" +
                elapsedSinceLastEvent() + "ms)");
        });

        stage.setTitle("lite-type");
        stage.setScene(scene);
        stage.show();
        lastEventNanos = System.nanoTime();
    }

    private long elapsedSinceLastEvent() {
        long now = System.nanoTime();
        long deltaMs = (now - lastEventNanos) / 1_000_000;
        lastEventNanos = now;
        return deltaMs;
    }

    private static String printable(String s) {
        if (s.isEmpty()) return "";
        int cp = s.codePointAt(0);
        if (cp < 32) return "\\x" + Integer.toHexString(cp);
        return s;
    }
}
