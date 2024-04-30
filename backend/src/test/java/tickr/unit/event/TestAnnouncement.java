package tickr.unit.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.AnnouncementRequest;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockEmailAPI;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnnouncement {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String authToken;
    private String hostToken;
    private String adminToken;
    private String eventId;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<CreateEventRequest.SeatingDetails> seatingDetails;
    private MockUnitPurchaseAPI purchaseAPI;
    private MockEmailAPI emailAPI;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        emailAPI = new MockEmailAPI();
        ApiLocator.addLocator(IEmailAPI.class, () -> emailAPI);
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));

        seatingDetails = List.of(
                new CreateEventRequest.SeatingDetails("test_section", 101, 1, true),
                new CreateEventRequest.SeatingDetails("test_section2", 201, 4, true)
        );

        session = model.makeSession();
        authToken = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        hostToken = controller.userRegister(session, new UserRegisterRequest("TestUsername1", "Test", "User", "test1@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        adminToken = controller.userRegister(session, new UserRegisterRequest("TestUsername1", "Test", "User", "test2@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        eventId = controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
                .withAdmins(Set.of(controller.userSearch(session, Map.of("email", "test2@example.com")).userId))
                .withEventName("Test Event Name")
                .build(hostToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId, hostToken, null, null, null,
                null, null, null, null, null, null, null, true, null));

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("test_section", 1, List.of()),
                new TicketReserve.TicketDetails("test_section2", 1, List.of())
        )));
        session = TestHelper.commitMakeSession(model, session);
        var requestIds = response.reserveTickets.stream().map(t -> t.reserveId).collect(Collectors.toList());
        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "http://example.com/success", "http://example.com/failure", requestIds.stream()
                .map(i -> new TicketPurchase.TicketDetails(i, null, null, null)).collect(Collectors.toList()))).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.addCustomer("test_customer", 1000000);
        assertEquals("http://example.com/success", purchaseAPI.fulfillOrder(redirectUrl, "test_customer"));
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(IEmailAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequests () {
        assertThrows(UnauthorizedException.class, () -> controller.makeAnnouncement(session, new AnnouncementRequest()));
        assertThrows(UnauthorizedException.class, () -> controller.makeAnnouncement(session,
                new AnnouncementRequest(TestHelper.makeFakeJWT(), eventId, "testing")));
        assertThrows(BadRequestException.class, () -> controller.makeAnnouncement(session,
                new AnnouncementRequest(hostToken, null, "testing")));
        assertThrows(BadRequestException.class, () -> controller.makeAnnouncement(session,
                new AnnouncementRequest(hostToken, eventId, null)));
        assertThrows(BadRequestException.class, () -> controller.makeAnnouncement(session,
                new AnnouncementRequest(hostToken, eventId, "")));

        assertThrows(ForbiddenException.class, () -> controller.makeAnnouncement(session,
                new AnnouncementRequest(hostToken, UUID.randomUUID().toString(), "testing")));

        assertThrows(ForbiddenException.class, () -> controller.makeAnnouncement(session,
                new AnnouncementRequest(authToken, eventId, "testing")));
    }

    @Test
    public void testHostAnnounce () {
        controller.makeAnnouncement(session, new AnnouncementRequest(hostToken, eventId, "Test announcement lorem ipsum yay :)"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(1, emailAPI.getSentMessages().size());

        var msg = emailAPI.getSentMessages().get(0);
        assertEquals("test@example.com", msg.getToEmail());
        assertEquals("Test Event Name announcement", msg.getSubject());
        assertTrue(msg.getBody().contains("Test announcement lorem ipsum yay :)"));
    }

    @Test
    public void testAdminAnnounce () {
        controller.makeAnnouncement(session, new AnnouncementRequest(adminToken, eventId, "Test announcement lorem ipsum yay :)"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(1, emailAPI.getSentMessages().size());

        var msg = emailAPI.getSentMessages().get(0);
        assertEquals("test@example.com", msg.getToEmail());
        assertEquals("Test Event Name announcement", msg.getSubject());
        assertTrue(msg.getBody().contains("Test announcement lorem ipsum yay :)"));
    }
}
