package tickr.application.serialised.responses.user;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ViewProfileResponse {
    @SerializedName("user_name")
    public String userName;

    @SerializedName("first_name")
    public String firstName;

    @SerializedName("last_name")
    public String lastName;

    @SerializedName("profile_picture")
    public String profilePicture;

    public String email;

    @SerializedName("profile_description")
    public String description;

    public List<String> events = new ArrayList<>();

    public ViewProfileResponse () {

    }

    public ViewProfileResponse (String userName, String firstName, String lastName, String profilePicture, String email, String description) {
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePicture = profilePicture;
        this.email = email;
        this.description = description;
    }
}
