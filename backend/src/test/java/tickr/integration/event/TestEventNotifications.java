package tickr.integration.event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EventNotificationsRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.mock.MockHttpPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
public class TestEventNotifications {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private MockHttpPurchaseAPI purchaseAPI;

    private String authToken;

    private String eventId;

    private String requestId;
    private List<String> requestIds;
    private float requestPrice;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(hibernateModel));
        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;

        List<CreateEventRequest.SeatingDetails> seatingDetails = new ArrayList<>();
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionA", 10, 50, true));
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionB", 20, 30, true));

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(1))
                .withEndDate(ZonedDateTime.now().plusDays(2))
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventId = response.getBody(CreateEventResponse.class).event_id;
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();

        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequests () {
        var response = httpHelper.get("/api/event/notifications", Map.of("auth_token", authToken));
        assertEquals(400, response.getStatus());
        response = httpHelper.put("/api/event/notifications/update", new EventNotificationsRequest(authToken, UUID.randomUUID().toString()));
        assertEquals(403, response.getStatus());
        response = httpHelper.get("/api/event/notifications", Map.of("event_id", eventId));
        assertEquals(400, response.getStatus());
        response = httpHelper.put("/api/event/notifications/update", new EventNotificationsRequest(UUID.randomUUID().toString(), eventId));
        assertEquals(401, response.getStatus());
        response = httpHelper.get("/api/event/notifications", Map.of("auth_token", UUID.randomUUID().toString(),"event_id", eventId));
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testEventNotifications() {
        var response = httpHelper.get("/api/event/notifications", Map.of("auth_token", authToken, "event_id", eventId));
        assertEquals(200, response.getStatus());
        response = httpHelper.put("/api/event/notifications/update", new EventNotificationsRequest(authToken, eventId));
        assertEquals(200, response.getStatus());
    }
}