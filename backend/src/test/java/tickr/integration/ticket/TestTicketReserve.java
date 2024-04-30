package tickr.integration.ticket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestTicketReserve {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;
    private String authToken;

    private String eventId;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<CreateEventRequest.SeatingDetails> seatingDetails;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(hibernateModel));

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));

        seatingDetails = List.of(
                new CreateEventRequest.SeatingDetails("test_section", 10, 1, true),
                new CreateEventRequest.SeatingDetails("test_section2", 20, 4, true)
        );

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
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

        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequests () {
        var ticketDetails = List.of(new TicketReserve.TicketDetails("test_section", 1, List.of()));
        var response = httpHelper.post("/api/ticket/reserve",
                new TicketReserve.Request(authToken, null, (String)null, null));
        assertEquals(400, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve",
                new TicketReserve.Request(TestHelper.makeFakeJWT(), eventId, startTime, ticketDetails));
        assertEquals(401, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve",
                new TicketReserve.Request(authToken, UUID.randomUUID().toString(), startTime, ticketDetails));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve",
                new TicketReserve.Request(authToken, eventId, "abcdefg", ticketDetails));
        assertEquals(400, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve",
                new TicketReserve.Request(authToken, eventId, startTime, List.of()));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testOneTicket () {
        var ticketDetails = List.of(new TicketReserve.TicketDetails("test_section", 1, List.of()));

        var response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId, startTime, ticketDetails));
        assertEquals(200, response.getStatus());
        assertEquals(1, response.getBody(TicketReserve.Response.class).reserveTickets.get(0).price);
    }

    @Test
    public void testMultipleTickets () {
        var ticketDetails = List.of(
                /*new TicketReserve.TicketDetails( null, null, null, "test_section", 4),
                new TicketReserve.TicketDetails( null, null, null, "test_section", 5),
                new TicketReserve.TicketDetails( null, null, null, "test_section", 6),*/
                new TicketReserve.TicketDetails("test_section", 4, List.of(4, 6, 7, 5))
        );

        var response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId, startTime, ticketDetails));
        assertEquals(200, response.getStatus());
        //assertEquals(4, Float.parseFloat(response.getBody(TicketReserve.ResponseNew.class).price));
        var reserveTickets = response.getBody(TicketReserve.Response.class).reserveTickets;
        assertEquals(4, reserveTickets.size());
        //assertNotNull(response.reserveId);
        //assertEquals(4, Float.parseFloat(response.price));
        reserveTickets.sort(Comparator.comparing(r -> r.seatNum));
        var seenIds = new HashSet<String>();
        for (int i = 0; i < 4; i++) {
            var reservation = reserveTickets.get(i);
            assertEquals(4 + i, reservation.seatNum);
            assertEquals("test_section", reservation.section);
            assertFalse(seenIds.contains(reservation.reserveId));
            assertEquals(1, reservation.price);
            seenIds.add(reservation.reserveId);
        }
    }

    @Test
    public void testMultipleSections () {
        var ticketDetails = List.of(
                new TicketReserve.TicketDetails("test_section", 4, List.of()),
                new TicketReserve.TicketDetails("test_section2", 5, List.of())
        );

        var response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId, startTime, ticketDetails));
        assertEquals(200, response.getStatus());
        assertEquals(9, response.getBody(TicketReserve.Response.class).reserveTickets.size());
    }
}
