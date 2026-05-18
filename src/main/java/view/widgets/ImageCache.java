package view.widgets;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Application-wide cache for JavaFX Image objects.
 * Each resource path is loaded at most once per session.
 */
public class ImageCache {

    private static final Map<String, Image> cache = new HashMap<>();

    public static Image get(String resourcePath) {
        return cache.computeIfAbsent(resourcePath, path -> {
            InputStream s = ImageCache.class.getResourceAsStream(path);
            return s != null ? new Image(s) : null;
        });
    }
}
