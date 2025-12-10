package characters;

import java.util.Random;

import config.GameBalance;
import items.Inventory;
import items.Armor;
import items.Potion;
import items.Weapon;
import items.Potion.StatType;

public class Hero extends AbstractCharacter {

    private int mana;
    private int maxMana;
    private int strength;
    private int dexterity;
    private int agility;
    private long experience;
    private int gold;

    private HeroType type; // WARRIOR, SORCERER, PALADIN

    private Weapon equippedWeapon;
    private Armor equippedArmor;
    private boolean usingWeaponTwoHanded;

    private Inventory inventory; // assume you have this class

    public Hero(String name, HeroType type, int level,
                int maxHp, int mana, int strength, int dexterity, int agility,
                int gold) {
        super(name, level, maxHp);
        this.type = type;
        this.mana = mana;
        this.maxMana = mana;
        this.strength = strength;
        this.dexterity = dexterity;
        this.agility = agility;
        this.experience = 0;
        this.gold = gold;
        this.inventory = new Inventory();
        applyInitialClassBonus();
    }

    private void applyInitialClassBonus() {
        int bonus = GameBalance.HERO_INITIAL_FAVORED_BONUS;
        switch (type) {
            case WARRIOR:
                strength += bonus;
                agility  += bonus;
                break;
            case SORCERER:
                dexterity += bonus;
                agility   += bonus;
                break;
            case PALADIN:
                strength  += bonus;
                dexterity += bonus;
                break;
            default:
                break;
        }
    }

    private long expRequiredForLevel(int targetLevel) {
        return GameBalance.xpRequiredForLevel(targetLevel);
    }

    public void gainExperience(long xp) {
        experience += xp;
        while (experience >= expRequiredForLevel(level + 1)) {
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        maxHp += GameBalance. HERO_LEVELUP_HP_BONUS;
        hp = maxHp;
        maxMana += GameBalance.HERO_LEVELUP_MANA_BONUS;
        mana = maxMana;

        strength  += GameBalance.HERO_LEVELUP_STAT_BONUS;
        dexterity += GameBalance.HERO_LEVELUP_STAT_BONUS;
        agility   += GameBalance.HERO_LEVELUP_STAT_BONUS;

        switch (type) {
            case WARRIOR:
                strength  += GameBalance.HERO_LEVELUP_FAVORED_BONUS;
                agility   += GameBalance.HERO_LEVELUP_FAVORED_BONUS;
                break;
            case SORCERER:
                dexterity += GameBalance.HERO_LEVELUP_FAVORED_BONUS;
                agility   += GameBalance.HERO_LEVELUP_FAVORED_BONUS;
                break;
            case PALADIN:
                strength  += GameBalance.HERO_LEVELUP_FAVORED_BONUS;
                dexterity += GameBalance.HERO_LEVELUP_FAVORED_BONUS;
                break;
            default:
                break;
        }
    }

    public int basicAttackDamage() {
        int weaponDamage = 0;
        if (equippedWeapon != null) {
            weaponDamage = equippedWeapon.getEffectiveDamage(usingWeaponTwoHanded);
        }
        return (int)(strength * GameBalance.HERO_ATTACK_STRENGTH_FACTOR + weaponDamage);
    }

    public boolean tryDodge() {
        Random r = new Random();
        int roll = r.nextInt(100);
        int chance = (int)(agility / GameBalance.HERO_DODGE_AGILITY_DIVISOR);
        if (chance > 100) chance = 100;
        return roll < chance;
    }

    public int getArmorReduction() {
        return equippedArmor != null ? equippedArmor.getDamageReduction() : 0;
    }

    public void equipWeapon(Weapon weapon, boolean useTwoHands) {
        this.equippedWeapon = weapon;
        if (weapon != null && weapon.getHandsRequired() == 2) {
            this.usingWeaponTwoHanded = true;
        } else {
            this.usingWeaponTwoHanded = useTwoHands;
        }
    }

    public void equipWeapon(Weapon weapon) {
        equipWeapon(weapon, false);
    }

    public void equipArmor(Armor armor) {
        this.equippedArmor = armor;
    }

    public Weapon getEquippedWeapon() { return equippedWeapon; }
    public Armor  getEquippedArmor()  { return equippedArmor; }
    public boolean isUsingWeaponTwoHanded() { return usingWeaponTwoHanded; }

    public void applyPotion(Potion potion) {
        int amt = potion.getAmount();
        switch (potion.getStatType()) {
            case HP:
                heal(amt);
                break;
            case MANA:
                restoreMana(amt);
                break;
            case STRENGTH:
                strength += amt;
                break;
            case DEXTERITY:
                dexterity += amt;
                break;
            case AGILITY:
                agility += amt;
                break;
            case ALL:
                heal(amt);
                restoreMana(amt);
                strength  += amt;
                dexterity += amt;
                agility   += amt;
                break;
            default:
                break;
        }
    }

    public void restoreMana(int delta) {
        mana += delta;
        if (mana > maxMana) mana = maxMana;
        if (mana < 0) mana = 0;
    }

    public int getMana() {
        return mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        gold += amount;
        if (gold < 0) gold = 0;
    }

    public boolean spendGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public long getExperience() {
        return experience;
    }

    public long getXpToNextLevel() {
        return expRequiredForLevel(level + 1) - experience;
    }

    public int getStrength() {
        return strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public int getAgility() {
        return agility;
    }


    // getters for stats, xp, gold, inventory...
}
