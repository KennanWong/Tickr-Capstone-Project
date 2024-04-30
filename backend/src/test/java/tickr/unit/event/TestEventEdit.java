package tickr.unit.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import tickr.server.exceptions.UnauthorizedException;
import tickr.util.CryptoHelper;
import tickr.util.FileHelper;

import static org.junit.jupiter.api.Assertions.*;

public class TestEventEdit {
    private DataModel model;
    private TickrController controller;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

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
    public void testExceptions() {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        var authToken = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(authTokenString);
        var id = authToken.getBody().getSubject();
        assertNotNull(id);

        var testAuthTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test2@example.com",
                "Password123!", "2022-04-14")).authToken;

        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        seats.add(seats1);
        seats.add(seats2);

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", 
                                                            "2000", "NSW", "Aus", "", "");

        Set<String> admins = new HashSet<>();
        admins.add(id);
        Set<String> invalidAdmins = new HashSet<>();
        invalidAdmins.add(UUID.randomUUID().toString());
        Set<String> invalidAdmins2 = new HashSet<>();
        invalidAdmins2.add("aaa");

        Set<String> categories = new HashSet<>();
        categories.add("Movie");
        categories.add("Sport");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        var newSession = TestHelper.commitMakeSession(model, session);  
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(UUID.randomUUID().toString(), authTokenString, null, 
            null, null, null,null, null, null, null, null, null, false, null)));
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
            null, null, "aaa",null, null, null, null, null, null, false, null)));
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
            null, null, null,"aaa", null, null, null, null, null, false, null)));
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
            null, null, null, null , null, null, invalidAdmins, null, null, false, null)));
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
            null, null, null, null , null, null, invalidAdmins2, null, null, false, null)));
        assertThrows(UnauthorizedException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, "asd", null, 
            null, null, null, null , null, null, null, null, null, false, null)));
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, testAuthTokenString, null, 
            null, null, null, null , null, null, null, null, null, false, null)));
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
            "asd", null, null, null , null, null, null, null, null, false, null)));
        assertThrows(ForbiddenException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, testAuthTokenString, null, 
            null, null, null, null , null, null, null, null, null, false, null)));
        EditEventRequest.SeatingDetails invalidSeats1 = new EditEventRequest.SeatingDetails(null, 50, 50, true);
        List<EditEventRequest.SeatingDetails> invalidSeatsList1 = new ArrayList<EditEventRequest.SeatingDetails>();
        invalidSeatsList1.add(invalidSeats1);
        assertThrows(BadRequestException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
        null, null, null,null, null, invalidSeatsList1, null, null, null, false, null)));

            EditEventRequest.SeatingDetails invalidSeats2 = new EditEventRequest.SeatingDetails("", 100, 50, true);
        List<EditEventRequest.SeatingDetails> invalidSeatsList2 = new ArrayList<EditEventRequest.SeatingDetails>();
        invalidSeatsList2.add(invalidSeats2);
        assertThrows(BadRequestException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
        null, null, null,null, null, invalidSeatsList2, null, null, null, false, null)));
        //     EditEventRequest.SeatingDetails invalidSeats3 = new EditEventRequest.SeatingDetails("SectionA", 0, 50, true);
        // List<EditEventRequest.SeatingDetails> invalidSeatsList3 = new ArrayList<EditEventRequest.SeatingDetails>();
        // invalidSeatsList3.add(invalidSeats3);
        // assertThrows(BadRequestException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
        //     null, null, null,null, null, invalidSeatsList3, null, null, null, false)));

        // EditEventRequest.SeatingDetails invalidSeats4 = new EditEventRequest.SeatingDetails("SectionA", 100, 50, false);
        // List<EditEventRequest.SeatingDetails> invalidSeatsList4 = new ArrayList<EditEventRequest.SeatingDetails>();
        // invalidSeatsList4.add(invalidSeats4);
        // assertThrows(BadRequestException.class, () -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, null, 
        //     null, null, null,null, null, invalidSeatsList4, null, null, null, false)));
    }

    @Test 
    public void testNull() {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
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

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", 
                                                            "2000", "NSW", "Aus", "", "");

        Set<String> admins = new HashSet<>();
        admins.add(id);

        Set<String> categories = new HashSet<>();
        categories.add("Movie");
        categories.add("Sport");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        session = TestHelper.commitMakeSession(model, session);  
        controller.editEvent(session, new EditEventRequest(event_id, authTokenString, null, null, null, null,null,
        null, null, null, null, null, false, null));
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
        assertEquals("description", response.description);

        assertEquals(seats.get(0).section, response.seatingDetails.get(0).section);
        assertEquals(seats.get(0).availability, response.seatingDetails.get(0).availableSeats);
        assertEquals(seats.get(1).section, response.seatingDetails.get(1).section);
        assertEquals(seats.get(1).availability, response.seatingDetails.get(1).availableSeats);

        assertEquals(admins, response.admins);
        assertEquals(categories, response.categories);
        assertEquals(tags, response.tags);
    }

    @Test 
    public void testEdit () {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var authToken = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(authTokenString);
        var id = authToken.getBody().getSubject();
        assertNotNull(id);

        var testAuthTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test2@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var authTokenTest = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(testAuthTokenString);
        var idTest = authTokenTest.getBody().getSubject();
        assertNotNull(idTest);

        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        seats.add(seats1);
        seats.add(seats2);
        EditEventRequest.SeatingDetails seats3 = new EditEventRequest.SeatingDetails("sectionC", 101, (float)51.5, true);
        EditEventRequest.SeatingDetails seats4 = new EditEventRequest.SeatingDetails("sectionD", 51, 51, true);
        List<EditEventRequest.SeatingDetails> updatedSeats = new ArrayList<EditEventRequest.SeatingDetails>();
        updatedSeats.add(seats3);
        updatedSeats.add(seats4);

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");
        SerializedLocation updatedLocation = new SerializedLocation("update street", 12, null, "Melbourne", "20200", "NS2W", "A2us", "", "");


        Set<String> admins = new HashSet<>();
        admins.add(id);

        Set<String> categories = new HashSet<>();
        categories.add("Movie");
        categories.add("Sport");
        Set<String> updateCategories = new HashSet<>();
        updateCategories.add("Basketball");
        updateCategories.add("Cricket");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");
        Set<String> updateTags = new HashSet<>();
        updateTags.add("updatetags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        admins.add(idTest);
        controller.editEvent(session, new EditEventRequest(event_id, authTokenString, "update name", null, updatedLocation, "2031-12-04T10:15:30Z","2031-12-05T10:15:30Z",
        "updated description", updatedSeats, admins, updateCategories, updateTags, true, null));
        session = TestHelper.commitMakeSession(model, session);
        var response = controller.eventView(session, Map.of("event_id", event_id)); 
        var newSession = TestHelper.commitMakeSession(model, session); 
        assertEquals(id, response.host_id);
        assertEquals("update name", response.eventName);
        assertEquals("", response.picture);
        assertEquals(updatedLocation.streetName, response.location.streetName);
        assertEquals(updatedLocation.streetNo, response.location.streetNo);
        assertEquals(updatedLocation.unitNo, response.location.unitNo);
        assertEquals(updatedLocation.postcode, response.location.postcode);
        assertEquals(updatedLocation.state, response.location.state);
        assertEquals(updatedLocation.country, response.location.country);
        //assertNull(response.location.longitude);
        //assertNull(response.location.latitude);
        assertEquals("2031-12-04T10:15:30Z", response.startDate);
        assertEquals("2031-12-05T10:15:30Z", response.endDate);
        assertEquals("updated description", response.description);

        assertEquals(updatedSeats.get(0).section, response.seatingDetails.get(0).section);
        assertEquals(updatedSeats.get(0).availability, response.seatingDetails.get(0).availableSeats);
        assertEquals(updatedSeats.get(0).ticketPrice, response.seatingDetails.get(0).ticketPrice);
        assertEquals(updatedSeats.get(1).section, response.seatingDetails.get(1).section);
        assertEquals(updatedSeats.get(1).availability, response.seatingDetails.get(1).availableSeats);
        assertEquals(updatedSeats.get(1).ticketPrice, response.seatingDetails.get(1).ticketPrice);

        assertEquals(admins, response.admins);
        assertEquals(updateCategories, response.categories);
        assertEquals(updateTags, response.tags);

        assertTrue(response.published);

        assertDoesNotThrow(() -> controller.editEvent(newSession, new EditEventRequest(event_id, authTokenString, "update name", null, null, "2031-12-04T10:15:30Z","2031-12-05T10:15:30Z",
        "updated description", null, admins, updateCategories, updateTags, false, null)));
        var newSession1 = TestHelper.commitMakeSession(model, newSession);
        assertDoesNotThrow(() -> controller.editEvent(newSession1, new EditEventRequest(event_id, authTokenString, "update name", null, updatedLocation, "2031-12-04T10:15:30Z","2031-12-05T10:15:30Z",
        "updated description", null, admins, updateCategories, updateTags, false, null)));
        var newSession2 = TestHelper.commitMakeSession(model, newSession1);
        assertDoesNotThrow(() -> controller.editEvent(newSession2, new EditEventRequest(event_id, authTokenString, "update name", null, null, "2031-12-04T10:15:30Z","2031-12-05T10:15:30Z",
        "updated description", updatedSeats, admins, updateCategories, updateTags, false, null)));
        TestHelper.commitMakeSession(model, newSession2);
    }

    @Test
    public void testUploadPfp() {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
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

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", 
                                                            "2000", "NSW", "Aus", "", "");

        Set<String> admins = new HashSet<>();
        admins.add(id);
        Set<String> invalidAdmins = new HashSet<>();
        invalidAdmins.add(UUID.randomUUID().toString());
        Set<String> invalidAdmins2 = new HashSet<>();
        invalidAdmins2.add("aaa");

        Set<String> categories = new HashSet<>();
        categories.add("Movie");
        categories.add("Sport");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        session = TestHelper.commitMakeSession(model, session);  
        controller.editEvent(session, new EditEventRequest(event_id, authTokenString, null, 
        FileHelper.readToDataUrl("/test_images/smile.jpg"), null, null,null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);
        var response = controller.eventView(session, Map.of("event_id", event_id));
        assertNotEquals("", response.picture);
        
        var newFilePath = FileHelper.getStaticPath() + "/" + response.picture;

        assertTrue(TestHelper.fileDiff("/test_images/smile.jpg", newFilePath));

    }

    @Test 
    public void testAdminEdit() {
        var session = model.makeSession();
        var authTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var authToken = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(authTokenString);
        var id = authToken.getBody().getSubject();
        assertNotNull(id);

        var adminAuthTokenString = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test2@example.com",
                "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var authTokenAdmin = CryptoHelper.makeJWTParserBuilder()
        .build()
        .parseClaimsJws(adminAuthTokenString);
        var idAdmin = authTokenAdmin.getBody().getSubject();
        assertNotNull(idAdmin);

        CreateEventRequest.SeatingDetails seats1 = new CreateEventRequest.SeatingDetails("sectionA", 100, 50, true);
        CreateEventRequest.SeatingDetails seats2 = new CreateEventRequest.SeatingDetails("sectionB", 50, 50, true);
        List<CreateEventRequest.SeatingDetails> seats = new ArrayList<CreateEventRequest.SeatingDetails>();
        seats.add(seats1);
        seats.add(seats2);

        SerializedLocation location = new SerializedLocation("test street", 12, null, "Sydney", "2000", "NSW", "Aus", "", "");


        Set<String> admins = new HashSet<>();
        admins.add(idAdmin);

        Set<String> categories = new HashSet<>();
        categories.add("Movie");
        categories.add("Sport");

        Set<String> tags = new HashSet<>();
        tags.add("testtags");

        var event_id = controller.createEvent(session, new CreateEventRequest(authTokenString, "test event", null, location
                                            , "2031-12-03T10:15:30Z",
                                            "2031-12-04T10:15:30Z", "description", seats, admins, categories, tags, null)).event_id;
        var newSession = TestHelper.commitMakeSession(model, session);  
        assertDoesNotThrow(() -> controller.editEvent(newSession, new EditEventRequest(event_id, adminAuthTokenString, "update name", null, null, "2031-12-04T10:15:30Z","2031-12-05T10:15:30Z",
        "updated description", null, null, null, null, true, null)));
        session = TestHelper.commitMakeSession(model, newSession);

        var response = controller.eventView(session, Map.of("event_id", event_id));
        assertEquals(id, response.host_id);
        assertEquals("update name", response.eventName);
        assertEquals("", response.picture);
        assertEquals("2031-12-04T10:15:30Z", response.startDate);
        assertEquals("2031-12-05T10:15:30Z", response.endDate);
        assertEquals("updated description", response.description);

    }
}
