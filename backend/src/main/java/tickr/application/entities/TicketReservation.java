package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.application.apis.purchase.IOrderBuilder;
import tickr.application.apis.purchase.LineItem;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.responses.event.EventReservedSeatsResponse;
import tickr.application.serialised.responses.ticket.ReserveDetailsResponse;
import tickr.application.serialised.responses.group.GroupDetailsResponse.GroupMember;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.ForbiddenException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "ticket_reservation")
public class TicketReservation {

    private static final Duration EXPIRY_DURATION = Duration.ofMinutes(5);
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seating_id")
    private SeatingPlan section;

    private float price;

    @Column(name = "seat_num")
    private int seatNum;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "expiry_time")
    private ZonedDateTime expiryTime;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "ticketReservation", cascade = CascadeType.REMOVE)
    private PurchaseItem purchaseItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "ticketReservation", cascade = CascadeType.REMOVE)
    private Invitation invitation;

    @Column(name = "group_accepted")
    private boolean groupAccepted;

    public TicketReservation () {

    }

    public TicketReservation (User user, SeatingPlan section, int seatNum, float price) {
        this.user = user;
        this.section = section;
        this.seatNum = seatNum;
        this.price = price;
        this.expiryTime = ZonedDateTime.now(ZoneId.of("UTC"))
                .plus(EXPIRY_DURATION);
    }

    public int getSeatNum () {
        return seatNum;
    }

    public float getPrice () {
        return price;
    }

    public void setExpiry (ZonedDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean hasExpired () {
        return ZonedDateTime.now(ZoneId.of("UTC"))
                .isAfter(expiryTime);
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TicketReserve.ReserveDetails getDetails () {
        return new TicketReserve.ReserveDetails(id.toString(), seatNum, section.getSection(), price);
    }

    public IOrderBuilder registerPurchaseItem (ModelSession session, IOrderBuilder builder, UUID purchaseId, User user,
                                               String firstName, String lastName, String email) {
        if (!this.user.getId().equals(user.getId())) {
            throw new ForbiddenException("User is forbidden from purchasing this reservation!");
        }

        if (hasExpired()) {
            throw new ForbiddenException("Ticket reservation has expired!");
        }

        // Make purchase item
        var purchaseItem = new PurchaseItem(purchaseId, this, firstName, lastName, email);
        session.save(purchaseItem);

        // Add line item with correct formatting
        return builder.withLineItem(new LineItem(String.format("%s (%s%d)", getSection().getEvent().getEventName(),
                getSection().getSection().charAt(0), getSeatNum()), this.price, purchaseItem));
    }

    public Ticket convert (String firstName, String lastName, String email, String paymentId) {
        if (paymentId == null && price != 0) {
            throw new RuntimeException("Invalid payment id!");
        }
        Ticket ticket = new Ticket(user, section, seatNum, firstName, lastName, email, paymentId, (long)Math.floor(price * 100));
        if (group != null) {
            group.convert(ticket, this);
        }
        return ticket;
    }

    public SeatingPlan getSection() {
        return section;
    }

    public void setSection(SeatingPlan section) {
        this.section = section;
    }

    public boolean canCancel (User user) {
        return this.user.getId().equals(user.getId()) && purchaseItem == null;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Invitation getInvitation() {
        return invitation;
    }

    public void setInvitation(Invitation invitation) {
        this.invitation = invitation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isGroupAccepted() {
        return groupAccepted;
    }

    public void setGroupAccepted(boolean groupAccepted) {
        this.groupAccepted = groupAccepted;
    }
    
    public void acceptInvitation (User user) {
        setUser(user);
        setInvitation(null);
        setGroupAccepted(true);
    }

    public void denyInvitation() {
        setInvitation(null);
        setGroupAccepted(false);
    }

    public void removeUserFromGroup(User leader) {
        this.groupAccepted = false;
        this.user = leader;
    }

    public GroupMember createGroupMemberDetails() {
        if (invitation == null && groupAccepted) {
            return new GroupMember(user.getEmail(), section.getSection(), seatNum, false);
        } else {
            return null;
        }
    }

    public ReserveDetailsResponse getReserveDetailsResponse() {
        return new ReserveDetailsResponse(section.getSection(), seatNum, price, section.getEvent().getId().toString());
    }

    public EventReservedSeatsResponse.Reserved getReservedResponse () {
        return new EventReservedSeatsResponse.Reserved(seatNum, section.getSection());
    }

    public boolean ownedBy (User user) {
        return this.user.equals(user);
    }

    public boolean inGroup () {
        return group != null;
    }
}
