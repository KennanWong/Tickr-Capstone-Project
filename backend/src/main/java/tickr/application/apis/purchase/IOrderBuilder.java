package tickr.application.apis.purchase;

public interface IOrderBuilder {
    IOrderBuilder withLineItem (LineItem lineItem);
    IOrderBuilder withUrls (String successUrl, String cancelUrl);
}
