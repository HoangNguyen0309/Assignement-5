package controller;

import java.util.List;

import characters.Hero;
import io.InputHandler;
import io.Renderer;
import items.Armor;
import items.Potion;
import items.Weapon;

/**
 * Reusable inventory UI + operations.
 * Call this from BOTH GameEngine and ValorGameEngine instead of duplicating menus.
 *
 * This controller is intentionally mode-agnostic.
 */
public class InventoryController {

    /**
     * Adapter interface so you don't have to perfectly match method names.
     * Implement this with a tiny wrapper around your Hero/Inventory API.
     */
    public interface HeroInventoryAdapter {
        Hero getHero();

        List<Weapon> getWeapons();
        List<Armor> getArmors();
        List<Potion> getPotions();

        void equipWeapon(Weapon w);
        void equipArmor(Armor a);
        void usePotion(Potion p);

        Weapon getEquippedWeapon(); // may return null
        Armor getEquippedArmor();   // may return null
    }

    private int readIntInRange(InputHandler input, Renderer renderer, int min, int max) {
        while (true) {
            int val = input.readInt(); // <-- must exist in your InputHandler
            if (val >= min && val <= max) return val;
            renderer.renderMessage("Please enter a number between " + min + " and " + max + ".");
        }
    }

    public void openInventoryMenu(HeroInventoryAdapter adapter, Renderer renderer, InputHandler input) {
        while (true) {
            renderer.renderMessage("");
            renderer.renderMessage("===== Inventory Menu (" + safeName(adapter.getHero()) + ") =====");
            renderer.renderMessage("1) View equipped");
            renderer.renderMessage("2) Equip weapon");
            renderer.renderMessage("3) Equip armor");
            renderer.renderMessage("4) Use potion");
            renderer.renderMessage("0) Exit inventory");

            int choice = readIntInRange(input, renderer, 0, 4);


            if (choice == 0) return;

            switch (choice) {
                case 1:
                    showEquipped(adapter, renderer);
                    break;
                case 2:
                    equipWeaponFlow(adapter, renderer, input);
                    break;
                case 3:
                    equipArmorFlow(adapter, renderer, input);
                    break;
                case 4:
                    usePotionFlow(adapter, renderer, input);
                    break;
                default:
                    renderer.renderMessage("Invalid choice.");
            }
        }
    }

    private void showEquipped(HeroInventoryAdapter adapter, Renderer renderer) {
        Weapon w = adapter.getEquippedWeapon();
        Armor a = adapter.getEquippedArmor();
        renderer.renderMessage("Equipped weapon: " + (w == null ? "(none)" : w.toString()));
        renderer.renderMessage("Equipped armor : " + (a == null ? "(none)" : a.toString()));
    }

    private void equipWeaponFlow(HeroInventoryAdapter adapter, Renderer renderer, InputHandler input) {
        List<Weapon> weapons = adapter.getWeapons();
        if (weapons == null || weapons.isEmpty()) {
            renderer.renderMessage("No weapons available.");
            return;
        }

        renderer.renderMessage("Choose a weapon:");
        for (int i = 0; i < weapons.size(); i++) {
            renderer.renderMessage((i + 1) + ") " + weapons.get(i));
        }
        renderer.renderMessage("0) Cancel");

        int c = readIntInRange(input, renderer, 0, weapons.size());

        if (c == 0) return;

        Weapon selected = weapons.get(c - 1);
        adapter.equipWeapon(selected);
        renderer.renderMessage("Equipped: " + selected);
    }

    private void equipArmorFlow(HeroInventoryAdapter adapter, Renderer renderer, InputHandler input) {
        List<Armor> armors = adapter.getArmors();
        if (armors == null || armors.isEmpty()) {
            renderer.renderMessage("No armor available.");
            return;
        }

        renderer.renderMessage("Choose armor:");
        for (int i = 0; i < armors.size(); i++) {
            renderer.renderMessage((i + 1) + ") " + armors.get(i));
        }
        renderer.renderMessage("0) Cancel");

        int c = readIntInRange(input, renderer, 0, armors.size());

        if (c == 0) return;

        Armor selected = armors.get(c - 1);
        adapter.equipArmor(selected);
        renderer.renderMessage("Equipped: " + selected);
    }

    private void usePotionFlow(HeroInventoryAdapter adapter, Renderer renderer, InputHandler input) {
        List<Potion> potions = adapter.getPotions();
        if (potions == null || potions.isEmpty()) {
            renderer.renderMessage("No potions available.");
            return;
        }

        renderer.renderMessage("Choose a potion:");
        for (int i = 0; i < potions.size(); i++) {
            renderer.renderMessage((i + 1) + ") " + potions.get(i));
        }
        renderer.renderMessage("0) Cancel");

        int c = readIntInRange(input, renderer, 0, potions.size());

        if (c == 0) return;

        Potion selected = potions.get(c - 1);
        adapter.usePotion(selected);
        renderer.renderMessage("Used: " + selected);
    }

    private String safeName(Hero hero) {
        try {
            // If Hero has getName()
            return (String) hero.getClass().getMethod("getName").invoke(hero);
        } catch (Exception ignored) {
            return hero == null ? "Hero" : hero.toString();
        }
    }
}
