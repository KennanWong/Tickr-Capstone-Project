package tickr.application.serialised.requests.user;

public class UserLoginRequest {
    public String email;
    public String password;

    public boolean isValid () {
        return email != null && password != null;
    }

    public UserLoginRequest () {

    }

    public UserLoginRequest (String email, String password) {
        this.email = email;
        this.password = password;
    }
}
