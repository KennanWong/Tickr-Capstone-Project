package tickr.integration.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.TestHelper;
import tickr.application.serialised.requests.user.UserLoginRequest;
import tickr.application.serialised.requests.user.UserLogoutRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUserLogout {
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
    public void testBadRequests () {
        var response = httpHelper.delete("/api/user/logout", new UserLogoutRequest(null));
        assertEquals(401, response.getStatus());

        response = httpHelper.delete("/api/user/logout", new UserLogoutRequest("testing123"));
        assertEquals(401, response.getStatus());

        response = httpHelper.delete("/api/user/logout", new UserLogoutRequest(TestHelper.makeFakeJWT()));
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testLogout () {
        var response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());

        var authToken = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.get("/api/user/settings", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());

        response = httpHelper.delete("/api/user/logout", new UserLogoutRequest(authToken));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/settings", Map.of("auth_token", authToken));
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testDoubleLogout () {
        var response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());

        var authToken = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.delete("/api/user/logout", new UserLogoutRequest(authToken));
        assertEquals(200, response.getStatus());

        response = httpHelper.delete("/api/user/logout", new UserLogoutRequest(authToken));
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testTwoAuthTokens () {
        var regReq = TestHelper.makeRegisterRequest();

        var response = httpHelper.post("/api/user/register", regReq);
        assertEquals(200, response.getStatus());
        var authToken1 = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/login", new UserLoginRequest(regReq.email, regReq.password));
        assertEquals(200, response.getStatus());
        var authToken2 = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.delete("/api/user/logout", new UserLogoutRequest(authToken1));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/settings", Map.of("auth_token", authToken2));
        assertEquals(200, response.getStatus());

        response = httpHelper.delete("/api/user/logout", new UserLogoutRequest(authToken2));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/settings", Map.of("auth_token", authToken2));
        assertEquals(401, response.getStatus());
    }
}
