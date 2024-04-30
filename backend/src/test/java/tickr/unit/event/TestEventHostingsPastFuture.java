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
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;

public class TestEventHostingsPastFuture {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String eventId;
    private String authToken; 
    List<CreateEventRequest.SeatingDetails> seatingDetails;
    private String email = "test1@example.com";

    private int maxEvents = 16; 
    
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
        
        controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(5))
                .withEndDate(ZonedDateTime.now().minusDays(3))
                .build(authToken));
        session = TestHelper.commitMakeSession(model, session);

        controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().minusDays(2))
                .withEndDate(ZonedDateTime.now().minusDays(1))
                .build(authToken));
        session = TestHelper.commitMakeSession(model, session);

        controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(1))
                .withEndDate(ZonedDateTime.now().plusDays(2))
                .build(authToken));
        session = TestHelper.commitMakeSession(model, session);
        
        controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(1))
                .withEndDate(ZonedDateTime.now().plusDays(2))
                .build(authToken));
        session = TestHelper.commitMakeSession(model, session);

        controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(ZonedDateTime.now().plusDays(1))
                .withEndDate(ZonedDateTime.now().plusDays(2))
                .build(authToken));
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        ApiLocator.clearLocator(ILocationAPI.class);
        model.cleanup();
    }

    @Test 
    public void testFutureHostings() {
        int pageStart = 0;
        int maxResults = 10;
        var events = controller.eventHostingFuture(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(3, events.eventIds.size());
        assertEquals(3, events.numResults);
    }

    @Test 
    public void testPastHostings() {
        int pageStart = 0;
        int maxResults = 10;
        var events = controller.eventHostingPast(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(2, events.eventIds.size());
        assertEquals(2, events.numResults);
    }

    @Test 
    public void testExceptions() {
        int pageStart = 0;
        int maxResults = 10;
        assertThrows(BadRequestException.class, () -> controller.eventHostingPast(session, Map.of(
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingPast(session, Map.of(
            "email", email, 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingPast(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart)
        )));
        assertThrows(ForbiddenException.class, () -> controller.eventHostingPast(session, Map.of(
            "email", UUID.randomUUID().toString(), 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingPast(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(-1), 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingPast(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(-1)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingFuture(session, Map.of(
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingFuture(session, Map.of(
            "email", email, 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingFuture(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart)
        )));
        assertThrows(ForbiddenException.class, () -> controller.eventHostingFuture(session, Map.of(
            "email", UUID.randomUUID().toString(), 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingFuture(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(-1), 
            "max_results", Integer.toString(maxResults)
        )));
        assertThrows(BadRequestException.class, () -> controller.eventHostingFuture(session, Map.of(
            "email", email, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(-1)
        )));
    }
}
