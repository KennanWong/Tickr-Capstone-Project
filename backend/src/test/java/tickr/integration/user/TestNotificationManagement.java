package tickr.integration.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import spark.Spark;
import tickr.TestHelper;
import tickr.application.serialised.combined.user.NotificationManagement;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

import java.util.Map;

public class TestNotificationManagement {
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
        var response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());
        var authToken = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.get("/api/user/settings");
        assertEquals(401, response.getStatus());
        response = httpHelper.get("/api/user/settings", Map.of("auth_token", TestHelper.makeFakeJWT()));
        assertEquals(401, response.getStatus());

        response = httpHelper.put("/api/user/settings/update", new NotificationManagement.UpdateRequest(TestHelper.makeFakeJWT(),
                new NotificationManagement.Settings()));
        assertEquals(401, response.getStatus());

        var jsonObject = new JsonObject();
        jsonObject.add("auth_token", new JsonPrimitive(authToken));
        response = httpHelper.put("/api/user/settings/update", jsonObject);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testGetDefault () {
        var response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());
        var authToken = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.get("/api/user/settings", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());

        var settings = response.getBody(NotificationManagement.GetResponse.class).settings;
        assertTrue(settings.reminders);
    }

    @Test
    public void testSettingsSet () {
        var response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());
        var authToken = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.put("/api/user/settings/update", new NotificationManagement.UpdateRequest(authToken,
                new NotificationManagement.Settings(false)));
        assertEquals(200, response.getStatus());
        var putSettings = response.getBody(NotificationManagement.GetResponse.class).settings;

        response = httpHelper.get("/api/user/settings", Map.of("auth_token", authToken));
        assertEquals(200, response.getStatus());
        var getSettings = response.getBody(NotificationManagement.GetResponse.class).settings;

        assertFalse(putSettings.reminders);
        assertFalse(getSettings.reminders);
    }

    @Test
    public void testEmptySettings () {
        var response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());
        var authToken = response.getBody(AuthTokenResponse.class).authToken;

        JsonObject object = new JsonObject();
        object.add("auth_token", new JsonPrimitive(authToken));
        object.add("settings", new JsonObject());

        response = httpHelper.put("/api/user/settings/update", object);
        assertEquals(200, response.getStatus());

        var settings = response.getBody(NotificationManagement.GetResponse.class).settings;
        assertTrue(settings.reminders);
    }
}
