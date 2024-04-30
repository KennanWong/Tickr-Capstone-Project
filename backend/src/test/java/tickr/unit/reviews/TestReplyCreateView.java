package tickr.unit.reviews;

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
import tickr.application.serialised.combined.comments.ReplyCreate;
import tickr.application.serialised.combined.comments.ReviewCreate;
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockLocationApi;
import tickr.mock.MockUnitPurchaseAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestReplyCreateView {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String hostToken;
    private String authToken;
    private String eventId;

    private String reviewId;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<CreateEventRequest.SeatingDetails> seatingDetails;
    private MockUnitPurchaseAPI purchaseAPI;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));

        seatingDetails = List.of(
                new CreateEventRequest.SeatingDetails("test_section", 101, 1, true),
                new CreateEventRequest.SeatingDetails("test_section2", 201, 4, true)
        );

        session = model.makeSession();
        authToken = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07")).authToken;
        hostToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);

        eventId = controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
                .build(hostToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId, hostToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("test_section", 1, List.of()),
                new TicketReserve.TicketDetails("test_section2", 1, List.of())
        )));
        session = TestHelper.commitMakeSession(model, session);
        var requestIds = response.reserveTickets.stream().map(t -> t.reserveId).collect(Collectors.toList());
        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        var redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken,
                "http://example.com/success", "http://example.com/failure", requestIds.stream()
                .map(i -> new TicketPurchase.TicketDetails(i, null, null, null)).collect(Collectors.toList()))).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.addCustomer("test_customer", 1000000);
        assertEquals("http://example.com/success", purchaseAPI.fulfillOrder(redirectUrl, "test_customer"));

        reviewId = controller.reviewCreate(session, new ReviewCreate.Request(authToken, eventId, "title", "text", 1.0f)).reviewId;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequestsCreate () {
        assertThrows(UnauthorizedException.class, () -> controller.replyCreate(session, new ReplyCreate.Request(null, reviewId, "abc")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(UnauthorizedException.class, () -> controller.replyCreate(session,
                new ReplyCreate.Request(TestHelper.makeFakeJWT(), reviewId, "abc")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.replyCreate(session,
                new ReplyCreate.Request(hostToken, null, "abc")));
        session = TestHelper.rollbackMakeSession(model, session);

        assertThrows(BadRequestException.class, () -> controller.replyCreate(session,
                new ReplyCreate.Request(hostToken, reviewId, null)));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.replyCreate(session,
                new ReplyCreate.Request(hostToken, reviewId, "")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.replyCreate(session,
                new ReplyCreate.Request(hostToken, UUID.randomUUID().toString(), "abc")));
        session = TestHelper.rollbackMakeSession(model, session);

        var newAuth = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.replyCreate(session,
                new ReplyCreate.Request(newAuth, reviewId, "abc")));
    }

    @Test
    public void testBadRequestsView () {
        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of()));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId)));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId, "page_start", "0")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(UnauthorizedException.class, () -> controller.repliesView(session, Map.of("auth_token", TestHelper.makeFakeJWT(), "review_id", reviewId,
                "page_start", "0", "max_results", "10")));
        session = TestHelper.rollbackMakeSession(model, session);

        assertThrows(ForbiddenException.class, () -> controller.repliesView(session, Map.of("review_id", UUID.randomUUID().toString(),
                "page_start", "0", "max_results", "10")));
        session = TestHelper.rollbackMakeSession(model, session);

        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId,
                "page_start", "abc", "max_results", "10")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId,
                "page_start", "0", "max_results", "def")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId,
                "page_start", "-1", "max_results", "10")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId,
                "page_start", "0", "max_results", "0")));
        session = TestHelper.rollbackMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId,
                "page_start", "0", "max_results", "-1")));
        session = TestHelper.rollbackMakeSession(model, session);
    }

    @Test
    public void testReplyCreate () {
        var replyId = controller.replyCreate(session, new ReplyCreate.Request(authToken, reviewId, "reply text")).replyId;
        session = TestHelper.commitMakeSession(model, session);
        var response = controller.repliesView(session, Map.of("review_id", reviewId, "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        var authorId = controller.userSearch(session, Map.of("email", "test@example.com")).userId;
        assertEquals(1, response.numResults);

        var replies = response.replies;
        assertEquals(1, replies.size());

        var reply = replies.get(0);
        assertEquals(replyId, reply.replyId);
        assertEquals(authorId, reply.authorId);
        assertEquals("reply text", reply.text);
    }

    @Test
    public void testPagination () {
        int numReplies = 30;
        int pageNum = 7;
        int curr = 0;
        var replyIds1 = new ArrayList<String>();
        for (int i = 0; i < numReplies; i++) {
            var replyId = controller.replyCreate(session, new ReplyCreate.Request(authToken, reviewId, "reply text")).replyId;
            session = TestHelper.commitMakeSession(model, session);
            replyIds1.add(replyId);
        }

        var replyIds2 = new ArrayList<String>();
        for (int i = 0; i < numReplies / pageNum; i++) {
            var response = controller.repliesView(session, Map.of("review_id", reviewId, "page_start", Integer.toString(curr), "max_results", Integer.toString(pageNum)));
            session = TestHelper.commitMakeSession(model, session);
            assertEquals(numReplies, response.numResults);
            assertEquals(pageNum, response.replies.size());
            replyIds2.addAll(response.replies.stream().map(r -> r.replyId).collect(Collectors.toList()));
            curr += pageNum;
        }

        var response = controller.repliesView(session, Map.of("review_id", reviewId, "page_start", Integer.toString(curr),
                "max_results", Integer.toString(pageNum)));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(numReplies, response.numResults);
        assertEquals(numReplies - curr, response.replies.size());
        replyIds2.addAll(response.replies.stream().map(r -> r.replyId).collect(Collectors.toList()));

        assertEquals(replyIds1.size(), replyIds2.size());
        for (int i = 0; i < replyIds1.size(); i++) {
            assertEquals(replyIds1.get(i), replyIds2.get(replyIds2.size() - 1 - i));
        }
    }
}
