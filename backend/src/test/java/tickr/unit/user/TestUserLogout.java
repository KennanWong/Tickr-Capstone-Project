package tickr.unit.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.serialised.requests.user.UserLoginRequest;
import tickr.application.serialised.requests.user.UserLogoutRequest;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.UnauthorizedException;

import java.util.Map;

public class TestUserLogout {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        session = model.makeSession();
    }

    @AfterEach
    public void cleanup () {
        session.rollback();
        session.close();
        model.cleanup();
    }

    @Test
    public void testBadRequests () {
        var session = model.makeSession();
        assertThrows(UnauthorizedException.class, () -> controller.userLogout(session, new UserLogoutRequest(null)));
        assertThrows(UnauthorizedException.class, () -> controller.userLogout(session, new UserLogoutRequest("testing123")));
        assertThrows(UnauthorizedException.class, () -> controller.userLogout(session, new UserLogoutRequest(TestHelper.makeFakeJWT())));
    }

    @Test
    public void testLogout () {
        var authToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);

        assertDoesNotThrow(() -> controller.userGetSettings(session, Map.of("auth_token", authToken)));

        controller.userLogout(session, new UserLogoutRequest(authToken));
        session = TestHelper.commitMakeSession(model, session);

        assertThrows(UnauthorizedException.class, () -> controller.userGetSettings(session, Map.of("auth_token", authToken)));
    }

    @Test
    public void testDoubleLogout () {
        var authToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);

        controller.userLogout(session, new UserLogoutRequest(authToken));
        session = TestHelper.commitMakeSession(model, session);

        assertThrows(UnauthorizedException.class, () -> controller.userLogout(session, new UserLogoutRequest(authToken)));
    }

    @Test
    public void testTwoAuthTokens () {
        var regReq = TestHelper.makeRegisterRequest();
        var authToken1 = controller.userRegister(session, regReq).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var authToken2 = controller.userLogin(session, new UserLoginRequest(regReq.email, regReq.password)).authToken;
        session = TestHelper.commitMakeSession(model, session);

        controller.userLogout(session, new UserLogoutRequest(authToken1));

        assertDoesNotThrow(() -> controller.userGetSettings(session, Map.of("auth_token", authToken2)));

        controller.userLogout(session, new UserLogoutRequest(authToken2));

        assertThrows(UnauthorizedException.class, () -> controller.userGetSettings(session, Map.of("auth_token", authToken2)));
    }
}
