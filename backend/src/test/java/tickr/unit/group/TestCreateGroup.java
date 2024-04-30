package tickr.unit.group;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.combined.tickets.TicketReserve.ReserveDetails;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.group.GroupCreateRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.AbstractMockPurchaseAPI;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestCreateGroup {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;
    private AbstractMockPurchaseAPI purchaseAPI;

    private String eventId;
    private String authToken; 
    private String authToken2;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<String> requestIds;
    private float requestPrice;

    private List<String> reserveIdList;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();

        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        List<CreateEventRequest.SeatingDetails> seatingDetails = new ArrayList<>();
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionA", 10, 50, true));
        seatingDetails.add(new CreateEventRequest.SeatingDetails("SectionB", 20, 30, true));

        session = model.makeSession();

        authToken = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14")).authToken;

        session = TestHelper.commitMakeSession(model, session);
        
        authToken2 = controller.userRegister(session,
        new UserRegisterRequest("test", "first", "last", "test2@example.com",
                "Password123!", "2022-04-14")).authToken;

        session = TestHelper.commitMakeSession(model, session);

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));
        
        eventId = controller.createEvent(session, new CreateEventReqBuilder()
            .withEventName("Test Event")
            .withSeatingDetails(seatingDetails)
            .withStartDate(startTime.minusMinutes(2))
            .withEndDate(endTime)
            .build(authToken)).event_id;
        
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("SectionA", 1, List.of(1)),
                new TicketReserve.TicketDetails("SectionB", 1, List.of(2))
        )));
        session = TestHelper.commitMakeSession(model, session);
        reserveIdList = response.reserveTickets.stream()
                .map(ReserveDetails::getReserveId)
                .collect(Collectors.toList());
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testCreateGroup() {
        var group = controller.groupCreate(session, new GroupCreateRequest(authToken, reserveIdList, reserveIdList.get(0)));
        assertNotNull(group.groupId);
        session = TestHelper.commitMakeSession(model, session);
        var groups = controller.getGroupIds(session, Map.of("auth_token", authToken, "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(1, groups.groups.size());
        assertEquals(1, groups.numResults);
        assertThrows(ForbiddenException.class, () -> controller.groupCreate(session, new GroupCreateRequest(authToken, reserveIdList, reserveIdList.get(0))));
    }

    @Test 
    public void testGroupCreateExceptions() {
        assertThrows(UnauthorizedException.class, () -> controller.groupCreate(session, new GroupCreateRequest(null, reserveIdList, reserveIdList.get(0))));
        assertThrows(ForbiddenException.class, () -> controller.groupCreate(session, new GroupCreateRequest(authToken, List.of(UUID.randomUUID().toString()), reserveIdList.get(0))));
        assertThrows(BadRequestException.class, () -> controller.groupCreate(session, new GroupCreateRequest(authToken, null, reserveIdList.get(0))));
        assertThrows(BadRequestException.class, () -> controller.groupCreate(session, new GroupCreateRequest(authToken, reserveIdList, null)));
        assertThrows(ForbiddenException.class, () -> controller.groupCreate(session, new GroupCreateRequest(authToken, reserveIdList, UUID.randomUUID().toString())));
    }

    @Test 
    public void testGetGroupIdExceptions() {
        assertThrows(BadRequestException.class, () -> controller.getGroupIds(session, Map.of(
            "page_start", "0", 
            "max_results", "10"
        )));
        assertThrows(BadRequestException.class, () -> controller.getGroupIds(session, Map.of(
            "auth_token", authToken, 
            "max_results", "10"
        )));
        assertThrows(BadRequestException.class, () -> controller.getGroupIds(session, Map.of(
            "auth_token", authToken, 
            "page_start", "0"
        )));
        assertThrows(BadRequestException.class, () -> controller.getGroupIds(session, Map.of(
            "auth_token", authToken, 
            "page_start", "-1", 
            "max_results", "10"
        )));
        assertThrows(BadRequestException.class, () -> controller.getGroupIds(session, Map.of(
            "auth_token", authToken, 
            "page_start", "0", 
            "max_results", "-1"
        )));
    }

}
