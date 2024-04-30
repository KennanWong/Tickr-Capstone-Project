package tickr.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Helper class representing a HTTP connection to a server
 */
public class HTTPHelper {
    static Logger logger = LogManager.getLogger();
    private HttpClient client;
    private Gson gson;
    private String serverUrl;
    public HTTPHelper (String serverUrl) {
        client = HttpClient.newHttpClient();
        gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        this.serverUrl = serverUrl;
    }

    /**
     * Sends a GET request to a route
     * @param route
     * @param params the query parameters to be sent
     * @return the response object for the request
     */
    public Response get (String route, Map<String, String> params) {
        // Make request
        var request = HttpRequest.newBuilder()
                .uri(URI.create(buildParamsUrl(serverUrl + route, params)))
                .GET()
                .build();

        logger.debug("Sending GET request {}", request.uri());

        // Send and wait for response
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new Response(r.statusCode(), r.body(), gson))
                .orTimeout(1000, TimeUnit.MILLISECONDS)
                .join();
    }
    public Response get (String route, Map<String, String> params, Map<String, String> headers, long timeoutMs) {
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(buildParamsUrl(serverUrl + route, params)))
                .GET();

        for (var i : headers.entrySet()) {
            requestBuilder.header(i.getKey(), i.getValue());
        }

        var request = requestBuilder.build();

        logger.debug("Sending GET request {}", request.uri());

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new Response(r.statusCode(), r.body(), gson))
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .join();
    }

    /**
     * Sends a GET request to a route
     * @param route
     * @return the response object for the request
     */
    public Response get (String route) {
        return get(route, Map.of());
    }

    /**
     * Sends a POST request to a route
     * @param route
     * @param body object representing the body of the request to be serialised
     * @return the response for the request
     */
    public <T> Response post (String route, T body) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + route))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        logger.debug("Sending POST request {}", request.uri());

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new Response(r.statusCode(), r.body(), gson))
                .orTimeout(1000, TimeUnit.MILLISECONDS)
                .join();
    }

    public <T> Response post (String route, T body, Map<String, String> headers, long timeout) {
        System.out.println(serverUrl + route);
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + route))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)));

        for (var i : headers.entrySet()) {
            requestBuilder.header(i.getKey(), i.getValue());
        }
        var request = requestBuilder.build();

        logger.debug("Sending POST request {}", request.uri());


        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new Response(r.statusCode(), r.body(), gson))
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .join();
    }

    /**
     * Sends a PUT request to a route
     * @param route
     * @param body object representing the body of the request to be serialised
     * @return the response for the request
     */
    public <T> Response put (String route, T body) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + route))
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        logger.debug("Sending PUT request {}", request.uri());

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new Response(r.statusCode(), r.body(), gson))
                .orTimeout(1000, TimeUnit.MILLISECONDS)
                .join();
    }

    /**
     * Sends a DELETE request to a route
     * @param route
     * @param body object representing the body of the request to be serialised
     * @return the response for the request
     */
    public <T> Response delete (String route, T body) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + route))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        logger.debug("Sending DELETE request {}", request.uri());

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new Response(r.statusCode(), r.body(), gson))
                .orTimeout(1000, TimeUnit.MILLISECONDS)
                .join();
    }

    private String buildParamsUrl (String routeURL, Map<String, String> params) {
        var builder = new StringBuilder(routeURL);
        boolean first = true;

        for (var i : params.entrySet()) {
            if (first) {
                // Add ? to url
                builder.append("?");
                first = false;
            } else {
                // Add & between params
                builder.append("&");
            }

            builder.append(URLEncoder.encode(i.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(i.getValue(), StandardCharsets.UTF_8));
        }

        return builder.toString();
    }

    /**
     * Represents a HTTP response to a request, encoding the status and
     * body of the request
     */
    public static class Response {
        private int statusCode;

        private String body;
        private Gson gson;

        public Response (int statusCode, String body, Gson gson) {
            this.statusCode = statusCode;
            this.body = body;
            this.gson = gson;
        }

        public int getStatus () {
            return statusCode;
        }

        public JsonElement getBody () {
            return gson.fromJson(body, JsonElement.class);
        }

        public <T> T getBody (Class<T> tClass) {
            return gson.fromJson(body, tClass);
        }

        public String getBodyRaw () {
            return body;
        }
    }
}
