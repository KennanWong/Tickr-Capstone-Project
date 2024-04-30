package tickr.application.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.util.CryptoHelper;

import java.sql.Date;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "auth_token")
public class AuthToken {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "issue_time")
    private ZonedDateTime issueTime;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "expiry_time")
    private ZonedDateTime expiryTime;

    public AuthToken () {

    }

    public AuthToken (User user, ZonedDateTime issueTime, Duration expiryDuration) {
        this.user = user;
        this.issueTime = issueTime.truncatedTo(ChronoUnit.SECONDS);
        this.expiryTime = issueTime.plus(expiryDuration).truncatedTo(ChronoUnit.SECONDS);
    }

    private UUID getId () {
        return id;
    }

    public User getUser () {
        return user;
    }

    private ZonedDateTime getIssueTime () {
        return issueTime;
    }

    private ZonedDateTime getExpiryTime () {
        return expiryTime;
    }

    public String makeJWT () {
        return CryptoHelper.makeJWTBuilder()
                .setSubject(getUser().getId().toString()) // Subject is user id
                .setId(getId().toString()) // Id is auth token id
                .setIssuedAt(Date.from(getIssueTime().toInstant()))
                .setExpiration(Date.from(getExpiryTime().toInstant()))
                .compact();
    }
}
