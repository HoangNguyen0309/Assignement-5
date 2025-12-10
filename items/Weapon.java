package items;

import characters.Hero;
import config.GameBalance;

public class Weapon extends AbstractItem implements Equippable {

    private int damage;
    private int handsRequired; // 1 or 2

    public Weapon(String name, int price, int requiredLevel,
                  int damage, int handsRequired) {
        super(name, price, requiredLevel);
        this.damage = damage;
        this.handsRequired = handsRequired;
    }

    public int getDamage() {
        return damage;
    }

    public int getHandsRequired() {
        return handsRequired;
    }

    public int getEffectiveDamage(boolean usingTwoHands) {
        if (handsRequired == 2) {
            return damage;
        }
        if (usingTwoHands) {
            return (int) Math.round(damage * GameBalance.TWO_HAND_BONUS_MULTIPLIER);
        }
        return damage;
    }

    @Override
    public void equipTo(Hero hero) {
        // default to one-hand (GameEngine asks player about 1/2 hands)
        hero.equipWeapon(this, false);
    }
}
