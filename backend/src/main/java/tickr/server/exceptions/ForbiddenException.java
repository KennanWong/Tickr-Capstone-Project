package tickr.server.exceptions;

public class ForbiddenException extends ServerException {

    public ForbiddenException (String reason, Throwable cause) {
        super(403, "Forbidden", reason, cause);
    }

    public ForbiddenException (String reason) {
        super(403, "Forbidden", reason);
    }
}
