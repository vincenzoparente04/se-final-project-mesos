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

    /**Map that is the cache*/
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();
    /**Missing images paths*/
    private static final Set<String> missing = ConcurrentHashMap.newKeySet();

    /**
     * nooo
     */
    private ImageCache() {}

    /**Main method of the image cache.
     *If the cache has already loaded the image, it returns it from the cache. If not, loads it and return it.
     * If the image is missing, it returns null and adds the path to the missing set.
      * @param resourcePath path of the resource to be loaded
      * @return Image object of the resource, or null if not found
      * @see Image
     */
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
