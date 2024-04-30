package tickr.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class FileHelper {
    private static final String STATIC_FILE_PATH = "public";

    private static final Map<String, String> VALID_MEDIA_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpeg"
    );

    private static final Map<String, String> VALID_FILE_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg"
    );

    static final Logger logger = LogManager.getLogger();

    public synchronized static String getStaticPath () {
        // Getting static path
        var file = new File(STATIC_FILE_PATH);
        if (!file.exists()) {
            logger.debug("Creating static directory!");
            if (!file.mkdirs()) {
                logger.error("Failed to make static directory!");
            }
        }
        return STATIC_FILE_PATH;
    }

    public synchronized static Optional<String> uploadFromDataUrl (String subdirectory, String filePrefix, String dataUrl) {
        // Constructing path to new file
        var pathFile = new File(STATIC_FILE_PATH + "/" + subdirectory);
        if (!pathFile.exists() && !pathFile.mkdirs()) {
            logger.error("Failed to create directories for static file path {}!", STATIC_FILE_PATH + "/" + subdirectory);
            return Optional.empty();
        }

        // Making data uri
        DataUri dataUri;
        try {
            dataUri = new DataUri(dataUrl);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to parse data uri!");
            return Optional.empty();
        }

        if (!VALID_MEDIA_TYPES.containsKey(dataUri.getMediaType())) {
            logger.warn("Unsupported media type {}!", dataUri.getMediaType());
            return Optional.empty();
        }

        var fileType = VALID_MEDIA_TYPES.get(dataUri.getMediaType());
        var filePath = STATIC_FILE_PATH + "/" + subdirectory + "/" + filePrefix + "." + fileType;

        try (var fileStream = openOutputStream(filePath, false)) {
            if (!dataUri.writeData(fileStream)) {
                // Failed to write data
                return Optional.empty();
            }
        } catch (IOException e) {
            logger.error("Failed to write to file {}!", filePath);
            throw new RuntimeException(e);
        }

        logger.debug("Wrote data url to {} (url: {})", filePath, "/" + subdirectory + "/" + filePrefix + "." + fileType);

        return Optional.of("/" + subdirectory + "/" + filePrefix + "." + fileType);
    }

    public static String readToDataUrl (String filePath) {
        // Read file
        String fileType;
        try {
            fileType = Files.probeContentType(Path.of(filePath));
        } catch (IOException e) {
            logger.error("Failed to read file {}!", filePath);
            throw new RuntimeException(e);
        }

        // Check for valid type
        if (!VALID_MEDIA_TYPES.containsKey(fileType)) {
            throw new RuntimeException("Unsupported file type: " + fileType);
        }

        // Convert to data url
        try (var fileStream = openInputStream(filePath)) {
            var bytes = fileStream.readAllBytes();
            return new DataUri(fileType, bytes).toString();
        } catch (IOException e) {
            logger.error("Failed to read file {}!", filePath);
            throw new RuntimeException(e);
        }
    }

    public static void deleteFileAtUrl (String staticUrl) {
        var file = new File(getStaticPath() + staticUrl);
        if (file.exists() && !file.delete()) {
            logger.error("Failed to delete static file at \"{}\"", STATIC_FILE_PATH + staticUrl);
        }
    }

    public static OutputStream openOutputStream (String filePath, boolean append) throws IOException {
        return new FileOutputStream(filePath, append);
    }

    public static InputStream openInputStream (String filePath) throws IOException {
        if (filePath.startsWith("/")) {
            // Open classpath file
            return FileHelper.class
                    .getClassLoader()
                    .getResourceAsStream(filePath.substring(1));
        } else {
            return new FileInputStream(filePath);
        }
    }

    public static void createFileAtPath (String filePath) {
        try {
            Files.createFile(Path.of(filePath));
        } catch (IOException e) {
            logger.error("Failed to create file at path " + filePath + "!");
            throw new RuntimeException("Failed to create file at path " + filePath + "!");
        }
    }

    private static class DataUri {
        String mediaType;
        String data;
        private static final String SCHEME_STR = "data:";

        public DataUri (String uri) {
            var parts = uri.split(",", 2);
            var header = parts[0];
            if (parts.length != 2) {
                data = "";
            } else {
                data = parts[1];
            }

            logger.debug("Data url header: \"{}\"", header);

            if (!header.startsWith(SCHEME_STR)) {
                logger.warn("Data url does not start with scheme!");
                throw new IllegalArgumentException("Data url is missing a scheme");
            }

            // Extract options
            var opts = header.substring(SCHEME_STR.length()).split(";");

            if (opts.length < 2) {
                logger.warn("Data url missing base64 extension!");
                throw new IllegalArgumentException("Data url is missing base64 extension");
            }

            if (!opts[opts.length - 1].trim().equals("base64")) {
                // Only base64 encoded urls are supported
                logger.warn("Non-base64 encoded data urls not supported!");
                throw new IllegalArgumentException("Data url is missing base64 extension");
            }

            mediaType = opts[0].trim();
        }

        public DataUri (String mediaType, byte[] dataBytes) {
            this.mediaType = mediaType;
            this.data = Base64.getEncoder().encodeToString(dataBytes);
        }


        public String getMediaType () {
            return mediaType;
        }

        public boolean writeData (OutputStream outputStream) throws IOException {
            byte[] dataBytes;
            try {
                // Convert from base64
                dataBytes = Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid base64 string in data url!");
                return false;
            }

            outputStream.write(dataBytes);

            return true;
        }

        public String toString () {
            return SCHEME_STR + mediaType + ";base64," + data;
        }
    }
}
