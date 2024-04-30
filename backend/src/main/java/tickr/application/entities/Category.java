package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {
    private static final List<String> VALID_CATEGORIES = List.of(
            "Food",
            "Music",
            "Travel & Outdoor",
            "Health",
            "Sport & Fitness",
            "Hobbies",
            "Business",
            "Free",
            "Tourism",
            "Education"
    );
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    //@Column(name = "event_id")
    //private int eventId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    private String category;

    public Category () {}

    public Category (Event event, String category) {
        this.event = event;
        this.category = category; 
    }

    public Event getEvent () {
        return event;
    }

    public String getCategory () {
        return category;
    }

    public static List<String> getValidCategories () {
        return VALID_CATEGORIES;
    }

    public static boolean validCategory (String category) {
        return VALID_CATEGORIES.contains(category);
    }
}
