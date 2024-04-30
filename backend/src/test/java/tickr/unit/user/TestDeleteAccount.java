package tickr.unit.user;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.serialised.requests.user.UserDeleteRequest;
import tickr.application.serialised.requests.user.UserLoginRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;

public class TestDeleteAccount {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String authToken;
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();

        session = model.makeSession();
        authToken = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
    }

    @AfterAll
    public static void clearStaticFiles () {
        TestHelper.clearStaticFiles();
    }

    @Test
    public void testBadRequests () {
        assertThrows(BadRequestException.class, () -> controller.userDeleteAccount(session,
                new UserDeleteRequest(null, "Password123!")));
        assertThrows(UnauthorizedException.class, () -> controller.userDeleteAccount(session,
                new UserDeleteRequest(TestHelper.makeFakeJWT(), "Password123!")));

        assertThrows(ForbiddenException.class, () -> controller.userDeleteAccount(session,
                new UserDeleteRequest(authToken, "WrongPassword123!")));
        assertThrows(BadRequestException.class, () -> controller.userDeleteAccount(session,
                new UserDeleteRequest(authToken, null)));
    }

    @Test
    public void testAccountExists () {
        assertDoesNotThrow(() -> controller.userDeleteAccount(session, new UserDeleteRequest(authToken, "Password123!")));
        session = TestHelper.commitMakeSession(model, session);
    }

    @Test
    public void testCannotLoginDeleted () {
        controller.userDeleteAccount(session, new UserDeleteRequest(authToken, "Password123!"));
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.userLogin(session, new UserLoginRequest("test@example.com", "Password123!")));
    }

    @Test
    public void testCanRegisterAgain () {
        controller.userDeleteAccount(session, new UserDeleteRequest(authToken, "Password123!"));
        session = TestHelper.commitMakeSession(model, session);
        assertDoesNotThrow(() -> controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
        "Password123!", "2010-10-07")).authToken);
    }

    @Test
    public void testCannotDoubleDelete () {
        String oldAuthToken = authToken;
        assertDoesNotThrow(() -> controller.userDeleteAccount(session, new UserDeleteRequest(authToken, "Password123!")));
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(UnauthorizedException.class, () -> controller.userDeleteAccount(session,
                new UserDeleteRequest(oldAuthToken, "Password123!")));
    }
}
