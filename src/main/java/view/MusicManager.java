package view;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

/**
 * Singleton that owns the background music MediaPlayer.
 * Call MusicManager.getInstance().play("music/yourfile.mp3") once at startup.
 */
public class MusicManager {

    private static MusicManager instance;

    private MediaPlayer player;

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    /**
     * Loads and plays the given resource path (e.g. "music/background.mp3").
     * Loops indefinitely from the beginning. Safe to call multiple times —
     * stops the previous track before starting the new one.
     */
    public void play(String resourcePath) {
        stop();
        URL url = getClass().getResource("/" + resourcePath);
        if (url == null) {
            System.err.println("[MusicManager] resource not found: " + resourcePath);
            return;
        }
        Media media = new Media(url.toExternalForm());
        player = new MediaPlayer(media);
        player.setOnEndOfMedia(() -> {
            player.seek(Duration.ZERO);
            player.play();
        });
        player.play();
    }

    public void stop() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    public void setVolume(double volume) {
        if (player != null) player.setVolume(volume);
    }
}
