package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.application.serialised.SerialisedReaction;
import tickr.application.serialised.SerialisedReply;
import tickr.application.serialised.SerialisedReview;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "event_comments")
public class Comment {
    // Contains both reviews and replies, differentiated by whether parent is null or not
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.REMOVE)
    private Set<Comment> children = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "comment", cascade = CascadeType.REMOVE)
    private Set<Reaction> reactions = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "comment_text")
    private String commentText;

    @Column(name = "comment_title")
    private String title;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "comment_time")
    private ZonedDateTime commentTime;

    private Float rating;

    public UUID getId () {
        return id;
    }

    public Event getEvent () {
        return event;
    }

    private Set<Comment> getChildren () {
        return children;
    }

    private Set<Reaction> getReactions () {
        return reactions;
    }

    public User getAuthor () {
        return author;
    }

    public ZonedDateTime getCommentTime () {
        return commentTime;
    }

    public Comment () {}

    public Comment (Event event, User author, Comment parent, String title, String commentText, Float rating) {
        this.event = event;
        this.parent = parent;
        this.author = author;
        this.title = title;
        this.commentText = commentText;
        this.rating = rating;
        commentTime = ZonedDateTime.now(ZoneId.of("UTC"));
    }

    public boolean isWrittenBy (User user) {
        return author.getId().equals(user.getId());
    }

    public boolean isReply () {
        // Only replies have parent comments
        return parent != null;
    }

    public SerialisedReview makeSerialisedReview () {
        if (isReply()) {
            throw new RuntimeException("Tried to make review out of reply!");
        }

        return new SerialisedReview(id.toString(), author.getId().toString(), title, commentText, rating, makeReactions());
    }

    public SerialisedReply makeSerialisedReply () {
        if (!isReply()) {
            throw new RuntimeException("Tried to make reply out of review!");
        }
        return new SerialisedReply(id.toString(), author.getId().toString(), commentText, commentTime.format(DateTimeFormatter.ISO_INSTANT), makeReactions());
    }

    public Comment addReply (User replyAuthor, String replyText) {
        if (!event.canReply(replyAuthor)) {
            throw new ForbiddenException("You are not an admin or have a ticket!");
        }

        return Comment.makeReply(event, replyAuthor, this, replyText);
    }

    public Stream<Comment> getReplies () {
        return getChildren().stream();
    }

    public void react (ModelSession session, User user, String reactType) {
        if (isWrittenBy(user)) {
            throw new ForbiddenException("Cannot react own comment!");
        }

        var react = getReactions().stream()
                .filter(r -> r.isAuthor(user))
                .filter(r -> r.getReactType().equals(reactType))
                .findFirst()
                .orElse(null);

        if (react != null) {
            // React already exists, instead unreact
            session.remove(react);
        } else {
            var newReact = new Reaction(this, user, reactType);
            session.save(newReact);
            reactions.add(newReact);
        }
    }

    private List<SerialisedReaction> makeReactions () {
        return getReactions().stream()
                .collect(Collectors.groupingBy(Reaction::getReactType, Collectors.counting())) // Group reactions into type, amount pairs
                .entrySet()
                .stream()
                .map(e -> new SerialisedReaction(e.getKey(), Math.toIntExact(e.getValue())))
                .collect(Collectors.toList());

    }

    public static Comment makeReview (Event event, User author, String title, String text, float rating) {
        if (title == null || title.equals("") || text == null) {
            // Title cannot be null or empty, text can be empty but not null
            throw new BadRequestException("Invalid review title or text!");
        }

        if (rating < 0.0f || rating > 10.0f) {
            throw new ForbiddenException("Invalid review rating!");
        }

        return new Comment(event, author, null, title, text, rating);
    }

    public static Comment makeReply (Event event, User author, Comment parent, String text) {
        if (text == null || text.equals("")) {
            throw new BadRequestException("Invalid reply text!");
        }

        return new Comment(event, author, parent, null, text, null);
    }
}
