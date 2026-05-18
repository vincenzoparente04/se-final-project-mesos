package view;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

/**
 * Singleton that owns the background music MediaPlayer.
 * Supports play/pause, next/prev, volume, and track-change callbacks.
 */
public class MusicManager {

    private static MusicManager instance;

    private MediaPlayer player;
    private final Random random = new Random();

    private File[] trackList;
    private int currentIndex = -1;
    private boolean paused = false;
    private String currentFolder;

    private Runnable onTrackChange;

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    /** Called by the UI to be notified whenever the playing track changes. */
    public void setOnTrackChange(Runnable callback) {
        this.onTrackChange = callback;
    }

    // ── Public playback controls ─────────────────────────────────────────────

    public void playRandom(String resourceFolder) {
        loadTracks(resourceFolder);
        if (trackList == null || trackList.length == 0) return;
        currentIndex = random.nextInt(trackList.length);
        playFile(trackList[currentIndex].toURI().toString());
    }

    public void playNext() {
        if (trackList == null || trackList.length == 0) return;
        currentIndex = (currentIndex + 1) % trackList.length;
        playFile(trackList[currentIndex].toURI().toString());
    }

    public void playPrev() {
        if (trackList == null || trackList.length == 0) return;
        currentIndex = (currentIndex - 1 + trackList.length) % trackList.length;
        playFile(trackList[currentIndex].toURI().toString());
    }

    /** Toggles between paused and playing. */
    public void pauseResume() {
        if (player == null) return;
        if (paused) {
            player.play();
            paused = false;
        } else {
            player.pause();
            paused = true;
        }
        fireTrackChange();
    }

    public void stop() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
        paused = false;
    }

    public void setVolume(double volume) {
        if (player != null) player.setVolume(volume);
    }

    // ── State queries ────────────────────────────────────────────────────────

    public boolean isPaused() { return paused; }

    public boolean isPlaying() { return player != null && !paused; }

    /** Returns the display name of the currently playing track (no extension). */
    public String getCurrentTrackName() {
        if (trackList == null || currentIndex < 0 || currentIndex >= trackList.length) return "";
        String name = trackList[currentIndex].getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Number of tracks in the loaded folder. */
    public int getTrackCount() {
        return trackList == null ? 0 : trackList.length;
    }

    public int getCurrentIndex() { return currentIndex; }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void loadTracks(String resourceFolder) {
        if (resourceFolder.equals(currentFolder) && trackList != null) return;
        currentFolder = resourceFolder;

        URL folderUrl = getClass().getResource("/" + resourceFolder);
        if (folderUrl == null) {
            System.err.println("[MusicManager] folder not found: " + resourceFolder);
            trackList = new File[0];
            return;
        }
        File folder;
        try {
            folder = Paths.get(folderUrl.toURI()).toFile();
        } catch (Exception e) {
            System.err.println("[MusicManager] cannot resolve folder URI: " + e.getMessage());
            trackList = new File[0];
            return;
        }
        File[] files = folder.listFiles(f ->
                f.isFile() && f.getName().matches(".*\\.(mp3|wav|aac|m4a|ogg)"));
        if (files == null || files.length == 0) {
            System.err.println("[MusicManager] no audio files in: " + resourceFolder);
            trackList = new File[0];
            return;
        }
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        trackList = files;
    }

    private void playFile(String uri) {
        stop();
        paused = false;
        Media media = new Media(uri);
        player = new MediaPlayer(media);
        player.setOnEndOfMedia(this::playNext);
        player.play();
        System.out.println("[MusicManager] playing: " + getCurrentTrackName());
        fireTrackChange();
    }

    private void fireTrackChange() {
        if (onTrackChange != null) onTrackChange.run();
    }
}
