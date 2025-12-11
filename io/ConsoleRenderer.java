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
                            Map<Monster, Position> monsterPositions) {
        int size = world.getSize();
        System.out.println("=== LEGENDS OF VALOR BOARD ===");

        int cellWidth  = 4; // interior characters
        int cellHeight = 4; // interior characters

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
                    char baseChar;
                    switch (tile.getType()) {
                        case INACCESSIBLE:
                            baseChar = 'X';
                            break;
                        case HERO_NEXUS:
                            baseChar = 'H';
                            break;
                        case MONSTER_NEXUS:
                            baseChar = 'M';
                            break;
                        case BUSH:
                            baseChar = 'B';
                            break;
                        case CAVE:
                            baseChar = 'C';
                            break;
                        case KOULOU:
                            baseChar = 'K';
                            break;
                        case OBSTACLE:
                            baseChar = 'O';
                            break;
                        default:
                            baseChar = 'P'; // Plain/common
                            break;
                    }

                    // Overlay heroes / monsters
                    char cellChar = getOccupantChar(r, c, heroPositions, monsterPositions, baseChar);

                    int centerRow = 1; // which interior row to draw symbol on
                    int centerCol = 1; // which interior col to draw symbol on

                    for (int ic = 0; ic < cellWidth; ic++) {
                        if (ir == centerRow && ic == centerCol) {
                            line.append(cellChar);
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
        System.out.println("  H = Hero Nexus");
        System.out.println("  M = Monster Nexus");
        System.out.println("  P = Plain tile");
        System.out.println("  X = Inaccessible");
        System.out.println("  B/C/K = Bush/Cave/Koulou");
        System.out.println("  O = Obstacle");
        System.out.println("  h = Hero");
        System.out.println("  m = Monster");
        System.out.println();
    }

    private static class CharacterCell {
        char ch;
        CharacterCell(char ch) { this.ch = ch; }
    }

    private char getOccupantChar(int row, int col,
                                 Map<Hero, Position> heroPositions,
                                 Map<Monster, Position> monsterPositions,
                                 char baseChar) {
        for (Map.Entry<Hero, Position> e : heroPositions.entrySet()) {
            Position p = e.getValue();
            if (p != null && p.getRow() == row && p.getCol() == col) {
                return 'h'; // later: H1/H2/H3 if you want
            }
        }
        for (Map.Entry<Monster, Position> e : monsterPositions.entrySet()) {
            Position p = e.getValue();
            if (p != null && p.getRow() == row && p.getCol() == col) {
                return 'm';
            }
        }
        return baseChar;
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
