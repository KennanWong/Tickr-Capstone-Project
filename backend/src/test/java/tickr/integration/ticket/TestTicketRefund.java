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
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.ticket.TicketRefundRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.ticket.TicketBookingsResponse;
import tickr.mock.MockHttpPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestTicketRefund {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private MockHttpPurchaseAPI purchaseAPI;

    private String authToken;

    private String eventId;

    private String requestId;
    private List<String> requestIds;
    private List<String> ticketIds;
    private float requestPrice;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        purchaseAPI = new MockHttpPurchaseAPI("http://localhost:8080");
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
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

        List<CreateEventRequest.SeatingDetails> seatingDetails = List.of(
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

        response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("test_section", 1, List.of()),
                new TicketReserve.TicketDetails("test_section2", 1, List.of())
        )));
        assertEquals(200, response.getStatus());
        var reserveResponse = response.getBody(TicketReserve.Response.class);
        requestIds = reserveResponse.reserveTickets.stream()
                .map(t -> t.reserveId)
                .collect(Collectors.toList());
        requestPrice = reserveResponse.reserveTickets.stream()
                .map(t -> t.price)
                .reduce(0.0f, Float::sum);

        var requestDetails = requestIds.stream()
                .map(TicketPurchase.TicketDetails::new)
                .collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 20);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken, "https://example.com/success", "https://example.com/cancel", requestDetails));
        assertEquals(200, response.getStatus());
        var redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));
        var result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        response = httpHelper.get("/api/event/bookings", Map.of("auth_token", authToken, "event_id", eventId));
        assertEquals(200, response.getStatus());
        ticketIds = response.getBody(TicketBookingsResponse.class).tickets;
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
        var response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(null, null));
        assertEquals(401, response.getStatus());

        response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(TestHelper.makeFakeJWT(), ticketIds.get(0)));
        assertEquals(401, response.getStatus());

        response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(authToken, UUID.randomUUID().toString()));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());
        var newUser = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(newUser, ticketIds.get(0)));
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testRefund () {
        assertEquals(15, purchaseAPI.getCustomer("test_customer").getBalance());

        var response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(authToken, ticketIds.get(0)));
        assertEquals(200, response.getStatus());

        var balance = purchaseAPI.getCustomer("test_customer").getBalance();
        assertTrue(balance == 16 || balance == 19);

        response = httpHelper.get("/api/ticket/view", Map.of("ticket_id", ticketIds.get(0)));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(authToken, ticketIds.get(1)));
        assertEquals(200, response.getStatus());

        assertEquals(20, purchaseAPI.getCustomer("test_customer").getBalance());

        response = httpHelper.get("/api/ticket/view", Map.of("ticket_id", ticketIds.get(1)));
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDoubleRefund () {
        var response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(authToken, ticketIds.get(0)));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(authToken, ticketIds.get(1)));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/ticket/refund", new TicketRefundRequest(authToken, ticketIds.get(0)));
        assertEquals(403, response.getStatus());

        assertEquals(20, purchaseAPI.getCustomer("test_customer").getBalance());
    }
}
