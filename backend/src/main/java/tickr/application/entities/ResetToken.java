package tickr.application.entities;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;

@Entity
@Table (name = "reset_tokens")
public class ResetToken {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "expiry_time")
    private ZonedDateTime expiryTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public ResetToken(User user, Duration expiryDuration) {
        this.user = user;
        expiryTime = ZonedDateTime.now(ZoneId.of("UTC")).plus(expiryDuration);
    }

    public ResetToken() {
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }
    
}
