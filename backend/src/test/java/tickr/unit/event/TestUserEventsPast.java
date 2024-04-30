package tickr.unit.event;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;

public class TestUserEventsPast {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String authToken; 

    private List<CreateEventRequest.SeatingDetails> seatingDetails;
    
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        seatingDetails = new ArrayList<>();
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionA", 10, 50, true));
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionB", 20, 30, true));

        session = model.makeSession();

        authToken = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        
        var event1 = controller.createEventUnsafe(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(ZonedDateTime.now().minusDays(2))
            .withEndDate(ZonedDateTime.now().minusDays(1))
            .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(event1, authToken, null, null, null, null, null, null, null, null, null, null, true, null));


        var event2 = controller.createEventUnsafe(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(ZonedDateTime.now().minusDays(4))
            .withEndDate(ZonedDateTime.now().minusDays(3))
            .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(event2, authToken, null, null, null, null, null, null, null, null, null, null, true, null));


        var event3 = controller.createEventUnsafe(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(ZonedDateTime.now().minusDays(6))
            .withEndDate(ZonedDateTime.now().minusDays(5))
            .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        controller.editEvent(session, new EditEventRequest(event3, authToken, null, null, null, null, null, null, null, null, null, null, true, null));


        var event4 = controller.createEventUnsafe(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(ZonedDateTime.now().minusDays(16))
            .withEndDate(ZonedDateTime.now().minusDays(15))
            .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        controller.editEvent(session, new EditEventRequest(event4, authToken, null, null, null, null, null, null, null, null, null, null, true, null));


        var event5 = controller.createEventUnsafe(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(ZonedDateTime.now().minusDays(21))
            .withEndDate(ZonedDateTime.now().minusDays(20))
            .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        controller.editEvent(session, new EditEventRequest(event5, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);
        
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testGetUserEventsPast() {
        var events = controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(5), 
            "after", ZonedDateTime.now().minusDays(22).format(DateTimeFormatter.ISO_DATE_TIME)
        )).getEventIds();
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(5, events.size());
        session = TestHelper.commitMakeSession(model, session);

        events = controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(5), 
            "after", ZonedDateTime.now().minusDays(17).format(DateTimeFormatter.ISO_DATE_TIME)
        )).getEventIds();
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(4, events.size());
        session = TestHelper.commitMakeSession(model, session);

        events = controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(5), 
            "after", ZonedDateTime.now().minusDays(14).format(DateTimeFormatter.ISO_DATE_TIME)
        )).getEventIds();
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(3, events.size());
        session = TestHelper.commitMakeSession(model, session);

        events = controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(5), 
            "after", ZonedDateTime.now().minusMinutes(10).format(DateTimeFormatter.ISO_DATE_TIME)
        )).getEventIds();
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(0, events.size());
        session = TestHelper.commitMakeSession(model, session);
    }

    @Test 
    public void testExceptions () {
        assertThrows(BadRequestException.class, () -> controller.userEventsPast(session, Map.of(          
            "max_results", Integer.toString(5), 
            "after", ZonedDateTime.now().plusDays(0).format(DateTimeFormatter.ISO_DATE_TIME)
        )));
        assertThrows(BadRequestException.class, () -> controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(0), 
            "after", ZonedDateTime.now().plusDays(0).format(DateTimeFormatter.ISO_DATE_TIME)
        )));
        assertThrows(BadRequestException.class, () -> controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(-1), 
            "max_results", Integer.toString(5), 
            "after", ZonedDateTime.now().plusDays(0).format(DateTimeFormatter.ISO_DATE_TIME)
        )));
        assertThrows(BadRequestException.class, () -> controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(-1), 
            "after", ZonedDateTime.now().plusDays(0).format(DateTimeFormatter.ISO_DATE_TIME)
        )));
        assertThrows(BadRequestException.class, () -> controller.userEventsPast(session, Map.of(
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(5), 
            "after", "21-01-2001"
        )));
    }
}
