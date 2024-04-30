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
import tickr.application.serialised.requests.ticket.ReviewDeleteRequest;
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

public class TestReviewDelete {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String hostToken;
    private String authToken;
    private String authToken2;
    private String eventId;

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

        authToken2 = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test2@example.com",
                "Password123!", "2010-10-07")).authToken;
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

        response = controller.ticketReserve(session, new TicketReserve.Request(authToken2, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("test_section", 1, List.of()),
                new TicketReserve.TicketDetails("test_section2", 1, List.of())
        )));
        session = TestHelper.commitMakeSession(model, session);
        requestIds = response.reserveTickets.stream().map(t -> t.reserveId).collect(Collectors.toList());
        purchaseAPI = new MockUnitPurchaseAPI(controller, model);
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        redirectUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken2,
                "http://example.com/success", "http://example.com/failure", requestIds.stream()
                .map(i -> new TicketPurchase.TicketDetails(i, null, null, null)).collect(Collectors.toList()))).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.addCustomer("test_customer", 1000000);
        assertEquals("http://example.com/success", purchaseAPI.fulfillOrder(redirectUrl, "test_customer"));

        
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testDeleteReview () {
        var reviewId1 = controller.reviewCreate(session, new ReviewCreate.Request(authToken, eventId, "title", "review 1", 1.0f)).reviewId;
        session = TestHelper.commitMakeSession(model, session);
        var replyId1 = controller.replyCreate(session, new ReplyCreate.Request(authToken, reviewId1, "reply text1")).replyId;
        session = TestHelper.commitMakeSession(model, session);
        var replyId2 = controller.replyCreate(session, new ReplyCreate.Request(authToken, reviewId1, "reply text2")).replyId;
        session = TestHelper.commitMakeSession(model, session);

        controller.reviewDelete(session, new ReviewDeleteRequest(authToken, reviewId1));
        session = TestHelper.commitMakeSession(model, session);


        var reviews = controller.reviewsView(session,
                Map.of("event_id", eventId, "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(0, reviews.numResults);
        assertThrows(ForbiddenException.class, () -> controller.repliesView(session, Map.of("review_id", reviewId1, "page_start", "0", "max_results", "10")));
        assertThrows(ForbiddenException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(authToken, reviewId1)));
        assertThrows(ForbiddenException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(authToken, replyId1)));
        assertThrows(ForbiddenException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(authToken, replyId2)));
    }

    @Test 
    public void testDeleteReply () {
        var reviewId1 = controller.reviewCreate(session, new ReviewCreate.Request(authToken, eventId, "title", "review 1", 1.0f)).reviewId;
        session = TestHelper.commitMakeSession(model, session);
        var replyId1 = controller.replyCreate(session, new ReplyCreate.Request(authToken, reviewId1, "reply text1")).replyId;
        session = TestHelper.commitMakeSession(model, session);
        var replyId2 = controller.replyCreate(session, new ReplyCreate.Request(authToken, reviewId1, "reply text2")).replyId;
        session = TestHelper.commitMakeSession(model, session);

        controller.reviewDelete(session, new ReviewDeleteRequest(authToken, replyId1));
        session = TestHelper.commitMakeSession(model, session);
        var replies = controller.repliesView(session, Map.of("auth_token", authToken, "review_id", reviewId1, "page_start", "0", "max_results", "10")).replies;
        assertEquals(1, replies.size());
        assertEquals(replyId2, replies.get(0).replyId);
        assertEquals("reply text2", replies.get(0).text);
        assertThrows(ForbiddenException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(authToken, replyId1)));
    }

    @Test 
    public void testExceptions() {
        var reviewId1 = controller.reviewCreate(session, new ReviewCreate.Request(authToken, eventId, "title", "review 1", 1.0f)).reviewId;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(BadRequestException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(authToken, null)));
        assertThrows(ForbiddenException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(authToken, UUID.randomUUID().toString())));
        assertThrows(UnauthorizedException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(null, reviewId1)));

        var reviewId2 = controller.reviewCreate(session, new ReviewCreate.Request(authToken2, eventId, "title", "review 2", 1.0f)).reviewId;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.reviewDelete(session, new ReviewDeleteRequest(authToken, reviewId2)));
    }
}
