package view;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Singleton that owns the background music MediaPlayer.
 */
public class MusicManager {

    private static MusicManager instance;
    private MediaPlayer player;
    private final Random random = new Random();

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    /** Picks a random audio file from the given resource folder and plays it. */
    public void playRandom(String resourceFolder) {
        URL folderUrl = getClass().getResource("/" + resourceFolder);
        if (folderUrl == null) {
            System.err.println("[MusicManager] folder not found: " + resourceFolder);
            return;
        }
        File folder;
        try {
            folder = Paths.get(folderUrl.toURI()).toFile();
        } catch (Exception e) {
            System.err.println("[MusicManager] cannot resolve folder URI: " + e.getMessage());
            return;
        }
        File[] files = folder.listFiles(f ->
                f.isFile() && f.getName().matches(".*\\.(mp3|wav|aac|m4a|ogg)"));
        if (files == null || files.length == 0) {
            System.err.println("[MusicManager] no audio files in: " + resourceFolder);
            return;
        }
        File chosen = files[random.nextInt(files.length)];
        System.out.println("[MusicManager] playing: " + chosen.getName());
        playFile(chosen.toURI().toString());
    }

    public void playSequential(String resourceFolder, int playing) {
        URL folderUrl = getClass().getResource("/" + resourceFolder);
        if (folderUrl == null) {
            System.err.println("[MusicManager] folder not found: " + resourceFolder);
            return;
        }
        File folder;
        try {
            folder = Paths.get(folderUrl.toURI()).toFile();
        } catch (Exception e) {
            System.err.println("[MusicManager] cannot resolve folder URI: " + e.getMessage());
            return;
        }
        File[] files = folder.listFiles(f ->
                f.isFile() && f.getName().matches(".*\\.(mp3|wav|aac|m4a|ogg)"));
        if (files == null || files.length == 0) {
            System.err.println("[MusicManager] no audio files in: " + resourceFolder);
            return;
        }
        int nextSong = (playing + 1) > files.length ? 0: playing ;
        File chosen = files[random.nextInt(files.length)];
        System.out.println("[MusicManager] playing: " + chosen.getName());
        playFile(chosen.toURI().toString());
    }

    /** Plays a specific resource path (e.g. "music/track.mp3"). */
    public void play(String resourcePath) {
        URL url = getClass().getResource("/" + resourcePath);
        if (url == null) {
            System.err.println("[MusicManager] resource not found: " + resourcePath);
            return;
        }
        playFile(url.toExternalForm());
    }

    private void playFile(String uri) {
        stop();
        Media media = new Media(uri);
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
