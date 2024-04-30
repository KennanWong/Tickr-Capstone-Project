package tickr.integration.user;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

public class TestUserRegister {
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
    public void testEmptyValues () {
        assertEquals(400, httpHelper.post("/api/user/register", new UserRegisterRequest("", "first", "last", "test@example.com",
                "password123!!", "2022-04-14")).getStatus());

        assertEquals(400, httpHelper.post("/api/user/register", new UserRegisterRequest("test", "", "last", "test@example.com",
                "password123!!", "2022-04-14")).getStatus());

        assertEquals(400, httpHelper.post("/api/user/register", new UserRegisterRequest("test", "", "last", "test@example.com",
                "password123!!", "2022-04-14")).getStatus());

        assertEquals(400, httpHelper.post("/api/user/register", new JsonObject()).getStatus());
    }

    @Test
    public void testBadPassword () {
        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "aA1!", "2022-04-14")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "AAAAAAAA1!", "2022-04-14")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "aaaaaaaa1!", "2022-04-14")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "aaaaaaaaA!", "2022-04-14")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "aaaaaaaaA1", "2022-04-14")).getStatus());
    }

    @Test
    public void testBadEmail () {
        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test", "Password123!", "2022-04-14")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@", "Password123!", "2022-04-14")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example", "Password123!", "2022-04-14")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "@example.com", "Password123!", "2022-04-14")).getStatus());
    }

    @Test
    public void testBadDOB () {
        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "abdefg")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "2022")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "2022-04")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "2100-12-25")).getStatus());

    }

    @Test
    public void testRegister () {
        var response = httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "2022-04-14"));

        assertEquals(200, response.getStatus());

        assertDoesNotThrow(() -> {
            var responseObj = response.getBody(AuthTokenResponse.class);
            assertNotNull(responseObj.authToken);
        });
    }

    @Test
    public void testTwoRegister () {
        var response = httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());

        var token1 = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test2@example.com", "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());

        var token2 = response.getBody(AuthTokenResponse.class).authToken;

        assertNotEquals(token1, token2);
    }

    @Test
    public void testSameEmail () {
        var response = httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "test@example.com", "Password123!", "2022-04-14"));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/user/register",
                new UserRegisterRequest("test", "first", "last", "TeST@ExAMpLe.cOM", "Password123!", "2022-04-14"));
        assertEquals(403, response.getStatus());
    }
}
