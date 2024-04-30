package tickr.unit.recommendations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.SerializedLocation;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.mock.MockLocationApi;
import tickr.mock.MockUnitPurchaseAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestEventUserEventRecommendations {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String authToken;
    private String hostToken;
    private MockLocationApi locationApi;
    private MockUnitPurchaseAPI purchaseAPI;

    private List<String> eventIds;

    private ZonedDateTime eventStart;
    private ZonedDateTime eventEnd;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        locationApi = new MockLocationApi(model);
        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(ILocationAPI.class, () -> locationApi);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);

        session = model.makeSession();
        authToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        hostToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);

        eventStart = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        eventEnd = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1)).plusHours(1);

        eventIds = addTestEvents();
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
        ApiLocator.clearLocator(IPurchaseAPI.class);
    }

    @Test
    public void testBadRequests () {
        assertThrows(UnauthorizedException.class, () -> controller.recommendEventUserEvent(session, Map.of()));
        assertThrows(BadRequestException.class, () -> controller.recommendEventUserEvent(session, Map.of("auth_token", authToken)));
        assertThrows(BadRequestException.class, () -> controller.recommendEventUserEvent(session, Map.of("auth_token", authToken, "event_id", eventIds.get(0))));
        assertThrows(BadRequestException.class, () -> controller.recommendEventUserEvent(session,
                Map.of("auth_token", authToken, "event_id", eventIds.get(0), "page_start", "0")));

        assertThrows(UnauthorizedException.class, () -> controller.recommendEventUserEvent(session,
                Map.of("auth_token", TestHelper.makeFakeJWT(), "event_id", eventIds.get(0), "page_start", "0", "max_results", "1")));
        assertThrows(ForbiddenException.class, () -> controller.recommendEventUserEvent(session,
                Map.of("auth_token",authToken, "event_id", UUID.randomUUID().toString(), "page_start", "0", "max_results", "1")));

        assertThrows(BadRequestException.class, () -> controller.recommendEventUserEvent(session,
                Map.of("auth_token", authToken, "event_id", eventIds.get(0), "page_start", "-1", "max_results", "1")));
        assertThrows(BadRequestException.class, () -> controller.recommendEventUserEvent(session,
                Map.of("auth_token", authToken, "event_id", eventIds.get(0),"page_start", "0", "max_results", "0")));
        assertThrows(BadRequestException.class, () -> controller.recommendEventUserEvent(session,
                Map.of("auth_token", authToken, "event_id", eventIds.get(0),"page_start", "0", "max_results", "-1")));
        assertThrows(BadRequestException.class, () -> controller.recommendEventUserEvent(session,
                Map.of("auth_token", authToken, "event_id", eventIds.get(0),"page_start", "abc", "max_results", "def")));
    }

    @Test
    public void testCombinedRecommendations () {
        var eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("cookie test")
                .withDescription("hydrogen banana")
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        var result = controller.recommendEventUserEvent(session,
                Map.of("auth_token", authToken, "event_id", eventId, "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(eventIds.size(), result.numResults);

        var resultEvents = result.events;

        assertEquals(eventIds.get(1), resultEvents.get(0).id);
        assertEquals(eventIds.get(0), resultEvents.get(1).id);
        assertEquals(eventIds.get(2), resultEvents.get(2).id);

        for (var i = 0; i < 20; i++) {
            controller.eventView(session, Map.of("auth_token", authToken, "event_id", eventIds.get(2)));
            session = TestHelper.commitMakeSession(model, session);
        }

        result = controller.recommendEventUserEvent(session, Map.of("auth_token", authToken, "event_id", eventId, "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals(eventIds.size(), result.numResults);

        resultEvents = result.events;

        assertEquals(eventIds.get(2), resultEvents.get(0).id);
        assertEquals(eventIds.get(1), resultEvents.get(1).id);
        assertEquals(eventIds.get(0), resultEvents.get(2).id);
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
                .withSeatingDetails(List.of(new CreateEventRequest.SeatingDetails("test_section", 10, 0.0f, false)))
                .withStartDate(eventStart.minusMinutes(2))
                .withEndDate(eventEnd)
                .build(hostToken)).event_id);
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventIds.get(0), hostToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        eventIds.add(controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("Test three testing")
                .withDescription("metal hydrogen force hydrogen hydrogen hydrogen cookie testing sugar")
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
                .withSeatingDetails(List.of(new CreateEventRequest.SeatingDetails("test_section", 10, 0.0f, false)))
                .withStartDate(eventStart.minusMinutes(2))
                .withEndDate(eventEnd)
                .build(hostToken)).event_id);
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventIds.get(1), hostToken, null, null, null, null,
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
                .withCategories(Set.of("food", "education"))
                .withTags(Set.of("sweet", "delicious", "test"))
                .withSeatingDetails(List.of(new CreateEventRequest.SeatingDetails("test_section", 10, 0.0f, false)))
                .withStartDate(eventStart.minusMinutes(2))
                .withEndDate(eventEnd)
                .build(hostToken)).event_id);
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventIds.get(2), hostToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        controller.createEvent(session, new CreateEventReqBuilder().build(hostToken));
        session = TestHelper.commitMakeSession(model, session);
        var hostId = controller.createEvent(session, new CreateEventReqBuilder().build(authToken)).event_id;
        controller.editEvent(session, new EditEventRequest(hostId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        locationApi.awaitLocations();
        return eventIds;
    }
}
