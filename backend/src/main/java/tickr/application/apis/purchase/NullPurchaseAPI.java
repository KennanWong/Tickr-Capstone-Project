package tickr.application.apis.purchase;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.application.TickrController;
import tickr.persistence.DataModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.util.HTTPHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NullPurchaseAPI implements IPurchaseAPI {
    static final Logger logger = LogManager.getLogger();

    private HTTPHelper httpHelper;
    private Gson gson;
    private String signature;

    public NullPurchaseAPI (String serverUrl) {
        this.httpHelper = new HTTPHelper(serverUrl);
        this.signature = UUID.randomUUID().toString();
        this.gson = new Gson();
    }

    @Override
    public IOrderBuilder makePurchaseBuilder (String orderId) {
        return new OrderBuilder(orderId);
    }

    @Override
    public String registerOrder (IOrderBuilder builder) {
        // Record registered order
        var orderBuilder = (OrderBuilder)builder;
        logger.info("Registered order {}: ", orderBuilder.getReserveId());
        for (var i : orderBuilder.getItems()) {
            logger.info("Line item: {}, price: {}", i.getItemName(), i.getPrice());
            i.getPurchaseItem().setPaymentDetails(orderBuilder.getReserveId());
        }
        logger.info("Fulfilling order and returning success url \"{}\"", orderBuilder.getSuccessUrl());

        // Waiting for a bit to ensure the session has ended and then sending webhook for payment success
        var thread = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {

            }
            var response = httpHelper.post("/api/payment/webhook", new WebhookRequest("success", orderBuilder.getReserveId()),
                    Map.of(getSignatureHeader(), signature), 1000);
            if (response.getStatus() != 200) {
                logger.warn("Null webhook returned status code {} with body: \n{}", response.getStatus(), response.getBodyRaw());
            }
        });

        thread.start();

        // Returning success url to redirect to
        return orderBuilder.getSuccessUrl();
    }

    @Override
    public void handleWebhookEvent (TickrController controller, ModelSession session, String requestBody, String sigHeader) {
        if (!signature.equals(sigHeader)) {
            throw new BadRequestException("Invalid signature header!");
        }

        WebhookRequest webhookEvent;

        try {
            webhookEvent = gson.fromJson(requestBody, WebhookRequest.class);
        } catch (IllegalStateException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid webhook request!", e);
        }

        if (webhookEvent.type.equals("success")) {
            controller.ticketPurchaseSuccess(session, webhookEvent.reserveId, webhookEvent.reserveId);
        } else {
            throw new BadRequestException("Invalid webhook event type: \"" + webhookEvent.type + "\"!");
        }
    }

    @Override
    public String getSignatureHeader () {
        return "Null-Header";
    }

    @Override
    public void refundItem (String refundId, long refundAmount) {
        // Print out refund request
        logger.info("Refunded payment from order {} for ${}!", refundId, (double)refundAmount / 100);
    }

    private static class OrderBuilder implements IOrderBuilder {
        private String reserveId;
        private List<LineItem> items;
        private String successUrl;

        public OrderBuilder (String reserveId) {
            this.reserveId = reserveId;
            items = new ArrayList<>();
        }

        @Override
        public IOrderBuilder withLineItem (LineItem lineItem) {
            items.add(lineItem);
            return this;
        }

        @Override
        public IOrderBuilder withUrls (String successUrl, String cancelUrl) {
            this.successUrl = successUrl;
            return this;
        }

        public String getReserveId () {
            return reserveId;
        }

        public List<LineItem> getItems () {
            return items;
        }

        public String getSuccessUrl () {
            return successUrl;
        }
    }

    private static class WebhookRequest {
        public String type;
        public String reserveId;

        public WebhookRequest () {}

        public WebhookRequest (String type, String reserveId) {
            this.type = type;
            this.reserveId = reserveId;
        }
    }
}
