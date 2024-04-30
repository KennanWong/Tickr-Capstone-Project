package tickr;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.persistence.DataModel;
import tickr.persistence.ModelSession;
import tickr.util.CryptoHelper;
import tickr.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class TestHelper {
    static final Logger logger = LogManager.getLogger();
    public static ModelSession commitMakeSession (DataModel model, ModelSession session) {
        assertDoesNotThrow(session::commit);
        session.close();

        return model.makeSession();
    }

    public static ModelSession rollbackMakeSession (DataModel model, ModelSession session) {
        session.rollback();
        session.close();
        return model.makeSession();
    }

    public static void sleep (long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String makeFakeJWT (UUID userId) {
        return CryptoHelper.makeJWTBuilder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(30))))
                .compact();
    }

    public static String makeFakeJWT () {
        return makeFakeJWT(UUID.randomUUID());
    }

    public static UserRegisterRequest makeRegisterRequest () {
        var user = "x" + Long.toHexString(System.nanoTime());
        return new UserRegisterRequest(user, "First", "Last", user + "@example.com", "Password123!", "2022-01-01");
    }

    public static boolean fileDiff (String filepath1, String filepath2) {
        logger.debug("Comparing {} and {}!", filepath1, filepath2);
        try (var file1 = FileHelper.openInputStream(filepath1); var file2 = FileHelper.openInputStream(filepath2)) {
            byte[] bytes1 = file1.readAllBytes();
            byte[] bytes2 = file2.readAllBytes();

            return Arrays.equals(bytes1, bytes2);

        } catch (IOException e) {
            logger.info("Encountered IOException in diff: ", e);
            return false;
        }
    }

    public static void clearStaticFiles () {
        var file = new File(FileHelper.getStaticPath());
        if (file.exists() && file.isDirectory()) {
            for (var i : Objects.requireNonNull(file.listFiles())) {
                recursiveDelete(i);
            }
            assert file.delete();
        }
    }

    private static void recursiveDelete (File file) {
        var subdirectories = file.listFiles();
        if (subdirectories != null) {
            for (var i : subdirectories) {
                if (!Files.isSymbolicLink(file.toPath())) {
                    recursiveDelete(i);
                }
            }
        }

        assert file.delete();
    }
}
