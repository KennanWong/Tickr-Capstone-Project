package tickr.unit.ticket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.opentest4j.AssertionFailedError;
import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.entities.TicketReservation;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TestTicketReserve {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String authToken;
    private String eventId;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private List<CreateEventRequest.SeatingDetails> seatingDetails;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        startTime = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
        endTime = startTime.plus(Duration.ofHours(1));

        seatingDetails = List.of(
                new CreateEventRequest.SeatingDetails("test_section", 10, 1, true),
                new CreateEventRequest.SeatingDetails("test_section2", 20, 4, true)
        );

        session = model.makeSession();
        authToken = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withStartDate(startTime.minusMinutes(2))
                .withEndDate(endTime)
                .withSeatingDetails(seatingDetails)
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        controller.editEvent(session, new EditEventRequest(eventId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IPurchaseAPI.class);
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    /*@Test
    public void testBadRequest () {
        var ticketDetails = List.of(new TicketReserve.TicketDetails("test_section"));

        assertThrows(UnauthorizedException.class, () -> controller.ticketReserveOld(session, new TicketReserve.Request()));
        assertThrows(UnauthorizedException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(TestHelper.makeFakeJWT(), eventId, startTime, ticketDetails)));

        assertThrows(BadRequestException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, null, startTime, ticketDetails)));
        assertThrows(BadRequestException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, (String)null, ticketDetails)));
        assertThrows(BadRequestException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime, List.of())));
        assertThrows(BadRequestException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails(null, "last", "test@example.com", "test_section")))));
        assertThrows(BadRequestException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("first", null, "test@example.com", "test_section")))));
        assertThrows(BadRequestException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("first", "last", "test@", "test_section")))));

        assertThrows(ForbiddenException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, UUID.randomUUID().toString(), startTime, ticketDetails)));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, ZonedDateTime.now(), ticketDetails)));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, ZonedDateTime.now().plus(Duration.ofDays(365)), ticketDetails)));

        assertThrows(ForbiddenException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime, List.of(new TicketReserve.TicketDetails("testing")))));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("first", "last", "test@example.com", "test_section", 0)))));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("first", "last", "test@example.com", "test_section", -1)))));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserveOld(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("first", "last", "test@example.com", "test_section", 1000000)))));
    }*/

    @Test
    public void testBadRequest () {
        var ticketDetails = List.of(new TicketReserve.TicketDetails("test_section", 1, List.of()));

        assertThrows(UnauthorizedException.class, () -> controller.ticketReserve(session, new TicketReserve.Request()));
        assertThrows(UnauthorizedException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(TestHelper.makeFakeJWT(), eventId, startTime, ticketDetails)));

        assertThrows(BadRequestException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, null, startTime, ticketDetails)));
        assertThrows(BadRequestException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, (String)null, ticketDetails)));
        assertThrows(BadRequestException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, startTime, List.of())));
        assertThrows(BadRequestException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails(null, 1, List.of())))));
        assertThrows(BadRequestException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("test_section", 2, List.of(1))))));

        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, UUID.randomUUID().toString(), startTime, ticketDetails)));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, ZonedDateTime.now(), ticketDetails)));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, ZonedDateTime.now().plus(Duration.ofDays(365)), ticketDetails)));

        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("testing", 1, List.of())))));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(0))))));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(-1))))));
        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session,
                new TicketReserve.Request(authToken, eventId, startTime,
                        List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(1000000))))));

        session = TestHelper.rollbackMakeSession(model, session);
        var newEventId = controller.createEvent(session, new CreateEventReqBuilder()
                        .withSeatingDetails(List.of(new CreateEventRequest.SeatingDetails("test_section", 10, 1.0f, true)))
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session, new TicketReserve.Request(authToken, newEventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of())))));
    }

    @Test
    public void testOneTicket () {
        var ticketDetails = List.of(new TicketReserve.TicketDetails("test_section", 1, List.of()));

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, ticketDetails));

        //assertNotNull(response.reserveId);
        //assertEquals(1, Float.parseFloat(response.price));
        assertEquals(1, response.reserveTickets.size());
        var reservation = response.reserveTickets.get(0);
        assertNotNull(reservation.reserveId);
        assertTrue(0 < reservation.seatNum && reservation.seatNum <= 10);
        assertEquals("test_section", reservation.section);
        assertEquals(1, reservation.price);
    }

    @Test
    public void testMultipleTickets () {
        var ticketDetails = List.of(
                /*new TicketReserve.TicketDetailsNew( null, null, null, "test_section", 4),
                new TicketReserve.TicketDetailsNew( null, null, null, "test_section", 5),
                new TicketReserve.TicketDetails( null, null, null, "test_section", 6),
                new TicketReserve.TicketDetails( null, null, null, "test_section", 7)*/
                new TicketReserve.TicketDetails("test_section", 4, List.of(4, 6, 7, 5))
        );

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, ticketDetails));
        assertEquals(4, response.reserveTickets.size());
        //assertNotNull(response.reserveId);
        //assertEquals(4, Float.parseFloat(response.price));
        response.reserveTickets.sort(Comparator.comparing(r -> r.seatNum));
        var seenIds = new HashSet<String>();
        for (int i = 0; i < 4; i++) {
            var reservation = response.reserveTickets.get(i);
            assertEquals(4 + i, reservation.seatNum);
            assertEquals("test_section", reservation.section);
            assertFalse(seenIds.contains(reservation.reserveId));
            assertEquals(1, reservation.price);
            seenIds.add(reservation.reserveId);
        }
    }

    @Test
    public void testMultipleSections () {
        var ticketDetails = List.of(
                new TicketReserve.TicketDetails("test_section", 4, List.of()),
                new TicketReserve.TicketDetails("test_section2", 5, List.of())
        );

        var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime, ticketDetails));

        //assertNotNull(response.reserveId);
        //assertEquals(5, Float.parseFloat(response.price));
        assertEquals(9, response.reserveTickets.size());
        response.reserveTickets.sort(Comparator.comparing(r -> r.section));
        var seenNums1 = new HashSet<Integer>();
        var seenNums2 = new HashSet<Integer>();

        for (int i = 0; i < 4; i++) {
            assertEquals("test_section", response.reserveTickets.get(i).section);
            assertFalse(seenNums1.contains(response.reserveTickets.get(i).seatNum));
            assertEquals(1, response.reserveTickets.get(i).price);

            seenNums1.add(response.reserveTickets.get(i).seatNum);
        }

        for (int i = 4; i < 9; i++) {
            assertEquals("test_section2", response.reserveTickets.get(i).section);
            assertFalse(seenNums2.contains(response.reserveTickets.get(i).seatNum));
            assertEquals(4, response.reserveTickets.get(i).price);

            seenNums2.add(response.reserveTickets.get(i).seatNum);
        }
    }

    @Test
    public void testSectionLimits () {
        var seatNums = new HashSet<Integer>();
        for (int i = 0; i < 10; i++) {
            /*reserveIds.add(controller.ticketReserve(session, new TicketReserve.RequestNew(authToken, eventId, startTime,
                    List.of(new TicketReserve.TicketDetailsNew("test_section", 1, List.of())))).reserveTickets.get(0).reserveId);*/
            var response = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                            List.of(new TicketReserve.TicketDetails("test_section", 1, List.of()))));
            assertEquals(1, response.reserveTickets.size());
            seatNums.add(response.reserveTickets.get(0).seatNum);
            session = TestHelper.commitMakeSession(model, session);
        }

        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of())))));
        session.rollback();
        session.close();
        session = model.makeSession();

        /*var seatNums = new HashSet<Integer>();

        for (var i : reserveIds) {
            var eventReservation = session.getById(EventReservation.class, UUID.fromString(i)).orElse(null);
            var tickets = session.getAllWith(TicketReservation.class, "eventReservation", eventReservation);
            assertEquals(1, tickets.size());

            var ticket = tickets.get(0);
            seatNums.add(ticket.getSeatNum());
        }*/

        assertEquals(10, seatNums.size());
        assertEquals(1, seatNums.stream().min(Integer::compareTo).orElse(0));
        assertEquals(10, seatNums.stream().max(Integer::compareTo).orElse(0));
    }

    @Test
    public void testSpecifiedSeats () {
        var reserveIds = new ArrayList<String>();
        var seatNums = new ArrayList<Integer>();

        seatNums.add(controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(3))))).reserveTickets.get(0).seatNum);
        session = TestHelper.commitMakeSession(model, session);
        seatNums.add(controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(9))))).reserveTickets.get(0).seatNum);
        session = TestHelper.commitMakeSession(model, session);

        for (int i = 0; i < 8; i++) {
            seatNums.add(controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                    List.of(new TicketReserve.TicketDetails("test_section", 1, List.of())))).reserveTickets.get(0).seatNum);
            session = TestHelper.commitMakeSession(model, session);
        }

        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of())))));
        session.rollback();
        session.close();
        session = model.makeSession();

        /*var reserveId1 = reserveIds.get(0);
        var reserveId2 = reserveIds.get(1);

        var ticketReservation1 = session.getAllWith(TicketReservation.class, "eventReservation",
                session.getById(EventReservation.class, UUID.fromString(reserveId1)).orElse(null));
        var ticketReservation2 = session.getAllWith(TicketReservation.class, "eventReservation",
                session.getById(EventReservation.class, UUID.fromString(reserveId2)).orElse(null));*/

        //assertEquals(1, seatNums.size());
        assertEquals(3, seatNums.get(0));
        //assertEquals(1, ticketReservation2.size());
        assertEquals(9, seatNums.get(1));

        /*var seatNums = new HashSet<Integer>();

        for (var i : reserveIds) {
            var eventReservation = session.getById(EventReservation.class, UUID.fromString(i)).orElse(null);
            var tickets = session.getAllWith(TicketReservation.class, "eventReservation", eventReservation);
            assertEquals(1, tickets.size());

            var ticket = tickets.get(0);
            seatNums.add(ticket.getSeatNum());
        }*/

        assertEquals(10, seatNums.size());
        assertEquals(1, seatNums.stream().min(Integer::compareTo).orElse(0));
        assertEquals(10, seatNums.stream().max(Integer::compareTo).orElse(0));
    }

    @Test
    public void testReservationExpiry () {
        var id1 = controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(3))))).reserveTickets.get(0).reserveId;
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(3))))));
        session.rollback();
        session.close();
        session = model.makeSession();
        var reservation = session.getById(TicketReservation.class, UUID.fromString(id1))
                        .orElseThrow(AssertionFailedError::new);
        reservation.setExpiry(ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS).minusMinutes(5).minusSeconds(1));
        session = TestHelper.commitMakeSession(model, session);


        var eventReserveResponse = controller.eventReservedSeats(session, Map.of("auth_token", authToken, "event_id", eventId));
        assertEquals(0, eventReserveResponse.reserved.size());

        assertDoesNotThrow(() -> controller.ticketReserve(session, new TicketReserve.Request(authToken, eventId, startTime,
                List.of(new TicketReserve.TicketDetails("test_section", 1, List.of(3))))));
    }
}
