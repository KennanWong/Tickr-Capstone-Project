package tickr.integration.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import spark.Spark;
import tickr.TestHelper;
import tickr.application.serialised.requests.user.UserDeleteRequest;
import tickr.application.serialised.requests.user.UserLoginRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

public class TestDeleteAccount {
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
    public void testBadValues () {
        var response = httpHelper.delete("/api/user/delete", new UserDeleteRequest(null, null));
        assertEquals(400, response.getStatus());

        response = httpHelper.delete("/api/user/delete", new UserDeleteRequest(authToken, null));
        assertEquals(400, response.getStatus());

        response = httpHelper.delete("/api/user/delete", new UserDeleteRequest(null, "Password123!"));
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testAccountExists () {
        var response = httpHelper.post("/api/user/login", new UserLoginRequest ("test@example.com", "Password123!"));
        assertEquals(200, response.getStatus());
        
        response = httpHelper.delete("/api/user/delete", new UserDeleteRequest(authToken, "Password123!"));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/user/login", new UserLoginRequest ("test@example.com", "Password123!"));
        assertEquals(403, response.getStatus());

        response = httpHelper.delete("/api/user/delete", new UserDeleteRequest(TestHelper.makeFakeJWT(), "Password123!"));
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testRemakeAccount () {
        var response = httpHelper.delete("/api/user/delete", new UserDeleteRequest(authToken, "Password123!"));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/user/login", new UserLoginRequest ("test@example.com", "Password123!"));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testAuthTokenRemoved () {
        var oldAuthToken = authToken;
        var response = httpHelper.delete("/api/user/delete", new UserDeleteRequest(authToken, "Password123!"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        var newAuthToken = response.getBody(AuthTokenResponse.class).authToken;
        assertNotEquals(oldAuthToken, newAuthToken);
    }
}
