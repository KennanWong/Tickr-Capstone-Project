package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.ForbiddenException;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Entity
@Table(name = "seating_plan")
public class SeatingPlan {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    private String section;

    @Column(name = "seat_availability")
    public int availableSeats;

    @Column(name = "total_seats")
    private int totalSeats = 0;

    @Column(name = "ticket_price")
    public float ticketPrice;

    @Column(name = "has_seats")
    public boolean hasSeats;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "section", cascade = CascadeType.REMOVE)
    private Set<TicketReservation> reservations = new HashSet<>();


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "section", cascade = CascadeType.REMOVE)
    private Set<Ticket> tickets = new HashSet<>();

    public SeatingPlan () {}    

    public SeatingPlan(Event event, Location location, String section, int availableSeats, float ticketPrice, boolean hasSeats) {
        this.event = event;
        this.location = location;
        this.section = section;
        this.availableSeats = availableSeats;
        this.totalSeats = availableSeats;
        this.ticketPrice = ticketPrice;
        this.hasSeats = hasSeats;
    }

    public Event getEvent () {
        return event;
    }

    private void setLocation (Location location) {
        this.location = location;
    }

    public String getSection () {
        return section;
    }


    public int getAvailableSeats () {
        return totalSeats - getAllocatedNumbers().size();
    }

    public int getTotalSeats () {
        return totalSeats;
    }

    public void updateLocation (Location location) {
        setLocation(location);
    }

    

    public Set<TicketReservation> getReservations() {
        return reservations.stream()
                .filter(Predicate.not(TicketReservation::hasExpired))
                .collect(Collectors.toSet());
    }

    private Set<Integer> getAllocatedNumbers () {
        // Get ticket seat numbers
        var ticketSet = tickets.stream()
                .map(Ticket::getSeatNumber)
                .collect(Collectors.toSet());

        // Get reservation seat numbers
        var reservedSet = reservations.stream()
                .filter(Predicate.not(TicketReservation::hasExpired)) // Only include in date reservations
                .map(TicketReservation::getSeatNum)
                .collect(Collectors.toSet());

        ticketSet.addAll(reservedSet);

        return ticketSet;
    }

    private List<TicketReservation> makeReservations (ModelSession session, User user, List<Integer> seatNums) {
        // Makes reservations, assuming seatNums are all valid
        var newReservations = new ArrayList<TicketReservation>();
        for (var i : seatNums) {
            var reserve = new TicketReservation(user, this, i, ticketPrice);
            session.save(reserve);
            newReservations.add(reserve);
            reservations.add(reserve);
        }

        availableSeats -= seatNums.size();

        return newReservations;
    }

    public List<TicketReservation> reserveSeats (ModelSession session, User user, int quantity) {
        if (getAvailableSeats() < quantity) {
            throw new ForbiddenException("Not enough tickets remaining!");
        }

        var nums = getAllocatedNumbers().stream()
                .sorted()
                .collect(Collectors.toList());

        var reservedNums = new ArrayList<Integer>();


        // Get seat numbers starting from lowest up, filling gaps where possible
        int last = 0;
        int next = last;
        for (var i : nums) {
            if (reservedNums.size() == quantity) {
                // Required numbers achieved
                break;
            }
            next = i; // next seat number along

            // Add seats between last seat number read and next seat number
            for (int j = last + 1; j < next; j++) {
                reservedNums.add(j);
                if (reservedNums.size() == quantity) {
                    // Enough seat numbers
                    break;
                }
            }

            // Set last to current seat number
            last = next;
        }


        while (reservedNums.size() < quantity) {
            // More seat numbers required, allocate from end as next will be the last seat number read
            reservedNums.add(++next);
        }


        return makeReservations(session, user, reservedNums);
    }

    public List<TicketReservation> reserveSeats (ModelSession session, User user, List<Integer> seatNums) {
        var takenNums = getAllocatedNumbers();
        if (seatNums.stream().anyMatch(i -> i <= 0 || i > totalSeats || takenNums.contains(i))) {
            throw new ForbiddenException("One or more ticket number is already taken!");
        }

        return makeReservations(session, user, seatNums);
    }
}
