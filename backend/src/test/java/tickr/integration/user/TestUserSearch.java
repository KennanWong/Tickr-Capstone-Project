package tickr.integration.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.TestHelper;
import tickr.application.serialised.responses.user.UserIdResponse;
import tickr.application.serialised.responses.user.ViewProfileResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUserSearch {
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
    public void testBadRequest () {
        var response = httpHelper.get("/api/user/search");
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/user/search", Map.of("email", "testing123"));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testNoUser () {
        var response = httpHelper.get("/api/user/search", Map.of("email", "test@example.com"));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/search", Map.of("email", "test@example.com"));
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testUserSearch () {
        var regReq = TestHelper.makeRegisterRequest();
        var response = httpHelper.post("/api/user/register", regReq);
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/user/search", Map.of("email", regReq.email));
        assertEquals(200, response.getStatus());
        var userId = response.getBody(UserIdResponse.class).userId;

        response = httpHelper.get("/api/user/profile", Map.of("user_id", userId));
        assertEquals(200, response.getStatus());
        var profileResponse = response.getBody(ViewProfileResponse.class);

        assertEquals(regReq.userName, profileResponse.userName);
        assertEquals(regReq.email, profileResponse.email);
        assertEquals(regReq.firstName, profileResponse.firstName);
        assertEquals(regReq.lastName, profileResponse.lastName);
    }
}
