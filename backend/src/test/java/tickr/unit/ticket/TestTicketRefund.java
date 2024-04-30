package tickr.unit.ticket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.event.EventDeleteRequest;
import tickr.application.serialised.requests.ticket.TicketRefundRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.AbstractMockPurchaseAPI;
import tickr.mock.MockEmailAPI;
import tickr.mock.MockLocationApi;
import tickr.mock.MockUnitPurchaseAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestTicketRefund {
    private DataModel model;
    private TickrController controller;
    private AbstractMockPurchaseAPI purchaseAPI;

    private ModelSession session;
    private String authToken;
    private String eventId;

    private List<String> ticketIds;
    private List<String> requestIds;
    private float requestPrice;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        purchaseAPI.addCustomer("test_customer", 20);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);

        ApiLocator.addLocator(IEmailAPI.class, () -> new MockEmailAPI());

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));

        List<CreateEventRequest.SeatingDetails> seatingDetails = List.of(
                new CreateEventRequest.SeatingDetails("test_section", 10, 1, true),
                new CreateEventRequest.SeatingDetails("test_section2", 20, 4, true)
        );

        session = model.makeSession();
        authToken = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("test_section", 1, List.of()),
                new TicketReserve.TicketDetails("test_section2", 1, List.of())
        )));
        session = TestHelper.commitMakeSession(model, session);
        requestIds = response.reserveTickets.stream().map(t -> t.reserveId).collect(Collectors.toList());
        requestPrice = response.reserveTickets.stream().map(t -> t.price).reduce(0.0f, Float::sum);

        var url = controller.ticketPurchase(session, new TicketPurchase.Request(authToken, "http://example.com", "http://example.com",
                requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList()))).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(url, "test_customer");

        ticketIds = controller.ticketBookings(session, Map.of("auth_token", authToken, "event_id", eventId)).tickets;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
        ApiLocator.clearLocator(IEmailAPI.class);
    }

    @Test
    public void testBadRequests () {
        assertThrows(UnauthorizedException.class, () -> controller.ticketRefund(session, new TicketRefundRequest(null, null)));
        assertThrows(UnauthorizedException.class, () -> controller.ticketRefund(session, new TicketRefundRequest(TestHelper.makeFakeJWT(), ticketIds.get(0))));
        assertThrows(ForbiddenException.class, () -> controller.ticketRefund(session, new TicketRefundRequest(authToken, UUID.randomUUID().toString())));
        session = TestHelper.rollbackMakeSession(model, session);


        var newUser = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);

        assertThrows(ForbiddenException.class, () -> controller.ticketRefund(session, new TicketRefundRequest(newUser, ticketIds.get(0))));
    }

    @Test
    public void testRefund () {
        assertEquals(15, purchaseAPI.getCustomer("test_customer").getBalance());

        controller.ticketRefund(session, new TicketRefundRequest(authToken, ticketIds.get(0)));
        session = TestHelper.commitMakeSession(model, session);

        var balance = purchaseAPI.getCustomer("test_customer").getBalance();
        assertTrue(balance == 16 || balance == 19);

        assertThrows(ForbiddenException.class, () -> controller.ticketView(session, Map.of("ticket_id", ticketIds.get(0))));
        session = TestHelper.rollbackMakeSession(model, session);
        assertDoesNotThrow(() -> controller.ticketView(session, Map.of("ticket_id", ticketIds.get(1))));
        session = TestHelper.commitMakeSession(model, session);

        var bookings = controller.ticketBookings(session, Map.of("auth_token", authToken, "event_id", eventId)).tickets;
        assertEquals(1, bookings.size());
        assertEquals(ticketIds.get(1), bookings.get(0));

        controller.ticketRefund(session, new TicketRefundRequest(authToken, ticketIds.get(1)));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals(20, purchaseAPI.getCustomer("test_customer").getBalance());
        assertThrows(ForbiddenException.class, () -> controller.ticketView(session, Map.of("ticket_id", ticketIds.get(1))));

        bookings = controller.ticketBookings(session, Map.of("auth_token", authToken, "event_id", eventId)).tickets;
        assertEquals(0, bookings.size());
    }

    @Test
    public void testDoubleRefund () {
        controller.ticketRefund(session, new TicketRefundRequest(authToken, ticketIds.get(0)));
        session = TestHelper.commitMakeSession(model, session);

        controller.ticketRefund(session, new TicketRefundRequest(authToken, ticketIds.get(1)));
        session = TestHelper.commitMakeSession(model, session);

        assertThrows(ForbiddenException.class, () -> controller.ticketRefund(session, new TicketRefundRequest(authToken, ticketIds.get(0))));
        session = TestHelper.rollbackMakeSession(model, session);

        assertEquals(20, purchaseAPI.getCustomer("test_customer").getBalance());
    }

    @Test
    public void testEventCancelRefund () {
        controller.eventDelete(session, new EventDeleteRequest(authToken, eventId));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals(20, purchaseAPI.getCustomer("test_customer").getBalance());
    }

    @Test
    public void testFreeRefund () {
        var newEvent = controller.createEvent(session, new CreateEventReqBuilder()
                .withSeatingDetails(List.of(new CreateEventRequest.SeatingDetails("TestSection", 10, 0, false)))
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(newEvent, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        var reqId = controller.ticketReserve(session, new TicketReserve.Request(authToken, newEvent, startTime, List.of(
                new TicketReserve.TicketDetails("TestSection", 1, List.of())
        ))).reserveTickets.stream().map(TicketReserve.ReserveDetails::getReserveId).collect(Collectors.toList()).get(0);
        session = TestHelper.commitMakeSession(model, session);
        var url = controller.ticketPurchase(session, new TicketPurchase.Request(authToken, "http://example.com", "http://example.com", List.of(
            new TicketPurchase.TicketDetails(reqId)
        ))).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(url, "test_customer");

        assertEquals(15, purchaseAPI.getCustomer("test_customer").getBalance());

        var ticketId = controller.ticketBookings(session, Map.of("auth_token", authToken, "event_id", newEvent)).tickets.get(0);
        session = TestHelper.commitMakeSession(model, session);

        controller.ticketRefund(session, new TicketRefundRequest(authToken, ticketId));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals(15, purchaseAPI.getCustomer("test_customer").getBalance());
        assertThrows(ForbiddenException.class, () -> controller.ticketView(session, Map.of("ticket_id", ticketId)));
    }
}
