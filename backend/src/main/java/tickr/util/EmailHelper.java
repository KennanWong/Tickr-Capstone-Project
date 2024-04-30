package tickr.util;

import tickr.application.entities.Event;
import tickr.application.entities.User;

public class EmailHelper {
    public static void sendAnnouncement (User sender, User recipient, Event event, String announcement) {
        String profileLink = String.format("http://localhost:3000/view_profile/%s", sender.getId());
        String eventLink = String.format("http://localhost:3000/view_event/%s", event.getId());
        String subject = String.format("%s announcement", event.getEventName());
        String body = String.format("<a href=\"%s\">%s %s</a> made an announcement in <a href=\"%s\">%s</a>:<br>\"%s\"", profileLink,
                sender.getFirstName(), sender.getLastName(), eventLink, event.getEventName(), announcement);

        recipient.sendEmail(subject, body);
    }

    public static void sendEventChangeNotification (User sender, Event event, User recipient, String notification) {
        String profileLink = String.format("http://localhost:3000/view_profile/%s", sender.getId());
        String eventLink = String.format("http://localhost:3000/view_event/%s", event.getId());
        String subject = String.format("%s Changes", event.getEventName());
        String body = String.format("<a href=\"%s\">%s %s</a> made a change in <a href=\"%s\">%s</a>:<br>\"%s\"", profileLink,
                sender.getFirstName(), sender.getLastName(), event.getEventName(), eventLink, notification);    

        recipient.sendEmail(subject, body);
    }

    public static void sendEventCancellationNotification (User sender, Event event, User recipient) {
        String profileLink = String.format("http://localhost:3000/view_profile/%s", sender.getId());
        String eventLink = String.format("http://localhost:3000/view_event/%s", event.getId());
        String subject = String.format("%s Cancellation", event.getEventName());
        String body = String.format("<a href=\"%s\">%s %s</a> has cancelled <a href=\"%s\">%s</a>:<br>", profileLink,
                sender.getFirstName(), sender.getLastName(), event.getEventName(), eventLink);    

        recipient.sendEmail(subject, body);
    }
}
