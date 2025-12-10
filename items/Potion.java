package items;

import characters.Hero;

public class Potion extends AbstractItem implements Consumable {

    public enum StatType {
        HP,
        MANA,
        STRENGTH,
        DEXTERITY,
        AGILITY,
        ALL
    }

    private StatType statType;
    private int amount;

    public Potion(String name, int price, int requiredLevel,
                  StatType statType, int amount) {
        super(name, price, requiredLevel);
        this.statType = statType;
        this.amount = amount;
    }

    public StatType getStatType() { return statType; }
    public int getAmount() { return amount; }

    @Override
    public void consume(Hero hero) {
        hero.applyPotion(this);
    }
}
