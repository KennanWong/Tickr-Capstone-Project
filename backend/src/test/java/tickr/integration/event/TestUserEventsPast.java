package tickr.integration.event;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.user.UserEventsResponse;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;
public class TestUserEventsPast {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private String authToken; 

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
                .withStartDate(ZonedDateTime.now().minusDays(2))
                .withEndDate(ZonedDateTime.now().minusDays(1))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event1 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(event1, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(4))
                .withEndDate(ZonedDateTime.now().minusDays(3))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event2 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(event2, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(6))
                .withEndDate(ZonedDateTime.now().minusDays(5))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event3 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(event3, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(16))
                .withEndDate(ZonedDateTime.now().minusDays(15))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event4 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(event4, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(21))
                .withEndDate(ZonedDateTime.now().minusDays(20))
                .build(authToken));
        assertEquals(200, response.getStatus());
        var event5 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(event5, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
        Spark.awaitStop();
    }

    @Test 
    public void testUserEvents () {
        var response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(0),
            "max_results", Integer.toString(5),
            "after", ZonedDateTime.now().minusDays(22).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(200, response.getStatus());
        assertEquals(response.getBody(UserEventsResponse.class).eventIds.size(), 5);

        response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(0),
            "max_results", Integer.toString(5),
            "after", ZonedDateTime.now().minusDays(17).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(200, response.getStatus());
        assertEquals(response.getBody(UserEventsResponse.class).eventIds.size(), 4);

        response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(0),
            "max_results", Integer.toString(5),
            "after", ZonedDateTime.now().minusDays(14).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(200, response.getStatus());
        assertEquals(response.getBody(UserEventsResponse.class).eventIds.size(), 3);

        response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(0),
            "max_results", Integer.toString(5),
            "after", ZonedDateTime.now().minusMinutes(10).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(200, response.getStatus());
        assertEquals(response.getBody(UserEventsResponse.class).eventIds.size(), 0);
    }


    @Test 
    public void testExceptions () {
        var response = httpHelper.get("/api/home/past", Map.of(
            "max_results", Integer.toString(5),
            "before", ZonedDateTime.now().plusDays(21).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(0),
            "before", ZonedDateTime.now().plusDays(21).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(0),
            "max_results", Integer.toString(5),
            "after", "20-20-2022"
        ));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(0),
            "max_results", Integer.toString(-1),
            "before", ZonedDateTime.now().plusDays(21).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/home/past", Map.of(
            "page_start", Integer.toString(-1),
            "max_results", Integer.toString(5),
            "before", ZonedDateTime.now().plusDays(21).format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        assertEquals(400, response.getStatus());

    }
}
