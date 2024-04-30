package tickr.integration.user;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import spark.Spark;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.serialised.requests.user.UserChangePasswordRequest;
import tickr.application.serialised.requests.user.UserLoginRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.requests.user.UserRequestChangePasswordRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.util.HTTPHelper;
import tickr.mock.MockEmailAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

public class TestChangePassword {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private String authToken;

    @BeforeAll
    public static void setupApis () {
        ApiLocator.addLocator(IEmailAPI.class, MockEmailAPI::new);
    }
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

    @AfterAll
    public static void cleanupApis () {
        ApiLocator.clearLocator(IEmailAPI.class);
    }

    @Test
    public void testBadEmail () {
        assertEquals(400, httpHelper.post("/api/user/reset/request", new UserRequestChangePasswordRequest(null)).getStatus());

        assertEquals(403, httpHelper.post("/api/user/reset/request", new UserRequestChangePasswordRequest("test")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/reset/request", new UserRequestChangePasswordRequest("test@")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/reset/request", new UserRequestChangePasswordRequest("test@example")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/reset/request", new UserRequestChangePasswordRequest("@example.com")).getStatus());
    }

    @Test void testNonexistentEmail () {
        assertEquals(403, httpHelper.post("/api/user/reset/request", new UserRequestChangePasswordRequest("test1@example.com")).getStatus());

        assertEquals(403, httpHelper.post("/api/user/reset/request", new UserRequestChangePasswordRequest("test2@example.com")).getStatus());
    }

    @Test
    public void testWrongPassword () {
        assertEquals(403, httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Wrongone123!", "Newpassword123!", authToken)).getStatus());

        assertEquals(400, httpHelper.put("/api/user/reset", new UserChangePasswordRequest(null, "Newpassword123!", authToken)).getStatus());
    }

    @Test
    public void testBadNewPassword () {
        assertEquals(400, httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Testing123!", null, authToken)).getStatus());

        assertEquals(403, httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Testing123!", "aA1!", authToken)).getStatus());

        assertEquals(403, httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Testing123!", "AAAAAAAA1!", authToken)).getStatus());

        assertEquals(403, httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Testing123!", "aaaaaaaa1!", authToken)).getStatus());

        assertEquals(403, httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Testing123!", "aaaaaaaaA!", authToken)).getStatus());

        assertEquals(403, httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Testing123!", "aaaaaaaaA1", authToken)).getStatus());
    }

    @Test
    public void testLoggedChangePassword () {
        var response = httpHelper.put("/api/user/reset", new UserChangePasswordRequest("Testing123!", "Newpassword123!", authToken));
        assertEquals(200, response.getStatus());

        assertEquals(200, httpHelper.post("/api/user/login",
                new UserLoginRequest("test@example.com", "Newpassword123!")).getStatus());
    }
}
