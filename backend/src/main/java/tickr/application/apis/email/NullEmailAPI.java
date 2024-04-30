package tickr.application.apis.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NullEmailAPI implements IEmailAPI {
    static final Logger logger = LogManager.getLogger();
    @Override
    public void sendEmail (String toEmail, String subject, String body) {
        // Simply print out email to log
        logger.info("Email send request to {} with subject \"{}\" and body:\n{}", toEmail, subject, body);
    }
}
