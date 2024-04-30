package tickr.application.entities;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.application.serialised.responses.group.GroupDetailsResponse.PendingInvite;

import java.util.UUID;

@Entity
@Table(name = "invitation")
public class Invitation {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserve_id")
    private TicketReservation ticketReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Invitation(Group group, TicketReservation ticketReservation, User user) {
        this.group = group;
        this.ticketReservation = ticketReservation;
        this.user = user;
    }

    public Invitation() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TicketReservation getTicketReservation() {
        return ticketReservation;
    }

    public void acceptInvitation(User user) {
        group.acceptInvitation(this, user);
        ticketReservation.acceptInvitation(user);
    }

    public void denyInvitation() {
        group.removeInvitation(this);
        user.removeInvitation(this);
        ticketReservation.denyInvitation();
    }

    public User getUser() {
        return user;
    }

    public void handleInvitation(Group group, TicketReservation reserve, User user) {
        group.addInvitation(this);
        reserve.setInvitation(this);
        user.addInvitation(this);
    }

    public PendingInvite createPendingInviteDetails() {
        return new PendingInvite(user.getEmail(), ticketReservation.getSection().getSection(), ticketReservation.getSeatNum(), id.toString());
    }
}
