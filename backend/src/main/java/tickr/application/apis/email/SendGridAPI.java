package tickr.application.apis.email;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.util.HTTPHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SendGridAPI implements IEmailAPI {
    private static final String API_KEY = "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";

    static final Logger logger = LogManager.getLogger();
    @Override
    public void sendEmail (String toEmail, String subject, String body) {
        if (toEmail.trim().toLowerCase().endsWith("@example.com")) {
            // Intercept example.com emails and dont send them
            logger.info("Intercepted email to example.com domain: \nto: {}, subject: \"{}\", body:\n{}", toEmail, subject, body);
            return;
        }

        // Create email request
        var request = new SendEmailRequest("tickr3900@gmail.com", toEmail, subject, body);

        var httpHelper = new HTTPHelper("https://api.sendgrid.com");
        logger.debug("Request string: {}", new GsonBuilder()
                .disableHtmlEscaping()
                .create()
                .toJson(request));

        // Send request
        var response = httpHelper.post("/v3/mail/send", request,
                Map.of("Authorization", "Bearer " + API_KEY, "Content-Type", "application/json"), 10000);

        if (response.getStatus() != 202) {
            logger.error("Failed to send email: error code {}, response: \n{}", response.getStatus(), response.getBodyRaw());
            throw new RuntimeException("Failed to send email!");
        }

        logger.info("Successfully sent email to {}!", toEmail);
    }

    private static class SendEmailRequest {
        private List<Personalisation> personalizations;
        private User from;
        private String subject;

        private List<Content> content;

        public SendEmailRequest () {}

        public SendEmailRequest (String fromEmail, String toEmail, String subject, String body) {
            personalizations = new ArrayList<>();
            personalizations.add(new Personalisation(List.of(toEmail)));

            from = new User(fromEmail);
            this.subject = subject;

            content = List.of(new Content("text/html", body));
        }

        private static class User {
            private String email;
            private String name;

            public User () {}

            public User (String email) {
                this.email = email;
                this.name = email;
            }
        }

        private static class Content {
            private String type;
            private String value;

            public Content () {}

            public Content (String type, String value) {
                this.type = type;
                this.value = value;
            }
        }

        private static class Personalisation {
            private List<User> to;

            public Personalisation () {}

            public Personalisation (List<String> toEmails) {
                to = toEmails.stream()
                        .map(User::new)
                        .collect(Collectors.toList());
            }
        }
    }

}
