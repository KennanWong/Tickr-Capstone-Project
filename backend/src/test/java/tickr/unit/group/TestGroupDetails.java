package tickr.unit.group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.combined.tickets.TicketReserve.ReserveDetails;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.group.GroupAcceptRequest;
import tickr.application.serialised.requests.group.GroupCreateRequest;
import tickr.application.serialised.requests.group.GroupDenyRequest;
import tickr.application.serialised.requests.group.GroupInviteRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.group.GroupDetailsResponse.GroupMember;
import tickr.application.serialised.responses.group.GroupDetailsResponse.PendingInvite;
import tickr.mock.AbstractMockPurchaseAPI;
import tickr.mock.MockEmailAPI;
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

public class TestGroupDetails {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;
    private AbstractMockPurchaseAPI purchaseAPI;
    private MockEmailAPI emailAPI;

    private String eventId;
    private String authToken; 
    private String authToken2;
    private String authToken3;
    private String authToken4;
    private String authToken5;

    private String hostEmail = "test1@example.com";

    private String inviteId1;
    private String inviteId2;
    private String inviteId3;
    private String inviteId4;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<String> requestIds;
    private float requestPrice;

    private List<String> reserveIdList;

    private String groupId;

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

        authToken3 = controller.userRegister(session,
                new UserRegisterRequest("test", "first", "last", "test3@example.com",
                        "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        authToken4 = controller.userRegister(session,
                new UserRegisterRequest("test", "first", "last", "test4@example.com",
                        "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        authToken5 = controller.userRegister(session,
                new UserRegisterRequest("test", "first", "last", "test5@example.com",
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
                new TicketReserve.TicketDetails("SectionA", 3, List.of(1, 2, 3)),
                new TicketReserve.TicketDetails("SectionB", 2, List.of(1, 2))
        )));
        session = TestHelper.commitMakeSession(model, session);
        reserveIdList = response.reserveTickets.stream()
                .map(ReserveDetails::getReserveId)
                .collect(Collectors.toList());
        
        groupId = controller.groupCreate(session, new GroupCreateRequest(authToken, reserveIdList, reserveIdList.get(0))).groupId;
        session = TestHelper.commitMakeSession(model, session);

        emailAPI = new MockEmailAPI();
        ApiLocator.addLocator(IEmailAPI.class, () -> emailAPI);

        controller.groupInvite(session, new GroupInviteRequest(authToken, groupId, reserveIdList.get(1), "test2@example.com"));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupInvite(session, new GroupInviteRequest(authToken, groupId, reserveIdList.get(2), "test3@example.com"));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupInvite(session, new GroupInviteRequest(authToken, groupId, reserveIdList.get(3), "test4@example.com"));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupInvite(session, new GroupInviteRequest(authToken, groupId, reserveIdList.get(4), "test5@example.com"));
        session = TestHelper.commitMakeSession(model, session);

        inviteId1 = emailAPI.getSentMessages().get(0).getBody().split("/group/")[1].split("\"")[0];
        inviteId2 = emailAPI.getSentMessages().get(1).getBody().split("/group/")[1].split("\"")[0];
        inviteId3 = emailAPI.getSentMessages().get(2).getBody().split("/group/")[1].split("\"")[0];
        inviteId4 = emailAPI.getSentMessages().get(3).getBody().split("/group/")[1].split("\"")[0];
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IEmailAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testNoAcceptDetails() {
        var hostId = controller.authenticateToken(session, authToken).getId().toString();
        session = TestHelper.commitMakeSession(model, session);
        var response = controller.groupDetails(session, Map.of("auth_token", authToken, "group_id", groupId));
        session = TestHelper.commitMakeSession(model, session);

        var members = response.groupMembers;
        var invites = response.pendingInvites;
        
        assertEquals(1, members.size());
        assertEquals(4, invites.size());
        assertEquals(0, response.availableReserves.size());
        assertEquals(hostId, response.hostId);
        assertEquals(hostEmail, members.get(0).email);
        assertEquals(eventId, response.eventId);
        for (PendingInvite i : invites) {
            assertNotNull(i.email);
            assertNotNull(i.inviteId);
            assertNotNull(i.seatNum);
            assertNotNull(i.section);
        }
    }

    @Test 
    public void testAllAcceptDetails() {
        controller.groupAccept(session, new GroupAcceptRequest(authToken2, inviteId1));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupAccept(session, new GroupAcceptRequest(authToken3, inviteId2));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupAccept(session, new GroupAcceptRequest(authToken4, inviteId3));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupAccept(session, new GroupAcceptRequest(authToken5, inviteId4));
        session = TestHelper.commitMakeSession(model, session);

        var hostId = controller.authenticateToken(session, authToken).getId().toString();
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.groupDetails(session, Map.of("auth_token", authToken, "group_id", groupId));
        session = TestHelper.commitMakeSession(model, session);

        var members = response.groupMembers;
        var invites = response.pendingInvites;

        assertEquals(5, members.size());
        assertEquals(0, invites.size());
        assertEquals(0, response.availableReserves.size());
        assertEquals(hostId, response.hostId);

        for (GroupMember m : members) {
            assertNotNull(m.email);
            assertNotNull(m.section);
            assertNotNull(m.seatNum);   
            assertEquals(false, m.purchased);         
        }
    }

    @Test 
    public void testSomeDenyDetails () {
        controller.groupAccept(session, new GroupAcceptRequest(authToken2, inviteId1));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupAccept(session, new GroupAcceptRequest(authToken3, inviteId2));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupDeny(session, new GroupDenyRequest(inviteId3));
        session = TestHelper.commitMakeSession(model, session);

        var hostId = controller.authenticateToken(session, authToken).getId().toString();
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.groupDetails(session, Map.of("auth_token", authToken, "group_id", groupId));
        session = TestHelper.commitMakeSession(model, session);

        var members = response.groupMembers;
        var invites = response.pendingInvites;
        
        assertEquals(3, members.size());
        assertEquals(1, invites.size());
        assertEquals(1, response.availableReserves.size());
        assertEquals(hostId, response.hostId);
    }

    @Test 
    public void testAllDenyDetails() {
        controller.groupDeny(session, new GroupDenyRequest(inviteId1));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupDeny(session, new GroupDenyRequest(inviteId2));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupDeny(session, new GroupDenyRequest(inviteId3));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupDeny(session, new GroupDenyRequest(inviteId4));
        session = TestHelper.commitMakeSession(model, session);

        var hostId = controller.authenticateToken(session, authToken).getId().toString();
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.groupDetails(session, Map.of("auth_token", authToken, "group_id", groupId));
        session = TestHelper.commitMakeSession(model, session);

        var members = response.groupMembers;
        var invites = response.pendingInvites;

        assertEquals(1, members.size());
        assertEquals(0, invites.size());
        assertEquals(4, response.availableReserves.size());
        assertEquals(hostId, response.hostId);
    }

    @Test 
    public void testGroupTicketPurchaseDetails() {
        controller.groupAccept(session, new GroupAcceptRequest(authToken2, inviteId1));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupAccept(session, new GroupAcceptRequest(authToken3, inviteId2));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupDeny(session, new GroupDenyRequest(inviteId3));
        session = TestHelper.commitMakeSession(model, session);

        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);

        var reqIds = List.of(reserveIdList.get(1)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken2,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(redirectUrl, "test_customer");

        reqIds = List.of(reserveIdList.get(2)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken3,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(redirectUrl, "test_customer");

        reqIds = List.of(reserveIdList.get(0)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", reqIds)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(redirectUrl, "test_customer");

        var hostId = controller.authenticateToken(session, authToken).getId().toString();
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.groupDetails(session, Map.of("auth_token", authToken, "group_id", groupId));
        session = TestHelper.commitMakeSession(model, session);

        var members = response.groupMembers;
        var invites = response.pendingInvites;

        assertEquals(3, members.size());
        assertEquals(1, invites.size());
        assertEquals(1, response.availableReserves.size());
        assertEquals(hostId, response.hostId);

        for (GroupMember m : members) {
            assertEquals(true, m.purchased);
        }
    }

    @Test 
    public void testGroupTicketView() {
        controller.groupAccept(session, new GroupAcceptRequest(authToken2, inviteId1));
        session = TestHelper.commitMakeSession(model, session);
        controller.groupDeny(session, new GroupDenyRequest(inviteId3));
        session = TestHelper.commitMakeSession(model, session);

        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);

        var reqIds1 = List.of(reserveIdList.get(1)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken2,
                "https://example.com/success", "https://example.com/cancel", reqIds1)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(redirectUrl, "test_customer");

        var reqIds2 = List.of(reserveIdList.get(0)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "https://example.com/success", "https://example.com/cancel", reqIds2)).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(redirectUrl, "test_customer");

        var ticketId = controller.ticketBookings(session, Map.of("auth_token", authToken, "event_id", eventId)).tickets.get(0);
        session = TestHelper.commitMakeSession(model, session);
        var view = controller.ticketView(session, Map.of("ticket_id", ticketId));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(groupId, view.groupId);

        ticketId = controller.ticketBookings(session, Map.of("auth_token", authToken2, "event_id", eventId)).tickets.get(0);
        session = TestHelper.commitMakeSession(model, session);
        view = controller.ticketView(session, Map.of("ticket_id", ticketId));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(null, view.groupId);
    }

    @Test 
    public void testExceptions() {
        assertThrows(BadRequestException.class, () -> controller.groupDetails(session, Map.of("group_id", groupId)));
        assertThrows(BadRequestException.class, () -> controller.groupDetails(session, Map.of("auth_token", authToken)));
        assertThrows(UnauthorizedException.class, () -> controller.groupDetails(session, Map.of("auth_token", " ", "group_id", groupId)));
        assertThrows(ForbiddenException.class, () -> controller.groupDetails(session, Map.of("auth_token", authToken, "group_id", UUID.randomUUID().toString())));
    }
}
