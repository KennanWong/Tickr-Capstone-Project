package tickr.application.serialised.responses.user;
 
public class RequestChangePasswordResponse {
    public Boolean success;
   
    public RequestChangePasswordResponse () {
 
    }
 
    public RequestChangePasswordResponse (Boolean success) {
        this.success = success;
    }
}
