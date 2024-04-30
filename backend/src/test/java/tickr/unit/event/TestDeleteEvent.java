package tickr.unit.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EventDeleteRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

public class TestDeleteEvent {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String eventId;
    private String authToken; 
    
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        List<CreateEventRequest.SeatingDetails> seatingDetails = new ArrayList<>();
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionA", 10, 50, true));
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionB", 20, 30, true));

        session = model.makeSession();

        authToken = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        
        eventId = controller.createEvent(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(ZonedDateTime.now().plusDays(1))
            .withEndDate(ZonedDateTime.now().plusDays(2))
            .build(authToken)).event_id;

        session = TestHelper.commitMakeSession(model, session);

    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testDeleteEvent () {
        assertDoesNotThrow(() -> controller.eventDelete(session, new EventDeleteRequest(authToken, eventId)));
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.eventView(session, Map.of("event_id", eventId)));
        assertThrows(ForbiddenException.class, () -> controller.eventDelete(session, new EventDeleteRequest(authToken, eventId)));
    }

    @Test 
    public void testExceptions () {
        assertThrows(UnauthorizedException.class, () -> controller.eventDelete(session, new EventDeleteRequest("authToken", eventId)));
        assertThrows(BadRequestException.class, () -> controller.eventDelete(session, new EventDeleteRequest(null, eventId)));
        assertThrows(BadRequestException.class, () -> controller.eventDelete(session, new EventDeleteRequest(authToken, null)));
        assertThrows(ForbiddenException.class, () -> controller.eventDelete(session, new EventDeleteRequest(authToken, UUID.randomUUID().toString())));
        var authToken2 = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test2@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.eventDelete(session, new EventDeleteRequest(authToken2, eventId)));
    }
}
