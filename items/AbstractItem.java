package items;

public abstract class AbstractItem implements Item {
    protected String name;
    protected int price;
    protected int requiredLevel;

    protected AbstractItem(String name, int price, int requiredLevel) {
        this.name = name;
        this.price = price;
        this.requiredLevel = requiredLevel;
    }

    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getRequiredLevel() { return requiredLevel; }
}
