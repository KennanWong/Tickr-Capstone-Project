package tickr.application.serialised.requests.user;

import com.google.gson.annotations.SerializedName;

public class EditProfileRequest {
    @SerializedName("auth_token")
    public String authToken;
    @SerializedName("user_name")
    public String username;

    @SerializedName("first_name")
    public String firstName;
    @SerializedName("last_name")
    public String lastName;

    @SerializedName("profile_picture")
    public String pfpDataUrl;

    @SerializedName("email")
    public String email;
    @SerializedName("profile_description")
    public String profileDescription;

    public EditProfileRequest () {

    }

    public EditProfileRequest (String authToken, String username, String firstName, String lastName, String pfpDataUrl, String email, String profileDescription) {
        this.authToken = authToken;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.pfpDataUrl = pfpDataUrl;
        this.email = email;
        this.profileDescription = profileDescription;
    }
}
