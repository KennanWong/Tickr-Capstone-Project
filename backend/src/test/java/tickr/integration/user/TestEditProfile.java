package tickr.integration.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import spark.Spark;
import tickr.TestHelper;
import tickr.application.serialised.requests.user.EditProfileRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.user.ViewProfileResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.FileHelper;

import java.util.Map;

public class TestEditProfile {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;
    private String authToken;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();
    }

    @AfterEach
    public void clearStaticFiles () {
        TestHelper.clearStaticFiles();
    }

    @Test
    public void testBadRequest () {
        var response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(null, null, null, null, null, null, null));
        assertEquals(401, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(TestHelper.makeFakeJWT(), null, null, null, null, null, null));
        assertEquals(401, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, null, "test@", null));
        assertEquals(403, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, null, "test bad url", null));
        assertEquals(403, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, null, "data:aksdhasjd;base64,askdasdasdas=", null));
        assertEquals(403, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, null, "data:image/png,askdasdasdas=", null));
        assertEquals(403, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, null, "data:image/png;stuff,askdasdasdas=", null));
        assertEquals(403, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, null, "data:image/png;base64,askdasdasdas=", null));
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testNoChanges () {
        var response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, null, null, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/profile", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());

        var profileResponse = response.getBody(ViewProfileResponse.class);
        assertEquals("TestUsername", profileResponse.userName);
        assertEquals("Test", profileResponse.firstName);
        assertEquals("User", profileResponse.lastName);
        assertEquals("test@example.com", profileResponse.email);
        assertEquals("", profileResponse.description);
        assertEquals("", profileResponse.profilePicture);
    }

    @Test
    public void testChangesNoPfp () {
        var response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, "TestNew", "John", "Doe", null, "test2@gmail.com", "testing123"));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/profile", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());

        var profileResponse = response.getBody(ViewProfileResponse.class);
        assertEquals("TestNew", profileResponse.userName);
        assertEquals("John", profileResponse.firstName);
        assertEquals("Doe", profileResponse.lastName);
        assertEquals("test2@gmail.com", profileResponse.email);
        assertEquals("testing123", profileResponse.description);
        assertEquals("", profileResponse.profilePicture);
    }

    @Test
    public void testUploadPfp () {
        var response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, FileHelper.readToDataUrl("/test_images/smile.jpg"), null, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/profile", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());

        var jpgUrl = response.getBody(ViewProfileResponse.class).profilePicture;
        response = httpHelper.get(jpgUrl);
        assertEquals(200, response.getStatus());

        response = httpHelper.put("/api/user/editprofile",
                new EditProfileRequest(authToken, null, null, null, FileHelper.readToDataUrl("/test_images/smile.png"), null, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/profile", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());

        var pngUrl = response.getBody(ViewProfileResponse.class).profilePicture;

        response = httpHelper.get(pngUrl);
        assertEquals(200, response.getStatus());

        response = httpHelper.get(jpgUrl);
        assertEquals(404, response.getStatus());
    }
}
