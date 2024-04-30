package tickr.unit.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import tickr.application.serialised.responses.event.EventReservedSeatsResponse.Reserved;
import tickr.mock.AbstractMockPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.mock.MockUnitPurchaseAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;

public class TestEventReserved {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;
    private AbstractMockPurchaseAPI purchaseAPI;

    private String eventId;
    private String authToken; 
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<String> requestIds;
    private float requestPrice;

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

        controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("SectionA", 1, List.of(1)),
                new TicketReserve.TicketDetails("SectionB", 1, List.of(2))
        )));
        session = TestHelper.commitMakeSession(model, session);
        controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("SectionA", 2, List.of(5, 6)),
                new TicketReserve.TicketDetails("SectionB", 3, List.of(3, 4, 5))
        )));
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testEventReserved() {
        var reserved = controller.eventReservedSeats(session, Map.of("auth_token", authToken, "event_id", eventId)).reserved;
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
        assertEquals(5, reserved.get(1).seatNumber);
        assertEquals(6, reserved.get(2).seatNumber);
        for (int i = 3; i < 7; i ++) {
            assertEquals("SectionB", reserved.get(i).section);
        }
        assertEquals(2, reserved.get(3).seatNumber);
        assertEquals(3, reserved.get(4).seatNumber);
        assertEquals(4, reserved.get(5).seatNumber);
        assertEquals(5, reserved.get(6).seatNumber);
    }

    @Test 
    public void testExceptions () {
        assertThrows(BadRequestException.class, () -> controller.eventReservedSeats(session, Map.of("event_id", eventId)));
        assertThrows(BadRequestException.class, () -> controller.eventReservedSeats(session, Map.of("auth_token", authToken)));
        assertThrows(ForbiddenException.class, () -> controller.eventReservedSeats(session, Map.of("auth_token", authToken, "event_id", UUID.randomUUID().toString())));
    }
}
