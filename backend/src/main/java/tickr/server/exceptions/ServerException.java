package tickr.server.exceptions;

import com.google.gson.annotations.SerializedName;

/**
 * Base class for all server exceptions used for error routes
 */
public class ServerException extends RuntimeException {
    private final int statusCode;
    private final String statusText;

    public ServerException (int statusCode, String statusText, String reason, Throwable cause) {
        super(reason, cause);
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    public ServerException (int statusCode, String statusText, String reason) {
        super(reason);
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    public int getStatusCode () {
        return statusCode;
    }

    public String getStatusString () {
        return statusCode + " " + statusText;
    }

    public Serialised getSerialised () {
        return new Serialised(statusCode, getMessage());
    }

    public static class Serialised {
        @SerializedName("status_code")
        public int statusCode;
        public String reason;

        private Serialised (int statusCode, String reason) {
            this.statusCode = statusCode;
            this.reason = reason;
        }
    }
}
