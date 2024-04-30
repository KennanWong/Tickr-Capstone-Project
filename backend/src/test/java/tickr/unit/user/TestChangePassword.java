package tickr.unit.user;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.entities.ResetToken;
import tickr.application.entities.User;
import tickr.application.serialised.requests.user.UserChangePasswordRequest;
import tickr.application.serialised.requests.user.UserCompleteChangePasswordRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.requests.user.UserRequestChangePasswordRequest;
import tickr.mock.MockEmailAPI;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.UnauthorizedException;

import java.util.regex.Pattern;


public class TestChangePassword {
    private DataModel model;
    private TickrController controller;

    private MockEmailAPI emailAPI;

    private ModelSession session;
    private String authToken;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        emailAPI = new MockEmailAPI();
        ApiLocator.addLocator(IEmailAPI.class, () -> emailAPI);

        session = model.makeSession();
        authToken = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(IEmailAPI.class);
    }

    @Test
    public void testBadRequest() {
        assertThrows(BadRequestException.class, () -> controller.loggedChangePassword(session,
                new UserChangePasswordRequest("Password123!", "NewPassword123!", null)));
        assertThrows(UnauthorizedException.class, () -> controller.loggedChangePassword(session,
                new UserChangePasswordRequest("Password123!", "NewPassword123!", TestHelper.makeFakeJWT())));
        assertThrows(BadRequestException.class, () -> controller.loggedChangePassword(session,
                new UserChangePasswordRequest("Password123!", null, authToken)));
        assertThrows(BadRequestException.class, () -> controller.loggedChangePassword(session,
                new UserChangePasswordRequest(null, "Password123!", authToken)));
   }

    @Test
    public void testResetToken () {
        var session = model.makeSession();
        controller.unloggedChangePassword(session, new UserRequestChangePasswordRequest("test@example.com"));
        session = TestHelper.commitMakeSession(model, session);

        assertEquals(1, emailAPI.getSentMessages().size());
        var message = emailAPI.getSentMessages().get(0);

        assertEquals("test@example.com", message.getToEmail());
        assertNotNull(message.getSubject());

        var pattern = Pattern.compile("<a href=\"http://localhost:3000/change_password/(.*)/(.*)\">.*</a>");
        var matcher = pattern.matcher(message.getBody());
        assertTrue(matcher.find());
        assertEquals("test@example.com", matcher.group(1));


        var user = session.getByUnique(User.class, "email", "test@example.com").orElse(null);
        var list = session.getAllWith(ResetToken.class, "user", user);
        assertEquals(1, list.size());
        assertEquals(list.get(0).getId().toString(), matcher.group(2));
        
        session = TestHelper.commitMakeSession(model, session);
        var resetToken = session;
        assertDoesNotThrow(() -> controller.unloggedComplete(resetToken,
                new UserCompleteChangePasswordRequest("test@example.com", "NewPassword123!", list.get(0).toString())));
        assertThrows(BadRequestException.class, () -> controller.unloggedComplete(resetToken,
                new UserCompleteChangePasswordRequest("test@example.com", "NewPassword123!", null)));
    }
}
