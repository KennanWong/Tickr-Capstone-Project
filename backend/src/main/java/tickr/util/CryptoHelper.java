package tickr.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Key;
import java.util.Base64;
import java.util.Optional;

public class CryptoHelper {
    static final Logger logger = LogManager.getLogger();
    private static Key JWT_KEY = null;

    private static Optional<Key> readJWTKey (String keyPath) {
        // Try reading JWT key from file
        try (var reader = new BufferedReader(new FileReader(keyPath))) {
            var contents = reader.lines().reduce(String::concat);
            // Decode from base64
            return contents.map(Base64.getDecoder()::decode)
                    .map(bytes -> new SecretKeySpec(bytes, SignatureAlgorithm.HS256.getJcaName()));
        } catch (FileNotFoundException e) {
            logger.info("Failed to find JWT key file!");
            return Optional.empty();
        } catch (IOException e) {
            logger.error("Failed to read JWT key file!");
            return Optional.empty();
        }
    }

    private static Key makeJWTKey (String keyPath) {
        // Making new JWT key and storing in file
        logger.info("Making new JWT key!");
        var key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        try (var writer = new BufferedWriter(new FileWriter(keyPath))) {
            writer.write(Base64.getEncoder().encodeToString(key.getEncoded()));
        } catch (IOException e) {
            logger.error("Failed to write JWT key file!");
        }

        return key;
    }

    private static synchronized Key getJwtKey () {
        // Getting JWT signing key
        if (JWT_KEY == null) {
            JWT_KEY = readJWTKey("jwt_key")
                    .orElseGet(() -> makeJWTKey("jwt_key"));
        }

        return JWT_KEY;
    }

    public static JwtBuilder makeJWTBuilder () {
        return Jwts.builder()
                .signWith(getJwtKey());
    }

    public static JwtParserBuilder makeJWTParserBuilder () {
        return Jwts.parserBuilder()
                .setSigningKey(getJwtKey());
    }

    public static char[] hashPassword (String password) {
        return BCrypt.withDefaults().hashToChar(10, password.toCharArray());
    }

    public static boolean verifyHash (String password, char[] hash) {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}
