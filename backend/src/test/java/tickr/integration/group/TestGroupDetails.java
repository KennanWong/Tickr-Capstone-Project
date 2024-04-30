package tickr.integration.group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.CreateEventReqBuilder;
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
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.group.GroupCreateResponse;
import tickr.application.serialised.responses.group.GroupDetailsResponse;
import tickr.application.serialised.responses.ticket.TicketBookingsResponse;
import tickr.application.serialised.responses.ticket.TicketViewResponse;
import tickr.application.serialised.responses.user.UserIdResponse;
import tickr.application.serialised.responses.group.GroupDetailsResponse.GroupMember;
import tickr.application.serialised.responses.group.GroupDetailsResponse.PendingInvite;
import tickr.mock.MockEmailAPI;
import tickr.mock.MockHttpPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGroupDetails {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private MockHttpPurchaseAPI purchaseAPI;
    private MockEmailAPI emailAPI;

    private String hostId;
    private String hostEmail = "test1@example.com";

    private String authToken;
    private String authToken2;
    private String authToken3;
    private String authToken4;
    private String authToken5;

    private String inviteId1;
    private String inviteId2;
    private String inviteId3;
    private String inviteId4;

    private String eventId;

    private String requestId;
    private List<String> reserveIdList;
    private float requestPrice;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private String groupId;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        purchaseAPI = new MockHttpPurchaseAPI("http://localhost:8080");
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(hibernateModel));

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test1@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;

        response =  httpHelper.get("/api/user/search", Map.of("email", "test1@example.com"));
        assertEquals(200, response.getStatus());
        hostId = response.getBody(UserIdResponse.class).userId;

        response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test2@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken2 = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test3@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken3 = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test4@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken4 = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test5@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken5 = response.getBody(AuthTokenResponse.class).authToken;

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));

        List<CreateEventRequest.SeatingDetails> seatingDetails = List.of(
                new CreateEventRequest.SeatingDetails("test_section", 10, 1, true),
                new CreateEventRequest.SeatingDetails("test_section2", 20, 4, true)
        );

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventId = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("test_section", 3, List.of(1, 2, 3)),
                new TicketReserve.TicketDetails("test_section2", 2, List.of(1, 2))
        )));
        assertEquals(200, response.getStatus());
        var reserveResponse = response.getBody(TicketReserve.Response.class);
        reserveIdList = reserveResponse.reserveTickets.stream()
                .map(ReserveDetails::getReserveId)
                .collect(Collectors.toList());

        response = httpHelper.post("/api/group/create", new GroupCreateRequest(authToken, reserveIdList, reserveIdList.get(0)));
        assertEquals(200, response.getStatus());
        groupId = response.getBody(GroupCreateResponse.class).groupId;
        
        emailAPI = new MockEmailAPI();
        ApiLocator.addLocator(IEmailAPI.class, () -> emailAPI);

        response = httpHelper.post("/api/group/invite", new GroupInviteRequest(authToken, groupId, reserveIdList.get(1), "test2@example.com"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/invite", new GroupInviteRequest(authToken, groupId, reserveIdList.get(2), "test3@example.com"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/invite", new GroupInviteRequest(authToken, groupId, reserveIdList.get(3), "test4@example.com"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/invite", new GroupInviteRequest(authToken, groupId, reserveIdList.get(4), "test5@example.com"));
        assertEquals(200, response.getStatus());

        inviteId1 = emailAPI.getSentMessages().get(0).getBody().split("/group/")[1].split("\"")[0];
        inviteId2 = emailAPI.getSentMessages().get(1).getBody().split("/group/")[1].split("\"")[0];
        inviteId3 = emailAPI.getSentMessages().get(2).getBody().split("/group/")[1].split("\"")[0];
        inviteId4 = emailAPI.getSentMessages().get(3).getBody().split("/group/")[1].split("\"")[0];
    }


    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();

        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testNoAcceptDetails() {
        var response = httpHelper.get("/api/group/details", Map.of("auth_token", authToken, "group_id", groupId));
        assertEquals(200, response.getStatus());

        var body = response.getBody(GroupDetailsResponse.class);
        var members = body.groupMembers;
        var invites = body.pendingInvites;

        assertEquals(1, members.size());
        assertEquals(4, invites.size());
        assertEquals(0, body.availableReserves.size());
        assertEquals(hostId, body.hostId);
        assertEquals(hostEmail, members.get(0).email);
        assertEquals(eventId, body.eventId);

        for (PendingInvite i : invites) {
            assertNotNull(i.email);
            assertNotNull(i.inviteId);
            assertNotNull(i.seatNum);
            assertNotNull(i.section);
        }
    }

    @Test 
    public void testAllAcceptDetails () {
        var response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken2, inviteId1));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken3, inviteId2));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken4, inviteId3));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken5, inviteId4));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/group/details", Map.of("auth_token", authToken, "group_id", groupId));
        assertEquals(200, response.getStatus());

        var body = response.getBody(GroupDetailsResponse.class);
        var members = body.groupMembers;
        var invites = body.pendingInvites;

        assertEquals(5, members.size());
        assertEquals(0, invites.size());
        assertEquals(0, body.availableReserves.size());
        assertEquals(hostId, body.hostId);

        for (GroupMember m : members) {
            assertNotNull(m.email);
            assertNotNull(m.section);
            assertNotNull(m.seatNum);   
            assertEquals(false, m.purchased);         
        }
    }

    @Test 
    public void testSomeDenyDetails () {
        var response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken2, inviteId1));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken3, inviteId2));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/deny", new GroupDenyRequest(inviteId3));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/group/details", Map.of("auth_token", authToken, "group_id", groupId));
        assertEquals(200, response.getStatus());

        var body = response.getBody(GroupDetailsResponse.class);
        var members = body.groupMembers;
        var invites = body.pendingInvites;

        assertEquals(3, members.size());
        assertEquals(1, invites.size());
        assertEquals(1, body.availableReserves.size());
        assertEquals(hostId, body.hostId);
    }

    @Test 
    public void testAllDenyDetails() {
        var response = httpHelper.post("/api/group/deny", new GroupDenyRequest(inviteId1));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/deny", new GroupDenyRequest(inviteId2));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/deny", new GroupDenyRequest(inviteId3));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/deny", new GroupDenyRequest(inviteId4));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/group/details", Map.of("auth_token", authToken, "group_id", groupId));
        assertEquals(200, response.getStatus());

        var body = response.getBody(GroupDetailsResponse.class);
        var members = body.groupMembers;
        var invites = body.pendingInvites;

        assertEquals(1, members.size());
        assertEquals(0, invites.size());
        assertEquals(4, body.availableReserves.size());
        assertEquals(hostId, body.hostId);
    }

    @Test 
    public void testGroupTicketPurchaseDetails() {
        var response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken2, inviteId1));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken3, inviteId2));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/group/deny", new GroupDenyRequest(inviteId3));
        assertEquals(200, response.getStatus());

        var reqIds = List.of(reserveIdList.get(1)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken2, "https://example.com/success", "https://example.com/cancel", reqIds));
        assertEquals(200, response.getStatus());

        var redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));

        var result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        reqIds = List.of(reserveIdList.get(2)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken3, "https://example.com/success", "https://example.com/cancel", reqIds));
        assertEquals(200, response.getStatus());

        redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));

        result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        reqIds = List.of(reserveIdList.get(0)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken, "https://example.com/success", "https://example.com/cancel", reqIds));
        assertEquals(200, response.getStatus());

        redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));

        result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        response = httpHelper.get("/api/group/details", Map.of("auth_token", authToken, "group_id", groupId));
        assertEquals(200, response.getStatus());

        var body = response.getBody(GroupDetailsResponse.class);
        var members = body.groupMembers;
        var invites = body.pendingInvites;

        assertEquals(3, members.size());
        assertEquals(1, invites.size());
        assertEquals(1, body.availableReserves.size());
        assertEquals(hostId, body.hostId);

        for (GroupMember m : members) {
            assertEquals(true, m.purchased);
        }
    }

    @Test 
    public void testGroupTicketView() {
        var response = httpHelper.post("/api/group/accept", new GroupAcceptRequest(authToken2, inviteId1));
        assertEquals(200, response.getStatus());

        var reqIds = List.of(reserveIdList.get(1)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken2, "https://example.com/success", "https://example.com/cancel", reqIds));
        assertEquals(200, response.getStatus());

        var redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));

        var result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        reqIds = List.of(reserveIdList.get(0)).stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken, "https://example.com/success", "https://example.com/cancel", reqIds));
        assertEquals(200, response.getStatus());

        redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));

        result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);

        response = httpHelper.get("/api/event/bookings", Map.of("auth_token", authToken, "event_id", eventId));
        assertEquals(200, response.getStatus());
        var ticketId = response.getBody(TicketBookingsResponse.class).tickets.get(0);
        response = httpHelper.get("/api/ticket/view", Map.of("ticket_id", ticketId));
        assertEquals(200, response.getStatus());
        assertEquals(groupId, response.getBody(TicketViewResponse.class).groupId);

        response = httpHelper.get("/api/event/bookings", Map.of("auth_token", authToken2, "event_id", eventId));
        assertEquals(200, response.getStatus());
        ticketId = response.getBody(TicketBookingsResponse.class).tickets.get(0);
        response = httpHelper.get("/api/ticket/view", Map.of("ticket_id", ticketId));
        assertEquals(200, response.getStatus());
        assertEquals(null, response.getBody(TicketViewResponse.class).groupId);
    }

    @Test 
    public void testExceptions() {
        var response = httpHelper.get("/api/group/details", Map.of("group_id", groupId));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/group/details", Map.of("auth_token", authToken));
        assertEquals(400, response.getStatus());
        response = httpHelper.get("/api/group/details", Map.of("auth_token", "", "group_id", groupId));
        assertEquals(401, response.getStatus());
        response = httpHelper.get("/api/group/details", Map.of("auth_token", authToken, "group_id", UUID.randomUUID().toString()));
        assertEquals(403, response.getStatus());
    }
}
