package tickr.integration.reviews;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
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
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.comment.RepliesViewResponse;
import tickr.application.serialised.responses.comment.ReviewsViewResponse;
import tickr.mock.MockHttpPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestReactions {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;

    private MockHttpPurchaseAPI purchaseAPI;

    private String authToken;
    private String hostToken;

    private String eventId;
    private String reviewId;
    private String replyId;

    private String requestId;
    private List<String> requestIds;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        purchaseAPI = new MockHttpPurchaseAPI("http://localhost:8080");
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(hibernateModel));

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;

        response = httpHelper.post("/api/user/register", TestHelper.makeRegisterRequest());
        assertEquals(200, response.getStatus());
        hostToken = response.getBody(AuthTokenResponse.class).authToken;

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));

        List<CreateEventRequest.SeatingDetails> seatingDetails = List.of(
                new CreateEventRequest.SeatingDetails("test_section", 10, 1, true),
                new CreateEventRequest.SeatingDetails("test_section2", 20, 4, true)
        );

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
                .build(hostToken));
        assertEquals(200, response.getStatus());
        eventId = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId, hostToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId, startTime, List.of(
                new TicketReserve.TicketDetails("test_section", 1, List.of()),
                new TicketReserve.TicketDetails("test_section2", 1, List.of())
        )));
        assertEquals(200, response.getStatus());
        var reserveResponse = response.getBody(TicketReserve.Response.class);
        requestIds = reserveResponse.reserveTickets.stream()
                .map(t -> t.reserveId)
                .collect(Collectors.toList());
        response = httpHelper.post("/api/ticket/purchase", new TicketPurchase.Request(authToken, "http://success.com", "http://failure.com",
                requestIds.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList())));
        assertEquals(200, response.getStatus());
        purchaseAPI.addCustomer("test_customer", 10_000_000);
        purchaseAPI.fulfillOrder(response.getBody(TicketPurchase.Response.class).redirectUrl, "test_customer");

        response = httpHelper.post("/api/event/review/create", new ReviewCreate.Request(authToken, eventId, "test", "test", 1.0f));
        assertEquals(200, response.getStatus());
        reviewId = response.getBody(ReviewCreate.Response.class).reviewId;

        response = httpHelper.post("/api/event/review/reply", new ReplyCreate.Request(hostToken, reviewId, "test"));
        assertEquals(200, response.getStatus());
        replyId = response.getBody(ReplyCreate.Response.class).replyId;
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
    public void testBadRequests () {
        var response = httpHelper.post("/api/event/review/react", new ReactRequest());
        assertEquals(401, response.getStatus());

        response = httpHelper.post("/api/event/review/react", new ReactRequest(TestHelper.makeFakeJWT(), replyId, "laugh"));
        assertEquals(401, response.getStatus());

        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, null, "laugh"));
        assertEquals(400, response.getStatus());

        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, UUID.randomUUID().toString(), "laugh"));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "meme"));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, reviewId, "heart"));
        assertEquals(403, response.getStatus());

        response = httpHelper.post("/api/event/review/react", new ReactRequest(hostToken, replyId, "heart"));
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testReviewReact () {
        var response = httpHelper.post("/api/event/review/react", new ReactRequest(hostToken, reviewId, "heart"));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/event/reviews", Map.of(
                "event_id", eventId,
                "page_start", "0",
                "max_results", "10"
        ));
        assertEquals(200, response.getStatus());

        var review = response.getBody(ReviewsViewResponse.class).reviews.get(0);
        assertEquals(1, review.reactions.size());
        var react = review.reactions.get(0);
        assertEquals(1, react.reactNum);
        assertEquals("heart", react.reactType);
    }

    @Test
    public void testReplyReact () {
        var response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "heart"));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/event/reviews/replies", Map.of(
                "review_id", reviewId,
                "page_start", "0",
                "max_results", "10"
        ));
        assertEquals(200, response.getStatus());

        var reply = response.getBody(RepliesViewResponse.class).replies.get(0);
        assertEquals(1, reply.reactions.size());
        var react = reply.reactions.get(0);
        assertEquals(1, react.reactNum);
        assertEquals("heart", react.reactType);
    }

    @Test
    public void testReactTypes () {
        var response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "heart"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "laugh"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "cry"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "angry"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "thumbs_up"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "thumbs_down"));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUnreact () {
        var response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "heart"));
        assertEquals(200, response.getStatus());
        response = httpHelper.post("/api/event/review/react", new ReactRequest(authToken, replyId, "heart"));
        assertEquals(200, response.getStatus());

        response = httpHelper.get("/api/event/reviews/replies", Map.of(
                "review_id", reviewId,
                "page_start", "0",
                "max_results", "10"
        ));
        assertEquals(200, response.getStatus());
        var reply = response.getBody(RepliesViewResponse.class).replies.get(0);
        assertEquals(0, reply.reactions.size());
    }
}
