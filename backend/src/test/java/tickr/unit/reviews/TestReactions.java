package tickr.unit.reviews;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import tickr.application.serialised.requests.comment.ReactRequest;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestReactions {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String hostToken;
    private String authToken;
    private String eventId;

    private String reviewId;
    private String replyId;

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

        replyId = controller.replyCreate(session, new ReplyCreate.Request(hostToken, reviewId, "text")).replyId;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequests () {
        assertThrows(UnauthorizedException.class, () -> controller.commentReact(session, new ReactRequest()));
        assertThrows(UnauthorizedException.class, () -> controller.commentReact(session, new ReactRequest(TestHelper.makeFakeJWT(), replyId, "heart")));

        assertThrows(BadRequestException.class, () -> controller.commentReact(session, new ReactRequest(authToken, null, "heart")));
        assertThrows(BadRequestException.class, () -> controller.commentReact(session, new ReactRequest(authToken, replyId, null)));

        assertThrows(ForbiddenException.class, () -> controller.commentReact(session, new ReactRequest(authToken, UUID.randomUUID().toString(), "heart")));
        assertThrows(ForbiddenException.class, () -> controller.commentReact(session, new ReactRequest(authToken, replyId, "meme")));

        // Cannot react to own review/reply
        assertThrows(ForbiddenException.class, () -> controller.commentReact(session, new ReactRequest(authToken, reviewId, "heart")));
        assertThrows(ForbiddenException.class, () -> controller.commentReact(session, new ReactRequest(hostToken, replyId, "heart")));
    }

    @Test
    public void testReviewReact () {
        controller.commentReact(session, new ReactRequest(hostToken, reviewId, "heart"));
        session = TestHelper.commitMakeSession(model, session);
        var review = controller.reviewsView(session, Map.of("event_id", eventId, "page_start", "0", "max_results", "10")).reviews.get(0);
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(1, review.reactions.size());
        var react = review.reactions.get(0);
        assertEquals("heart", react.reactType);
        assertEquals(1, react.reactNum);
    }

    @Test
    public void testReplyReact () {
        controller.commentReact(session, new ReactRequest(authToken, replyId, "heart"));
        session = TestHelper.commitMakeSession(model, session);
        var review = controller.repliesView(session, Map.of("review_id", reviewId, "page_start", "0", "max_results", "10")).replies.get(0);
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(1, review.reactions.size());
        var react = review.reactions.get(0);
        assertEquals("heart", react.reactType);
        assertEquals(1, react.reactNum);
    }

    @Test
    public void testReactTypes () {
        controller.commentReact(session, new ReactRequest(hostToken, reviewId, "heart"));
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(hostToken, reviewId, "laugh"));
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(hostToken, reviewId, "cry"));
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(hostToken, reviewId, "angry"));
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(hostToken, reviewId, "thumbs_up"));
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(hostToken, reviewId, "thumbs_down"));
        session = TestHelper.commitMakeSession(model, session);
        var review = controller.reviewsView(session, Map.of("event_id", eventId, "page_start", "0", "max_results", "10")).reviews.get(0);
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(6, review.reactions.size());

        var reactTypes = Stream.of("heart", "laugh", "cry", "angry", "thumbs_up", "thumbs_down").sorted().collect(Collectors.toList());
        var reacts = review.reactions.stream().sorted(Comparator.comparing(r -> r.reactType)).collect(Collectors.toList());

        for (int i = 0; i < 6; i++) {
            assertEquals(reactTypes.get(i), reacts.get(i).reactType);
            assertEquals(1, reacts.get(i).reactNum);
        }
    }

    @Test
    public void testReactCombination () {
        var newUser = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(authToken, replyId, "thumbs_down"));
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(newUser, replyId, "thumbs_down"));
        session = TestHelper.commitMakeSession(model, session);

        var reply = controller.repliesView(session, Map.of("review_id", reviewId, "page_start", "0", "max_results", "10")).replies.get(0);
        assertEquals(1, reply.reactions.size());
        var react = reply.reactions.get(0);

        assertEquals("thumbs_down", react.reactType);
        assertEquals(2, react.reactNum);
    }

    @Test
    public void testUnreact () {
        controller.commentReact(session, new ReactRequest(authToken, replyId, "thumbs_down"));
        session = TestHelper.commitMakeSession(model, session);
        controller.commentReact(session, new ReactRequest(authToken, replyId, "thumbs_down"));
        session = TestHelper.commitMakeSession(model, session);

        var reply = controller.repliesView(session, Map.of("review_id", reviewId, "page_start", "0", "max_results", "10")).replies.get(0);
        assertEquals(0, reply.reactions.size());
    }
}
