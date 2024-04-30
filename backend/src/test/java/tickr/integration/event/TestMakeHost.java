package tickr.integration.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.event.EditHostRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.event.EventViewResponse;
import tickr.application.serialised.responses.user.UserIdResponse;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

public class TestMakeHost {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private String eventId;
    private String authToken; 
    private String newHostAuthToken;
    private String newHostEmail = "newhost@example.com";
    private String newHostId;

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

        response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last", "newhost@example.com",
                "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());

        List<CreateEventRequest.SeatingDetails> seatingDetails = new ArrayList<>();
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionA", 10, 50, true));
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionB", 20, 30, true));

        response =  httpHelper.get("/api/user/search", Map.of("email", newHostEmail));
        assertEquals(200, response.getStatus());
        newHostId = response.getBody(UserIdResponse.class).userId;

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(1))
                .withEndDate(ZonedDateTime.now().plusDays(2))
                .withAdmins(Set.of(newHostId))
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventId = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();

        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testExceptions () {
        var response = httpHelper.put("/api/event/make_host", new EditHostRequest(authToken, eventId, "invalidemail@email.com"));
        assertEquals(403, response.getStatus());
        response = httpHelper.put("/api/event/make_host", new EditHostRequest(authToken, UUID.randomUUID().toString(), newHostEmail));
        assertEquals(403, response.getStatus());
        response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last", "randomuser@example.com",
                "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());
        response = httpHelper.put("/api/event/make_host", new EditHostRequest(authToken, eventId, "randomuser@example.com"));
        assertEquals(400, response.getStatus());
    }

    @Test 
    public void testMakeHost () {
        var response = httpHelper.put("/api/event/make_host", new EditHostRequest(authToken, eventId, newHostEmail));
        assertEquals(200, response.getStatus());
        response = httpHelper.get("/api/event/view", Map.of("event_id", eventId));
        assertEquals(200, response.getStatus());
        var actualHostId = response.getBody(EventViewResponse.class).host_id;
        assertEquals(newHostId, actualHostId);

        
    }
}

