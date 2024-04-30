package tickr.integration.event;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.event.EventReservedSeatsResponse;
import tickr.application.serialised.responses.event.EventReservedSeatsResponse.Reserved;
import tickr.mock.MockHttpPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

public class TestEventReserved {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;
    private MockHttpPurchaseAPI purchaseAPI;

    private String authToken; 
    private String eventId1; 
    private String eventId2;
    private String eventId3;

    private List<String> requestIds;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        purchaseAPI = new MockHttpPurchaseAPI("http://localhost:8080");
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
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

        var startTime1 = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        var endTime1 = startTime1.plus(Duration.ofDays(1));

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(startTime1.minusMinutes(2))
                .withEndDate(endTime1)
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventId1 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId1, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId1, startTime1, List.of(
                new TicketReserve.TicketDetails("SectionA", 3, List.of(1, 2, 3)),
                new TicketReserve.TicketDetails("SectionB", 4, List.of(4, 5, 6, 7))
        )));
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
    public void testEventReserved() {
        var response = httpHelper.get("/api/event/reserved", Map.of("auth_token", authToken, "event_id", eventId1));
        assertEquals(200, response.getStatus());
        var reserved = response.getBody(EventReservedSeatsResponse.class).reserved;
        assertEquals(7, reserved.size());
        Collections.sort(reserved, new Comparator<Reserved> () {
            @Override 
            public int compare(Reserved r1, Reserved r2) {
                if (r1.section.equals(r2.section)) {
                    return Integer.valueOf(r1.seatNumber).compareTo(Integer.valueOf(r2.seatNumber));
                }
                return r1.section.compareTo(r2.section);
            }
        });
        for (int i = 0; i < 3; i ++) {
            assertEquals("SectionA", reserved.get(i).section);
        }
        assertEquals(1, reserved.get(0).seatNumber);
        assertEquals(2, reserved.get(1).seatNumber);
        assertEquals(3, reserved.get(2).seatNumber);
        for (int i = 3; i < 7; i ++) {
            assertEquals("SectionB", reserved.get(i).section);
        }
        assertEquals(4, reserved.get(3).seatNumber);
        assertEquals(5, reserved.get(4).seatNumber);
        assertEquals(6, reserved.get(5).seatNumber);
        assertEquals(7, reserved.get(6).seatNumber);
    }

    @Test 
    public void testExceptions() {
        var response = httpHelper.get("/api/event/reserved", Map.of("event_id", eventId1));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/event/reserved", Map.of("auth_token", authToken));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/event/reserved", Map.of("auth_token", authToken, "event_id", UUID.randomUUID().toString()));
        assertEquals(403, response.getStatus());
    }
}
