package tickr.application.apis.location;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.util.FileHelper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LocationCache {
    static final Logger logger = LogManager.getLogger();
    private static final String FILE_SEPARATOR = ";";

    private String cachePath;
    private Map<String, LocationPoint> cache;

    public LocationCache (String cachePath) {
        this.cachePath = cachePath;
        cache = loadCache(cachePath);
    }

    public synchronized Optional<LocationPoint> lookupPoint (String searchText) {
        return Optional.ofNullable(cache.get(searchText));
    }

    public synchronized void cachePoint (String searchText, LocationPoint point) {
        // Put a point in the cache
        if (cache.containsKey(searchText)) {
            logger.error("Attempted to cache {} twice!", searchText);
            throw new RuntimeException("Attempted to recache already cached point!");
        }

        cache.put(searchText, point);
        try (var writer = new BufferedWriter(new OutputStreamWriter(FileHelper.openOutputStream(cachePath, true)))) {
            // Write line to output
            writer.write(String.format("%s%s%s\n", searchText, FILE_SEPARATOR, point.serialise()));
        } catch (IOException e) {
            logger.error("Failed to write to cache file {}!", cachePath);
        }
    }

    private static Map<String, LocationPoint> loadCache (String cachePath) {
        // Load cache from file
        var cache = new HashMap<String, LocationPoint>();
        try (var reader = new BufferedReader(new InputStreamReader(FileHelper.openInputStream(cachePath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var parts = line.split(FILE_SEPARATOR, 2);
                cache.put(parts[0], LocationPoint.deserialise(parts[1]));
            }

        } catch (FileNotFoundException e) {
            logger.info("Failed to open cache file {} - creating new cache file.", cachePath);
            FileHelper.createFileAtPath(cachePath);
        } catch (IOException e) {
            logger.error("Failed to read from cache file {}!", cachePath, e);
        }

        return cache;
    }
}
