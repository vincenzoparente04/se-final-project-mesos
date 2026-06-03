package view.widgets;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application-wide cache for JavaFX Image objects.
 * Each resource path is loaded at most once per session.
 * Missing paths are also cached so repeated lookups for absent assets do not
 * hit the classpath on every call.
 */
public class ImageCache {

    private static final Map<String, Image> cache = new ConcurrentHashMap<>();
    private static final Set<String> missing = ConcurrentHashMap.newKeySet();

    private ImageCache() {}

    public static Image get(String resourcePath) {
        if (missing.contains(resourcePath)) return null;
        return cache.computeIfAbsent(resourcePath, path -> {
            try (InputStream s = ImageCache.class.getResourceAsStream(path)) {
                if (s == null) {
                    missing.add(path);
                    return null;
                }
                return new Image(s);
            } catch (IOException e) {
                missing.add(path);
                return null;
            }
        });
    }
}
