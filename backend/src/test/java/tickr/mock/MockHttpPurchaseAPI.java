package tickr.mock;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Assertions;
import tickr.application.TickrController;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.util.HTTPHelper;

import java.util.Map;
import java.util.UUID;

public class MockHttpPurchaseAPI extends AbstractMockPurchaseAPI {
    private boolean received = false;
    private Gson gson;

    private String signature;

    private HTTPHelper httpHelper;

    public MockHttpPurchaseAPI (String hostUrl) {
        gson = new Gson();
        signature = UUID.randomUUID().toString();
        httpHelper = new HTTPHelper(hostUrl);
    }

    @Override
    public void handleWebhookEvent (TickrController controller, ModelSession session, String requestBody, String sigHeader) {
        received = true;

        if (!signature.equals(sigHeader)) {
            throw new BadRequestException("Invalid signature header!");
        }

        WebhookRequest request;
        try {
            request = gson.fromJson(requestBody, WebhookRequest.class);
        } catch (IllegalStateException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid webhook request!", e);
        }

        switch (request.type) {
            case "success":
                controller.ticketPurchaseSuccess(session, request.reserveId, request.paymentId);
                break;
            case "failure":
                controller.ticketPurchaseFailure(session, request.reserveId);
                break;
            case "cancel":
                controller.ticketPurchaseCancel(session, request.reserveId);
                break;
            default:
                throw new BadRequestException("Invalid webhook event type " + request.type + "!");
        }
    }

    @Override
    public String getSignatureHeader () {
        return "Mock-Signature";
    }

    @Override
    protected void onSuccess (Order order) {
        var response = httpHelper.post("/api/payment/webhook", new WebhookRequest("success", order.getReserveId(), order.getPayment().getId()),
                Map.of(getSignatureHeader(), signature), 1000);
        Assertions.assertEquals(200, response.getStatus());
    }

    @Override
    protected void onFailure (Order order) {
        var response = httpHelper.post("/api/payment/webhook", new WebhookRequest("failure", order.getReserveId(), order.getPayment().getId()),
                Map.of(getSignatureHeader(), signature), 1000);
        Assertions.assertEquals(200, response.getStatus());
    }

    @Override
    protected void onCancel (Order order) {
        var response = httpHelper.post("/api/payment/webhook", new WebhookRequest("cancel", order.getReserveId(), order.getPayment().getId()),
                Map.of(getSignatureHeader(), signature), 1000);
        Assertions.assertEquals(200, response.getStatus());
    }

    public boolean hasReceivedWebhook () {
        var result = received;
        received = false;
        return result;
    }

    private static class WebhookRequest {
        public String type;
        public String reserveId;
        public String paymentId;

        public WebhookRequest () {}

        public WebhookRequest (String type, String reserveId, String paymentId) {
            this.type = type;
            this.reserveId = reserveId;
            this.paymentId = paymentId;
        }
    }
}
