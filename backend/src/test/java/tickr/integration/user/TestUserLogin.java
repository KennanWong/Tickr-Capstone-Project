package tickr.integration.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import spark.Spark;
import tickr.application.serialised.requests.user.UserLoginRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

public class TestUserLogin {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();
    }

    @Test
    public void testBadValues () {
        var response = httpHelper.post("/api/user/login", new UserLoginRequest(null, null));
        assertEquals(400, response.getStatus());

        response = httpHelper.post("/api/user/login", "{,}");
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testNoUsers () {
        assertEquals(403, httpHelper.post("/api/user/login",
                new UserLoginRequest("test@example.com", "Password123!")).getStatus());
    }

    @Test
    public void testIncorrectEmail () {
        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());

        assertEquals(403, httpHelper.post("/api/user/login",
                new UserLoginRequest("test2@example.com", "Password123!")).getStatus());
    }

    @Test
    public void testIncorrectPassword () {
        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());

        assertEquals(403, httpHelper.post("/api/user/login",
                new UserLoginRequest("test@example.com", "Password123!!!!")).getStatus());
    }

    @Test
    public void testSuccessfulLogin () {
        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());
        var tokenStr = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/login", new UserLoginRequest("test@example.com", "Password123!"));
        assertEquals(200, response.getStatus());
        assertNotEquals(tokenStr, response.getBody(AuthTokenResponse.class).authToken);
    }

    @Test
    public void testDoubleLogin () {
        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());
        var tokenStr = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/login", new UserLoginRequest("test@example.com", "Password123!"));
        assertEquals(200, response.getStatus());
        var login1Str = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/login", new UserLoginRequest("test@example.com", "Password123!"));
        assertEquals(200, response.getStatus());
        var login2Str = response.getBody(AuthTokenResponse.class).authToken;
 
        assertNotEquals(tokenStr, login1Str);
        assertNotEquals(tokenStr, login2Str);
        assertNotEquals(login1Str, login2Str);

    }
}
