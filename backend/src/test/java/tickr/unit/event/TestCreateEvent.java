package tickr.unit.event;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.entities.Category;
import tickr.application.entities.Event;
import tickr.application.entities.SeatingPlan;
import tickr.application.entities.Tag;
import tickr.application.entities.User;
import tickr.application.serialised.SerializedLocation;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;
import tickr.util.CryptoHelper;
import tickr.util.FileHelper;

import static org.junit.jupiter.api.Assertions.*;

public class TestCreateEvent {
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

    @AfterAll
    public static void clearStaticFiles () {
        TestHelper.clearStaticFiles();
    }

    @Test
    public void testMissingParamaters () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        seats.add(seats1);
        seats.add(seats2);
        Set<String> admins = new HashSet<>();
        admins.add(UUID.randomUUID().toString());
        Set<String> categories = new HashSet<>();
        categories.add("testcategory");
        Set<String> tags = new HashSet<>();
        tags.add("testtags");
        tickr.persistence.ModelSession finalSession = session;
        assertThrows(ForbiddenException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "test event",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, Set.of(UUID.randomUUID().toString()), categories, tags, null)));
        assertThrows(ForbiddenException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "test event",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(UnauthorizedException.class, () -> controller.createEvent(finalSession, new CreateEventRequest("authTokenString", "test event",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest("", "test event",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, null,
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "",
            null, null, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "",
            null, location, null, "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "",
            null, location, "2031-12-03T10:15:30Z", null, "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, null, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, null, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, null, null)));
        CreateEventRequest.SeatingDetails invalidSeats1 = new CreateEventRequest.SeatingDetails(null, 50, 50, true);
        List<CreateEventRequest.SeatingDetails> invalidSeatsList1 = new ArrayList<CreateEventRequest.SeatingDetails>();
        invalidSeatsList1.add(invalidSeats1);
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", invalidSeatsList1, admins, categories, tags, null)));

        CreateEventRequest.SeatingDetails invalidSeats2 = new CreateEventRequest.SeatingDetails("", 100, 50, true);
        List<CreateEventRequest.SeatingDetails> invalidSeatsList2 = new ArrayList<CreateEventRequest.SeatingDetails>();
        invalidSeatsList2.add(invalidSeats2);
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
            null, location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", invalidSeatsList2, admins, categories, tags, null)));

        // CreateEventRequest.SeatingDetails invalidSeats3 = new CreateEventRequest.SeatingDetails("SectionA", 0, 50, true);
        // List<CreateEventRequest.SeatingDetails> invalidSeatsList3 = new ArrayList<CreateEventRequest.SeatingDetails>();
        // invalidSeatsList3.add(invalidSeats3);
        // assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
        //     null, location, "2031-12-03T10:15:30", "2031-12-04T10:15:30", "description", invalidSeatsList3, admins, categories, tags)));

        // CreateEventRequest.SeatingDetails invalidSeats4 = new CreateEventRequest.SeatingDetails("SectionA", 100, 50, false);
        // List<CreateEventRequest.SeatingDetails> invalidSeatsList4 = new ArrayList<CreateEventRequest.SeatingDetails>();
        // invalidSeatsList4.add(invalidSeats4);
        // assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
        //     null, location, "2031-12-03T10:15:30", "2031-12-04T10:15:30", "description", invalidSeatsList4, admins, categories, tags)));
    }

    @Test 
    public void testMissingLocationParamaters () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        seats.add(seats1);
        seats.add(seats2);
        Set<String> admins = new HashSet<>();
        admins.add("test1@example.com");
        Set<String> categories = new HashSet<>();
        categories.add("testcategory");
        Set<String> tags = new HashSet<>();
        tags.add("testtags");
        SerializedLocation location1 = new SerializedLocation("test street", 12, null, "Sydney",null, "NSW", "Aus", "", "");
        SerializedLocation location2 = new SerializedLocation("test street", 12, null, "Sydney","2000", null, "Aus", "", "");
        SerializedLocation location3 = new SerializedLocation("test street", 12, null, "Sydney","2000", "NSW", null, "", "");
        SerializedLocation location4 = new SerializedLocation("test street", 12, null, "Sydney","2000", "NSW", "Aus", null, "");
        SerializedLocation location5 = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", null);
        var finalSession = session;
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
            null, location1, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
            null, location2, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
            null, location3, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
            null, location4, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(BadRequestException.class, () -> controller.createEvent(finalSession, new CreateEventRequest(authTokenString, "asd",
            null, location5, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
    }

    @Test 
    public void testInvalidToken () {
        var session = model.makeSession();
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        seats.add(seats1);
        seats.add(seats2);
        Set<String> admins = new HashSet<>();
        admins.add("test1@example.com");
        Set<String> categories = new HashSet<>();
        categories.add("testcategory");
        Set<String> tags = new HashSet<>();
        tags.add("testtags");
        assertThrows(UnauthorizedException.class, () -> controller.createEvent(session, new CreateEventRequest("authTokenString", "test event", 
        null, location
        , "2031-12-03T10:15:30Z",
        "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
        assertThrows(UnauthorizedException.class, () -> controller.createEvent(session, new CreateEventRequest(null, "test event", 
        null, location
        , "2031-12-03T10:15:30Z",
        "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)));
    }

    @Test 
    public void testInvalidDates () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        seats.add(seats1);
        seats.add(seats2);
        Set<String> admins = new HashSet<>();
        admins.add("test1@example.com");
        Set<String> categories = new HashSet<>();
        categories.add("testcategory");
        Set<String> tags = new HashSet<>();
        tags.add("testtags");
        assertThrows(BadRequestException.class, () -> controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", 
        null, location
        , "2031-12-04T10:15:30Z",
        "2031-12-03T10:15:30Z", "description", seats, admins, categories, tags, null)));

        assertThrows(ForbiddenException.class, () -> controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", 
        null, location
        , "2031-12-04 10:15:30Z",
        "2031-12-03 10:15:30Z", "description", seats, admins, categories, tags, null)));
    }

    @Test
    public void testCreateEvent () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        var authToken = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(authTokenString);
        var id = authToken.getBody().getSubject();
        assertNotNull(id);
        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, (float)50.5, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 70, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        seats.add(seats1);
        seats.add(seats2);
        Set<String> admins = new HashSet<>();
        admins.add(id);
        Set<String> categories = new HashSet<>();
        categories.add("testcategory");
        Set<String> tags = new HashSet<>();
        tags.add("testtags");
        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        var event_uuid = UUID.fromString(event_id);
        session = TestHelper.commitMakeSession(model, session);  
        var eventOpt = session.getById(Event.class, event_uuid);
        assertTrue(eventOpt.isPresent());
        var event = eventOpt.orElse(null);
        assertEquals("test event", event.getEventName());
        // assertEquals(location, event.getLocation());
        ZonedDateTime startDate = ZonedDateTime.parse("2031-12-03T10:15:30Z");
        ZonedDateTime endDate = ZonedDateTime.parse("2031-12-04T10:15:30Z");
        assertEquals(startDate.withZoneSameLocal(ZoneId.of("UTC")), event.getEventStart().withZoneSameLocal(ZoneId.of("UTC")));
        assertEquals(endDate.withZoneSameLocal(ZoneId.of("UTC")), event.getEventEnd().withZoneSameLocal(ZoneId.of("UTC")));
        assertEquals("description", event.getEventDescription());
        assertEquals(150, event.getSeatAvailability());
        for (Tag tag : event.getTags()) {
            assertEquals("testtags", tag.getTags());
        }
        for (Category cat : event.getCategories()) {
            assertEquals("testcategory", cat.getCategory());
        }
        for (User admin : event.getAdmins()) {
            assertEquals(id, admin.getId().toString());
        }
        Event event1 = session.getById(Event.class, UUID.fromString(event_id)).orElse(null);
        List<SeatingPlan> seatings = session.getAllWith(SeatingPlan.class, "event", event1);
        assertEquals(seatings.get(0).getSection(), "sectionA");
        assertEquals(seatings.get(0).availableSeats, 100);
        assertEquals(seatings.get(0).ticketPrice, 50.5);
        assertEquals(seatings.get(1).getSection(), "sectionB");
        assertEquals(seatings.get(1).availableSeats, 50);
        assertEquals(seatings.get(1).ticketPrice, 70);
        assertEquals(event.getLocation().getStreetName(), location.streetName);
        assertEquals(event.getLocation().getStreetNo(), location.streetNo);
        assertEquals(event.getLocation().getUnitNo(), location.unitNo);
        assertEquals(event.getLocation().getPostcode(), location.postcode);
        assertEquals(event.getLocation().getState(), location.state);
        assertEquals(event.getLocation().getCountry(), location.country);
        assertNull(event.getLocation().getLatitudeStr());
        assertNull(event.getLocation().getLongitudeStr());

        var newSession = TestHelper.commitMakeSession(model, session); 
        assertDoesNotThrow(() -> controller.createEvent(newSession, new CreateEventRequest(authTokenString, "test event", null, location
        , "2031-12-03T10:15:30Z",
        "2031-12-04T10:15:30Z", "description", null, admins, categories, tags, null)));
        var newSession1 = TestHelper.commitMakeSession(model, newSession);
        assertDoesNotThrow(() -> controller.createEvent(newSession1, new CreateEventRequest(authTokenString, "test event", null, location
        , "2031-12-03T10:15:30Z",
        "2031-12-04T10:15:30Z", "description", seats, null, categories, tags, null)));
        var newSession2 = TestHelper.commitMakeSession(model, newSession1);
        assertDoesNotThrow(() -> controller.createEvent(newSession2, new CreateEventRequest(authTokenString, "test event", null, location
        , "2031-12-03T10:15:30Z",
        "2031-12-04T10:15:30Z", "description", seats, admins, null, tags, null)));
        var newSession3 = TestHelper.commitMakeSession(model, newSession2);
        assertDoesNotThrow(() -> controller.createEvent(newSession3, new CreateEventRequest(authTokenString, "test event", null, location
        , "2031-12-03T10:15:30Z",
        "2031-12-04T10:15:30Z", "description", seats, admins, categories, null, null)));
        TestHelper.commitMakeSession(model, newSession3);
    }

    @Test
    public void testTwoCreateEvent () {
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
        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        seats.add(seats1);
        seats.add(seats2);
        Set<String> admins = new HashSet<>();
        admins.add(id);
        Set<String> categories = new HashSet<>();
        categories.add("testcategory");
        Set<String> tags = new HashSet<>();
        tags.add("testtags");
        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        var event_uuid = UUID.fromString(event_id);
        session = TestHelper.commitMakeSession(model, session);  
        var eventOpt = session.getById(Event.class, event_uuid);
        assertTrue(eventOpt.isPresent());
        var event = eventOpt.orElse(null);
        assertEquals("test event", event.getEventName());
        // assertEquals(location, event.getLocation());
        ZonedDateTime startDate = ZonedDateTime.parse("2031-12-03T10:15:30Z");
        ZonedDateTime endDate = ZonedDateTime.parse("2031-12-04T10:15:30Z");
        assertEquals(startDate.withZoneSameLocal(ZoneId.of("UTC")), event.getEventStart().withZoneSameLocal(ZoneId.of("UTC")));
        assertEquals(endDate.withZoneSameLocal(ZoneId.of("UTC")), event.getEventEnd().withZoneSameLocal(ZoneId.of("UTC")));
        assertEquals("description", event.getEventDescription());
        assertEquals(150, event.getSeatAvailability());
        for (Tag tag : event.getTags()) {
            assertEquals("testtags", tag.getTags());
        }
        for (Category cat : event.getCategories()) {
            assertEquals("testcategory", cat.getCategory());
        }
        for (User admin : event.getAdmins()) {
            assertEquals(id, admin.getId().toString());
        }
        Event event1 = session.getById(Event.class, UUID.fromString(event_id)).orElse(null);
        List<SeatingPlan> seatings = session.getAllWith(SeatingPlan.class, "event", event1);
        assertEquals(seatings.get(0).getSection(), "sectionA");
        assertEquals(seatings.get(0).availableSeats, 100);
        assertEquals(seatings.get(0).ticketPrice, 50);
        assertEquals(seatings.get(1).getSection(), "sectionB");
        assertEquals(seatings.get(1).availableSeats, 50);
        assertEquals(seatings.get(1).ticketPrice, 50);
        assertEquals(event.getLocation().getStreetName(), location.streetName);
        assertEquals(event.getLocation().getStreetNo(), location.streetNo);
        assertEquals(event.getLocation().getUnitNo(), location.unitNo);
        assertEquals(event.getLocation().getPostcode(), location.postcode);
        assertEquals(event.getLocation().getState(), location.state);
        assertEquals(event.getLocation().getCountry(), location.country);
        assertNull(event.getLocation().getLatitudeStr());
        assertNull(event.getLocation().getLongitudeStr());
        var event_id2 = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event2", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        var event_uuid2 = UUID.fromString(event_id2);
        session = TestHelper.commitMakeSession(model, session);  
        var eventOpt2 = session.getById(Event.class, event_uuid2);
        assertTrue(eventOpt2.isPresent());
        var event2 = eventOpt2.orElse(null);
        assertEquals("test event2", event2.getEventName());
        // assertEquals(location, event.getLocation());
        assertEquals(startDate, event.getEventStart());
        assertEquals(endDate, event.getEventEnd());
        assertEquals("description", event2.getEventDescription());
        assertEquals(150, event2.getSeatAvailability());      
        
        Event event3 = session.getById(Event.class, UUID.fromString(event_id)).orElse(null);
        List<SeatingPlan> seatings2 = session.getAllWith(SeatingPlan.class, "event", event3);
        assertEquals(seatings2.get(0).getSection(), "sectionA");
        assertEquals(seatings2.get(0).availableSeats, 100);
        assertEquals(seatings2.get(0).ticketPrice, 50);
        assertEquals(seatings2.get(1).getSection(), "sectionB");
        assertEquals(seatings2.get(1).availableSeats, 50);
        assertEquals(seatings2.get(1).ticketPrice, 50);
        assertEquals(event2.getLocation().getStreetName(), location.streetName);
        assertEquals(event2.getLocation().getStreetNo(), location.streetNo);
        assertEquals(event2.getLocation().getUnitNo(), location.unitNo);
        assertEquals(event2.getLocation().getPostcode(), location.postcode);
        assertEquals(event2.getLocation().getState(), location.state);
        assertEquals(event2.getLocation().getCountry(), location.country);
        assertNull(event2.getLocation().getLatitudeStr());
        assertNull(event2.getLocation().getLongitudeStr());
    }

    @Test
    public void testCreateEventImage () {
        var session = model.makeSession();
        var authToken = controller.userRegister(session,
                new UserRegisterRequest("test", "first", "last", "test1@example.com",
                        "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        var event_id = controller.createEvent(session, new CreateEventRequest(authToken, "test event", FileHelper.readToDataUrl("/test_images/smile.png"),
                location, "2031-12-03T10:15:30Z", "2031-12-04T10:15:30Z", "description", List.of(), Set.of(), Set.of(), Set.of(), null)).event_id;

        session = TestHelper.commitMakeSession(model, session);
        var event = session.getById(Event.class, UUID.fromString(event_id)).orElseThrow(AssertionError::new);
        var newFilePath = FileHelper.getStaticPath() + "/" + event.getEventPicture();

        assertTrue(TestHelper.fileDiff("/test_images/smile.png", newFilePath));
    }

    @Test
    public void testCreateEventLocation () {
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
        session.commit();
        session.close();
        locationApi.awaitLocations();
        session = model.makeSession();

        var event = session.getById(Event.class, UUID.fromString(eventId)).orElseThrow(AssertionError::new);

        assertEquals("-33.9148449", event.getLocation().getLatitudeStr());
        assertEquals("151.2254725", event.getLocation().getLongitudeStr());
    }
}
