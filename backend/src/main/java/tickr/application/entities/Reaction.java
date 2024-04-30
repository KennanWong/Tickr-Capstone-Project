package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.server.exceptions.ForbiddenException;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "reactions")
public class Reaction {
    private static final Set<String> VALID_REACT_TYPES = Set.of("heart", "laugh", "cry", "angry", "thumbs_up", "thumbs_down");

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "react_time")
    private ZonedDateTime reactTime;

    @Column(name = "react_type")
    private String reactType;

    public Reaction () {}
    public Reaction (Comment comment, User author, String reactType) {
        if (!VALID_REACT_TYPES.contains(reactType)) {
            throw new ForbiddenException("Unknown react type \"" + reactType + "\"!");
        }

        this.comment = comment;
        this.author = author;
        this.reactTime = ZonedDateTime.now(ZoneId.of("UTC"));
        this.reactType = reactType;
    }

    private User getAuthor () {
        return author;
    }

    public String getReactType () {
        return reactType;
    }


    public boolean isAuthor (User user) {
        return author.getId().equals(user.getId());
    }
}
