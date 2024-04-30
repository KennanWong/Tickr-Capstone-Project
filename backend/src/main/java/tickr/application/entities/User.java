package tickr.application.entities;

import jakarta.persistence.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.serialised.combined.user.NotificationManagement;
import tickr.application.serialised.responses.user.ViewProfileResponse;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.ForbiddenException;
import tickr.util.CryptoHelper;
import tickr.util.FileHelper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"email"}))
public class User {
    private static final Pattern EMAIL_REGEX = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])");
    private static final Pattern PASS_REGEX = Pattern.compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$");

    static final Logger logger = LogManager.getLogger();

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "password_hash")
    private char[] passwordHash;
    private String username;
    private LocalDate dob;

    private boolean reminders = true;

    private String description;

    @Column(name = "profile_pic")
    private String profilePicture;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
    private Set<AuthToken> tokens = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
    private Set<ResetToken> resetTokens = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "host", cascade = CascadeType.ALL)
    private Set<Event> hostingEvents = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "admins", cascade = {})
    private Set<Event> adminEvents = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "leader", cascade = CascadeType.ALL)
    private Set<Group> ownedGroups = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users", cascade = {})
    private Set<Group> groups = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Ticket> tickets = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    private Set<Reaction> reactions = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
    private Set<TicketReservation> reservations;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "notificationMembers", cascade = {})
    private Set<Event> notificationEvents = new HashSet<>();
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
    private Set<UserInteraction> interactions;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
    private Set<Invitation> invitations;

    public User () {

    }

    public User (String email, String password, String username, String firstName, String lastName, LocalDate dob) {
        if (!EMAIL_REGEX.matcher(email).matches()) {
            logger.debug("Email did not match regex!");
            throw new ForbiddenException("Invalid email!");
        }

        if (!PASS_REGEX.matcher(password).matches()) {
            logger.debug("Password did not match regex!");
            throw new ForbiddenException("Invalid password!");
        }

        if (dob.isAfter(LocalDate.now(ZoneId.of("UTC")))) {
            logger.debug("Date of birth is in the future!");
            throw new ForbiddenException("Invalid date of birth!");
        }

        this.email = email;
        this.passwordHash = CryptoHelper.hashPassword(password);
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dob = dob;

        this.description = "";
        this.profilePicture = "";
    }

    /**
     * Makes an auth token for the user
     * @param session
     * @param expiryDuration the requested expiry duration for the token
     * @return the resulting auth token
     */
    public AuthToken makeToken (ModelSession session, Duration expiryDuration) {
        var token = new AuthToken(this, ZonedDateTime.now(ZoneId.of("UTC")), expiryDuration);
        session.save(token);
        getTokens().add(token);

        return token;
    }

    public AuthToken authenticatePassword (ModelSession session, String password, Duration tokenExpiryDuration) {
        if (CryptoHelper.verifyHash(password, getPasswordHash())) {
            return makeToken(session, tokenExpiryDuration);
        } else {
            throw new ForbiddenException("Incorrect password.");
        }
    }

    /**
     * Checks that the password matches the stored password hash
     * @param password
     * @return true if valid, false if not
     */
    public boolean verifyPassword (String password) {
        return CryptoHelper.verifyHash(password, getPasswordHash());
    }

    /**
     * Changes password hash to another password hash
     * @param newPassword
     * @return
     */
    public void changePassword (ModelSession session, String newPassword) {
        this.passwordHash = CryptoHelper.hashPassword(newPassword);
        
        for (var i : tokens) {
            session.remove(i);
        } 
        tokens.clear();
        
    }

    public UUID getId () {
        return id;
    }

    public String getEmail () {
        return email;
    }

    public String getFirstName () {
        return firstName;
    }

    public String getLastName () {
        return lastName;
    }

    private char[] getPasswordHash () {
        return passwordHash;
    }

    public String getUsername () {
        return username;
    }

    public LocalDate getDob () {
        return dob;
    }

    public Set<Event> getHostingEvents () {
        return hostingEvents;
    }

    public Set<Group> getGroups () {
        return groups;
    }

    public void addGroup (Group group) {
        this.groups.add(group);
    }

    public Set<Ticket> getTickets () {
        return tickets;
    }

    public Set<AuthToken> getTokens () {
        return tokens;
    }

    public boolean doReminders () {
        return reminders;
    }

    private void setReminders (boolean reminders) {
        this.reminders = reminders;
    }

    private String getDescription () {
        return description;
    }

    private String getProfilePicture () {
        return profilePicture;
    }

    public NotificationManagement.Settings getSettings () {
        return new NotificationManagement.Settings(doReminders());
    }

    public Set<UserInteraction> getInteractions () {
        return interactions;
    }

    public void setSettings (NotificationManagement.Settings settings) {
        if (settings.reminders != null) {
            setReminders(settings.reminders);
        }
    }

    public Set<Event> getNotificationEvents () {
        return notificationEvents;
    }

    public ViewProfileResponse getProfile () {
        return new ViewProfileResponse(getUsername(), getFirstName(), getLastName(), getProfilePicture(), getEmail(), getDescription());
    }

    public void editProfile (String username, String firstName, String lastName, String email, String description, String pfpUrl) {
        if (username != null) {
            this.username = username;
        }

        if (firstName != null) {
            this.firstName = firstName;
        }

        if (lastName != null) {
            this.lastName = lastName;
        }

        if (email != null) {
            this.email = email;
        }

        if (description != null) {
            this.description = description;
        }

        if (pfpUrl != null) {
            if (!getProfilePicture().equals("")) {
                FileHelper.deleteFileAtUrl(getProfilePicture());
            }
            this.profilePicture = pfpUrl;
        }
    }

    public void invalidateToken (ModelSession session, AuthToken token) {
        getTokens().remove(token);
        session.remove(token);
    }

    public void onDelete (ModelSession session) {
        if (profilePicture != null) {
            FileHelper.deleteFileAtUrl(profilePicture);
        }
    }

    public Set<String> getTicketIds () {
        return this.tickets.stream()
                .map(Ticket::getId)
                .map(UUID::toString)
                .collect(Collectors.toSet());
    }


    public Stream<Event> getStreamHostingEvents() {
        return getHostingEvents().stream();
    }
    
    public void sendEmail (String subject, String message) {
        ApiLocator.locateApi(IEmailAPI.class).sendEmail(email, subject, message);
    }

    public void addReservation(TicketReservation t) {
        reservations.add(t);
    }

    public void editEventNotifications (ModelSession session, Event event, Boolean notifications) {
        if (notifications) {
            notificationEvents.add(event);
        } else {
            notificationEvents.remove(event);
        }
    }

    public void addInvitation(Invitation i) {
        invitations.add(i);
    }

    public void removeInvitation(Invitation i) {
        invitations.remove(i);
    }

    /*public void userAcceptInvitation(Group group, TicketReservation reserve, Invitation invitation) {
        addReservation(reserve);
        addGroup(group);
        removeInvitation(invitation);
    }*/

    public void sendPasswordReset (ModelSession session) {
        var resetToken = new ResetToken(this, Duration.ofHours(24));
        session.save(resetToken);

        // localhost:3000/change_password/{email}/{reset_token}
        var resetUrl = String.format("http://localhost:3000/change_password/%s/%s", this.getEmail(), resetToken.getId().toString());

        var messageString = String.format("Please reset your Tickr account password <a href=\"%s\">here</a>.\n", resetUrl);

        sendEmail("Tickr account password reset", messageString);
    }
}
