package market;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data.ItemFactory;
import items.Armor;
import items.Item;
import items.Potion;
import items.Weapon;
import config.GameBalance;

public class Market {

    private final List<Item> stock;
    private final Random random;

    public Market(ItemFactory itemFactory, int approxLevel) {
        this.stock = new ArrayList<Item>();
        this.random = new Random();

        // Populate market with a mix of weapons, armor, and potions.
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

            int typeRoll = random.nextInt(3);
            Item item = null;
            if (typeRoll == 0) {
                Weapon w = itemFactory.getRandomWeaponForLevel(targetLevel);
                item = w;
            } else if (typeRoll == 1) {
                Armor a = itemFactory.getRandomArmorForLevel(targetLevel);
                item = a;
            } else {
                Potion p = itemFactory.getRandomPotionForLevel(targetLevel);
                item = p;
            }

            if (item != null) {
                stock.add(item);
            }
        }
    }

    public List<Item> getStock() {
        return stock;
    }
}
