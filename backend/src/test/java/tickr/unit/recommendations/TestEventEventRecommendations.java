package tickr.unit.recommendations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.SerializedLocation;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;

import java.util.*;

public class TestEventEventRecommendations {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String authToken;
    private MockLocationApi locationApi;
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        locationApi = new MockLocationApi(model);
        ApiLocator.addLocator(ILocationAPI.class, () -> locationApi);

        session = model.makeSession();
        authToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequests () {
        assertThrows(BadRequestException.class, () -> controller.recommendEventEvent(session, Map.of()));
        assertThrows(BadRequestException.class, () -> controller.recommendEventEvent(session, Map.of("max_results", "1")));
        assertThrows(BadRequestException.class, () -> controller.recommendEventEvent(session, Map.of("page_start", "1", "max_results", "1")));
        assertThrows(ForbiddenException.class, () -> controller.recommendEventEvent(session,
                Map.of("event_id", UUID.randomUUID().toString(), "page_start", "0", "max_results", "1")));

        session = TestHelper.rollbackMakeSession(model, session);
        var eventId = controller.createEvent(session, new CreateEventReqBuilder().build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        assertThrows(BadRequestException.class, () -> controller.recommendEventEvent(session,
                Map.of("event_id", eventId, "page_start", "-1", "max_results", "1")));
        assertThrows(BadRequestException.class, () -> controller.recommendEventEvent(session,
                Map.of("event_id", eventId, "page_start", "0", "max_results", "0")));
        assertThrows(BadRequestException.class, () -> controller.recommendEventEvent(session,
                Map.of("event_id", eventId, "page_start", "0", "max_results", "-1")));
        assertThrows(BadRequestException.class, () -> controller.recommendEventEvent(session,
                Map.of("event_id", eventId, "page_start", "abc", "max_results", "def")));
    }

    @Test
    public void testEventNameDescription () {
        var eventIds = addTestEvents();

        var eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("cookie test")
                .withDescription("hydrogen banana")
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        var result = controller.recommendEventEvent(session, Map.of("event_id", eventId, "page_start", "0", "max_results", "10"));
        assertEquals(eventIds.size(), result.numResults);

        var resultEvents = result.events;

        assertEquals(eventIds.get(1), resultEvents.get(0).id);
        assertEquals(eventIds.get(0), resultEvents.get(1).id);
        assertEquals(eventIds.get(2), resultEvents.get(2).id);
    }

    @Test
    public void testEventTags () {
        var eventIds = addTestEvents();

        var eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("testing")
                .withDescription("testing")
                .withTags(Set.of("test", "yummy", "delicious"))
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        var result = controller.recommendEventEvent(session, Map.of("event_id", eventId, "page_start", "0", "max_results", "10"));
        assertEquals(eventIds.size(), result.numResults);

        var resultEvents = result.events;

        assertEquals(eventIds.get(0), resultEvents.get(0).id);
        assertEquals(eventIds.get(2), resultEvents.get(1).id);
        assertEquals(eventIds.get(1), resultEvents.get(2).id);
    }

    @Test
    public void testEventCategories () {
        var eventIds = addTestEvents();

        var eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("testing")
                .withDescription("testing")
                .withCategories(Set.of("education", "food", "health"))
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        var result = controller.recommendEventEvent(session, Map.of("event_id", eventId, "page_start", "0", "max_results", "10"));
        assertEquals(eventIds.size(), result.numResults);

        var resultEvents = result.events;

        assertEquals(eventIds.get(0), resultEvents.get(0).id);
        assertEquals(eventIds.get(2), resultEvents.get(1).id);
        assertEquals(eventIds.get(1), resultEvents.get(2).id);
    }

    @Test
    public void testEventHost () {
        var eventIds = addTestEvents();
        var authToken2 = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;

        var event1 = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("testing alpha")
                .withDescription("testing")
                .build(authToken2)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        controller.editEvent(session, new EditEventRequest(event1, authToken2, null, null, null, null, null,
                null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);
        var event2 = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("testing beta")
                .withDescription("testing")
                .build(authToken2)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        controller.editEvent(session, new EditEventRequest(event2, authToken2, null, null, null, null, null,
                null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        var result = controller.recommendEventEvent(session, Map.of("event_id", event1, "page_start", "0", "max_results", "10"));
        assertEquals(eventIds.size() + 1, result.numResults);

        var resultEvents = result.events;

        assertEquals(event2, resultEvents.get(0).id);
    }

    @Test
    public void testLocation () {
        var eventIds = addTestEvents();
        var eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("testing")
                .withDescription("testing")
                .withLocation(new SerializedLocation.Builder()
                        .withStreetNo(235)
                        .withStreetName("Anzac Parade")
                        .withSuburb("Kensington")
                        .withPostcode("2033")
                        .withState("NSW")
                        .withCountry("Australia")
                        .build())
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        locationApi.awaitLocations();

        var result = controller.recommendEventEvent(session, Map.of("event_id", eventId, "page_start", "0", "max_results", "10"));
        assertEquals(eventIds.size(), result.numResults);

        var resultEvents = result.events;

        assertEquals(eventIds.get(2), resultEvents.get(0).id);
    }


    private List<String> addTestEvents () {
        var eventIds = new ArrayList<String>();

        eventIds.add(controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("Test event testing")
                .withDescription("apple banana banana cookie testing")
                .withLocation(new SerializedLocation.Builder()
                        .withStreetNo(1)
                        .withStreetName("High St")
                        .withSuburb("Kensington")
                        .withPostcode("2052")
                        .withState("NSW")
                        .withCountry("Australia")
                        .build())
                .withCategories(Set.of("food", "health"))
                .withTags(Set.of("yummy", "delicious", "test"))
                .build(authToken)).event_id);
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventIds.get(0), authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        eventIds.add(controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("Test three testing")
                .withDescription("metal hydrogen force hydrogen hydrogen hydrogen cookie testing")
                .withLocation(new SerializedLocation.Builder()
                        .withStreetNo(1)
                        .withStreetName("High St")
                        .withSuburb("Kensington")
                        .withPostcode("2052")
                        .withState("NSW")
                        .withCountry("Australia")
                        .build())
                .withCategories(Set.of("business", "education"))
                .withTags(Set.of("science", "experiment", "test"))
                .build(authToken)).event_id);
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventIds.get(1), authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        eventIds.add(controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("sweet testing")
                .withDescription("sugar chocolate cookie jelly banana cookie cookie cookie cookie testing")
                .withLocation(new SerializedLocation.Builder()
                        .withStreetNo(235)
                        .withStreetName("Anzac Parade")
                        .withSuburb("Kensington")
                        .withPostcode("2033")
                        .withState("NSW")
                        .withCountry("Australia")
                        .build())
                .withCategories(Set.of("food"))
                .withTags(Set.of("sweet", "delicious", "test"))
                .build(authToken)).event_id);
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventIds.get(2), authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        controller.createEvent(session, new CreateEventReqBuilder().build(authToken));
        session = TestHelper.commitMakeSession(model, session);

        locationApi.awaitLocations();
        return eventIds;
    }
}
