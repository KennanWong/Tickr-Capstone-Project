package tickr.unit.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.SerializedLocation;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.util.CryptoHelper;

import static org.junit.jupiter.api.Assertions.*;

public class TestViewEvent {
    private DataModel model;
    private TickrController controller;
    private MockLocationApi locationApi;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        locationApi = new MockLocationApi(model);
        ApiLocator.addLocator(ILocationAPI.class, () -> locationApi);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void invalidEventId () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        var authToken = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(authTokenString);
        var id = authToken.getBody().getSubject();
        assertNotNull(id);
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);

        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        seats.add(seats1);
        seats.add(seats2);

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        
        Set<String> admins = new HashSet<>();
        admins.add(id);

        Set<String> categories = new HashSet<>();
        categories.add("testcategory");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        session = TestHelper.commitMakeSession(model, session); 
        var finalSession = session;
        assertThrows(BadRequestException.class, () -> controller.eventView(finalSession, Map.of("event_i", event_id)));
    }

    @Test 
    public void testViewEvent () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        var authToken = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(authTokenString);
        var id = authToken.getBody().getSubject();
        assertNotNull(id);
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, (float)50.5, true);

        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        seats.add(seats1);
        seats.add(seats2);

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        
        Set<String> admins = new HashSet<>();
        admins.add(id);

        Set<String> categories = new HashSet<>();
        categories.add("testcategory");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;

        /*controller.editEvent(session, new EditEventRequest(authTokenString, event_id, null, null, null, null,
                null, null, null, null, null, null, true));*/

        session = TestHelper.commitMakeSession(model, session); 

        var response = controller.eventView(session, Map.of("event_id", event_id, "auth_token", authTokenString));
        
        assertEquals(id, response.host_id);
        assertEquals("test event", response.eventName);
        assertEquals("", response.picture);
        assertEquals(location.streetName, response.location.streetName);
        assertEquals(location.streetNo, response.location.streetNo);
        assertEquals(location.unitNo, response.location.unitNo);
        assertEquals(location.postcode, response.location.postcode);
        assertEquals(location.state, response.location.state);
        assertEquals(location.country, response.location.country);
        //assertNull(response.location.longitude);
        //assertNull(response.location.latitude);
        assertEquals("2031-12-03T10:15:30Z", response.startDate);
        assertEquals("2031-12-04T10:15:30Z", response.endDate);

        assertEquals(seats.get(0).section, response.seatingDetails.get(0).section);
        assertEquals(seats.get(0).availability, response.seatingDetails.get(0).availableSeats);
        assertEquals(seats.get(0).ticketPrice, response.seatingDetails.get(0).ticketPrice);
        assertEquals(seats.get(1).section, response.seatingDetails.get(1).section);
        assertEquals(seats.get(1).availability, response.seatingDetails.get(1).availableSeats);
        assertEquals(seats.get(1).ticketPrice, response.seatingDetails.get(1).ticketPrice);
        assertEquals(150, response.seatAvailability);
        assertEquals(150, response.seatCapacity);

        Set<String> testAdmins = new HashSet<>();
        testAdmins.add(id); 
        assertEquals(testAdmins, response.admins);

        Set<String> testCategories = new HashSet<>(); 
        testCategories.add("testcategory");
        assertEquals(testCategories, response.categories);

        Set<String> testTags = new HashSet<>(); 
        testTags.add("testtags");
        assertEquals(testTags, response.tags);

        
    }

    @Test
    public void testPublished () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
                new UserRegisterRequest("test", "first", "last", "test1@example.com",
                        "Password123!", "2022-04-14")).authToken;
        var authToken = CryptoHelper.makeJWTParserBuilder()
                .build()
                .parseClaimsJws(authTokenString);
        session = TestHelper.commitMakeSession(model, session);
        var id = authToken.getBody().getSubject();
        assertNotNull(id);
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, (float)50.5, true);

        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        seats.add(seats1);
        seats.add(seats2);

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");

        Set<String> admins = new HashSet<>();
        admins.add(id);

        Set<String> categories = new HashSet<>();
        categories.add("testcategory");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                , "2031-12-03T10:15:30Z",
                "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        var session1 = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.eventView(session1, Map.of("event_id", event_id)));
        var session2 = TestHelper.rollbackMakeSession(model, session1);
        controller.editEvent(session2, new EditEventRequest(event_id, authTokenString, null, null, null, null,
                null, null, null, null, null, null, true, null));
        var session3 = TestHelper.commitMakeSession(model, session2);
        assertDoesNotThrow(() -> controller.eventView(session3, Map.of("event_id", event_id)));
    }

    @Test
    public void testLocation () {
        var session = model.makeSession();
        var authToken = controller.userRegister(session,
                new UserRegisterRequest("test", "first", "last", "test1@example.com",
                        "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var location = new SerializedLocation.Builder()
                .withStreetNo(1)
                .withStreetName("High St")
                .withSuburb("Kensington")
                .withPostcode("2052")
                .withState("NSW")
                .withCountry("Australia")
                .build();

        var eventId = controller.createEvent(session, new CreateEventReqBuilder().withLocation(location).build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        locationApi.awaitLocations();


        var response = controller.eventView(session, Map.of(
                "event_id", eventId,
                "auth_token", authToken
        ));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals("-33.9148449", response.location.latitude);
        assertEquals("151.2254725", response.location.longitude);
    }

    @Test 
    public void testCreateWithSpotifyPlaylist () {
        var session = model.makeSession();
        var authToken = controller.userRegister(session,
                new UserRegisterRequest("test", "first", "last", "test1@example.com",
                        "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var eventId = controller.createEvent(session, new CreateEventReqBuilder().withSpotifyPlaylist("spotifyplaylist").build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.eventView(session, Map.of(
                "event_id", eventId,
                "auth_token", authToken
        ));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals("spotifyplaylist", response.spotifyPlaylist);

        var eventId2 = controller.createEvent(session, new CreateEventReqBuilder()
                .withSpotifyPlaylist("spotifyplaylist")
                .withPicture("/test_images/smile.png")
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        response = controller.eventView(session, Map.of(
                "event_id", eventId2,
                "auth_token", authToken
        ));
        session = TestHelper.commitMakeSession(model, session);
        
        assertEquals("spotifyplaylist", response.spotifyPlaylist);

        controller.editEvent(session, new EditEventRequest(eventId2, authToken, null, null, null, null,
                null, null, null, null, null, null, true, "spotify2"));
        session = TestHelper.commitMakeSession(model, session);

        response = controller.eventView(session, Map.of(
                "event_id", eventId2,
                "auth_token", authToken
        ));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals("spotify2", response.spotifyPlaylist);

        controller.editEvent(session, new EditEventRequest(eventId2, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        response = controller.eventView(session, Map.of(
                "event_id", eventId2,
                "auth_token", authToken
        ));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals("spotify2", response.spotifyPlaylist);
    }
}
