package tickr.unit.event;

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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestEventCategory {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String authToken;
    private MockLocationApi locationApi;

    private List<String> eventIds;
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        locationApi = new MockLocationApi(model);
        ApiLocator.addLocator(ILocationAPI.class, () -> locationApi);

        session = model.makeSession();
        authToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);

        eventIds = addTestEvents();
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testCategoryList () {
        var categories = List.of("Food", "Music", "Travel & Outdoor", "Health", "Sport & Fitness", "Hobbies", "Business", "Free", "Tourism", "Education");

        assertArrayEquals(categories.toArray(new String[0]), controller.categoriesList(session).categories.toArray(new String[0]));
    }

    @Test
    public void testBadRequest () {
        assertThrows(BadRequestException.class, () -> controller.eventsByCategory(session, Map.of()));
        assertThrows(BadRequestException.class, () -> controller.eventsByCategory(session, Map.of("category", "Free")));
        assertThrows(ForbiddenException.class, () -> controller.eventsByCategory(session,
                Map.of("category", "expensive", "page_start", "0", "max_results", "10")));
        assertThrows(ForbiddenException.class, () -> controller.eventsByCategory(session,
                Map.of("category", "fREe", "page_start", "0", "max_results", "10")));

        assertThrows(BadRequestException.class, () -> controller.eventsByCategory(session,
                Map.of("category", "Free", "page_start", "-1", "max_results", "10")));
        assertThrows(BadRequestException.class, () -> controller.eventsByCategory(session,
                Map.of("category", "Free", "page_start", "0", "max_results", "0")));
        assertThrows(BadRequestException.class, () -> controller.eventsByCategory(session,
                Map.of("category", "Free", "page_start", "0", "max_results", "-1")));
        assertThrows(BadRequestException.class, () -> controller.eventsByCategory(session,
                Map.of("category", "Free", "page_start", "abc", "max_results", "def")));
    }

    @Test
    public void testCategories () {
        var response = controller.eventsByCategory(session,
                Map.of("category", "Food", "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(2, response.numResults);
        assertEquals(2, response.eventIds.size());

        assertEquals(eventIds.get(2), response.eventIds.get(0));
        assertEquals(eventIds.get(0), response.eventIds.get(1));

        response = controller.eventsByCategory(session,
                Map.of("category", "Travel & Outdoor", "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(1, response.numResults);
        assertEquals(1, response.eventIds.size());
        assertEquals(eventIds.get(0), response.eventIds.get(0));

        response = controller.eventsByCategory(session,
                Map.of("category", "Hobbies", "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(0, response.numResults);
        assertEquals(0, response.eventIds.size());
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
                .withCategories(Set.of("Food", "Travel & Outdoor"))
                .withTags(Set.of("yummy", "delicious", "test"))
                .withStartDate(ZonedDateTime.now().plusDays(5))
                .withEndDate(ZonedDateTime.now().plusDays(6))
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
                .withCategories(Set.of("Business", "Education"))
                .withTags(Set.of("Science", "Experiment", "Test"))
                .withStartDate(ZonedDateTime.now().plusDays(1))
                .withEndDate(ZonedDateTime.now().plusDays(2))
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
                .withCategories(Set.of("Food"))
                .withTags(Set.of("sweet", "delicious", "test"))
                .withStartDate(ZonedDateTime.now().plusDays(3))
                .withEndDate(ZonedDateTime.now().plusDays(4))
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
