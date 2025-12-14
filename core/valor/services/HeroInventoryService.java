package core.valor.services;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import core.valor.ValorContext;
import items.Armor;
import items.Item;
import items.Potion;
import items.Weapon;

public class HeroInventoryService {

    public boolean openInventory(ValorContext ctx, Hero hero) {
        boolean acted = false;
        boolean done = false;

        while (!done) {
            ctx.renderer.renderMessage("Manage " + hero.getName() + ":");
            ctx.renderer.renderMessage("  1) Equip weapon");
            ctx.renderer.renderMessage("  2) Equip armor");
            ctx.renderer.renderMessage("  3) Use potion");
            ctx.renderer.renderMessage("  4) View inventory");
            ctx.renderer.renderMessage("  5) Back");

            int choice = ctx.input.readInt();
            switch (choice) {
                case 1: acted |= equipWeapon(ctx, hero); break;
                case 2: acted |= equipArmor(ctx, hero); break;
                case 3: acted |= usePotion(ctx, hero); break;
                case 4: renderInventory(ctx, hero); break;
                case 5: done = true; break;
                default: ctx.renderer.renderMessage("Invalid choice.");
            }
        }

        return acted;
    }

    private void renderInventory(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        if (items.isEmpty()) {
            ctx.renderer.renderMessage(hero.getName() + " has no items.");
            return;
        }
        ctx.renderer.renderMessage(hero.getName() + "'s inventory:");
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + it.getName()
                    + " (Lv " + it.getRequiredLevel() + ", Price " + it.getPrice() + ")");
        }
    }

    private boolean equipWeapon(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Weapon> weapons = new ArrayList<Weapon>();
        for (Item item : items) if (item instanceof Weapon) weapons.add((Weapon) item);

        if (weapons.isEmpty()) {
            ctx.renderer.renderMessage("No weapons available to equip.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a weapon to equip:");
        for (int i = 0; i < weapons.size(); i++) {
            Weapon w = weapons.get(i);
            String handsText = (w.getHandsRequired() == 2 ? "2H" : "1H");
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + w.getName()
                    + " (Damage " + w.getDamage()
                    + ", Req Lv " + w.getRequiredLevel()
                    + ", " + handsText + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int choice = ctx.input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= weapons.size()) {
            ctx.renderer.renderMessage("Invalid weapon choice.");
            return false;
        }

        Weapon selected = weapons.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            ctx.renderer.renderMessage("Hero level too low to equip this weapon.");
            return false;
        }

        boolean useTwoHands = false;
        if (selected.getHandsRequired() == 2) {
            useTwoHands = true;
            ctx.renderer.renderMessage("Equipping " + selected.getName() + " (2H required).");
        } else {
            ctx.renderer.renderMessage("Use this one-handed weapon with:");
            ctx.renderer.renderMessage("  1) One hand (normal damage)");
            ctx.renderer.renderMessage("  2) Two hands (increased damage)");
            int handChoice = ctx.input.readInt();
            useTwoHands = (handChoice == 2);
        }

        hero.equipWeapon(selected, useTwoHands);
        ctx.renderer.renderMessage(hero.getName() + " equipped weapon: " + selected.getName()
                + (useTwoHands ? " (using both hands)" : ""));
        return true;
    }

    private boolean equipArmor(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Armor> armors = new ArrayList<Armor>();
        for (Item item : items) if (item instanceof Armor) armors.add((Armor) item);

        if (armors.isEmpty()) {
            ctx.renderer.renderMessage("No armor available to equip.");
            return false;
        }

        ctx.renderer.renderMessage("Choose armor to equip:");
        for (int i = 0; i < armors.size(); i++) {
            Armor a = armors.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + a.getName()
                    + " (Reduction " + a.getDamageReduction()
                    + ", Req Lv " + a.getRequiredLevel() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int choice = ctx.input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= armors.size()) {
            ctx.renderer.renderMessage("Invalid armor choice.");
            return false;
        }

        Armor selected = armors.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            ctx.renderer.renderMessage("Hero level too low to equip this armor.");
            return false;
        }

        hero.equipArmor(selected);
        ctx.renderer.renderMessage(hero.getName() + " equipped armor: " + selected.getName());
        return true;
    }

    private boolean usePotion(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Potion> potions = new ArrayList<Potion>();
        for (Item item : items) if (item instanceof Potion) potions.add((Potion) item);

        if (potions.isEmpty()) {
            ctx.renderer.renderMessage("No potions available.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a potion to use:");
        for (int i = 0; i < potions.size(); i++) {
            Potion p = potions.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + p.getName()
                    + " (Effect " + p.getAmount()
                    + " on " + p.getStatType()
                    + ", Req Lv " + p.getRequiredLevel() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int choice = ctx.input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= potions.size()) {
            ctx.renderer.renderMessage("Invalid potion choice.");
            return false;
        }

        Potion selected = potions.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            ctx.renderer.renderMessage("Hero level too low to use this potion.");
            return false;
        }

        selected.consume(hero);
        hero.getInventory().remove(selected);
        ctx.renderer.renderMessage(hero.getName() + " used potion: " + selected.getName());
        return true;
    }
}
