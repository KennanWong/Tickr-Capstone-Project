package tickr.mock;

import tickr.application.apis.purchase.IOrderBuilder;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.apis.purchase.LineItem;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractMockPurchaseAPI implements IPurchaseAPI {

    private final Map<String, Customer> customers;
    private final Map<String, Order> orders;

    private final Map<String, Payment> payments;

    public AbstractMockPurchaseAPI () {
        customers = new HashMap<>();
        orders = new HashMap<>();
        payments = new HashMap<>();
    }

    public void addCustomer (String customerId, float defaultBalance) {
        customers.put(customerId, new Customer(customerId, defaultBalance));
    }

    public Customer getCustomer (String customerId) {
        return customers.get(customerId);
    }

    public boolean isUrlValid (String orderUrl) {
        return orders.containsKey(orderUrl);
    }

    public String fulfillOrder (String orderUrl, String customerId) {
        var order = getOrder(orderUrl);
        orders.remove(orderUrl);

        var customer = getCustomer(customerId);

        if (!customer.deductBalance(order.getPrice())) {
            onFailure(order);
            return order.getCancelUrl();
        } else {
            onSuccess(order);

            var payment = order.getPayment();

            payments.put(payment.getId(), payment);
            payment.setCustomer(customer);

            return order.getSuccessUrl();
        }
    }

    public String cancelOrder (String orderUrl) {
        var order = getOrder(orderUrl);
        onCancel(order);
        orders.remove(orderUrl);
        return order.getCancelUrl();
    }

    public Order getOrder (String orderUrl) {
        return orders.get(orderUrl);
    }

    protected void addOrder (Order order) {
        orders.put(order.getOrderId(), order);
    }

    @Override
    public IOrderBuilder makePurchaseBuilder (String orderId) {
        return new OrderBuilder(orderId);
    }

    @Override
    public String registerOrder (IOrderBuilder builder) {
        var order = ((OrderBuilder)builder).build();
        orders.put(order.getOrderId(), order);

        for (var i : ((OrderBuilder)builder).getLineItems()) {
            i.getPurchaseItem().setPaymentDetails(order.getPayment().getId());
        }

        return order.getOrderId();
    }

    @Override
    public void refundItem (String refundId, long refundAmount) {
        if (!payments.containsKey(refundId)) {
            throw new RuntimeException("Invalid refund id!");
        }

        payments.get(refundId).refund(refundAmount);
    }

    abstract protected void onSuccess (Order order);
    abstract protected void onFailure (Order order);
    abstract protected void onCancel (Order order);

    public static class Customer {
        private float balance;
        private final String customerId;
        public Customer (String customerId, float balance) {
            this.customerId = customerId;
            this.balance = balance;
        }

        public float getBalance () {
            return balance;
        }

        public String getCustomerId () {
            return customerId;
        }

        public boolean deductBalance (float amount) {
            assert amount >= 0;
            if (balance < amount) {
                return false;
            }

            balance -= amount;

            return true;
        }

        public void deposit (float amount) {
            assert amount >= 0;

            balance += amount;
        }
    }

    public static class Order {
        private final List<OrderItem> items;
        private String orderId;
        private String reserveId;
        private String successUrl;
        private String cancelUrl;

        private Payment payment;

        public Order (String orderId, String reserveId, List<LineItem> items, String successUrl, String cancelUrl) {
            this.orderId = orderId;
            this.reserveId = reserveId;
            this.items = items.stream().map(OrderItem::new).collect(Collectors.toList());
            this.successUrl = successUrl;
            this.cancelUrl = cancelUrl;
            this.payment = new Payment(items.stream()
                    .map(LineItem::getPrice)
                    .reduce(0L, Long::sum));
        }

        public float getPrice () {
            return (float)items.stream()
                    .map(OrderItem::getPrice)
                    .reduce(0L, Long::sum) / 100;
        }

        public String getOrderId () {
            return orderId;
        }

        public String getSuccessUrl () {
            return successUrl;
        }

        public String getCancelUrl () {
            return cancelUrl;
        }

        public String getReserveId () {
            return reserveId;
        }

        public Payment getPayment () {
            return payment;
        }
    }

    protected static class OrderItem {
        private String itemName;
        private long price;

        public OrderItem (LineItem item) {
            this.itemName = item.getItemName();
            this.price = item.getPrice();
        }

        public String getItemName () {
            return itemName;
        }

        public long getPrice () {
            return price;
        }
    }

    protected static class OrderBuilder implements IOrderBuilder {
        private List<LineItem> items;
        private String reserveId;
        private String successUrl;
        private String cancelUrl;

        public OrderBuilder (String reserveId) {
            items = new ArrayList<>();
            this.reserveId = reserveId;
        }

        @Override
        public IOrderBuilder withLineItem (LineItem lineItem) {
            items.add(lineItem);
            return this;
        }

        @Override
        public IOrderBuilder withUrls (String successUrl, String cancelUrl) {
            this.successUrl = successUrl;
            this.cancelUrl = cancelUrl;
            return this;
        }

        public Order build () {
            return new Order("test://example.com/test/" + UUID.randomUUID(), reserveId, items, successUrl, cancelUrl);
        }

        public List<LineItem> getLineItems () {
            return items;
        }
    }

    protected static class Payment {
        private String id;
        private long amountRemaining;
        private Customer customer;

        public Payment (long amountRemaining) {
            this.id  = UUID.randomUUID().toString();
            this.amountRemaining = amountRemaining;
        }

        public void refund (long amount) {
            if (amount > amountRemaining) {
                throw new RuntimeException("Attempted to refund over payment amount!");
            }

            if (customer == null) {
                throw new RuntimeException("Attempted to refund unpaid payment!");
            }

            amountRemaining -= amount;

            customer.deposit(amount / 100.0f);
        }

        public void setCustomer (Customer customer) {
            this.customer = customer;
        }

        public String getId () {
            if (amountRemaining != 0) {
                return id;
            } else {
                return null;
            }
        }
    }
}
