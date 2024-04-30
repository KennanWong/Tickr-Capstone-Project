package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import tickr.application.recommendations.EventVector;
import tickr.application.recommendations.SparseVector;
import tickr.application.serialised.SerializedLocation;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.responses.event.EventReservedSeatsResponse;
import tickr.application.serialised.responses.event.EventViewResponse;
import tickr.application.serialised.responses.event.EventAttendeesResponse.Attendee;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.util.EmailHelper;
import tickr.util.FileHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.util.Pair;
import tickr.util.Utils;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "events")
public class Event {
    static final Logger logger = LogManager.getLogger();

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "admins",
            joinColumns = {@JoinColumn(name = "event_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private Set<User> admins = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.REMOVE)
    private Set<Ticket> tickets = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.REMOVE)
    private Set<Category> categories = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.REMOVE)
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.REMOVE)
    private Set<Comment> comments;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "termId.event", cascade = CascadeType.REMOVE)
    private Set<TfIdf> tfIdfs = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.REMOVE)
    private List<SeatingPlan> seatingPlans;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "notification_members",
            joinColumns = {@JoinColumn(name = "event_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private Set<User> notificationMembers = new HashSet<>();

    @Column(name = "event_name")
    private String eventName;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "event_start")
    private ZonedDateTime eventStart;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "event_end")
    private ZonedDateTime eventEnd;

    @Column(name = "event_description")
    private String eventDescription;

    @Column(name = "seat_availability")
    private int seatAvailability = 0;

    @Column(name = "seat_capacity")
    private int seatCapacity = 0;

    @Column(name = "event_pic")
    private String eventPicture;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.REMOVE)
    private Set<UserInteraction> interactions = new HashSet<>();

    private boolean published;

    @Column(name = "spotify_playlist")
    private String spotifyPlaylist;

    public Event() {}

    public Event(String eventName, User host, ZonedDateTime eventStart, ZonedDateTime eventEnd,
            String eventDescription, SerializedLocation location, int seatAvailability, String eventPicture) {
        this(eventName, host, eventStart, eventEnd, eventDescription, location, seatAvailability, eventPicture, null, true);
    }

    public Event(String eventName, User host, ZonedDateTime eventStart, ZonedDateTime eventEnd,
            String eventDescription, SerializedLocation location, int seatAvailability, String eventPicture, String spotifyPlaylist) {
        this(eventName, host, eventStart, eventEnd, eventDescription, location, seatAvailability, eventPicture, spotifyPlaylist, true);
    }

    public Event (String eventName, User host, ZonedDateTime eventStart, ZonedDateTime eventEnd,
                  String eventDescription, SerializedLocation serializedLocation, int seatAvailability,
                  String eventPicture, String spotifyPlaylist, boolean checkDates) {
        if (eventStart.isAfter(eventEnd)) {
            throw new BadRequestException("Event start time is later than event end time!");
        }

        if (checkDates && eventStart.isBefore(ZonedDateTime.now())) {
            throw new ForbiddenException("Cannot create event in the past!");
        }

        this.location = new Location(serializedLocation);
        this.eventName = eventName;
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.eventDescription = eventDescription;
        this.seatAvailability = seatAvailability;
        this.seatCapacity = seatAvailability;
        this.host = host;
        this.eventPicture = eventPicture;
        this.published = false;
        this.spotifyPlaylist = spotifyPlaylist;
    }

    public UUID getId () {
        return id;
    }

    public User getHost () {
        return host;
    }

    public Location getLocation () {
        return location;
    }

    public void setLocation (Location location) {
        this.location = location;
    }

    public Set<User> getAdmins () {
        return admins;
    }

    public void addAdmin (User admin) {
        this.admins.add(admin);
    }

    public Set<Ticket> getTickets () {
        return tickets;
    }

    public Set<Category> getCategories () {
        return categories;
    }

    public void addCategory (Category category) {
        this.categories.add(category);
    }

    public Set<Tag> getTags () {
        return tags;
    }

    public void addTag (Tag tag) {
        this.tags.add(tag);
    }

    private Set<Comment> getComments () {
        return comments;
    }

    public String getEventName () {
        return eventName;
    }

    public ZonedDateTime getEventStart () {
        return eventStart;
    }

    public ZonedDateTime getEventEnd () {
        return eventEnd;
    }

    public String getEventDescription () {
        return eventDescription;
    }

    public int getSeatAvailability () {
        return seatAvailability;
    }

    public String getEventPicture () {
        return eventPicture;
    }

    private void clearAdmins () {
        this.admins.clear();
    }

    public boolean isPublished() {
        return published;
    }
    public Set<User> getNotificationMembers() {
        return notificationMembers;
    }

    private boolean userHasTicket (User user) {
        return getTickets().stream()
                .anyMatch(t -> t.isOwnedBy(user));
    }

    private boolean userHasReview (User user) {
        return getComments().stream()
                .filter(Predicate.not(Comment::isReply))
                .anyMatch(c -> c.isWrittenBy(user));
    }

    private boolean userIsPrivileged (User user) {
        return getHost().getId().equals(user.getId())
                || getAdmins().stream().map(User::getId).anyMatch(id -> user.getId().equals(id));
    }

    public List<String> getUserTicketIds (User user) {
        return this.tickets.stream()
                .filter(t -> t.isOwnedBy(user)) // Get tickets owned by user
                .sorted(Comparator.comparing((Ticket t) -> t.getSection().getSection())
                        .thenComparing(Ticket::getSeatNumber)) // Sort first by section and then by seat number
                .map(Ticket::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());
    }

    public void onUpdate () {
        location.lookupLongitudeLatitude();
    }

    public void editEvent (User user, EditEventRequest request, ModelSession session, String picture) {
        if (!userIsPrivileged(user)) {
            throw new ForbiddenException("User cannot edit event!");
        }

        String notification = "";

        if (request.eventName != null) {
            this.eventName = request.eventName;
        }

        if (picture != null) {
            if (!getEventPicture().equals("")) {
                // Delete original picture
                FileHelper.deleteFileAtUrl(getEventPicture());
            }
            this.eventPicture = picture;
        }

        var oldStart = eventStart;
        var oldEnd = eventEnd;

        if (request.getStartDate() != null) {
            ZonedDateTime start_date;
            try {
                start_date = ZonedDateTime.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE_TIME);
                this.eventStart = start_date;
            } catch (DateTimeParseException e) {
                throw new ForbiddenException("Invalid date time string!", e);
            }
        }
        if (request.getEndDate() != null) {
            ZonedDateTime end_date;
            try {
                end_date = ZonedDateTime.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE_TIME);
                this.eventEnd = end_date;
            } catch (DateTimeParseException e) {
                throw new ForbiddenException("Invalid date time string!", e);
            }
        }

        if (!oldStart.isEqual(eventStart) || !oldEnd.isEqual(eventEnd)) {
            // Event dates changed, add to notification
            notification = notification.concat(String.format("Event dates has changed from %s -> %s to %s -> %s\n",
                    oldStart.format(DateTimeFormatter.RFC_1123_DATE_TIME), oldEnd.format(DateTimeFormatter.RFC_1123_DATE_TIME),
                    eventStart.format(DateTimeFormatter.RFC_1123_DATE_TIME), eventEnd.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
        }

        if (request.getDescription() != null) {
            // Description changed, add to notification
            if (!this.eventDescription.equals(request.getDescription())) {
                notification = notification.concat(String.format("Event description has changed: %s\n", request.getDescription()));
            }

            this.eventDescription = request.getDescription();
        }

        if (request.spotifyPlaylist != null) {
            this.spotifyPlaylist = request.spotifyPlaylist;
        }

        if (request.categories != null) {
            for (Category cat : this.categories) {
                session.remove(cat);
            }
            // Set new categories
            this.categories.clear();
            for (String cat : request.categories) {
                Category newCat = new Category(this, cat);
                session.save(newCat);
                this.addCategory(newCat); 
            }
        }
        if (request.tags != null) {
            for (Tag tag : this.tags) {
                session.remove(tag);
            }
            // Set new tags
            this.tags.clear();
            for (String tag : request.tags) {
                Tag newTag = new Tag(this, tag);
                session.save(newTag);
                this.addTag(newTag); 
            }
        }
        if (request.admins != null) {
            // Set new admins
            this.clearAdmins();

            for (String admin : request.admins) {
                User userAdmin;
                try {
                    userAdmin = session.getById(User.class, UUID.fromString(admin))
                        .orElseThrow(() -> new ForbiddenException(String.format("Unknown account \"%s\".", admin)));
                } catch (IllegalArgumentException e) {
                    throw new ForbiddenException("invalid admin Id");
                }
                this.addAdmin(userAdmin);
                //userAdmin.addAdminEvents(this);
            }
        }

        if (request.seatingDetails != null) {
            for (SeatingPlan seat : seatingPlans) {
                session.remove(seat);
            }

            seatingPlans.clear();
            for (EditEventRequest.SeatingDetails seats : request.seatingDetails) {
                SeatingPlan seatingPlan = new SeatingPlan(this, this.location, seats.section, seats.availability, seats.ticketPrice, seats.hasSeats);
                session.save(seatingPlan);
                seatingPlans.add(seatingPlan);
            }
            this.seatAvailability = request.getSeatCapacity();
            this.seatCapacity = request.getSeatCapacity();
        }


        if (request.location != null) {
            session.remove(this.location);
            Location newLocation = new Location(request.location);
            session.save(newLocation);
            this.location = newLocation;

            seatingPlans.forEach(s -> s.updateLocation(newLocation));
        }

        this.published = request.published;

        if (!notification.equals("")) {
            makeEventChangeNotification(user, notification);
        }

        onUpdate();
    }

    public EventViewResponse getViewResponse (User user) {
        if (!canView(user)) {
            throw new ForbiddenException("Unable to view event!");
        }

        List<EventViewResponse.SeatingDetails> seatingResponse = new ArrayList<EventViewResponse.SeatingDetails>();
        for (SeatingPlan seats : seatingPlans) {
            EventViewResponse.SeatingDetails newSeats = new EventViewResponse.SeatingDetails(seats.getSection(), seats.getAvailableSeats(), seats.ticketPrice, seats.getTotalSeats(), seats.hasSeats);
            seatingResponse.add(newSeats);
        }
        Set<String> tags = new HashSet<>();
        for (Tag tag : getTags()) {
            tags.add(tag.getTags());
        }
        Set<String> categories = new HashSet<>();
        for (Category category : getCategories()) {
            categories.add(category.getCategory());
        }
        Set<String> admins = new HashSet<>();
        for (User admin : getAdmins()) {
            admins.add(admin.getId().toString());
        }
        SerializedLocation location = getLocation().getSerialisedLocation();

        return new EventViewResponse(getHost().getId().toString(), eventName, eventPicture, location, eventStart.format(DateTimeFormatter.ISO_INSTANT), eventEnd.format(DateTimeFormatter.ISO_INSTANT),
                eventDescription, seatingResponse,
                admins, categories, tags, published, seatAvailability, seatCapacity, spotifyPlaylist);
    }

    public List<TicketReservation> makeReservations (ModelSession session, User user, ZonedDateTime requestedTime, String section,
                                                     int quantity, List<Integer> seatNums) {
        if (!published) {
            throw new ForbiddenException("Unable to reserve tickets from unpublished event!");
        }
        if (section == null) {
            throw new BadRequestException("Null section!");
        } else if (quantity <= 0 || (seatNums.size() != 0 && seatNums.size() != quantity)) {
            throw new BadRequestException("Invalid quantity of seats!");
        } else if (eventStart.isAfter(requestedTime) || eventEnd.isBefore(requestedTime)) {
            throw new ForbiddenException("Invalid requested time!");
        }

        for (var i : seatingPlans) {
            if (i.getSection().equals(section)) {
                return seatNums.size() != 0 ? i.reserveSeats(session, user, seatNums) : i.reserveSeats(session, user, quantity);
            }
        }

        // Only gets here if the section is invalid
        throw new ForbiddenException("Invalid section!");
    }

    public List<Attendee> getAttendees (User user) {
        if (!canView(user)) {
            throw new ForbiddenException("Unable to view event!");
        }

        return this.tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getUser, Collectors.toList()))
                .entrySet()
                .stream()
                .map(e -> new Pair<>(e.getKey(), e.getValue()
                        .stream()
                        .map(Ticket::getId)
                        .map(UUID::toString)
                        .collect(Collectors.toList())))
                .map(p -> new Attendee(p.getFirst().getId().toString(), p.getSecond()))
                .collect(Collectors.toList());
    }
    
    public Comment addReview (ModelSession session, User author, String title, String text, float rating) {
        if (!userHasTicket(author)) {
            throw new ForbiddenException("You do not own a ticket for this event!");
        }

        if (userHasReview(author)) {
            throw new ForbiddenException("You have already made a review for this event!");
        }

        if (getEventStart().isAfter(ZonedDateTime.now(ZoneId.of("UTC")))) {
            throw new ForbiddenException("Cannot create review of event that hasn't happened!");
        }

        var comment = Comment.makeReview(this, author, title, text, rating);
        session.save(comment);
        getComments().add(comment);

        return comment;
    }

    public boolean canReply (User user) {
        return userIsPrivileged(user) || userHasTicket(user);
    }

    public void onDelete (User instigator) {
        if (eventPicture != null) {
            // Delete picture if necessary
            FileHelper.deleteFileAtUrl(eventPicture);
        }

        if (!getEventStart().isBefore(ZonedDateTime.now())) {
            // Refund tickets if event hasn't happened yet
            for (var i : tickets) {
                i.refund(i.getUser());
            }
            makeEventCancelNotification(instigator);
        }
    }

    public boolean matchesCategories (List<String> categories) {
        return categories.size() == 0 || getCategories().stream()
                .map(Category::getCategory)
                .anyMatch(categories::contains);
    }

    public boolean matchesTags (List<String> tags) {
        return tags.size() == 0 || getTags().stream()
                .map(Tag::getTags)
                .anyMatch(tags::contains);
    }

    public boolean startsAfter (ZonedDateTime startTime) {
        return startTime == null || getEventStart().isAfter(startTime) || getEventStart().isEqual(startTime);
    }

    public boolean endsBefore (ZonedDateTime endTime) {
        return endTime == null || getEventEnd().isBefore(endTime) || getEventEnd().isEqual(endTime);
    }

    public boolean matchesDescription (Set<String> words) {
        if (words.size() == 0) {
            return true;
        }
        var wordList = new HashSet<String>();
        wordList.addAll(Utils.toWords(getEventName()));
        wordList.addAll(Utils.toWords(getEventDescription()));

        return !Collections.disjoint(words, wordList);
    }

    public boolean canView (User user) {
        return published || (user != null && (getHost().getId().equals(user.getId()) ||
                getAdmins().stream().map(User::getId).anyMatch(Predicate.isEqual(user.getId()))));
    }

    public void makeAnnouncement (User user, String announcement) {
        if (announcement == null || announcement.equals("")) {
            throw new BadRequestException("Cannot make empty announcement!");
        }

        if (!userIsPrivileged(user)) {
            throw new ForbiddenException("You are not allowed to make an announcement!");
        }

        var seen = new HashSet<UUID>();

        for (var i : tickets) {
            if (!seen.contains(i.getUser().getId())) {
                EmailHelper.sendAnnouncement(user, i.getUser(), this, announcement);
                seen.add(i.getUser().getId());
            }
        }
    }

    public void makeEventCancelNotification(User user) {
        if (!userIsPrivileged(user)) {
            throw new ForbiddenException("You are not allowed to make a notification!");
        }
        var seen = new HashSet<>();

        for (var i : tickets) {
            if (!seen.contains(i.getUser().getId())) {
                EmailHelper.sendEventCancellationNotification(user, this, i.getUser());
                seen.add(i.getUser().getId());
            }
        }
    }

    public void makeEventChangeNotification(User user, String notification) {
        if (notification == null || notification.equals("")) {
            throw new BadRequestException("Empty notification!");
        }
        if (!userIsPrivileged(user)) {
            throw new ForbiddenException("You are not allowed to make a notification!");
        }

        for (var i : notificationMembers) {
            EmailHelper.sendEventChangeNotification(user, this, i, notification);
        }
    }

    public void editNotificationMembers(ModelSession session, User user, boolean notifications) {
        if (notifications) {
            notificationMembers.add(user);
        } else {
            notificationMembers.remove(user);
        }
    }
    
    public Map<String, Long> getWordCounts () {
        // Get counts of words in both event name and description
        var nameMap = Utils.toWordsMap(eventName);
        var descMap = Utils.toWordsMap(eventDescription);

        return Stream.concat(nameMap.entrySet().stream(), descMap.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));
    }

    public SparseVector<String> getTfIdfVector (int numDocuments) {
        // Convert tf idfs to sparse vector
        var keys = tfIdfs.stream().map(TfIdf::getTermString).collect(Collectors.toList());
        var values = tfIdfs.stream().map(t -> t.getTfIdf(numDocuments)).collect(Collectors.toList());

        return new SparseVector<>(keys, values).normalised();
    }

    public void setTfIdfs (List<TfIdf> tfIdfs) {
        this.tfIdfs.clear();
        this.tfIdfs.addAll(tfIdfs);
    }

    public SparseVector<String> getTagVector () {
        // Makes normalised sparse vector of tags, each with a frequency of 1
        return new SparseVector<>(tags.stream().map(Tag::getTags).collect(Collectors.toList()), Collections.nCopies(tags.size(), 1.0))
                .normalised();
    }

    public SparseVector<String> getCategoryVector () {
        // Makes normalised sparse vector of categories, each with a frequency of 1
        return new SparseVector<>(categories.stream().map(Category::getCategory).collect(Collectors.toList()), Collections.nCopies(categories.size(), 1.0))
                .normalised();
    }

    public double getDistance (Event other) {
        // Get distance between two events
        return getLocation().getDistance(other.getLocation());
    }

    public EventVector getEventVector (int numDocuments) {
        // Make event vector from component sparse vectors
        return new EventVector(getTfIdfVector(numDocuments), getTagVector(), getCategoryVector(),
                new SparseVector<>(List.of(host.getId().toString()), List.of(Utils.getIdf(host.getHostingEvents().size(), numDocuments))));
    }

    public void makeHost (User oldHost, User newHost) {
        if (!getHost().getId().equals(oldHost.getId())) {
            throw new BadRequestException("User is not the host!");
        }

        if (!getAdmins().contains(newHost)) {
            throw new BadRequestException("User is not an admin!");
        }

        admins.remove(newHost); // Host should not be an admin
        admins.add(oldHost); // Make original host admin
        host = newHost;
    }

    public List<EventReservedSeatsResponse.Reserved> getReservedSeats () {
        return seatingPlans.stream()
                .map(SeatingPlan::getReservations) // Get reservations from seat plans
                .flatMap(Collection::stream) // Flat map to single stream
                .map(TicketReservation::getReservedResponse) // Convert to responses
                .collect(Collectors.toList());
    }
}
