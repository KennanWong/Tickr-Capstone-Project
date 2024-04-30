package tickr.server.exceptions;

public class BadRequestException extends ServerException {
    public BadRequestException (String reason) {
        super(400, "Bad Request", reason);
    }

    public BadRequestException (String reason, Throwable cause) {
        super(400, "Bad Request", reason, cause);
    }
}
