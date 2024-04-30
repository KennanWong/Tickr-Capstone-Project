package tickr.integration.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import spark.Spark;
import tickr.TestHelper;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.user.ViewProfileResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

import java.util.Map;
import java.util.UUID;

public class TestViewProfile {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private String authToken;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("test_username", "TestFirst",
                "TestLast", "test@example.com", "Testing123!", "2022-04-14"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();
    }

    @Test
    public void testBadRequests () {
        var response = httpHelper.get("/api/user/profile");
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/user/profile", Map.of("auth_token", authToken, "user_id", UUID.randomUUID().toString()));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/user/profile", Map.of("auth_token", TestHelper.makeFakeJWT()));
        assertEquals(401, response.getStatus());

        response = httpHelper.get("/api/user/profile", Map.of("user_id", UUID.randomUUID().toString()));
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testOwnProfile () {
        var response = httpHelper.get("/api/user/profile", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());

        var profile = response.getBody(ViewProfileResponse.class);

        assertEquals("test_username", profile.userName);
        assertEquals("TestFirst", profile.firstName);
        assertEquals("TestLast", profile.lastName);
        assertEquals("test@example.com", profile.email);
        assertEquals("", profile.description);
        assertEquals("", profile.profilePicture);
    }
}
