package controller;

import java.util.List;

import characters.Hero;
import controller.InventoryController.HeroInventoryAdapter;
import items.Armor;
import items.Potion;
import items.Weapon;

/**
 * Helper adapters so engines can use InventoryController without rewriting Hero.
 */
public final class Adapters {

    private Adapters() {}

    /**
     * Reflection-based adapter (works even if method names vary slightly),
     * but you can also write a direct adapter if you prefer.
     */
    public static HeroInventoryAdapter heroInventory(final Hero hero) {
        return new HeroInventoryAdapter() {
            @Override public Hero getHero() { return hero; }

            @SuppressWarnings("unchecked")
            @Override public List<Weapon> getWeapons() {
                return (List<Weapon>) invokeChain(hero, "getInventory", "getWeapons");
            }

            @SuppressWarnings("unchecked")
            @Override public List<Armor> getArmors() {
                return (List<Armor>) invokeChain(hero, "getInventory", "getArmors");
            }

            @SuppressWarnings("unchecked")
            @Override public List<Potion> getPotions() {
                return (List<Potion>) invokeChain(hero, "getInventory", "getPotions");
            }

            @Override public void equipWeapon(Weapon w) {
                // Try hero.equipWeapon(w) else inventory.equipWeapon(w)
                if (!tryInvoke(hero, "equipWeapon", new Class[]{Weapon.class}, new Object[]{w})) {
                    Object inv = invoke(hero, "getInventory");
                    tryInvoke(inv, "equipWeapon", new Class[]{Weapon.class}, new Object[]{w});
                }
            }

            @Override public void equipArmor(Armor a) {
                if (!tryInvoke(hero, "equipArmor", new Class[]{Armor.class}, new Object[]{a})) {
                    Object inv = invoke(hero, "getInventory");
                    tryInvoke(inv, "equipArmor", new Class[]{Armor.class}, new Object[]{a});
                }
            }

            @Override public void usePotion(Potion p) {
                if (!tryInvoke(hero, "usePotion", new Class[]{Potion.class}, new Object[]{p})) {
                    Object inv = invoke(hero, "getInventory");
                    tryInvoke(inv, "usePotion", new Class[]{Potion.class}, new Object[]{p});
                }
            }

            @Override public Weapon getEquippedWeapon() {
                Object w = invokeChain(hero, "getInventory", "getEquippedWeapon");
                return (Weapon) w;
            }

            @Override public Armor getEquippedArmor() {
                Object a = invokeChain(hero, "getInventory", "getEquippedArmor");
                return (Armor) a;
            }
        };
    }

    private static Object invokeChain(Object base, String first, String second) {
        Object mid = invoke(base, first);
        return invoke(mid, second);
    }

    private static Object invoke(Object target, String method) {
        if (target == null) return null;
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean tryInvoke(Object target, String method, Class<?>[] types, Object[] args) {
        if (target == null) return false;
        try {
            target.getClass().getMethod(method, types).invoke(target, args);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
