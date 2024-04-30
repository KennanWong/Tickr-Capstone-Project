package tickr.application.apis.purchase;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.application.TickrController;
import tickr.application.entities.PurchaseItem;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.util.HTTPHelper;
import tickr.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StripeAPI implements IPurchaseAPI {
    static final Logger logger = LogManager.getLogger();

    private static final String API_KEY = "sk_test_51Ltt1uArvJ5MXKVUcYk5wKKUQwqFAsCq0zkmlnI96rB2CRdqtAWqS4EdckBPLLXMaJ7eoYyDEybFrkAlbPc6CXLw00chnKSWQn";

    private final String webhookSecret;

    public StripeAPI (String webhookSecret) {
        Stripe.apiKey = API_KEY;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public IOrderBuilder makePurchaseBuilder (String orderId) {
        return new PaymentSessionBuilder(orderId);
    }

    @Override
    public String registerOrder (IOrderBuilder builder) {
        logger.info("Registering a stripe order!");
        var paymentBuilder = (PaymentSessionBuilder)builder;

        if (paymentBuilder.getOrderPrice() == 0) {
            // Stripe doesn't handle free orders, do it ourselves
            return registerFreeOrder(paymentBuilder);
        }

        var session = paymentBuilder.build();

        var url = session.getUrl();
        logger.info("Created stripe session with url {}!", url);

        return url;
    }

    private String registerFreeOrder (PaymentSessionBuilder builder) {
        logger.info("Registering a free order, bypassing the Stripe API");
        new Thread(() -> {
            // Waiting a bit for request to end
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {}

            var httpHelper = new HTTPHelper("http://localhost:8080");

            // Sending free webhook
            var response = httpHelper.post("/api/payment/webhook", builder.buildFreeRequest(), Map.of(
                    getSignatureHeader(), "FREE"
            ), 1000);
            if (response.getStatus() != 200) {
                logger.error("Webhook for free event failed with status {} and body\n{}", response.getStatus(), response.getBodyRaw());
            }
        }).start();

        return builder.successUrl;
    }

    @Override
    public void handleWebhookEvent (TickrController controller, ModelSession session, String requestBody, String sigHeader) {
        logger.debug("Webhook event: \n{}", requestBody);
        if ("FREE".equals(sigHeader)) {
            // Free request, complete free payment
            completeFreePayment(controller, session, requestBody);
            return;
        }
        Event event;
        try {
            event = Webhook.constructEvent(requestBody, sigHeader, webhookSecret);
        } catch (JsonSyntaxException e) {
            throw new BadRequestException("Invalid payload!");
        } catch (SignatureVerificationException e) {
            throw new BadRequestException("Invalid signature!");
        }

        logger.info("Received Stripe webhook event \"{}\"!", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            // Payment success, complete payment
            event.getDataObjectDeserializer()
                    .getObject()
                    .ifPresent(s -> completePayment(controller, session, (Session) s));
        }
    }

    private void completeFreePayment (TickrController controller, ModelSession session, String requestBody) {
        var orderId = new Gson().fromJson(requestBody, FreeEventRequest.class).orderId;

        controller.ticketPurchaseSuccess(session, orderId, null);
    }

    private void completePayment (TickrController controller, ModelSession session, Session stripeSession) {
        // Get order id from metadata
        var metadata = stripeSession.getMetadata();
        if (!metadata.containsKey("reserve_id")) {
            throw new RuntimeException("Invalid metadata!");
        }

        var orderId = metadata.get("reserve_id");

        // Get payment intent from stripe session for refunding later
        var paymentIntent = stripeSession.getPaymentIntent();

        controller.ticketPurchaseSuccess(session, orderId, paymentIntent);
    }

    @Override
    public String getSignatureHeader () {
        return "Stripe-Signature";
    }

    @Override
    public void refundItem (String refundId, long refundAmount) {
        try {
            // Create refund from PaymentIntent and refund amount
            Refund.create(RefundCreateParams.builder()
                    .setPaymentIntent(refundId)
                    .setAmount(refundAmount)
                    .build());
        } catch (StripeException e) {
            throw new RuntimeException("Refund failed!", e);
        }
    }

    private static class PaymentSessionBuilder implements IOrderBuilder {
        SessionCreateParams.Builder paramsBuilder;
        String orderId;

        private List<PurchaseItem> purchaseItems;
        private long orderPrice = 0;
        private String successUrl;

        public PaymentSessionBuilder (String orderId) {
            logger.debug("Created builder!");
            paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT);
            this.orderId = orderId;
            purchaseItems = new ArrayList<>();
            successUrl = null;
        }

        @Override
        public IOrderBuilder withLineItem (LineItem lineItem) {
            // Add stripe line item, with currency in USD
            logger.debug("Added line item!");
            paramsBuilder = paramsBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(lineItem.getPrice())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(lineItem.getItemName())
                                    .build())
                            .build())
                    .build());
            purchaseItems.add(lineItem.getPurchaseItem());
            orderPrice += lineItem.getPrice();
            return this;
        }

        @Override
        public IOrderBuilder withUrls (String successUrl, String cancelUrl) {
            // Add urls. Wrap cancel url to go through backend first so that we can remove reservations
            // before handing back to the frontend
            cancelUrl = String.format("http://localhost:8080/api/payment/cancel?url=%s&order_id=%s", cancelUrl, orderId);
            logger.debug("Setting redirect urls: {}, {}!", successUrl, cancelUrl);
            paramsBuilder = paramsBuilder.setSuccessUrl(successUrl).setCancelUrl(cancelUrl);
            this.successUrl = successUrl;
            return this;
        }

        public Session build () {
            logger.debug("Building Stripe session!");
            try {
                var params = paramsBuilder.putMetadata("reserve_id", orderId).build();
                return Session.create(params);
            } catch (StripeException e) {
                throw new RuntimeException("Stripe exception while making checkout session!", e);
            }
        }

        public FreeEventRequest buildFreeRequest () {
            return new FreeEventRequest(orderId);
        }

        public long getOrderPrice () {
            return orderPrice;
        }
    }

    private static class FreeEventRequest {
        @SerializedName("order_id")
        public String orderId;

        public FreeEventRequest () {}

        public FreeEventRequest (String orderId) {
            this.orderId = orderId;
        }
    }
}
