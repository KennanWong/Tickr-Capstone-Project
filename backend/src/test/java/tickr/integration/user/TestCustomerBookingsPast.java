package tickr.integration.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.application.serialised.responses.event.CustomerEventsResponse;
import tickr.mock.MockHttpPurchaseAPI;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;
public class TestCustomerBookingsPast {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;
    private MockHttpPurchaseAPI purchaseAPI;

    private String authToken; 
    private String eventId1; 
    private String eventId2;
    private String eventId3;

    private List<String> requestIds;
    private float requestPrice;


    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        purchaseAPI = new MockHttpPurchaseAPI("http://localhost:8080");
        ApiLocator.addLocator(IPurchaseAPI.class, () -> purchaseAPI);
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(hibernateModel));

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("test", "first", "last", "test1@example.com",
                "Password123!", "2022-04-14"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;

        List<CreateEventRequest.SeatingDetails> seatingDetails = new ArrayList<>();
        seatingDetails.add(new CreateEventRequest.SeatingDetails("test_section", 10, 50, true));
        seatingDetails.add(new CreateEventRequest.SeatingDetails("test_section2", 20, 30, true));

        var startTime1 = ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.ofDays(1));
        var startTime2 = ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.ofDays(2));
        var startTime3 = ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.ofDays(3));
        var endTime1 = startTime1.plus(Duration.ofHours(1));
        var endTime2 = startTime2.plus(Duration.ofHours(1));
        var endTime3 = startTime3.plus(Duration.ofHours(1));

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(startTime1.minusMinutes(2))
                .withEndDate(endTime1)
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventId1 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId1, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(startTime2.minusMinutes(2))
                .withEndDate(endTime2)
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventId2 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId2, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/test/event/create", new CreateEventReqBuilder()
                .withEventName("Test Event")
                .withSeatingDetails(seatingDetails)
                .withStartDate(startTime3.minusMinutes(2))
                .withEndDate(endTime3)
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventId3 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId3, authToken, null, null, null, null, null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId1, startTime1, List.of(
                new TicketReserve.TicketDetails("test_section", 1, List.of(1)),
                new TicketReserve.TicketDetails("test_section2", 1, List.of(2))
        )));
        assertEquals(200, response.getStatus());
        var reserveResponse = response.getBody(TicketReserve.Response.class);
        requestIds = reserveResponse.reserveTickets.stream()
                .map(t -> t.reserveId)
                .collect(Collectors.toList());
        requestPrice = reserveResponse.reserveTickets.stream()
                .map(t -> t.price)
                .reduce(0.0f, Float::sum);

        var requestDetails = requestIds.stream()        
                .map(TicketPurchase.TicketDetails::new)
                .collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken, "https://example.com/success", "https://example.com/cancel", requestDetails));
        assertEquals(200, response.getStatus());
        var redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));
        var result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);


        response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId2, startTime2, List.of(
                new TicketReserve.TicketDetails("test_section", 2, List.of(3, 4)),
                new TicketReserve.TicketDetails("test_section2", 1, List.of(5))
        )));
        assertEquals(200, response.getStatus());
        reserveResponse = response.getBody(TicketReserve.Response.class);
        requestIds = reserveResponse.reserveTickets.stream()
                .map(t -> t.reserveId)
                .collect(Collectors.toList());
        requestPrice = reserveResponse.reserveTickets.stream()
                .map(t -> t.price)
                .reduce(0.0f, Float::sum);

        requestDetails = requestIds.stream()        
                .map(TicketPurchase.TicketDetails::new)
                .collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken, "https://example.com/success", "https://example.com/cancel", requestDetails));
        assertEquals(200, response.getStatus());
        redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));
        result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);



        response = httpHelper.post("/api/ticket/reserve", new TicketReserve.Request(authToken, eventId3, startTime3, List.of(
                new TicketReserve.TicketDetails("test_section", 2, List.of(6, 7)),
                new TicketReserve.TicketDetails("test_section2", 2, List.of(8, 9))
        )));
        assertEquals(200, response.getStatus());
        reserveResponse = response.getBody(TicketReserve.Response.class);
        requestIds = reserveResponse.reserveTickets.stream()
                .map(t -> t.reserveId)
                .collect(Collectors.toList());
        requestPrice = reserveResponse.reserveTickets.stream()
                .map(t -> t.price)
                .reduce(0.0f, Float::sum);

        requestDetails = requestIds.stream()        
                .map(TicketPurchase.TicketDetails::new)
                .collect(Collectors.toList());
        purchaseAPI.addCustomer("test_customer", 1000);
        response = httpHelper.post("/api/ticket/purchase",
                new TicketPurchase.Request(authToken, "https://example.com/success", "https://example.com/cancel", requestDetails));
        assertEquals(200, response.getStatus());
        redirectUrl = response.getBody(TicketPurchase.Response.class).redirectUrl;
        assertTrue(purchaseAPI.isUrlValid(redirectUrl));
        result = purchaseAPI.fulfillOrder(redirectUrl, "test_customer");
        assertEquals("https://example.com/success", result);
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test 
    public void testCustomerBookings() {
        int pageStart = 0;
        int maxResults = 10;
        var response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults)
        ));
        assertEquals(200, response.getStatus()); 
        var bookings = response.getBody(CustomerEventsResponse.class);
        assertEquals(3, bookings.eventIds.size());
        assertEquals(eventId3, bookings.eventIds.get(0));
        assertEquals(eventId2, bookings.eventIds.get(1));
        assertEquals(eventId1, bookings.eventIds.get(2));
        assertEquals(3, bookings.num_results);
        // var event1Ids = bookings.get(0).ticketIds; 
        // var event2Ids = bookings.get(1).ticketIds; 
        // var event3Ids = bookings.get(2).ticketIds; 
        // assertEquals(2, event1Ids.size());
        // assertEquals(3, event2Ids.size());
        // assertEquals(4, event3Ids.size());

        response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(2)
        ));
        assertEquals(200, response.getStatus()); 
        bookings = response.getBody(CustomerEventsResponse.class);
        assertEquals(2, bookings.eventIds.size());
        assertEquals(3, bookings.num_results);
        assertEquals(bookings.eventIds.get(0), eventId3);
        assertEquals(bookings.eventIds.get(1), eventId2);
    }

    @Test 
    public void testCustomerBookingsAfter () {
        int pageStart = 0;
        int maxResults = 10;
        var response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "page_start", Integer.toString(pageStart), 
            "max_results", Integer.toString(maxResults),
            "after", ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.ofDays(3)).toString()
        ));
        assertEquals(200, response.getStatus()); 
        var bookings = response.getBody(CustomerEventsResponse.class);
        assertEquals(2, bookings.eventIds.size());
        assertEquals(bookings.eventIds.get(0), eventId2);
        assertEquals(bookings.eventIds.get(1), eventId1);
        assertEquals(2, bookings.num_results);
    }

    @Test 
    public void testExceptions() {
        var response = httpHelper.get("/api/user/bookings/past", Map.of(
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(10)
        ));
        assertEquals(400, response.getStatus()); 
        response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "max_results", Integer.toString(10)
        ));
        assertEquals(400, response.getStatus()); 
        response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "page_start", Integer.toString(0)
        ));
        assertEquals(400, response.getStatus()); 
        response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "page_start", Integer.toString(-1), 
            "max_results", Integer.toString(10)
        ));
        assertEquals(400, response.getStatus()); 
        response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(-1)
        ));
        assertEquals(400, response.getStatus()); 
        response = httpHelper.get("/api/user/bookings/past", Map.of(
            "auth_token", authToken, 
            "page_start", Integer.toString(0), 
            "max_results", Integer.toString(10),
            "after", " "
        ));
        assertEquals(400, response.getStatus()); 
    }
}
