package tickr.application.apis.purchase;

import tickr.application.entities.PurchaseItem;

import java.util.UUID;

public class LineItem {
    private String itemName;
    private long price;
    private PurchaseItem item;

    public LineItem (String itemName, float price, PurchaseItem item) {
        this.itemName = itemName;
        this.price = (long)Math.floor(price * 100);
        this.item = item;
    }

    public String getItemName () {
        return itemName;
    }

    public long getPrice () {
        return price;
    }

    public PurchaseItem getPurchaseItem () {
        return item;
    }
}
