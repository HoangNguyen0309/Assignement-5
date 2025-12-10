package io;

import java.util.List;

import characters.Hero;
import characters.Monster;
import core.Position;
import world.Tile;
import world.TileType;
import world.World;
import items.Weapon;
import items.Spell;
import items.Armor;

public class ConsoleRenderer implements Renderer {

    public void renderWorld(World world) {
        int size = world.getSize();
        Position pos = world.getPartyPosition();

        System.out.println("=== WORLD MAP ===");

        for (int r = 0; r < size; r++) {
            // Top border for this row of tiles
            StringBuilder borderTop = new StringBuilder();
            for (int c = 0; c < size; c++) {
                borderTop.append("+----");
            }
            borderTop.append("+");
            System.out.println(borderTop.toString());

            // Two interior rows per tile (2x2 content)
            StringBuilder rowTop = new StringBuilder();
            StringBuilder rowBottom = new StringBuilder();

            for (int c = 0; c < size; c++) {
                Tile tile = world.getTile(r, c);
                boolean isParty = (r == pos.getRow() && c == pos.getCol());
                boolean isMarket = tile.getType() == TileType.MARKET;
                boolean accessible = tile.isAccessible();

                char base = accessible ? 'x' : '#';

                // 2x2 chars for this tile
                char tl = base; // top-left
                char tr = base; // top-right
                char bl = base; // bottom-left
                char br = base; // bottom-right

                if (isMarket) {
                    tr = 'M';
                }
                if (isParty) {
                    bl = 'P';
                }

                // Build top interior row for this tile
                rowTop.append("| ");
                rowTop.append(tl).append(tr);
                rowTop.append(" ");

                // Build bottom interior row for this tile
                rowBottom.append("| ");
                rowBottom.append(bl).append(br);
                rowBottom.append(" ");
            }

            // Close the line for each interior row
            rowTop.append("|");
            rowBottom.append("|");

            System.out.println(rowTop.toString());
            System.out.println(rowBottom.toString());
        }

        // Bottom border for the last row
        StringBuilder borderBottom = new StringBuilder();
        for (int c = 0; c < size; c++) {
            borderBottom.append("+----");
        }
        borderBottom.append("+");
        System.out.println(borderBottom.toString());

        System.out.println();
        System.out.println("Legend:");
        System.out.println("  P = Party (bottom-left of a tile)");
        System.out.println("  M = Market (top-right of a tile)");
        System.out.println("  x = Accessible tile");
        System.out.println("  # = Inaccessible tile");
        System.out.println();
    }

    public void renderHeroStats(List<Hero> heroes) {
        System.out.println("=== HERO PARTY ===");
        for (Hero h : heroes) {
            System.out.println(
                    h.getName() +
                            " | Lv " + h.getLevel() +
                            " | HP " + h.getHP() + "/" + h.getMaxHP() +
                            " | MP " + h.getMana() + "/" + h.getMaxMana()
            );
            System.out.println(
                    "  STR " + h.getStrength() +
                            "  DEX " + h.getDexterity() +
                            "  AGI " + h.getAgility()
            );
            System.out.println(
                    "  XP: " + h.getExperience() +
                            "  (Next level in: " + h.getXpToNextLevel() + " XP)"
            );
            System.out.println("  GOLD: " + h.getGold());

            // NEW: show currently equipped weapon/armor
            Weapon w = h.getEquippedWeapon();
            Armor a = h.getEquippedArmor();

            if (w != null) {
                String handsLabel = w.getHandsRequired() == 2
                        ? "2H"
                        : (h.isUsingWeaponTwoHanded() ? "1H (using 2H)" : "1H");
                System.out.println("  Weapon: " + w.getName() +
                        " | DMG " + w.getDamage() +
                        " | " + handsLabel);
            } else {
                System.out.println("  Weapon: none");
            }

            if (a != null) {
                System.out.println("  Armor : " + a.getName() +
                        " | RED " + a.getDamageReduction());
            } else {
                System.out.println("  Armor : none");
            }

            System.out.println();
        }
    }

    public void renderMonsterStats(List<Monster> monsters) {
        System.out.println("=== MONSTERS ===");
        for (Monster m : monsters) {
            System.out.println(
                    m.getName() +
                            " | Lv " + m.getLevel() +
                            " | HP " + m.getHP() + "/" + m.getMaxHP() +
                            " | DMG " + m.getDamage() +
                            " | DEF " + m.getDefense() +
                            " | DODGE " + m.getDodgeChance()
            );
        }
        System.out.println();
    }

    public void renderMessage(String message) {
        System.out.println(message);
    }
}
