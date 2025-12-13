package io;

import java.util.List;
import java.util.Map;

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

    @Override
    public void renderWorld(World world,
                            Map<Hero, Position> heroPositions,
                            Map<Monster, Position> monsterPositions,
                            Map<Hero, String> heroCodes,
                            Map<Monster, String> monsterCodes) {
        int size = world.getSize();
        System.out.println("=== LEGENDS OF VALOR BOARD ===");

        int cellWidth  = 4; // interior characters
        int cellHeight = 4; // interior characters

        // Use provided monster codes
        Map<Monster, String> monsterIds = monsterCodes;

        for (int r = 0; r < size; r++) {
            // Top border for this row
            StringBuilder border = new StringBuilder();
            for (int c = 0; c < size; c++) {
                border.append("+");
                for (int i = 0; i < cellWidth; i++) {
                    border.append("-");
                }
            }
            border.append("+");
            System.out.println(border.toString());

            // Interior rows for this board row
            for (int ir = 0; ir < cellHeight; ir++) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < size; c++) {
                    line.append("|");

                    Tile tile = world.getTile(r, c);
                    TileType type = tile.getType();
                    // Foreground colors for terrain
                    char terrainChar;
                    String color = "";
                    switch (type) {
                        case HERO_NEXUS:
                            terrainChar = 'H';
                            color = "\u001B[34m"; // blue text
                            break;
                        case MONSTER_NEXUS:
                            terrainChar = 'M';
                            color = "\u001B[31m"; // red text
                            break;
                        case BUSH:
                            terrainChar = 'B';
                            color = "\u001B[32m"; // green text
                            break;
                        case CAVE:
                            terrainChar = 'C';
                            color = "\u001B[35m"; // magenta text
                            break;
                        case KOULOU:
                            terrainChar = 'K';
                            color = "\u001B[33m"; // yellow text
                            break;
                        case OBSTACLE:
                            terrainChar = 'O';
                            color = "\u001B[90m"; // gray text
                            break;
                        case INACCESSIBLE:
                            terrainChar = 'X';
                            color = "\u001B[31m"; // red text
                            break;
                        default:
                            terrainChar = 'P';
                            color = "";
                            break;
                    }

                    // Find hero/monster on this tile
                    String heroMark = null;
                    for (Map.Entry<Hero, Position> e : heroPositions.entrySet()) {
                        Position p = e.getValue();
                        if (p != null && p.getRow() == r && p.getCol() == c) {
                            if (heroCodes != null) {
                                heroMark = heroCodes.get(e.getKey());
                            }
                            if (heroMark == null) {
                                heroMark = "h?";
                            }
                            break;
                        }
                    }
                    String monsterMark = null;
                    for (Map.Entry<Monster, Position> e : monsterPositions.entrySet()) {
                        Position p = e.getValue();
                        if (p != null && p.getRow() == r && p.getCol() == c) {
                            if (monsterIds != null) {
                                monsterMark = monsterIds.get(e.getKey());
                            }
                            if (monsterMark == null) {
                                monsterMark = "m?";
                            }
                            break;
                        }
                    }

                    // Build 4x4 cell
                    for (int ic = 0; ic < cellWidth; ic++) {
                        String reset = "\u001B[0m";
                        if (ir == 0 && ic == 0) {
                            line.append(color).append(terrainChar).append(reset);
                        } else if (monsterMark != null && ir == 0 && ic == 2) {
                            line.append('m');
                        } else if (monsterMark != null && ir == 0 && ic == 3) {
                            line.append(monsterMark.substring(1,2));
                        } else if (heroMark != null && ir == 3 && ic == 2) {
                            line.append('h');
                        } else if (heroMark != null && ir == 3 && ic == 3) {
                            line.append(heroMark.substring(1,2));
                        } else {
                            line.append(' ');
                        }
                    }
                }
                line.append("|");
                System.out.println(line.toString());
            }
        }

        // Bottom border
        StringBuilder bottom = new StringBuilder();
        for (int c = 0; c < size; c++) {
            bottom.append("+");
            for (int i = 0; i < cellWidth; i++) {
                bottom.append("-");
            }
        }
        bottom.append("+");
        System.out.println(bottom.toString());

        System.out.println("Legend:");
        System.out.println("  H (blue text) = Hero Nexus");
        System.out.println("  M (red text)  = Monster Nexus");
        System.out.println("  B/C/K = Bush/Cave/Koulou (colored text)");
        System.out.println("  O = Obstacle (gray text)");
        System.out.println("  P = Plain tile");
        System.out.println("  X = Inaccessible (red text)");
        System.out.println("  m1/m2/m3 at top-right, h1/h2/h3 at bottom-right of a cell");
        System.out.println();
    }

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
        renderHeroStats(heroes, null);
    }

    public void renderHeroStats(List<Hero> heroes, Map<Hero, String> heroCodes) {
        System.out.println("=== HERO PARTY ===");
        for (Hero h : heroes) {
            String code = heroCodes != null ? heroCodes.get(h) : null;
            String name = h.getName() + (code != null ? " (" + code + ")" : "");
            System.out.println(
                    name +
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
        renderMonsterStats(monsters, null);
    }

    public void renderMonsterStats(List<Monster> monsters, Map<Monster, String> monsterCodes) {
        System.out.println("=== MONSTERS ===");
        for (Monster m : monsters) {
            String code = monsterCodes != null ? monsterCodes.get(m) : null;
            String name = m.getName() + (code != null ? " (" + code + ")" : "");
            System.out.println(
                    name +
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
