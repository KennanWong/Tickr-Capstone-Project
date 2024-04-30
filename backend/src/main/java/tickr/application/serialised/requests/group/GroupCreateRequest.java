package tickr.application.serialised.requests.group;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;

import tickr.application.entities.TicketReservation;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.ForbiddenException;

public class GroupCreateRequest {
    @SerializedName("auth_token")
    public String authToken;

    @SerializedName("reserved_ids")
    public List<String> reservedIds;

    @SerializedName("host_reserve_id")
    public String hostReserveId;

    public GroupCreateRequest(String authToken, List<String> reservedIds, String hostReserveId) {
        this.authToken = authToken;
        this.reservedIds = reservedIds;
        this.hostReserveId = hostReserveId;
    } 

    public Set<TicketReservation> getTicketReservations (ModelSession session, TicketReservation reserve) {
        Set<TicketReservation> set = new HashSet<>();
        for (String id : reservedIds) {
            var reserveId = session.getById(TicketReservation.class, UUID.fromString(id))
                    .orElseThrow(() -> new ForbiddenException("Reserve ID does not exist!"));
            if (reserveId.equals(reserve)) {
                reserveId.setGroupAccepted(true);
                reserveId.setExpiry(ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofHours(24)));
            }
            set.add(reserveId);
        }
        return set;
    }
}
