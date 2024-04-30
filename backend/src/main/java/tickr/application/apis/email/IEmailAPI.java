package tickr.application.apis.email;

public interface IEmailAPI {
    void sendEmail (String toEmail, String subject, String body);
}
