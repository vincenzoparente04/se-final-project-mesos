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

    /**
     * The single instance of this class, created lazily on the first call to
     * {@link #getInstance()}.
     */
    private static MusicManager instance;

    /**
     * {@code file://} URIs of the audio tracks extracted to the temporary directory.
     * Populated by {@link #loadTracks(String)} and consumed by {@link #playUri(String)}.
     * Empty until the first call to {@link #playRandom(String)}.
     */
    private String[] trackUris = new String[0];

    /**
     * The JavaFX player that drives the currently active audio track.
     * {@code null} when nothing has been played yet or after {@link #stop()} is called.
     */
    private MediaPlayer player;

    /**
     * Source of randomness used to pick the initial track in {@link #playRandom(String)}
     * and the next track in {@link #playNext()}.
     */
    private final Random random = new Random();

    /**
     * Index into {@link #trackUris} of the track that is currently playing.
     * {@code -1} until the first track is started.
     */
    private int currentIndex = -1;

    /**
     * Whether playback is currently paused. Toggled by {@link #pauseResume()} and
     * reset to {@code false} by {@link #stop()} and {@link #playUri(String)}.
     */
    private boolean paused = false;

    /**
     * The resource-folder path passed to the most recent {@link #loadTracks(String)} call.
     * Used to skip redundant re-loading when the folder has not changed.
     */
    private String currentFolder;

    /**
     * Optional UI callback invoked whenever the playing track or the pause/resume state
     * changes. Registered via {@link #setOnTrackChange(Runnable)}.
     */
    private Runnable onTrackChange;

    /**
     * Private constructor — instantiation is controlled by {@link #getInstance()}.
     */
    private MusicManager() {}

    /**
     * Returns the single application-wide instance of {@code MusicManager},
     * creating it lazily on the first call.
     *
     * @return the singleton {@code MusicManager} instance
     */
    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    // ── UI Callback ──────────────────────────────────────────────────────────

    /**
     * Registers a callback that is invoked whenever the current track or the
     * pause/resume state changes. The callback runs on the JavaFX Application Thread
     * because all playback events originate there.
     *
     * @param callback the {@link Runnable} to invoke on track or state changes,
     *                 or {@code null} to remove a previously registered callback
     */
    public void setOnTrackChange(Runnable callback) {
        this.onTrackChange = callback;
    }

    // ── Public playback controls ─────────────────────────────────────────────

    /**
     * Loads the track list from the given resource folder and starts playback
     * from a randomly chosen track.
     *
     * @param resourceFolder classpath-relative folder that contains {@code tracks.txt}
     *                       and the audio files (e.g. {@code "music"})
     */
    public void playRandom(String resourceFolder) {
        loadTracks(resourceFolder);
        if (trackUris.length == 0) return;
        currentIndex = random.nextInt(trackUris.length);
        playUri(trackUris[currentIndex]);
    }

    /**
     * Advances to the next track, chosen at random while guaranteeing it differs
     * from the track currently playing. Does nothing if no tracks have been loaded.
     */
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

    /**
     * Goes back to the previous track in sequential order (wrapping around to the
     * last track when the first one is reached). Does nothing if no tracks have been loaded.
     */
    public void playPrev() {
        if (trackUris.length == 0) return;
        currentIndex = (currentIndex - 1 + trackUris.length) % trackUris.length;
        playUri(trackUris[currentIndex]);
    }

    /**
     * Toggles playback between paused and playing states.
     * Fires a track-change notification so UI widgets can refresh their state.
     * Does nothing if no {@link MediaPlayer} is active.
     */
    public void pauseResume() {
        if (player == null) return;
        if (paused) { player.play(); } else { player.pause(); }
        paused = !paused;
        fireTrackChange();
    }

    /**
     * Stops playback and releases the underlying {@link MediaPlayer} resources.
     * Resets the paused flag. Safe to call even when nothing is playing.
     */
    public void stop() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
        paused = false;
    }

    /**
     * Adjusts the playback volume of the current track.
     * Has no effect if no {@link MediaPlayer} is active.
     *
     * @param volume a value in the range {@code [0.0, 1.0]}, where {@code 0.0}
     *               is silent and {@code 1.0} is full volume
     */
    public void setVolume(double volume) {
        if (player != null) player.setVolume(volume);
    }

    // ── State queries ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if playback is currently paused.
     *
     * @return {@code true} when paused, {@code false} when playing or stopped
     */
    public boolean isPaused() { return paused; }

    /**
     * Returns the display name of the currently playing track, with the file extension
     * stripped and percent-encoded characters (e.g. {@code %20} → space) decoded.
     *
     * @return the track name, or an empty string if no track is loaded or playing
     */
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
                        //System.out.println("[MusicManager] Track loaded: " + filename);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[MusicManager] Error while loading tracks: " + e.getMessage());
        }

        trackUris = uriList.toArray(new String[0]);
        System.out.println("[MusicManager] Total tracks loaded: " + trackUris.length);
    }

    /**
     * Stops any active playback, creates a new {@link MediaPlayer} for the given
     * {@code file://} URI, and starts playing. Registers {@link #playNext()} as the
     * end-of-media handler so the queue advances automatically.
     *
     * @param uri a {@code file://} URI pointing to a local audio file
     */
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

    /**
     * Invokes the registered {@link #onTrackChange} callback, if any, to notify
     * UI components (e.g. {@code MusicPlayerWidget}) that the current track or
     * playback state has changed.
     */
    private void fireTrackChange() {
        if (onTrackChange != null) onTrackChange.run();
    }
}