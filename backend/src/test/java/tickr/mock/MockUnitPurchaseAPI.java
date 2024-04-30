package tickr.mock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.application.TickrController;
import tickr.persistence.DataModel;
import tickr.persistence.ModelSession;

public class MockUnitPurchaseAPI extends AbstractMockPurchaseAPI {
    private TickrController controller;
    private DataModel model;

    static final Logger logger = LogManager.getLogger();

    public MockUnitPurchaseAPI (TickrController controller, DataModel model) {
        super();
        this.controller = controller;
        this.model = model;
    }

    @Override
    protected void onSuccess (Order order) {
        var session = model.makeSession();
        controller.ticketPurchaseSuccess(session, order.getReserveId(), order.getPayment().getId());
        session.commit();
        session.close();
    }

    @Override
    protected void onFailure (Order order) {
        var session = model.makeSession();
        controller.ticketPurchaseFailure(session, order.getReserveId());
        session.commit();
        session.close();
    }

    @Override
    protected void onCancel (Order order) {
        var session = model.makeSession();
        controller.ticketPurchaseCancel(session, order.getReserveId());
        session.commit();
        session.close();
    }

    @Override
    public void handleWebhookEvent (TickrController controller, ModelSession session, String requestBody, String sigHeader) {
        logger.warn("Unit test PurchaseAPI received webhook event???");
    }

    @Override
    public String getSignatureHeader () {
        return null;
    }
}
