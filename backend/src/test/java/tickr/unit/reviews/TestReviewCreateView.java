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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestReviewCreateView {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String authToken;
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
        session = TestHelper.commitMakeSession(model, session);

        eventId = controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId, authToken, null, null, null, null,
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
        //session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testCreateBadRequest () {
        assertThrows(UnauthorizedException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(null, null, null, null, 1.0f)));
        assertThrows(BadRequestException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, null, null, null, 1.0f)));
        assertThrows(BadRequestException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, null, null, 1.0f)));
        assertThrows(BadRequestException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, "title", null, 1.0f)));
        assertThrows(BadRequestException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, null, "text", 1.0f)));
        assertThrows(ForbiddenException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, "title", "text", -1.0f)));
        assertThrows(ForbiddenException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, "title", "text", 11.0f)));
        assertThrows(BadRequestException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, "", "text", 1.0f)));

        assertThrows(UnauthorizedException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(TestHelper.makeFakeJWT(), eventId, "title", "text", 1.0f)));
        assertThrows(ForbiddenException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, UUID.randomUUID().toString(), "title", "text", 1.0f)));
        session.rollback();
        session.close();
        session = model.makeSession();
        var authToken2 = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        assertThrows(ForbiddenException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken2, eventId, "title", "null", 1.0f)));

        var eventId2 = controller.createEventUnsafe(session, new CreateEventReqBuilder()
                .withStartDate(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1))
                .withEndDate(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1).plusHours(1))
                .withSeatingDetails(seatingDetails)
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId2, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);

        var reqIds2 = controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId2, ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1), List.of(
                        new TicketReserve.TicketDetails("test_section", 1, List.of())
                ))).reserveTickets.stream().map(r -> r.reserveId).collect(Collectors.toList());
        session = TestHelper.commitMakeSession(model, session);

        var url = controller.ticketPurchase(session, new TicketPurchase.Request(authToken, "http://success.com", "http://failure.com",
                reqIds2.stream().map(TicketPurchase.TicketDetails::new).collect(Collectors.toList()))).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(url, "test_customer");
        controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId2, "test", "test", 1.0f));

        assertThrows(ForbiddenException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId2, "test", "test", 1.0f)));
        session = TestHelper.rollbackMakeSession(model, session);
        var newEventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withStartDate(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(1))
                .withEndDate(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(2))
                .withSeatingDetails(List.of(new CreateEventRequest.SeatingDetails("test_section", 10, 1.0f, true)))
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        controller.editEvent(session, new EditEventRequest(newEventId, authToken, null, null,
                null, null, null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);
        var newReqIds = controller.ticketReserve(session, new TicketReserve.Request(authToken2, newEventId,
                ZonedDateTime.now(ZoneId.of("UTC")).plusDays(1).plusMinutes(1),
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of())))).reserveTickets;
        session = TestHelper.commitMakeSession(model, session);
        var newUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken2, "http://example.com", "http://example.com",
                newReqIds.stream().map(e -> e.reserveId).map(TicketPurchase.TicketDetails::new).collect(Collectors.toList()))).redirectUrl;
        session = TestHelper.commitMakeSession(model, session);
        purchaseAPI.fulfillOrder(newUrl, "test_customer");

        assertThrows(ForbiddenException.class, () -> controller.reviewCreate(session, new ReviewCreate.Request(authToken2, newEventId, "test", "test", 1.0f)));
    }

    @Test
    public void testViewBadRequest () {
        assertThrows(BadRequestException.class, () -> controller.reviewsView(session,
                Map.of()));
        assertThrows(BadRequestException.class, () -> controller.reviewsView(session,
                Map.of("event_id", eventId)));
        assertThrows(BadRequestException.class, () -> controller.reviewsView(session,
                Map.of("event_id", eventId, "page_start", "1")));
        assertThrows(BadRequestException.class, () -> controller.reviewsView(session,
                Map.of("event_id", eventId, "max_results", "1")));
        assertThrows(ForbiddenException.class, () -> controller.reviewsView(session,
                Map.of("event_id", UUID.randomUUID().toString(), "page_start", "0", "max_results", "10")));
        assertThrows(UnauthorizedException.class, () -> controller.reviewsView(session,
                Map.of("auth_token", TestHelper.makeFakeJWT(), "event_id", eventId, "page_start", "0", "max_results", "10")));

        assertThrows(ForbiddenException.class, () -> controller.reviewsView(session,
                Map.of("auth_token", authToken, "event_id", eventId, "page_start", "-1", "max_results", "10")));
        assertThrows(ForbiddenException.class, () -> controller.reviewsView(session,
                Map.of("auth_token", authToken, "event_id", eventId, "page_start", "0", "max_results", "0")));
        assertThrows(ForbiddenException.class, () -> controller.reviewsView(session,
                Map.of("auth_token", authToken, "event_id", eventId, "page_start", "0", "max_results", "-1")));
        assertThrows(ForbiddenException.class, () -> controller.reviewsView(session,
                Map.of("auth_token", authToken, "event_id", eventId, "page_start", "-1", "max_results", "257")));
        assertThrows(ForbiddenException.class, () -> controller.reviewsView(session,
                Map.of("auth_token", authToken, "event_id", eventId, "page_start", "abc", "max_results", "257")));
        assertThrows(ForbiddenException.class, () -> controller.reviewsView(session,
                Map.of("auth_token", authToken, "event_id", eventId, "page_start", "0", "max_results", "def")));
    }

    @Test
    public void testReviewCreate () {
        var reviewId = controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, "test", "testing", 1.0f)).reviewId;
        session = TestHelper.commitMakeSession(model, session);
        assertNotNull(reviewId);

        var viewResponse = controller.reviewsView(session,
                Map.of("event_id", eventId, "page_start", "0", "max_results", "10"));
        session = TestHelper.commitMakeSession(model, session);
        assertEquals(1, viewResponse.numResults);
        assertEquals(1, viewResponse.reviews.size());

        var review = viewResponse.reviews.get(0);
        assertEquals(reviewId, review.reviewId);
        assertEquals(controller.userSearch(session, Map.of("email", "test@example.com")).userId, review.authorId);
        assertEquals("test", review.title);
        assertEquals("testing", review.text);
        assertEquals(1.0f, review.rating);
    }

    @Test
    public void testReviewTwice () {
        var reviewId = controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, "test", "testing", 1.0f)).reviewId;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.reviewCreate(session,
                new ReviewCreate.Request(authToken, eventId, "test", "testing", 1.0f)));
        session.rollback();
        session.close();
        session = model.makeSession();

        var viewResponse = controller.reviewsView(session,
                Map.of("event_id", eventId, "page_start", "0", "max_results", "10"));
        TestHelper.commitMakeSession(model, session);
        assertEquals(1, viewResponse.numResults);
        assertEquals(1, viewResponse.reviews.size());
    }

    @Test
    public void testPagination () {
        int numTest = 100;
        var reviewIds1 = new HashSet<String>();
        for (int i = 0; i < numTest; i++) {
            var authToken1 = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
            session = TestHelper.commitMakeSession(model, session);
            var ticketIds = controller.ticketReserve(session,
                    new TicketReserve.Request(authToken1, eventId, startTime,
                            List.of(new TicketReserve.TicketDetails("test_section", 1, List.of())))).reserveTickets.stream()
                    .map(t -> t.reserveId)
                    .collect(Collectors.toList());
            session = TestHelper.commitMakeSession(model, session);
            var orderUrl = controller.ticketPurchase(session, new TicketPurchase.Request(authToken1, "https://example.com",
                    "https://example.com",
                    ticketIds.stream()
                            .map(TicketPurchase.TicketDetails::new)
                            .collect(Collectors.toList()))).redirectUrl;
            session = TestHelper.commitMakeSession(model, session);
            purchaseAPI.fulfillOrder(orderUrl, "test_customer");
            var reviewId = controller.reviewCreate(session,
                    new ReviewCreate.Request(authToken1, eventId, "a", "b", 1.0f)).reviewId;
            session = TestHelper.commitMakeSession(model, session);
            reviewIds1.add(reviewId);
        }

        int pageTest = 15;
        int curr = 0;
        var reviewIds2 = new HashSet<String>();
        for (int i = 0; i < numTest / pageTest; i++) {
            var response = controller.reviewsView(session, Map.of(
                    "event_id", eventId,
                    "page_start", Integer.toString(curr),
                    "max_results", Integer.toString(pageTest)
            ));
            assertEquals(numTest, response.numResults);
            assertEquals(pageTest, response.reviews.size());
            reviewIds2.addAll(response.reviews.stream().map(r -> r.reviewId).collect(Collectors.toList()));
            curr += pageTest;
        }

        var response = controller.reviewsView(session, Map.of(
                "event_id", eventId,
                "page_start", Integer.toString(curr),
                "max_results", Integer.toString(pageTest)
        ));
        assertEquals(numTest, response.numResults);
        assertEquals(numTest % pageTest, response.reviews.size());
        reviewIds2.addAll(response.reviews.stream().map(r -> r.reviewId).collect(Collectors.toList()));

        response = controller.reviewsView(session, Map.of(
                "event_id", eventId,
                "page_start", Integer.toString(numTest),
                "max_results", Integer.toString(pageTest)
        ));
        assertEquals(numTest, response.numResults);
        assertEquals(0, response.reviews.size());

        assertTrue(reviewIds1.containsAll(reviewIds2));
        assertEquals(reviewIds1.size(), reviewIds2.size());
    }
}
