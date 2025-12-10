package items;

import characters.Hero;

public class Armor extends AbstractItem implements Equippable {

    private int damageReduction;

    public Armor(String name, int price, int requiredLevel, int damageReduction) {
        super(name, price, requiredLevel);
        this.damageReduction = damageReduction;
    }

    public int getDamageReduction() {
        return damageReduction;
    }

    @Override
    public void equipTo(Hero hero) {
        hero.equipArmor(this);
    }
}
