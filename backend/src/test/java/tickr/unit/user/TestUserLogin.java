package tickr.unit.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.entities.AuthToken;
import tickr.application.serialised.requests.user.UserLoginRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.util.CryptoHelper;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class TestUserLogin {
    private DataModel model;
    private TickrController controller;
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
    }

    @Test
    public void testBadValues () {
        var session = model.makeSession();
        assertThrows(BadRequestException.class, () -> controller.userLogin(session, new UserLoginRequest(null, null)));
    }

    @Test
    public void testNoUsers () {
        var session = model.makeSession();
        assertThrows(ForbiddenException.class, () -> controller.userLogin(session, new UserLoginRequest("test@example.com", "Password123!")));
    }

    @Test
    public void testIncorrectEmail () {
        var session = model.makeSession();
        controller.userRegister(session, new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14"));
        session = TestHelper.commitMakeSession(model, session);
        var finalSession = session;
        assertThrows(ForbiddenException.class, () -> controller.userLogin(finalSession, new UserLoginRequest("test2@example.com", "Password123!")));
    }

    @Test
    public void testIncorrectPassword () {
        var session = model.makeSession();
        controller.userRegister(session, new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14"));
        session = TestHelper.commitMakeSession(model, session);
        var finalSession = session;
        assertThrows(ForbiddenException.class, () -> controller.userLogin(finalSession, new UserLoginRequest("test@example.com", "Password1234!")));
    }

    @Test
    public void testSuccessfulLogin () {
        var session = model.makeSession();
        var tokenStr1 = controller.userRegister(session, new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var beforeDate = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS);
        var tokenStr2 = controller.userLogin(session, new UserLoginRequest("test@example.com", "Password123!")).authToken;
        var afterDate = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofSeconds(1)).truncatedTo(ChronoUnit.SECONDS);
        session = TestHelper.commitMakeSession(model, session);

        assertNotEquals(tokenStr1, tokenStr2);

        var token1 = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(tokenStr1);
        var token2 = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(tokenStr2);

        var token2Date = ZonedDateTime.ofInstant(token2.getBody().getIssuedAt().toInstant(), ZoneId.of("UTC"));

        assertEquals(token1.getBody().getSubject(), token2.getBody().getSubject());
        assertNotEquals(token1.getBody().getId(), token2.getBody().getId());
        assertTrue(beforeDate.compareTo(token2Date) <= 0);
        assertTrue(token2Date.compareTo(afterDate) <= 0);
    }

    @Test
    public void testCorrectUser () {
        var session = model.makeSession();

        var user1RegStr = controller.userRegister(session, new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var user2RegStr = controller.userRegister(session, new UserRegisterRequest("test", "first", "last",
                "test2@example.com", "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var loginStr = controller.userLogin(session, new UserLoginRequest("test@example.com", "Password123!")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        assertNotEquals(user1RegStr, loginStr);
        assertNotEquals(user2RegStr, loginStr);

        var user1Reg = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(user1RegStr);
        var user2Reg = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(user2RegStr);
        var login = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(loginStr);

        assertEquals(user1Reg.getBody().getSubject(), login.getBody().getSubject());
        assertNotEquals(user2Reg.getBody().getSubject(), login.getBody().getSubject());
    }

    @Test
    public void testDoubleLogin () {
        var session = model.makeSession();
        var tokenStr1 = controller.userRegister(session, new UserRegisterRequest("test", "first", "last",
                "test@example.com", "Password123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var tokenStr2 = controller.userLogin(session, new UserLoginRequest("test@example.com", "Password123!")).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var tokenStr3 = controller.userLogin(session, new UserLoginRequest("test@example.com", "Password123!")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        assertNotEquals(tokenStr1, tokenStr2);
        assertNotEquals(tokenStr1, tokenStr3);
        assertNotEquals(tokenStr2, tokenStr3);

        var token1 = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(tokenStr1);
        var token2 = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(tokenStr2);
        var token3 = CryptoHelper.makeJWTParserBuilder().build().parseClaimsJws(tokenStr3);
        assertNotEquals(token1.getBody().getId(), token2.getBody().getId());
        assertNotEquals(token1.getBody().getId(), token3.getBody().getId());
        assertNotEquals(token2.getBody().getId(), token3.getBody().getId());

        assertTrue(session.getById(AuthToken.class, UUID.fromString(token1.getBody().getId())).isPresent());
        assertTrue(session.getById(AuthToken.class, UUID.fromString(token2.getBody().getId())).isPresent());
        assertTrue(session.getById(AuthToken.class, UUID.fromString(token3.getBody().getId())).isPresent());
    }
}
