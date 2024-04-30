package tickr.application.apis.purchase;

import tickr.application.TickrController;
import tickr.persistence.ModelSession;

public interface IPurchaseAPI {
    IOrderBuilder makePurchaseBuilder (String orderId);
    String registerOrder (IOrderBuilder builder);

    void handleWebhookEvent (TickrController controller, ModelSession session, String requestBody, String sigHeader);
    String getSignatureHeader ();

    void refundItem (String refundId, long refundAmount);
}
