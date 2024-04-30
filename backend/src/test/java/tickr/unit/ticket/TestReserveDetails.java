package tickr.unit.ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestReserveDetails {
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
                new TicketReserve.TicketDetails("test_section", 2, List.of(1, 2)),
                new TicketReserve.TicketDetails("test_section2", 1, List.of(3))
        )));
        session = TestHelper.commitMakeSession(model, session);
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
    public void testGetReserveDetails() {
        var response = controller.getReserveDetails(session, Map.of("reserve_id", requestIds.get(0)));
        session = TestHelper.commitMakeSession(model, session);
        
        assertEquals(1, response.seatNum);
        assertEquals("test_section", response.section);
        assertEquals(1, response.price);
        assertEquals(eventId, response.eventId);

        response = controller.getReserveDetails(session, Map.of("reserve_id", requestIds.get(1)));
        session = TestHelper.commitMakeSession(model, session);
        
        assertEquals(2, response.seatNum);
        assertEquals("test_section", response.section);
        assertEquals(1, response.price);

        response = controller.getReserveDetails(session, Map.of("reserve_id", requestIds.get(2)));
        session = TestHelper.commitMakeSession(model, session);
        
        assertEquals(3, response.seatNum);
        assertEquals("test_section2", response.section);
        assertEquals(4, response.price);
    }

    @Test 
    public void testExceptions() {
        assertThrows(BadRequestException.class, () -> controller.getReserveDetails(session, Map.of()));
        assertThrows(ForbiddenException.class, () -> controller.getReserveDetails(session, Map.of("reserve_id", UUID.randomUUID().toString())));
    }
}
