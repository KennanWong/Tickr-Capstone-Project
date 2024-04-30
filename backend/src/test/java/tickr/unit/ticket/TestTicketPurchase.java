package tickr.unit.ticket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.opentest4j.AssertionFailedError;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.entities.PurchaseItem;
import tickr.application.entities.SeatingPlan;
import tickr.application.entities.Ticket;
import tickr.application.entities.TicketReservation;
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.AbstractMockPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.mock.MockUnitPurchaseAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestTicketPurchase {
    private DataModel model;
    private TickrController controller;
    private AbstractMockPurchaseAPI purchaseAPI;

    private ModelSession session;
    private String authToken;
    private String eventId;

    private String requestId;
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
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);

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
        requestIds = response.reserveTickets.stream().map(t -> t.reserveId).collect(Collectors.toList());
        requestPrice = response.reserveTickets.stream().map(t -> t.price).reduce(0.0f, Float::sum);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequest () {
        var reqIds = requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken, null, null, null)));
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,"http://testing.com", null, reqIds)));
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken, "http://testing.com", "http://testing.com", List.of())));
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken, null, "http://testing.com", reqIds)));

        assertThrows(UnauthorizedException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(null, "http://testing.com", "http://testing.com", reqIds)));
        assertThrows(UnauthorizedException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(TestHelper.makeFakeJWT(), "http://testing.com", "http://testing.com", reqIds)));

        assertThrows(ForbiddenException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,"http://testing.com", "http://testing.com",
                List.of(new TicketPurchase.TicketDetails(UUID.randomUUID().toString())))));

        var newUser = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(newUser,"http://testing.com", "http://testing.com", reqIds)));

        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,"http://testing.com", "http://testing.com",
                List.of(new TicketPurchase.TicketDetails(requestIds.get(0), null, "test", "test@example.com")))));
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,"http://testing.com", "http://testing.com",
                List.of(new TicketPurchase.TicketDetails(requestIds.get(0), "test", null, "test@example.com")))));
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,"http://testing.com", "http://testing.com",
                List.of(new TicketPurchase.TicketDetails(requestIds.get(0), "test", "test", "test@")))));
    }

    @Test
    public void testPurchase () {
        var reqIds = requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 10);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);

        assertTrue(purchaseAPI.isUrlValid(redirectUrl));

        var result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        assertEquals(5, purchaseAPI.getCustomer("test_customer").getBalance());

        assertEquals(2, session.getAll(Ticket.class).size());
        assertEquals(0, session.getAll(TicketReservation.class).size());
        assertEquals(0, session.getAll(PurchaseItem.class).size());
    }

    @Test
    public void testCancel () {
        var reqIds = requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 10);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);

        var result = purchaseAPI.cancelOrder(redirectUrl);
        assertEquals("https://example.com/cancel", result);

        assertEquals(10, purchaseAPI.getCustomer("test_customer").getBalance());
        assertEquals(0, session.getAll(Ticket.class).size());
        assertEquals(0, session.getAll(TicketReservation.class).size());
        assertEquals(0, session.getAll(PurchaseItem.class).size());
    }

    @Test
    public void testLowBalance () {
        var reqIds = requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 0.5f);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);

        var result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/cancel", result);

        assertEquals(0.5f, purchaseAPI.getCustomer("test_customer").getBalance());
        assertEquals(0, session.getAll(Ticket.class).size());
        assertEquals(0, session.getAll(TicketReservation.class).size());
        assertEquals(0, session.getAll(PurchaseItem.class).size());
    }

    @Test
    public void testBadUrls () {
        var reqIds = requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "example.com/success", "https://example.com/cancel", reqIds)));
        session.rollback();
        session.close();
        session = model.makeSession();
        assertThrows(BadRequestException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "not-real://example.com/cancel", reqIds)));
    }

    @Test
    public void testNamesEmail () {
        var reqIds = List.of(
                new TicketPurchase.TicketDetails(requestIds.get(0), "John", "Doe", null),
                new TicketPurchase.TicketDetails(requestIds.get(1), null, null, "test@gmail.com")
        );
        purchaseAPI.addCustomer("test_customer", 10);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);

        var result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        var section1 = session.getAllWith(SeatingPlan.class, "section", "test_section").get(0);
        var section2 = session.getAllWith(SeatingPlan.class, "section", "test_section2").get(0);

        var ticket1 = session.getAllWith(Ticket.class, "section", section1).get(0);
        var ticket2 = session.getAllWith(Ticket.class, "section", section2).get(0);

        assertEquals("John", ticket1.getFirstName());
        assertEquals("Doe", ticket1.getLastName());
        assertEquals("test@example.com", ticket1.getEmail());

        assertEquals("Test", ticket2.getFirstName());
        assertEquals("User", ticket2.getLastName());
        assertEquals("test@gmail.com", ticket2.getEmail());
    }

    @Test
    public void testReservationExpiry () {
        var id1 = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(3))))).reserveTickets.get(0).reserveId;
        session = TestHelper.commitMakeSession(model, session);
        var reserve1 = session.getById(TicketReservation.class, UUID.fromString(id1))
                .orElseThrow(AssertionFailedError::new);

        reserve1.setExpiry(ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS).minusMinutes(5).minusSeconds(1));
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", List.of(new TicketPurchase.TicketDetails(id1)))));
    }
}
