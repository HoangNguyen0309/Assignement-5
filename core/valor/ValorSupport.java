package core.valor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import characters.Hero;
import characters.Monster;
import config.GameBalance;
import core.Position;
import items.Armor;
import items.Item;
import items.Potion;
import items.Spell;
import items.Weapon;
import market.Market;
import world.CommonTile;
import world.MarketTile;
import world.Tile;
import world.TileType;

public final class ValorSupport {

    private ValorSupport() {}

    // ============================================================
    // Setup
    // ============================================================

    public static void initializeHeroPositions(ValorContext ctx) {
        Position[] starts = ctx.world.getValorHeroPosition();
        if (starts == null || starts.length == 0) {
            starts = new Position[] {
                    new Position(ctx.world.getSize() - 1, 0),
                    new Position(ctx.world.getSize() - 1, 3),
                    new Position(ctx.world.getSize() - 1, 6)
            };
        }

        int count = Math.min(ctx.heroes.size(), starts.length);
        for (int i = 0; i < count; i++) {
            Hero h = ctx.heroes.get(i);
            Position p = new Position(starts[i].getRow(), starts[i].getCol());
            ctx.heroPositions.put(h, p);
            ctx.heroSpawnPositions.put(h, new Position(p.getRow(), p.getCol()));
            applyTerrainEffects(ctx, h, null, p);
        }
    }

    public static void spawnInitialMonsters(ValorContext ctx) {
        int level = maxHeroLevel(ctx);
        List<Monster> spawned = ctx.monsterFactory.spawnMonstersForBattle(3, level);

        int[][] laneCols = {{0,1},{3,4},{6,7}};
        int idx = 0;

        for (int lane = 0; lane < laneCols.length && idx < spawned.size(); lane++) {
            for (int c = 0; c < laneCols[lane].length && idx < spawned.size(); c++) {
                int col = laneCols[lane][c];
                Position pos = new Position(0, col);

                if (isOccupiedByMonster(ctx, pos)) continue;

                Monster m = spawned.get(idx++);
                ctx.monsters.add(m);
                ctx.monsterPositions.put(m, pos);
                break;
            }
        }
        assignMonsterCodes(ctx);
    }

    // ============================================================
    // Codes / logging / UI
    // ============================================================

    public static void logAction(ValorContext ctx, String msg) {
        ctx.roundLog.add(msg);
    }

    public static void assignHeroCodes(ValorContext ctx) {
        ctx.heroCodes.clear();
        int idx = 1;
        for (Hero h : ctx.heroes) {
            ctx.heroCodes.put(h, "h" + idx++);
        }
    }

    public static void assignMonsterCodes(ValorContext ctx) {
        ctx.monsterCodes.clear();
        int idx = 1;
        for (Monster m : ctx.monsters) {
            ctx.monsterCodes.put(m, "m" + idx++);
        }
    }

    public static void renderHeroTurnMenu(ValorContext ctx, Hero hero, boolean showBoard) {
        if (showBoard) {
            ctx.renderer.renderWorld(ctx.world, ctx.heroPositions, ctx.monsterPositions, ctx.heroCodes, ctx.monsterCodes);
        }

        String code = ctx.heroCodes.get(hero);
        if (code == null) code = "h?";

        StringBuilder title = new StringBuilder();
        title.append(hero.getName())
                .append(" (")
                .append(colorize(code, "\u001B[36m"))
                .append(")");

        String terrainDesc = terrainBuffDescription(ctx, hero);
        if (!terrainDesc.isEmpty()) {
            title.append(", ").append(terrainDesc);
        }
        title.append(", choose your action:");

        ctx.renderer.renderMessage(title.toString());
        ctx.renderer.renderMessage("  1) Move");
        ctx.renderer.renderMessage("  2) Attack");
        ctx.renderer.renderMessage("  3) Cast Spell");
        ctx.renderer.renderMessage("  4) Inventory");
        ctx.renderer.renderMessage("  5) Recall");
        ctx.renderer.renderMessage("  6) Shop (free, if at Hero Nexus)");
        ctx.renderer.renderMessage("  7) Retreat (fall back one tile, engaged monster advances)");
        ctx.renderer.renderMessage("  8) Teleport");
        ctx.renderer.renderMessage("  9) Remove Obstacle");
        ctx.renderer.renderMessage("  10) View Party/Monsters (free)");
        ctx.renderer.renderMessage("  11) Skip");
    }

