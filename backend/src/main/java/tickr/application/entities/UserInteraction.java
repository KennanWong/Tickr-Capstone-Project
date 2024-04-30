package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.application.recommendations.EventVector;
import tickr.application.recommendations.InteractionType;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_interaction")
public class UserInteraction {
    private static final double RELEVANCE_HALF_LIFE_MS = 7 * 24 * 60 * 60 * 1000;
    private static final double RELEVANCE_DECAY_FACTOR = Math.log(0.5) / RELEVANCE_HALF_LIFE_MS;

    private static final double MAX_RATING = 5.0;

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "interact_time")
    private ZonedDateTime interactTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "interact_type")
    private InteractionType interactType;

    @Column(name = "rating")
    private Double rating;

    public UserInteraction () {}

    public UserInteraction (User user, Event event, InteractionType interactType, Double rating) {
        this.user = user;
        this.event = event;
        this.interactType = interactType;
        this.interactTime = ZonedDateTime.now(ZoneId.of("UTC"));
        this.rating = rating;
    }

    public EventVector getVector (int numEvents) {
        return event.getEventVector(numEvents).multiply(getWeight() * getRelevancy());
    }

    private double getRelevancy () {
        long msSinceInteraction = Duration.between(interactTime, ZonedDateTime.now(ZoneId.of("UTC"))).toMillis();

        // Exponentially scale relevancy down as time increases
        return Math.exp(-RELEVANCE_DECAY_FACTOR * msSinceInteraction);
    }

    private double getWeight () {
        if (interactType == InteractionType.REVIEW && rating != null) {
            // Weight according to rating
            return interactType.getWeight() * (rating - MAX_RATING / 2);
        }

        return interactType.getWeight();
    }
}
