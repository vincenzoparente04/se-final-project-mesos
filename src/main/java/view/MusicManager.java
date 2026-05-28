package view;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Singleton responsible for background music playback.
 * <p>
 * Loads audio files from the resources folder via {@link Class#getResourceAsStream},
 * ensuring compatibility both when running from the IDE (filesystem classpath)
 * and from a JAR (zip-internal classpath). Files are extracted to a temporary
 * directory since {@link javafx.scene.media.Media} only accepts {@code file://} URIs.
 * </p>
 * <p>
 * The resources folder must contain an index file {@code tracks.txt} that
 * lists one filename per line (e.g. {@code intro.mp3}).
 * </p>
 */
public class MusicManager {

    private static MusicManager instance;

    /** file:// URIs of the tracks extracted to the temp directory, ready for JavaFX Media. */
    private String[] trackUris = new String[0];

    private MediaPlayer player;
    private final Random random = new Random();

    private int     currentIndex = -1;
    private boolean paused       = false;
    private String  currentFolder;

    private Runnable onTrackChange;

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    // ── UI Callback ──────────────────────────────────────────────────────────

    /** Registers a callback invoked whenever the current track changes. */
    public void setOnTrackChange(Runnable callback) {
        this.onTrackChange = callback;
    }

    // ── Public playback controls ─────────────────────────────────────────────

    public void playRandom(String resourceFolder) {
        loadTracks(resourceFolder);
        if (trackUris.length == 0) return;
        currentIndex = random.nextInt(trackUris.length);
        playUri(trackUris[currentIndex]);
    }

    public void playNext() {
        if (trackUris.length == 0) return;
        if (trackUris.length == 1) {
            playUri(trackUris[0]);
            return;
        }
        int next;
        do {
            next = random.nextInt(trackUris.length);
        } while (next == currentIndex); // avoid replaying the current track
        currentIndex = next;
        playUri(trackUris[currentIndex]);
    }

    public void playPrev() {
        if (trackUris.length == 0) return;
        currentIndex = (currentIndex - 1 + trackUris.length) % trackUris.length;
        playUri(trackUris[currentIndex]);
    }

    public void pauseResume() {
        if (player == null) return;
        if (paused) { player.play(); } else { player.pause(); }
        paused = !paused;
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

    /** Returns the display name of the currently playing track without its file extension. */
    public String getCurrentTrackName() {
        if (trackUris.length == 0 || currentIndex < 0) return "";
        try {
            // URI.getPath() automatically decodes percent-encoding: %20 → space, %27 → ' etc.
            String path = new java.net.URI(trackUris[currentIndex]).getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            int dot = filename.lastIndexOf('.');
            return dot > 0 ? filename.substring(0, dot) : filename;
        } catch (java.net.URISyntaxException e) {
            System.err.println("[MusicManager] Malformed URI: " + e.getMessage());
            return "";
        }
    }

    // ── Internal logic ───────────────────────────────────────────────────────

    /**
     * Loads tracks from {@code resourceFolder} using an index file ({@code tracks.txt}).
     * Audio files are extracted to a temporary directory to allow playback
     * via {@link Media} both from the IDE and from a JAR.
     *
     * @param resourceFolder path relative to the classpath root (e.g. {@code "music"})
     */
    private void loadTracks(String resourceFolder) {
        if (resourceFolder.equals(currentFolder) && trackUris.length > 0) return;
        currentFolder = resourceFolder;

        String indexPath = "/" + resourceFolder + "/tracks.txt";
        List<String> uriList = new ArrayList<>();

        try (InputStream indexStream = getClass().getResourceAsStream(indexPath)) {
            if (indexStream == null) {
                System.err.println("[MusicManager] Index file not found: " + indexPath
                        + " — make sure tracks.txt exists under resources/" + resourceFolder);
                trackUris = new String[0];
                return;
            }

            // Create a temporary directory for audio files extracted from the JAR
            Path tempDir = Files.createTempDirectory("mesos_music_");
            tempDir.toFile().deleteOnExit();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(indexStream))) {
                String filename;
                while ((filename = reader.readLine()) != null) {
                    filename = filename.trim();
                    if (filename.isEmpty() || filename.startsWith("#")) continue;

                    String resourcePath = "/" + resourceFolder + "/" + filename;
                    try (InputStream audioStream = getClass().getResourceAsStream(resourcePath)) {
                        if (audioStream == null) {
                            System.err.println("[MusicManager] Track not found on classpath: " + resourcePath);
                            continue;
                        }
                        // Extract the audio file into the temporary directory
                        File tempFile = tempDir.resolve(filename).toFile();
                        tempFile.deleteOnExit();
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            audioStream.transferTo(fos);
                        }
                        uriList.add(tempFile.toURI().toString());
                        System.out.println("[MusicManager] Track loaded: " + filename);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[MusicManager] Error while loading tracks: " + e.getMessage());
        }

        trackUris = uriList.toArray(new String[0]);
        System.out.println("[MusicManager] Total tracks loaded: " + trackUris.length);
    }

    private void playUri(String uri) {
        stop();
        paused = false;
        Media media = new Media(uri);
        player = new MediaPlayer(media);
        player.setOnEndOfMedia(this::playNext);
        player.play();
        System.out.println("[MusicManager] Now playing: " + getCurrentTrackName());
        fireTrackChange();
    }

    private void fireTrackChange() {
        if (onTrackChange != null) onTrackChange.run();
    }
}