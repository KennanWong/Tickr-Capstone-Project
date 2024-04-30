package tickr.application.entities;

import jakarta.persistence.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.util.Utils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "purchase_item")
public class PurchaseItem {
    private static final Duration EXPIRY_DURATION = Duration.ofHours(24);
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "purchase_id")
    private UUID purchaseId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private TicketReservation ticketReservation;

    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "email")
    private String email;

    @Column(name = "payment_id")
    private String paymentId;

    static final Logger logger = LogManager.getLogger();

    public PurchaseItem () {

    }

    public PurchaseItem (UUID purchaseId, TicketReservation reservation, String firstName, String lastName, String email) {
        if ((firstName == null) != (lastName == null)) {
            throw new BadRequestException("Both or neither of first name and last name must be present!");
        } else if (email != null && !Utils.isValidEmail(email)) {
            throw new BadRequestException("Invalid email: " + email);
        }

        this.purchaseId = purchaseId;
        this.ticketReservation = reservation;
        reservation.setExpiry(ZonedDateTime.now(ZoneId.of("UTC")).plus(EXPIRY_DURATION));
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email != null ? email.toLowerCase().trim() : null;
    }

    public Ticket convert (ModelSession session, String paymentId) {
        // Convert reservation to ticket
        var ticket = ticketReservation.convert(firstName, lastName, email, paymentId);
        // Delete reservation
        session.remove(ticketReservation);

        return ticket;
    }

    public void cancel (ModelSession session) {
        session.remove(ticketReservation);
    }

    public void setPaymentDetails (String paymentId) {
        this.paymentId = paymentId;
    }

    public Event getEvent () {
        return ticketReservation.getSection().getEvent();
    }

    public User getUser () {
        return ticketReservation.getUser();
    }
}
