package tickr.server.exceptions;

public class UnauthorizedException extends ServerException {

    public UnauthorizedException (String reason, Throwable cause) {
        super(401, "Unauthorized", reason, cause);
    }

    public UnauthorizedException (String reason) {
        super(401, "Unauthorized", reason);
    }
}
