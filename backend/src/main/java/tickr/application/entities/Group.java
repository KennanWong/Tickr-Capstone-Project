package tickr.application.entities;

import jakarta.persistence.*;
import tickr.application.serialised.responses.group.GroupDetailsResponse;
import tickr.application.serialised.responses.group.GroupDetailsResponse.GroupMember;
import tickr.application.serialised.responses.group.GroupDetailsResponse.PendingInvite;
import tickr.server.exceptions.BadRequestException;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "user_groups")
public class Group {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    private int size;

    @Column(name = "time_created")
    private ZonedDateTime timeCreated;

    @Column(name = "ticket_available")
    private int ticketsAvailable;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "group_users",
            joinColumns = {@JoinColumn(name = "group_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private Set<User> users;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group", cascade = CascadeType.REMOVE)
    private Set<TicketReservation> ticketReservations;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group", cascade = CascadeType.REMOVE)
    private Set<Invitation> invitations; 

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group")
    private Set<Ticket> tickets; 

    public Group(User leader, ZonedDateTime timeCreated, int size, Set<TicketReservation> ticketReservations) {
        this.leader = leader;
        this.timeCreated = timeCreated;
        this.size = size;
        this.ticketsAvailable = ticketReservations.size();
        this.ticketReservations = ticketReservations;
        this.users = new HashSet<>();
        users.add(leader);
        for (TicketReservation t : ticketReservations) {
            t.setGroup(this);
        }
    }

    public Group()  {}

    public UUID getId () {
        return id;
    }

    public User getLeader () {
        return leader;
    }

    public int getSize () {
        return size;
    }

    private void setSize (int size) {
        this.size = size;
    }

    private void setTicketsAvailable (int ticketsAvailable) {
        this.ticketsAvailable = ticketsAvailable;
    }

    private Set<User> getUsers () {
        return users;
    }

    private void addUser(User user) {
        this.users.add(user);
    }

    public void addInvitation(Invitation invitation) {
        this.invitations.add(invitation);
    }

    public void removeInvitation(Invitation invitation) {
        invitations.remove(invitation);
    }

    private void addTicket (Ticket ticket) {
        this.tickets.add(ticket);
    }

    private void removeReservation (TicketReservation ticket) {
        this.ticketReservations.remove(ticket);
    }

    public void acceptInvitation(Invitation invitation, User user) {
        if (!getUsers().contains(user)) {
            addUser(user);
            setSize(this.size + 1);
        }
        removeInvitation(invitation);
    }

    public void convert(Ticket ticket, TicketReservation reserve) {
        addTicket(ticket);
        removeReservation(reserve);
        setTicketsAvailable(this.ticketsAvailable - 1);
        ticket.setGroup(this);
    }

    private List<GroupMember> getGroupMemberDetails() {
        List<GroupMember> list = new ArrayList<>();
        for (Ticket t : tickets) {
            list.add(t.createGroupMemberDetails());
        }
        for (TicketReservation t : ticketReservations) {
            if (t.createGroupMemberDetails() != null) {
                list.add(t.createGroupMemberDetails());
            }
        }
        return list;
    }
    
    private List<PendingInvite> getPendingInviteDetails() {
        List<PendingInvite> list = new ArrayList<>();
        for (Invitation i : invitations) {
            list.add(i.createPendingInviteDetails());
        }
        return list;
    }

    private List<String> getAvailableReserves(User host) {
        return  host.equals(leader) 
        ? ticketReservations.stream()
                .filter(t -> t.getInvitation() == null && !t.isGroupAccepted())
                .map(TicketReservation::getId)
                .map(UUID::toString)
                .collect(Collectors.toList())
        : null;
    }

    public void removeUser(User instigator, User user) {
        if (!leader.equals(instigator)) {
            throw new BadRequestException("Only the group leader can remove members!");
        }
        if (!users.contains(user)) {
            throw new BadRequestException("User is not a part of this group!");
        } else {
            users.remove(user);
        }
        for (TicketReservation t : ticketReservations) {
            if (t.getUser().equals(user)) {
                t.removeUserFromGroup(leader);
            }
        }
    }

    private String getEventId() {
        if (tickets.isEmpty() && ticketReservations.isEmpty()) {
            throw new BadRequestException("There is no event associated with this group!");
        }
        
        if (!ticketReservations.isEmpty()) {
            List<TicketReservation> r = new ArrayList<>(this.ticketReservations);
            return r.get(0).getSection().getEvent().getId().toString();
        } else {
            List<Ticket> t = new ArrayList<>(this.tickets);
            return t.get(0).getEvent().getId().toString();
        }
    }
    
    public GroupDetailsResponse getGroupDetailsResponse(User host) {
        return new GroupDetailsResponse(leader.getId().toString(), getGroupMemberDetails(), getPendingInviteDetails(), getAvailableReserves(host), getEventId());
    }
}
