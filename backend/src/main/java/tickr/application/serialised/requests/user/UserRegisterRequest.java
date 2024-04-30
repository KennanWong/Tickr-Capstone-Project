package tickr.application.serialised.requests.user;

import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class UserRegisterRequest {
    @SerializedName("user_name")
    public String userName;

    @SerializedName("first_name")
    public String firstName;
    @SerializedName("last_name")
    public String lastName;
    @SerializedName("email")
    public String email;

    public String password;

    @SerializedName("date_of_birth")
    public String dateOfBirth;

    public boolean isValid () {
       return userName != null && !userName.isEmpty() && firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()
               && email != null && !email.isEmpty() && dateOfBirth != null && !dateOfBirth.isEmpty();
    }

    public UserRegisterRequest (String userName, String firstName, String lastName,  String email, String password, String dateOfBirth) {
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.password = password;
    }

    public UserRegisterRequest () {

    }
}
