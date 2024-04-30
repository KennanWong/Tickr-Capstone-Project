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
import tickr.application.entities.Invitation;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.combined.tickets.TicketReserve.ReserveDetails;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.group.GroupCreateRequest;
import tickr.application.serialised.requests.group.GroupInviteRequest;
import tickr.application.serialised.requests.group.GroupRemoveInviteRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
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
public class TestGroupRemoveInvite {
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
    private String hostId;

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

        hostId = controller.authenticateToken(session, authToken).getId().toString();
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IEmailAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testGroupRemoveInvite () {
        assertEquals(4, session.getAll(Invitation.class).size());
        controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken, inviteId1, groupId));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(3, session.getAll(Invitation.class).size());
        controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken, inviteId2, groupId));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(2, session.getAll(Invitation.class).size());

        var response = controller.groupDetails(session, Map.of("auth_token", authToken, "group_id", groupId));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals(2, response.pendingInvites.size());
    }

    @Test 
    public void testExceptions () {
        assertThrows(UnauthorizedException.class, () -> controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(TestHelper.makeFakeJWT(), inviteId1, groupId)));
        assertThrows(BadRequestException.class, () -> controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(null, inviteId1, groupId)));
        assertThrows(BadRequestException.class, () -> controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken, null, groupId)));
        assertThrows(BadRequestException.class, () -> controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken, inviteId1, null)));
        assertThrows(ForbiddenException.class, () -> controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken, inviteId1, UUID.randomUUID().toString())));
        assertThrows(BadRequestException.class, () -> controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken2, inviteId1, groupId)));

        controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken, inviteId1, groupId));
        session = TestHelper.commitMakeSession(model, session);

        assertThrows(BadRequestException.class, () -> controller.groupRemoveInvite(session, new GroupRemoveInviteRequest(authToken, inviteId1, groupId)));
    }
}
