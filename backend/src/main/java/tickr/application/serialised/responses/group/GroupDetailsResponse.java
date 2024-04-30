package tickr.application.serialised.responses.group;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GroupDetailsResponse {
    @SerializedName("host_id")
    public String hostId;

    // public List<Users> users; 

    @SerializedName("group_members")
    public List<GroupMember> groupMembers;

    @SerializedName("pending_invites")
    public List<PendingInvite> pendingInvites;

    @SerializedName("available_reserves")
    public List<String> availableReserves;

    @SerializedName("event_id")
    public String eventId;
        
    public GroupDetailsResponse(String hostId, List<GroupMember> groupMembers, List<PendingInvite> pendingInvites, List<String> availableReserves, String eventId) {
        this.hostId = hostId;
        this.groupMembers = groupMembers;
        this.pendingInvites = pendingInvites;
        this.availableReserves = availableReserves;
        this.eventId = eventId;
    }

    static public class GroupMember {
        public String email;

        public String section;

        @SerializedName("seat_number")
        public int seatNum;

        public boolean purchased;

        public GroupMember(String email, String section, int seatNum, boolean purchased) {
            this.email = email;
            this.section = section;
            this.seatNum = seatNum;
            this.purchased = purchased;
        }
    }

    static public class PendingInvite {
        public String email;

        public String section;

        @SerializedName("seat_number")
        public int seatNum;

        @SerializedName("invite_id")
        public String inviteId;

        public PendingInvite(String email, String section, int seatNum, String inviteId) {
            this.email = email;
            this.section = section;
            this.seatNum = seatNum;
            this.inviteId = inviteId;
        }

        
    }

    // static public class Users {
    //     @SerializedName("user_id")
    //     public String userId;

    //     public String email;

    //     public String section;

    //     @SerializedName("seat_number")
    //     public int seatNumber;

    //     public boolean accepted;

    //     public Users(String userId, String email, String section, int seatNumber, boolean accepted) {
    //         this.userId = userId;
    //         this.email = email;
    //         this.section = section;
    //         this.seatNumber = seatNumber;
    //         this.accepted = accepted;
    //     }

    //     public Users(String section, int seatNumber, boolean accepted) {
    //         this.section = section;
    //         this.seatNumber = seatNumber;
    //         this.accepted = accepted;
    //     }
    // }


}
