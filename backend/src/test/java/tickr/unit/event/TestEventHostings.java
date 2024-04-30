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
import tickr.server.exceptions.UnauthorizedException;

public class TestEventHostings {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String eventId;
    private String authToken; 
    List<CreateEventRequest.SeatingDetails> seatingDetails;

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
        
        for (int i = 0; i < maxEvents; i ++) {
            controller.createEvent(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(ZonedDateTime.now().plusDays(1))
            .withEndDate(ZonedDateTime.now().plusDays(2))
            .build(authToken));
            session = TestHelper.commitMakeSession(model, session);
        }
        

    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testExceptions() {
        int numEvents = 16; 
        int pageStart = 0;
        assertThrows(BadRequestException.class, () ->
            controller.eventHostings(session, Map.of("page_start", Integer.toString(pageStart), "max_results", Integer.toString(numEvents))));
        assertThrows(BadRequestException.class, () ->
            controller.eventHostings(session, Map.of("auth_token", authToken, "max_results", Integer.toString(numEvents))));
        assertThrows(BadRequestException.class, () ->
            controller.eventHostings(session, Map.of("auth_token", authToken, "page_start", Integer.toString(pageStart))));
        assertThrows(BadRequestException.class, () ->
            controller.eventHostings(session, Map.of("auth_token", authToken, "page_start", Integer.toString(-1), "max_results", Integer.toString(numEvents))));
        assertThrows(BadRequestException.class, () ->
            controller.eventHostings(session, Map.of("auth_token", authToken, "page_start", Integer.toString(pageStart), "max_results", Integer.toString(-1))));
        assertThrows(UnauthorizedException.class, () ->
            controller.eventHostings(session, Map.of("auth_token", "authToken", "page_start", Integer.toString(pageStart), "max_results", Integer.toString(numEvents))));
        
    }

    @Test 
    public void testPagination () {
        int numEvents = 16; 
        int maxResults = 4;
        int pageStart = 0;
        var list1 = controller.eventHostings(session, Map.of("auth_token", authToken, "page_start", Integer.toString(pageStart), "max_results", Integer.toString(numEvents))).eventIds;
        List<String> list2 = new ArrayList<>(); 
        for (int i = 0; i < numEvents / maxResults; i++) {
            var list = controller.eventHostings(session, Map.of("auth_token", authToken, "page_start", Integer.toString(pageStart), "max_results", Integer.toString(maxResults)));
            session = TestHelper.commitMakeSession(model, session);
            assertEquals(numEvents, list.numResults);
            assertEquals(maxResults, list.eventIds.size());
            pageStart += maxResults;
            list2.addAll(list.eventIds);
        }
        assertEquals(list1, list2);
    }

}
