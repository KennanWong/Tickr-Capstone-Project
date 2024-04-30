package tickr.integration.event;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.event.EventHostingFutureResponse;
import tickr.application.serialised.responses.event.EventHostingPastResponse;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

public class TestEventHostingsPastFuture {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private String authToken; 
    private String email = "test1@example.com";

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

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(5))
                .withEndDate(ZonedDateTime.now().minusDays(4))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event1 = response.getBody(CreateEventResponse.class).event_id;


        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(2))
                .withEndDate(ZonedDateTime.now().minusDays(1))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event2 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(5))
                .withEndDate(ZonedDateTime.now().plusDays(6))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event3 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(15))
                .withEndDate(ZonedDateTime.now().plusDays(16))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event4 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(20))
                .withEndDate(ZonedDateTime.now().plusDays(21))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event5 = response.getBody(CreateEventResponse.class).event_id;
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();
        ApiLocator.clearLocator(ILocationAPI.class);
    }
    @Test 
    public void testFutureHostings() {
        int pageStart = 0;
        int maxResults = 10;
        var response = httpHelper.get("/api/user/hosting/future", Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(200, response.getStatus());
        var events = response.getBody(EventHostingFutureResponse.class);
        assertEquals(3, events.eventIds.size());
        assertEquals(3, events.numResults);
    }

    @Test 
    public void testPastHostings() {
        int pageStart = 0;
        int maxResults = 10;
        var response = httpHelper.get("/api/user/hosting/past", Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(200, response.getStatus());
        var events = response.getBody(EventHostingPastResponse.class);
        assertEquals(2, events.eventIds.size());
        assertEquals(2, events.numResults);
    }

    @Test 
    public void testExceptions () {
        int pageStart = 0;
        int maxResults = 10;
        var response = httpHelper.get("/api/user/hosting/past", Map.of(
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/past", Map.of(
            "email", email, 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/past", Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/past", Map.of(
            "email", UUID.randomUUID().toString(), 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(403, response.getStatus());
        response = httpHelper.get("/api/user/hosting/past", Map.of(
            "email", email, 
            "page_start", Integer.toString(-1), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/past", Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(-1)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/future", Map.of(
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/future", Map.of(
            "email", email, 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/future", Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/future", Map.of(
            "email", UUID.randomUUID().toString(), 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(403, response.getStatus());
        response = httpHelper.get("/api/user/hosting/future", Map.of(
            "email", email, 
            "page_start", Integer.toString(-1), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/user/hosting/future", Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(-1)
        ));
        assertEquals(400, response.getStatus());
    }
}
