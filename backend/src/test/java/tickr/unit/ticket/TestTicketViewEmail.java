package tickr.unit.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.ticket.TicketViewEmailRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.AbstractMockPurchaseAPI;
import tickr.mock.MockEmailAPI;
import tickr.mock.MockLocationApi;
import tickr.mock.MockUnitPurchaseAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

public class TestTicketViewEmail {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;
    private AbstractMockPurchaseAPI purchaseAPI;
    private MockEmailAPI emailAPI;

    private String eventId;
    private String authToken; 
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<String> requestIds;
    private float requestPrice;

    private List<String> ticketIds; 

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();

        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        List<CreateEventRequest.SeatingDetails> seatingDetails = new ArrayList<>();
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionA", 10, 50, true));
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionB", 20, 30, true));

        session = model.makeSession();

        authToken = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));
        
        eventId = controller.createEvent(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(startTime.minusMinutes(2))
            .withEndDate(endTime)
            .build(authToken)).event_id;
            
        
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("SectionA", 1, List.of(1)),
                new TicketReserve.TicketDetails("SectionB", 1, List.of(2))
        )));
        requestIds = response.reserveTickets.stream().map(t -> t.reserveId).collect(Collectors.toList());
        requestPrice = response.reserveTickets.stream().map(t -> t.price).reduce(0.0f, Float::sum);

        var reqIds = requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 100);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(redirectUrl, "test_customer");

        ticketIds = controller.ticketBookings(session, Map.of("event_id", eventId, "auth_token", authToken)).tickets;

        emailAPI = new MockEmailAPI();
        ApiLocator.addLocator(IEmailAPI.class, () -> emailAPI);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IEmailAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testTicketEmail() {
        controller.ticketViewSendEmail(session, new TicketViewEmailRequest(authToken, ticketIds.get(0), "test1@example.com"));

        session = TestHelper.commitMakeSession(model, session);

        assertEquals(1, emailAPI.getSentMessages().size());
        var message = emailAPI.getSentMessages().get(0);
        assertEquals("test1@example.com", message.getToEmail());
        assertEquals("View user ticket details", message.getSubject());

        var pattern = Pattern.compile("<a href=\"http://localhost:3000/ticket/(.*)\">.*</a>");
        var matcher = pattern.matcher(message.getBody());
        assertTrue(matcher.find());
    }

    @Test 
    public void testExceptions() {
        assertThrows(UnauthorizedException.class, () -> controller.ticketViewSendEmail(session,
                new TicketViewEmailRequest("authToken", ticketIds.get(0), "test1@example.com")));
        assertThrows(ForbiddenException.class, () -> controller.ticketViewSendEmail(session,
                new TicketViewEmailRequest(authToken, UUID.randomUUID().toString(), "test1@example.com")));
        assertThrows(ForbiddenException.class, () -> controller.ticketViewSendEmail(session,
                new TicketViewEmailRequest(authToken, ticketIds.get(0), "invalid@example.com")));
        assertThrows(BadRequestException.class, () -> controller.ticketViewSendEmail(session,
                new TicketViewEmailRequest(null, ticketIds.get(0), "test1@example.com")));
        assertThrows(BadRequestException.class, () -> controller.ticketViewSendEmail(session,
                new TicketViewEmailRequest(authToken, null, "test1@example.com")));
        assertThrows(BadRequestException.class, () -> controller.ticketViewSendEmail(session,
                new TicketViewEmailRequest(authToken, ticketIds.get(0), null)));

        var authToken2 = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test2@example.com",
                "Password123!", "2022-04-14")).authToken;
        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken2, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("SectionA", 1, List.of(3)),
                new TicketReserve.TicketDetails("SectionB", 1, List.of(4))
        )));
        requestIds = response.reserveTickets.stream().map(t -> t.reserveId).collect(Collectors.toList());
        requestPrice = response.reserveTickets.stream().map(t -> t.price).reduce(0.0f, Float::sum);

        var reqIds = requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 100);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken2,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(redirectUrl, "test_customer");

        var ticketIds2 = controller.ticketBookings(session, Map.of("event_id", eventId, "auth_token", authToken2)).tickets;

        assertThrows(BadRequestException.class, () -> controller.ticketViewSendEmail(session,
                new TicketViewEmailRequest(authToken, ticketIds2.get(0), "test1@example.com")));
    }
}