    private static String terrainBuffDescription(ValorContext ctx, Hero hero) {
        Position pos = ctx.heroPositions.get(hero);
        if (pos == null) return "";

        Tile tile = ctx.world.getTile(pos.getRow(), pos.getCol());
        TileType type = tile.getType();

        switch (type) {
            case HERO_NEXUS:
            case MARKET:
                return colorize("in Market, you can buy", "\u001B[33m");
            case BUSH:
                return colorize("in Bush, dexterity increased", "\u001B[32m");
            case CAVE:
                return colorize("in Cave, agility increased", "\u001B[35m");
            case KOULOU:
                return colorize("in Koulou, strength increased", "\u001B[34m");
            default:
                return "";
        }
    }

    private static String colorize(String text, String color) {
        return color + text + "\u001B[0m";
    }

    // ============================================================
    // Board helpers
    // ============================================================

    public static boolean isInsideBoard(ValorContext ctx, int r, int c) {
        int size = ctx.world.getSize();
        return r >= 0 && r < size && c >= 0 && c < size;
    }

    // In range = same tile or 8 neighbors, AND same lane
    public static boolean isInRange(ValorContext ctx, Position a, Position b) {
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getCol() - b.getCol());
        return dr <= 1 && dc <= 1 && ctx.world.sameLane(a, b);
    }

    public static boolean isOccupiedByHero(ValorContext ctx, Position p, Hero ignore) {
        for (Map.Entry<Hero, Position> entry : ctx.heroPositions.entrySet()) {
            if (ignore != null && entry.getKey() == ignore) continue;
            Position hp = entry.getValue();
            if (hp != null && hp.getRow() == p.getRow() && hp.getCol() == p.getCol()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOccupiedByMonster(ValorContext ctx, Position p) {
        return isOccupiedByMonster(ctx, p, null);
    }

    public static boolean isOccupiedByMonster(ValorContext ctx, Position p, Monster ignore) {
        for (Map.Entry<Monster, Position> entry : ctx.monsterPositions.entrySet()) {
            if (ignore != null && entry.getKey() == ignore) continue;
            Position mp = entry.getValue();
            if (mp != null && mp.getRow() == p.getRow() && mp.getCol() == p.getCol()) {
                return true;
            }
        }
        return false;
    }

    // Heroes advance upward; monsters advance downward; neither can move past foremost enemy in lane
    public static boolean wouldMovePastEnemy(ValorContext ctx, Position from, Position to, boolean isHero) {
        if (!ctx.world.sameLane(from, to)) return false;

        if (isHero) {
            if (to.getRow() >= from.getRow()) return false;
            for (Map.Entry<Monster, Position> entry : ctx.monsterPositions.entrySet()) {
                if (entry.getKey().isFainted()) continue;
                Position mp = entry.getValue();
                if (mp == null) continue;
                if (!ctx.world.sameLane(from, mp)) continue;
                if (to.getRow() < mp.getRow()) return true;
            }
        } else {
            if (to.getRow() <= from.getRow()) return false;
            for (Map.Entry<Hero, Position> entry : ctx.heroPositions.entrySet()) {
                if (entry.getKey().isFainted()) continue;
                Position hp = entry.getValue();
                if (hp == null) continue;
                if (!ctx.world.sameLane(from, hp)) continue;
                if (to.getRow() > hp.getRow()) return true;
            }
        }
        return false;
    }

    public static boolean isBehindEnemyInDestination(ValorContext ctx, Position dest, boolean isHero) {
        if (isHero) {
            for (Map.Entry<Monster, Position> entry : ctx.monsterPositions.entrySet()) {
                if (entry.getKey().isFainted()) continue;
                Position mp = entry.getValue();
                if (mp == null) continue;
                if (!ctx.world.sameLane(dest, mp)) continue;
                if (dest.getRow() < mp.getRow()) return true;
            }
        } else {
            for (Map.Entry<Hero, Position> entry : ctx.heroPositions.entrySet()) {
                if (entry.getKey().isFainted()) continue;
                Position hp = entry.getValue();
                if (hp == null) continue;
                if (!ctx.world.sameLane(dest, hp)) continue;
                if (dest.getRow() > hp.getRow()) return true;
            }
        }
        return false;
    }

    // ============================================================
    // Lane helpers
    // ============================================================

    public static void updateLaneMaxLevels(ValorContext ctx) {
        ctx.laneMaxLevels.clear();
        for (Hero h : ctx.heroes) {
            Position spawn = ctx.heroSpawnPositions.get(h);
            if (spawn == null) continue;
            int lane = ctx.world.laneIndexForCol(spawn.getCol());
            if (lane < 0) continue;
            int cur = ctx.laneMaxLevels.containsKey(lane) ? ctx.laneMaxLevels.get(lane) : 0;
            if (h.getLevel() > cur) ctx.laneMaxLevels.put(lane, h.getLevel());
        }
    }

    private static int getLaneMaxLevel(ValorContext ctx, int laneIndex) {
        Integer val = ctx.laneMaxLevels.get(laneIndex);
        return val == null ? 1 : val;
    }

    private static int laneIndexForHero(ValorContext ctx, Hero hero) {
        Position spawn = ctx.heroSpawnPositions.get(hero);
        if (spawn == null) return -1;
        return ctx.world.laneIndexForCol(spawn.getCol());
    }

    private static Position findAvailableHeroNexusSlot(ValorContext ctx, int laneIndex, Hero hero) {
        Position[] slots = ctx.world.getHeroNexusColumnsForLane(laneIndex);
        if (slots == null) return null;
        for (Position p : slots) {
            if (!isOccupiedByHero(ctx, p, hero)) return p;
        }
        return null;
    }

    public static int maxHeroLevel(ValorContext ctx) {
        int max = 1;
        for (Hero h : ctx.heroes) {
            if (h.getLevel() > max) max = h.getLevel();
        }
        return max;
    }

    // ============================================================
    // Hero action helpers (inventory/recall/shop/teleport/obstacle)
    // ============================================================

    public static boolean handleInventory(ValorContext ctx, Hero hero) {
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
                case 1: {
                    boolean eqW = equipWeaponForHero(ctx, hero);
                    acted |= eqW;
                    if (eqW) logAction(ctx, hero.getName() + " equipped a weapon.");
                    break;
                }
                case 2: {
                    boolean eqA = equipArmorForHero(ctx, hero);
                    acted |= eqA;
                    if (eqA) logAction(ctx, hero.getName() + " equipped armor.");
                    break;
                }
                case 3: {
                    boolean used = usePotionForHero(ctx, hero);
                    acted |= used;
                    if (used) logAction(ctx, hero.getName() + " used a potion.");
                    break;
                }
                case 4:
                    renderInventory(ctx, hero);
                    break;
                case 5:
                    done = true;
                    break;
                default:
                    ctx.renderer.renderMessage("Invalid choice.");
            }
        }
        return acted;
    }

    public static boolean handleRecall(ValorContext ctx, Hero hero) {
        Position spawn = ctx.heroSpawnPositions.get(hero);
        if (spawn == null) {
            ctx.renderer.renderMessage("No recall position for " + hero.getName() + ".");
            return false;
        }

        Position dest = spawn;
        if (isOccupiedByHero(ctx, spawn, hero)) {
            int lane = ctx.world.laneIndexForCol(spawn.getCol());
            Position alt = findAvailableHeroNexusSlot(ctx, lane, hero);
            if (alt != null) {
                dest = alt;
            } else {
                ctx.renderer.renderMessage("Your lane nexus is fully occupied. Recall failed.");
                return false;
            }
        }

        Position old = ctx.heroPositions.get(hero);
        dest = new Position(dest.getRow(), dest.getCol());
        ctx.heroPositions.put(hero, dest);
        applyTerrainEffects(ctx, hero, old, dest);

        ctx.renderer.renderMessage(hero.getName() + " recalls to their Hero Nexus.");
        logAction(ctx, hero.getName() + " recalled to (" + dest.getRow() + ", " + dest.getCol() + ").");
        return true;
    }

    public static void openShopIfAtHeroNexus(ValorContext ctx, Hero hero) {
        Position pos = ctx.heroPositions.get(hero);
        if (pos == null) return;

        Tile tile = ctx.world.getTile(pos.getRow(), pos.getCol());
        if (tile.getType() != TileType.HERO_NEXUS) {
            ctx.renderer.renderMessage("You must be standing on a Hero Nexus to shop.");
            return;
        }

        if (!(tile instanceof MarketTile)) {
            ctx.renderer.renderMessage("This Hero Nexus cannot host a market.");
            return;
        }

        MarketTile mTile = (MarketTile) tile;

        int lane = laneIndexForHero(ctx, hero);
        int laneLevel = getLaneMaxLevel(ctx, lane);

        if (mTile.getMarket() == null) {
            mTile.setMarket(new Market(ctx.itemFactory, laneLevel));
        } else {
            Market market = mTile.getMarket();
            if (laneLevel > market.getBaseLevel()) {
                market.restock(ctx.itemFactory, laneLevel);
                ctx.renderer.renderMessage("The market has refreshed its stock for your lane.");
            }
        }

        List<Hero> singleHeroList = new ArrayList<Hero>();
        singleHeroList.add(hero);
        ctx.marketController.openMarket(mTile.getMarket(), singleHeroList);
        logAction(ctx, hero.getName() + " opened the market.");
    }

    public static boolean handleTeleport(ValorContext ctx, Hero hero) {
        Position heroPos = ctx.heroPositions.get(hero);
        if (heroPos == null) return false;

        List<Hero> candidates = new ArrayList<Hero>();
        for (Hero h : ctx.heroes) {
            if (h == hero) continue;
            if (h.isFainted()) continue;
            candidates.add(h);
        }
        if (candidates.isEmpty()) {
            ctx.renderer.renderMessage("No other heroes to teleport near.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a hero to teleport next to:");
        for (int i = 0; i < candidates.size(); i++) {
            Hero h = candidates.get(i);
            Position hp = ctx.heroPositions.get(h);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + h.getName() +
                    " @ (" + hp.getRow() + "," + hp.getCol() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int choice = ctx.input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= candidates.size()) {
            ctx.renderer.renderMessage("Invalid choice.");
            return false;
        }

        Hero targetHero = candidates.get(choice);
        Position targetPos = ctx.heroPositions.get(targetHero);
        if (targetPos == null) {
            ctx.renderer.renderMessage("Target position invalid.");
            return false;
        }
        if (ctx.world.sameLane(heroPos, targetPos)) {
            ctx.renderer.renderMessage("Must teleport to a different lane.");
            return false;
        }

        List<Position> options = new ArrayList<Position>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = targetPos.getRow() + d[0];
            int nc = targetPos.getCol() + d[1];
            Position dest = new Position(nr, nc);

            if (!isInsideBoard(ctx, nr, nc)) continue;
            if (!ctx.world.isAccessible(dest)) continue;
            if (ctx.world.sameLane(heroPos, dest)) continue;
            if (dest.getRow() < targetPos.getRow()) continue;
            if (isOccupiedByHero(ctx, dest, hero)) continue;
            if (isBehindEnemyInDestination(ctx, dest, true)) continue;
            options.add(dest);
        }

        if (options.isEmpty()) {
            ctx.renderer.renderMessage("No valid teleport destinations.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a teleport destination:");
        for (int i = 0; i < options.size(); i++) {
            Position p = options.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") (" + p.getRow() + "," + p.getCol() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int destChoice = ctx.input.readInt();
        if (destChoice == 0) return false;
        destChoice--;

        if (destChoice < 0 || destChoice >= options.size()) {
            ctx.renderer.renderMessage("Invalid destination.");
            return false;
        }

        Position dest = options.get(destChoice);
        ctx.heroPositions.put(hero, dest);
        applyTerrainEffects(ctx, hero, heroPos, dest);

        ctx.renderer.renderMessage(hero.getName() + " teleports to (" + dest.getRow() + ", " + dest.getCol() + ")");
        logAction(ctx, hero.getName() + " teleported to (" + dest.getRow() + ", " + dest.getCol() + ").");
        return true;
    }

    public static boolean handleRemoveObstacle(ValorContext ctx, Hero hero) {
        Position pos = ctx.heroPositions.get(hero);
        if (pos == null) return false;

        int targetRow = pos.getRow() - 1;
        int targetCol = pos.getCol();

        if (!isInsideBoard(ctx, targetRow, targetCol)) {
            ctx.renderer.renderMessage("No obstacle in front (out of board).");
            return false;
        }

        Tile tile = ctx.world.getTile(targetRow, targetCol);
        if (tile.getType() != TileType.OBSTACLE) {
            ctx.renderer.renderMessage("Front tile is not an obstacle.");
            return false;
        }

        ctx.world.setTile(targetRow, targetCol, new CommonTile(TileType.COMMON));
        ctx.renderer.renderMessage(hero.getName() + " cleared the obstacle ahead.");
        logAction(ctx, hero.getName() + " cleared an obstacle at (" + targetRow + ", " + targetCol + ").");
        return true;
    }

    // ============================================================
    // Monster phase
    // ============================================================

    public static void monsterPhase(ValorContext ctx) {
        for (Monster monster : ctx.monsters) {
            if (monster.isFainted()) continue;

            Position mPos = ctx.monsterPositions.get(monster);
            if (mPos == null) continue;

            Hero target = findHeroInRange(ctx, mPos);
            if (target != null) {
                attackHero(ctx, monster, target);
                if (ctx.gameOver) return;
                continue;
            }

            int newRow = mPos.getRow() + 1;
            int newCol = mPos.getCol();

            if (isInsideBoard(ctx, newRow, newCol)) {
                Position dest = new Position(newRow, newCol);
                if (ctx.world.isAccessible(dest)
                        && !isOccupiedByMonster(ctx, dest)
                        && !wouldMovePastEnemy(ctx, mPos, dest, false)) {

                    ctx.monsterPositions.put(monster, dest);
                    ctx.renderer.renderMessage(monster.getName() +
                            " moves to (" + newRow + ", " + newCol + ").");
                    logAction(ctx, monster.getName() +
                            " moves to (" + newRow + ", " + newCol + ").");
                }
            }

            checkWinLoseConditions(ctx);
            if (ctx.gameOver) return;
        }
    }

    private static Hero findHeroInRange(ValorContext ctx, Position monsterPos) {
        for (Hero h : ctx.heroes) {
            if (h.isFainted()) continue;
            Position hp = ctx.heroPositions.get(h);
            if (hp == null) continue;
            if (isInRange(ctx, monsterPos, hp)) return h;
        }
        return null;
    }

    private static void attackHero(ValorContext ctx, Monster monster, Hero target) {
        int rawDamage = monster.getDamage();
        int reduced = rawDamage - target.getArmorReduction();
        if (reduced < 0) reduced = 0;

        if (target.tryDodge()) {
            ctx.renderer.renderMessage(target.getName() +
                    " dodged the attack from " + monster.getName() + "!");
            logAction(ctx, target.getName() +
                    " dodged the attack from " + monster.getName() + ".");
            return;
        }

        target.takeDamage(reduced);
        ctx.renderer.renderMessage(monster.getName() +
                " attacked " + target.getName() +
                " for " + reduced + " damage.");
        logAction(ctx, monster.getName() +
                " attacked " + target.getName() +
                " for " + reduced + " damage.");

        if (target.isFainted()) {
            ctx.renderer.renderMessage(target.getName() + " has fallen in this round.");
        }
    }

    // ============================================================
    // Cleanup & end-of-round
    // ============================================================

    public static void cleanupPhase(ValorContext ctx) {
        Iterator<Monster> it = ctx.monsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            if (m.isFainted()) {
                it.remove();
                ctx.monsterPositions.remove(m);
                ctx.monsterCodes.remove(m);
            }
        }
    }

    public static void endOfRound(ValorContext ctx) {

        // Respawn fainted heroes at their nexus + full restore
        for (Hero h : ctx.heroes) {
            if (h.isFainted()) {
                Position spawn = ctx.heroSpawnPositions.get(h);
                if (spawn != null) {
                    Position dest = spawn;
                    if (isOccupiedByHero(ctx, spawn, h)) {
                        int lane = ctx.world.laneIndexForCol(spawn.getCol());
                        Position alt = findAvailableHeroNexusSlot(ctx, lane, h);
                        if (alt != null) dest = alt;
                    }
                    dest = new Position(dest.getRow(), dest.getCol());
                    ctx.heroPositions.put(h, dest);
                    applyTerrainEffects(ctx, h, null, dest);
                }
                h.heal(h.getMaxHP());
                h.restoreMana(h.getMaxMana());
                ctx.renderer.renderMessage(h.getName() + " is revived at their Hero Nexus!");
            }
        }

        // 10% recovery for surviving heroes
        for (Hero h : ctx.heroes) {
            if (!h.isFainted()) {
                int healAmount = (int) (h.getMaxHP() * 0.1);
                int manaAmount = (int) (h.getMaxMana() * 0.1);
                h.heal(healAmount);
                h.restoreMana(manaAmount);
            }
        }

        // New monster wave
        if (ctx.roundCount > 0 && ctx.roundCount % ctx.monsterWavePeriod == 0) {
            spawnMonsterWave(ctx);
            logAction(ctx, "A new wave of monsters appeared.");
        }

        // Round summary
        ctx.renderer.renderMessage("------------------------------------------------------------");
        ctx.renderer.renderMessage("Round Info");
        ctx.renderer.renderMessage("------------------------------------------------------------");
        for (String line : ctx.roundLog) {
            ctx.renderer.renderMessage(line);
        }
        ctx.renderer.renderMessage("------------------------------------------------------------");

        checkWinLoseConditions(ctx);
    }

    // ============================================================
    // Win/lose and waves
    // ============================================================

    public static void checkWinLoseConditions(ValorContext ctx) {
        // Heroes win
        for (Map.Entry<Hero, Position> e : ctx.heroPositions.entrySet()) {
            Hero h = e.getKey();
            if (h.isFainted()) continue;
            Position p = e.getValue();
            if (p == null) continue;
            TileType t = ctx.world.getTile(p.getRow(), p.getCol()).getType();
            if (t == TileType.MONSTER_NEXUS) {
                ctx.renderer.renderMessage(h.getName() + " has reached the Monster Nexus! Heroes win!");
                ctx.gameOver = true;
                return;
            }
        }

        // Monsters win
        for (Map.Entry<Monster, Position> e : ctx.monsterPositions.entrySet()) {
            Monster m = e.getKey();
            if (m.isFainted()) continue;
            Position p = e.getValue();
            if (p == null) continue;
            TileType t = ctx.world.getTile(p.getRow(), p.getCol()).getType();
            if (t == TileType.HERO_NEXUS) {
                ctx.renderer.renderMessage(m.getName() + " has reached the Hero Nexus! Monsters win!");
                ctx.gameOver = true;
                return;
            }
        }
    }

    private static void spawnMonsterWave(ValorContext ctx) {
        ctx.renderer.renderMessage("A new wave of monsters appears!");

        int level = maxHeroLevel(ctx);
        List<Monster> spawned = ctx.monsterFactory.spawnMonstersForBattle(3, level);

        int[][] laneCols = {{0,1},{3,4},{6,7}};
        int idx = 0;

        for (int lane = 0; lane < laneCols.length && idx < spawned.size(); lane++) {
            for (int c = 0; c < laneCols[lane].length && idx < spawned.size(); c++) {
                int col = laneCols[lane][c];
                Position nexusPos = new Position(0, col);

                if (isOccupiedByMonster(ctx, nexusPos)) continue;

                Monster m = spawned.get(idx++);
                ctx.monsters.add(m);
                ctx.monsterPositions.put(m, nexusPos);
                break;
            }
        }
        assignMonsterCodes(ctx);
    }

    // ============================================================
    // Inventory helpers
    // ============================================================

    private static void renderInventory(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        if (items.isEmpty()) {
            ctx.renderer.renderMessage(hero.getName() + " has no items.");
            return;
        }
        ctx.renderer.renderMessage(hero.getName() + "'s inventory:");
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " +
                    it.getName() + " (Lv " + it.getRequiredLevel() +
                    ", Price " + it.getPrice() + ")");
        }
    }

    private static boolean equipWeaponForHero(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Weapon> weapons = new ArrayList<Weapon>();
        for (Item item : items) {
            if (item instanceof Weapon) weapons.add((Weapon) item);
        }
        if (weapons.isEmpty()) {
            ctx.renderer.renderMessage("No weapons available to equip.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a weapon to equip:");
        for (int i = 0; i < weapons.size(); i++) {
            Weapon w = weapons.get(i);
            String handsText = (w.getHandsRequired() == 2 ? "2H" : "1H");
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + w.getName() +
                    " (Damage " + w.getDamage() +
                    ", Req Lv " + w.getRequiredLevel() +
                    ", " + handsText + ")");
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
        ctx.renderer.renderMessage(hero.getName() + " equipped weapon: " +
                selected.getName() + (useTwoHands ? " (using both hands)" : ""));
        updateLaneMaxLevels(ctx);
        return true;
    }

    private static boolean equipArmorForHero(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Armor> armors = new ArrayList<Armor>();
        for (Item item : items) {
            if (item instanceof Armor) armors.add((Armor) item);
        }
        if (armors.isEmpty()) {
            ctx.renderer.renderMessage("No armor available to equip.");
            return false;
        }

        ctx.renderer.renderMessage("Choose armor to equip:");
        for (int i = 0; i < armors.size(); i++) {
            Armor a = armors.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + a.getName() +
                    " (Reduction " + a.getDamageReduction() +
                    ", Req Lv " + a.getRequiredLevel() + ")");
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
        updateLaneMaxLevels(ctx);
        return true;
    }

    private static boolean usePotionForHero(ValorContext ctx, Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Potion> potions = new ArrayList<Potion>();
        for (Item item : items) {
            if (item instanceof Potion) potions.add((Potion) item);
        }
        if (potions.isEmpty()) {
            ctx.renderer.renderMessage("No potions available.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a potion to use:");
        for (int i = 0; i < potions.size(); i++) {
            Potion p = potions.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + p.getName() +
                    " (Effect " + p.getAmount() +
                    " on " + p.getStatType() +
                    ", Req Lv " + p.getRequiredLevel() + ")");
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

    // ============================================================
    // Terrain effects
    // ============================================================

    private static void clearTerrainEffects(ValorContext ctx, Hero hero) {
        TileType prevType = ctx.activeTerrainBuff.remove(hero);
        int[] delta = ctx.terrainBuffDeltas.remove(hero);
        if (prevType != null && delta != null) {
            hero.adjustStrength(-delta[0]);
            hero.adjustDexterity(-delta[1]);
            hero.adjustAgility(-delta[2]);
        }
    }

    public static void applyTerrainEffects(ValorContext ctx, Hero hero, Position from, Position to) {
        clearTerrainEffects(ctx, hero);
        if (to == null) return;

        TileType type = ctx.world.getTile(to.getRow(), to.getCol()).getType();
        int str = 0, dex = 0, agi = 0;

        switch (type) {
            case BUSH:
                dex = Math.max(1, hero.getDexterity() / 10);
                hero.adjustDexterity(dex);
                break;
            case CAVE:
                agi = Math.max(1, hero.getAgility() / 10);
                hero.adjustAgility(agi);
                break;
            case KOULOU:
                str = Math.max(1, hero.getStrength() / 10);
                hero.adjustStrength(str);
                break;
            default:
                return;
        }

        ctx.activeTerrainBuff.put(hero, type);
        ctx.terrainBuffDeltas.put(hero, new int[]{str, dex, agi});
    }
}
