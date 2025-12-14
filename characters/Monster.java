package characters;

import config.GameBalance;

public class Monster extends AbstractCharacter {

    private int damage;
    private int defense;
    private int dodgeChance; // percent 0-100

    public Monster(String name, int level, int damage, int defense, int dodgeChance) {
        super(name, level, GameBalance.monsterHpForLevel(level));
        this.damage = damage;
        this.defense = defense;
        this.dodgeChance = dodgeChance;
    }

    public int getDamage()      { return damage; }
    public int getDefense()     { return defense; }
    public int getDodgeChance() { return dodgeChance; }

    public void reduceDamage(int amount) {
        damage -= amount;
        if (damage < 0) damage = 0;
    }

    public void reduceDefense(int amount) {
        defense -= amount;
        if (defense < 0) defense = 0;
    }

    public void reduceDodgeChance(int amount) {
        dodgeChance -= amount;
        if (dodgeChance < 0) dodgeChance = 0;
    }

    /**
     * Spell damage bypasses defense/dodge in Valor rules.
     */
    public void takeSpellDamage(int amount) {
        int effective = Math.max(0, amount);
        this.hp -= effective;
        if (hp < 0) hp = 0;
    }

    public void takeDamage(int rawDamage) {
        int effective = rawDamage - defense;
        if (effective < 0) effective = 0;
        this.hp -= effective;
        if (hp < 0) hp = 0;
    }
}
