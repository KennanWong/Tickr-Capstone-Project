package tickr.mock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.application.apis.email.IEmailAPI;

import java.util.ArrayList;
import java.util.List;

public class MockEmailAPI implements IEmailAPI {
    static final Logger logger = LogManager.getLogger();
    private List<Message> messages;

    public MockEmailAPI () {
        messages = new ArrayList<>();
    }


    @Override
    public void sendEmail (String toEmail, String subject, String body) {
        // TODO
        //logger.info("Email api received send request: \nto: {}, subject: \"{}\", message:\n{}", toEmail, subject, body);
        messages.add(new Message(toEmail, subject, body));
    }

    public List<Message> getSentMessages () {
        return messages;
    }

    public static class Message {
        private String toEmail;
        private String subject;
        private String body;

        public Message (String toEmail, String subject, String body) {
            this.toEmail = toEmail;
            this.subject = subject;
            this.body = body;
        }

        public String getToEmail () {
            return toEmail;
        }

        public String getSubject () {
            return subject;
        }

        public String getBody () {
            return body;
        }
    }
}
