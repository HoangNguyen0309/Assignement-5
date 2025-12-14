package market;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data.ItemFactory;
import items.Armor;
import items.Item;
import items.Potion;
import items.Spell;
import items.Weapon;
import config.GameBalance;

public class Market {

    private final List<Item> stock;
    private final Random random;
    private int baseLevel;

    public Market(ItemFactory itemFactory, int approxLevel) {
        this.stock = new ArrayList<Item>();
        this.random = new Random();
        this.baseLevel = approxLevel;
        populateStock(itemFactory, approxLevel);
    }

    private void populateStock(ItemFactory itemFactory, int approxLevel) {
        stock.clear();
        // Populate market with a mix of weapons, armor, potions, and spells.
        // Higher level items are rarer by biasing around approxLevel.
        for (int i = 0; i < 10; i++) {
            int roll = random.nextInt(100);
            int targetLevel;

            // Bias: most items near or below party level, some slightly above.
            if (roll < 60) {
                targetLevel = approxLevel;      // 60% near level
            } else if (roll < 90) {
                targetLevel = Math.max(1, approxLevel - 1); // 30% slightly lower
            } else {
                targetLevel = approxLevel + 2;  // 10% higher-level (rare)
            }

            int typeRoll = random.nextInt(4);
            Item item = null;
            if (typeRoll == 0) {
                Weapon w = itemFactory.getRandomWeaponForLevel(targetLevel);
                item = w;
            } else if (typeRoll == 1) {
                Armor a = itemFactory.getRandomArmorForLevel(targetLevel);
                item = a;
            } else if (typeRoll == 2) {
                Potion p = itemFactory.getRandomPotionForLevel(targetLevel);
                item = p;
            } else {
                Spell s = itemFactory.getRandomSpellForLevel(targetLevel);
                item = s;
            }

            if (item != null) {
                stock.add(item);
            }
        }
    }

    public void restock(ItemFactory itemFactory, int approxLevel) {
        this.baseLevel = approxLevel;
        populateStock(itemFactory, approxLevel);
    }

    public List<Item> getStock() {
        return stock;
    }

    public int getBaseLevel() {
        return baseLevel;
    }
}
