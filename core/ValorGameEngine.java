package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import battle.Battle;
import battle.BattleSupport;
import battle.CombatResolver;
import battle.RewardService;
import characters.Hero;
import characters.Monster;
import config.GameBalance;
import data.MonsterFactory;
import data.ItemFactory;
import io.InputHandler;
import io.Renderer;
import items.Armor;
import items.Item;
import items.Potion;
import items.Spell;
import items.Weapon;
import market.Market;
import market.MarketController;
import world.CommonTile;
import world.MarketTile;
import world.Tile;
import world.TileType;
import world.World;

/**
 * Legends of Valor game loop.
 * Uses World in "Valor" mode, heroes & monsters on lanes, and an
 * melee range of same tile + 8 neighbors (lane-restricted).
 */
public class ValorGameEngine implements Battle {

    // How often to spawn a new wave of monsters
    private static final int MONSTER_WAVE_PERIOD = 6;

    private final World world;
    private final List<Hero> heroes;
    private final Map<Hero, Position> heroPositions;
    private final Map<Hero, Position> heroSpawnPositions;
    private final Map<Hero, TileType> activeTerrainBuff;
    private final Map<Hero, int[]> terrainBuffDeltas;

    private final List<Monster> monsters;
    private final Map<Monster, Position> monsterPositions;
    private final Map<Monster, String> monsterCodes = new HashMap<Monster, String>();

    private final Renderer renderer;
    private final InputHandler input;
    private final MarketController marketController;
    private final MonsterFactory monsterFactory;
    private final ItemFactory itemFactory;
    private final Random random;

    private int roundCount;
    private boolean gameOver;
    private final Map<Hero, String> heroCodes = new HashMap<Hero, String>();
    private final List<String> roundLog = new ArrayList<String>();

    public ValorGameEngine(World world,
                           List<Hero> heroes,
                           Renderer renderer,
                           InputHandler input) {
        this.world = world;
        this.heroes = heroes;
        this.renderer = renderer;
        this.input = input;

        this.heroPositions = new HashMap<Hero, Position>();
        this.heroSpawnPositions = new HashMap<Hero, Position>();
        this.activeTerrainBuff = new HashMap<Hero, TileType>();
        this.terrainBuffDeltas = new HashMap<Hero, int[]>();
        this.monsters = new ArrayList<Monster>();
        this.monsterPositions = new HashMap<Monster, Position>();
        this.monsterCodes.clear();

        this.marketController = new MarketController(renderer, input);
        this.monsterFactory = new MonsterFactory();
        this.itemFactory = new ItemFactory();
        this.random = new Random();

        this.roundCount = 1;
        this.gameOver = false;

        initializeHeroPositions();
        assignHeroCodes();
        spawnInitialMonsters();
    }

    // --------------------------------------------------------
    // Initialization
    // --------------------------------------------------------

    private void initHeroPositions() {
        // We assume 8x8 Valor world:
        // row 7 (bottom) = HERO_NEXUS (except col 2 and 5)
        // columns: lane0 => 0,1 ; lane1 => 3,4 ; lane2 => 6,7
        // We place heroes at (7,0), (7,3), (7,6) in order.
        int size = world.getSize();
        int[][] spawnCoords = {
                { size - 1, 0 },
                { size - 1, 3 },
                { size - 1, 6 }
        };

        for (int i = 0; i < heroes.size() && i < spawnCoords.length; i++) {
            Hero h = heroes.get(i);
            int r = spawnCoords[i][0];
            int c = spawnCoords[i][1];
            Position pos = new Position(r, c);
            heroPositions.put(h, pos);
            heroSpawnPositions.put(h, new Position(r, c));
        }
    }

    public void run() {
        renderer.renderMessage("Starting Legends of Valor!");

        while (!gameOver) {
            roundLog.clear();
            // 1. (map rendering skipped at round start)

            // 2. Hero phase
            heroPhase();
            if (gameOver) break;

    private void spawnInitialMonsters() {
        int avgLevel = averageHeroLevel();
        List<Monster> initial = monsterFactory.spawnMonstersForBattle(3, avgLevel);

        if (initial.isEmpty()) {
            renderer.renderMessage("DEBUG: MonsterFactory returned no monsters!");
            return;
        }

        // Find one Monster Nexus tile per lane by scanning row 0
        int size = world.getSize();
        Position[] laneSpawns = new Position[3]; // lane 0,1,2

        for (int c = 0; c < size; c++) {
            Tile t = world.getTile(0, c);
            if (t.getType() != TileType.MONSTER_NEXUS) continue;

            Position p = new Position(0, c);
            int lane = laneIndex(p); // reuse your laneIndex method
            if (lane >= 0 && lane < 3 && laneSpawns[lane] == null) {
                laneSpawns[lane] = p;
            }
        }

        // Fallback: if any lane is missing, just skip that lane
        for (int i = 0; i < initial.size() && i < laneSpawns.length; i++) {
            if (laneSpawns[i] == null) {
                renderer.renderMessage("DEBUG: No Monster Nexus found for lane " + i);
                continue;
            }
            Monster m = initial.get(i);
            Position spawnPos = laneSpawns[i];

    // ------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------

    private void initializeHeroPositions() {
        // Use the Valor hero positions already provided by World
        Position[] starts = world.getValorHeroPosition();
        // Defensive: if null or wrong size, fallback
        if (starts == null || starts.length == 0) {
            // Default to bottom row lanes 0,3,6
            starts = new Position[] {
                    new Position(world.getSize() - 1, 0),
                    new Position(world.getSize() - 1, 3),
                    new Position(world.getSize() - 1, 6)
            };
        }

        int count = Math.min(heroes.size(), starts.length);
        for (int i = 0; i < count; i++) {
            Hero h = heroes.get(i);
            Position p = new Position(starts[i].getRow(), starts[i].getCol());
            heroPositions.put(h, p);
            heroSpawnPositions.put(h, new Position(p.getRow(), p.getCol()));
            applyTerrainEffects(h, null, p);
        }
    }

    private void spawnInitialMonsters() {
        int level = maxHeroLevel();
        List<Monster> spawned = monsterFactory.spawnMonstersForBattle(3, level);

        int[][] laneCols = {{0,1},{3,4},{6,7}};
        int idx = 0;
        for (int lane = 0; lane < laneCols.length && idx < spawned.size(); lane++) {
            for (int c = 0; c < laneCols[lane].length && idx < spawned.size(); c++) {
                int col = laneCols[lane][c];
                Position pos = new Position(0, col);
                if (isOccupiedByMonster(pos)) continue;
                Monster m = spawned.get(idx++);
                monsters.add(m);
                monsterPositions.put(m, pos);
                break;
            }
        }
        assignMonsterCodes();
    }

    // ------------------------------------------------------------
    // Hero Phase
    // ------------------------------------------------------------

    private void heroPhase() {
        assignHeroCodes();
        for (Hero hero : heroes) {
            if (hero.isFainted()) {
                continue;
            }
            if (!heroPositions.containsKey(hero)) {
                continue;
            }

            boolean actionTaken = false;
            boolean showBoard = true;
            while (!actionTaken && !gameOver) {
                renderHeroTurnMenu(hero, showBoard);
                int choice = input.readInt();
                switch (choice) {
                    case 1:
                        actionTaken = handleMove(hero);
                        break;
                    case 2:
                        actionTaken = handleAttack(hero);
                        break;
                    case 3:
                        actionTaken = handleCastSpell(hero);
                        break;
                    case 4:
                        actionTaken = handleInventory(hero);
                        break;
                    case 5:
                        actionTaken = handleRecall(hero);
                        break;
                    case 6:
                        openShopIfAtHeroNexus(hero); // free action
                        break;
                    case 7:
                        actionTaken = handleTeleport(hero);
                        break;
                    case 8:
                        actionTaken = handleRemoveObstacle(hero);
                        break;
                    case 9:
                        renderer.renderHeroStats(heroes, heroCodes);
                        renderer.renderMonsterStats(monsters, monsterCodes);
                        break;
                    case 10:
                        renderer.renderMessage(hero.getName() + " skips the turn.");
                        actionTaken = true;
                        break;
                    default:
                        renderer.renderMessage("Invalid choice.");
                        break;
                }
                // After each hero action, check immediate win condition
                checkWinLoseConditions();
                if (!actionTaken) {
                    showBoard = false; // on failure, do not redraw board next loop
                }
            }
            if (gameOver) return;
        }
    }

    private boolean handleMove(Hero hero) {
        Position pos = heroPositions.get(hero);
        if (pos == null) {
            return false;
        }

        renderer.renderMessage("Use W/A/S/D to move.");
        Direction dir = input.readMovement();

        int newRow = pos.getRow();
        int newCol = pos.getCol();
        switch (dir) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
            default: break;
        }

        if (!isInsideBoard(newRow, newCol)) {
            renderer.renderMessage("Cannot move outside the board.");
            return false;
        }

        Tile tile = world.getTile(newRow, newCol);
        if (!tile.isAccessible()) {
            renderer.renderMessage("That tile is not accessible.");
            return false;
        }

        Position dest = new Position(newRow, newCol);

        if (isOccupiedByHero(dest, hero)) {
            renderer.renderMessage("Another hero is already there.");
            return false;
        }

        // Prevent moving past an enemy in the same lane
        if (wouldMovePastEnemy(pos, dest, true)) {
            renderer.renderMessage("You cannot move past a monster in your lane.");
            return false;
        }

        heroPositions.put(hero, dest);
        applyTerrainEffects(hero, pos, dest);
        renderer.renderMessage(hero.getName() + " moved to (" + newRow + ", " + newCol + ").");
        logAction(hero.getName() + " moved to (" + newRow + ", " + newCol + ").");
        return true;
    }

    private boolean handleAttack(Hero hero) {
        Position heroPos = heroPositions.get(hero);
        if (heroPos == null) {
            return false;
        }

        List<Monster> targetsInRange = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (m.isFainted()) continue;
            Position mp = monsterPositions.get(m);
            if (mp == null) continue;
            if (isInRange(heroPos, mp)) {
                targetsInRange.add(m);
            }
        }

        if (targetsInRange.isEmpty()) {
            renderer.renderMessage("No monsters in range (same tile or adjacent required).");
            return false;
        }

        renderer.renderMessage("Choose a monster to attack:");
        for (int i = 0; i < targetsInRange.size(); i++) {
            Monster m = targetsInRange.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() + " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= targetsInRange.size()) {
            renderer.renderMessage("Invalid target.");
            return false;
        }

        Monster target = targetsInRange.get(choice);
        int baseDamage = hero.basicAttackDamage();

        int hpBefore = target.getHP();
        target.takeDamage(baseDamage); // Monster handles defense internally
        int hpAfter = target.getHP();
        int effective = hpBefore - hpAfter;
        if (effective < 0) effective = 0;

        renderer.renderMessage(hero.getName() +
                " attacked " + target.getName() +
                " for " + effective + " damage.");
        logAction(hero.getName() + " attacked " + target.getName() + " for " + effective + " damage.");

        if (target.isFainted()) {
            renderer.renderMessage(target.getName() + " has been defeated!");
            // Simple XP reward: proportional to monster level
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL;
            hero.gainExperience(xp);
            int gold = target.getLevel() * 500;
            hero.addGold(gold);
            renderer.renderMessage(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
            logAction(target.getName() + " defeated by " + hero.getName() + " (+" + xp + " XP, +" + gold + " gold).");
        }
        return true;
    }

    private boolean handleCastSpell(Hero hero) {
        Position heroPos = heroPositions.get(hero);
        if (heroPos == null) {
            return false;
        }

        List<Item> items = hero.getInventory().getItems();
        List<Spell> spells = new ArrayList<Spell>();
        for (Item item : items) {
            if (item instanceof Spell) {
                spells.add((Spell) item);
            }
        }

        if (spells.isEmpty()) {
            renderer.renderMessage(hero.getName() + " has no spells.");
            return false;
        }

        renderer.renderMessage("Choose a spell to cast:");
        for (int i = 0; i < spells.size(); i++) {
            Spell s = spells.get(i);
            String effectDesc = (s.getEffect() != null) ? s.getEffect().describe() : "";
            renderer.renderMessage("  " + (i + 1) + ") " + s.getName() +
                    " | Dmg: " + s.getBaseDamage() +
                    " | MP: " + s.getManaCost() +
                    " | Effect: " + effectDesc);
        }
        renderer.renderMessage("  0) Back");

        int spellChoice = input.readInt();
        if (spellChoice == 0) return false;
        spellChoice--;

        if (spellChoice < 0 || spellChoice >= spells.size()) {
            renderer.renderMessage("Invalid spell choice.");
            return false;
        }

        Spell spell = spells.get(spellChoice);

        if (hero.getMana() < spell.getManaCost()) {
            renderer.renderMessage("Not enough mana.");
            return false;
        }

        // Monsters in range (adjacent tiles only)
        List<Monster> targetsInRange = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (m.isFainted()) continue;
            Position mp = monsterPositions.get(m);
            if (mp == null) continue;
            if (isInRange(heroPos, mp)) {
                targetsInRange.add(m);
            }
        }

        if (targetsInRange.isEmpty()) {
            renderer.renderMessage("No monsters in range for the spell (same tile or adjacent required).");
            return false;
        }

        renderer.renderMessage("Choose a monster to target with " + spell.getName() + ":");
        for (int i = 0; i < targetsInRange.size(); i++) {
            Monster m = targetsInRange.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() +
                    " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        renderer.renderMessage("  0) Back");

        int targetChoice = input.readInt();
        if (targetChoice == 0) return false;
        targetChoice--;

        if (targetChoice < 0 || targetChoice >= targetsInRange.size()) {
            renderer.renderMessage("Invalid target choice.");
            return false;
        }

        Monster target = targetsInRange.get(targetChoice);

        // Spend mana and cast
        hero.restoreMana(-spell.getManaCost());
        TileType tileType = world.getTile(heroPos.getRow(), heroPos.getCol()).getType();
        int effective = CombatResolver.computeSpellDamage(hero, target, spell, tileType);

        renderer.renderMessage(hero.getName() + " casts " +
                spell.getName() + " on " + target.getName() +
                " for " + effective + " damage (raw: " + rawDealt + ").");
        logAction(hero.getName() + " casts " + spell.getName() +
                " on " + target.getName() + " for " + effective + " damage.");

        // Single-use spell
        hero.getInventory().remove(spell);

        if (target.isFainted()) {
            renderer.renderMessage(target.getName() + " has been defeated!");
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL;
            hero.gainExperience(xp);
            int gold = target.getLevel() * 500;
            hero.addGold(gold);
            renderer.renderMessage(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
            logAction(target.getName() + " defeated by " + hero.getName() + " (+" + xp + " XP, +" + gold + " gold).");
        }
        return true;
    }

    private boolean handleInventory(Hero hero) {
        boolean acted = false;
        boolean done = false;
        while (!done) {
            renderer.renderMessage("Manage " + hero.getName() + ":");
            renderer.renderMessage("  1) Equip weapon");
            renderer.renderMessage("  2) Equip armor");
            renderer.renderMessage("  3) Use potion");
            renderer.renderMessage("  4) View inventory");
            renderer.renderMessage("  5) Back");

            int choice = input.readInt();
            switch (choice) {
                case 1:
                    boolean eqW = equipWeaponForHero(hero);
                    acted |= eqW;
                    if (eqW) logAction(hero.getName() + " equipped a weapon.");
                    break;
                case 2:
                    boolean eqA = equipArmorForHero(hero);
                    acted |= eqA;
                    if (eqA) logAction(hero.getName() + " equipped armor.");
                    break;
                case 3:
                    boolean used = usePotionForHero(hero);
                    acted |= used;
                    if (used) logAction(hero.getName() + " used a potion.");
                    break;
                case 4:
                    renderInventory(hero);
                    break;
                case 5:
                    done = true;
                    break;
                default:
                    renderer.renderMessage("Invalid choice.");
            }
        }
        return acted;
    }

    private boolean handleRecall(Hero hero) {
        Position spawn = heroSpawnPositions.get(hero);
        if (spawn == null) {
            renderer.renderMessage("No recall position for " + hero.getName() + ".");
            return false;
        }
        Position dest = spawn;
        if (isOccupiedByHero(spawn, hero)) {
            int lane = world.laneIndexForCol(spawn.getCol());
            Position alt = findAvailableHeroNexusSlot(lane, hero);
            if (alt != null) {
                dest = alt;
            } else {
                renderer.renderMessage("Your lane nexus is fully occupied. Recall failed.");
                return false;
            }
        }
        Position old = heroPositions.get(hero);
        dest = new Position(dest.getRow(), dest.getCol());
        heroPositions.put(hero, dest);
        applyTerrainEffects(hero, old, dest);
        renderer.renderMessage(hero.getName() + " recalls to their Hero Nexus.");
        logAction(hero.getName() + " recalled to (" + dest.getRow() + ", " + dest.getCol() + ").");
        return true;
    }

    private void openShopIfAtHeroNexus(Hero hero) {
        Position pos = heroPositions.get(hero);
        if (pos == null) return;

        Tile tile = world.getTile(pos.getRow(), pos.getCol());
        if (tile.getType() != TileType.HERO_NEXUS) {
            renderer.renderMessage("You must be standing on a Hero Nexus to shop.");
            return;
        }

        Position dest = possible.get(destChoice);
        heroPositions.put(h, dest);
        renderer.renderMessage(h.getName() + " teleports to (" +
                dest.getRow() + ", " + dest.getCol() + ").");
    }

    private void handleRecall(Hero h) {
        Position spawn = heroSpawnPositions.get(h);
        if (spawn == null) {
            renderer.renderMessage("No Nexus spawn position for " + h.getName() + ".");
            return;
        }
        heroPositions.put(h, new Position(spawn.getRow(), spawn.getCol()));
        renderer.renderMessage(h.getName() + " recalls to their Nexus at (" +
                spawn.getRow() + ", " + spawn.getCol() + ").");
    }

    private void openShopIfAtHeroNexus(Hero h) {
        Position p = heroPositions.get(h);
        if (p == null) return;
        Tile tile = world.getTile(p.getRow(), p.getCol());
        if (tile.getType() != TileType.HERO_NEXUS) {
            renderer.renderMessage("You must stand on your Nexus to shop.");
            return;
        }

        // You can let the whole party shop, or just this hero. Reuse existing API:
        List<Hero> singleHeroList = new ArrayList<Hero>();
        singleHeroList.add(hero);
        marketController.openMarket(mTile.getMarket(), singleHeroList);
        logAction(hero.getName() + " opened the market.");
    }

    private boolean handleTeleport(Hero hero) {
        Position heroPos = heroPositions.get(hero);
        if (heroPos == null) return false;

        List<Hero> candidates = new ArrayList<Hero>();
        for (Hero h : heroes) {
            if (h == hero) continue;
            if (h.isFainted()) continue;
            candidates.add(h);
        }
        if (candidates.isEmpty()) {
            renderer.renderMessage("No other heroes to teleport near.");
            return false;
        }

        renderer.renderMessage("Choose a hero to teleport next to:");
        for (int i = 0; i < candidates.size(); i++) {
            Hero h = candidates.get(i);
            Position hp = heroPositions.get(h);
            renderer.renderMessage("  " + (i + 1) + ") " + h.getName() +
                    " @ (" + hp.getRow() + "," + hp.getCol() + ")");
        }
        renderer.renderMessage("  0) Back");
        int choice = input.readInt();
        if (choice == 0) return false;
        choice--;
        if (choice < 0 || choice >= candidates.size()) {
            renderer.renderMessage("Invalid choice.");
            return false;
        }

        Hero targetHero = candidates.get(choice);
        Position targetPos = heroPositions.get(targetHero);
        if (targetPos == null) {
            renderer.renderMessage("Target position invalid.");
            return false;
        }
        if (world.sameLane(heroPos, targetPos)) {
            renderer.renderMessage("Must teleport to a different lane.");
            return false;
        }

        List<Position> options = new ArrayList<Position>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) {
                int nr = targetPos.getRow() + d[0];
                int nc = targetPos.getCol() + d[1];
                Position dest = new Position(nr, nc);
                if (!isInsideBoard(nr, nc)) continue;
                if (!world.isAccessible(dest)) continue;
                if (world.sameLane(heroPos, dest)) continue; // must change lane
                // Destination cannot be ahead of the target hero
                if (dest.getRow() < targetPos.getRow()) continue;
                if (isOccupiedByHero(dest, hero)) continue;
                if (isBehindEnemyInDestination(dest, true)) continue;
                options.add(dest);
            }

        if (options.isEmpty()) {
            renderer.renderMessage("No valid teleport destinations.");
            return false;
        }

        renderer.renderMessage("Choose a teleport destination:");
        for (int i = 0; i < options.size(); i++) {
            Position p = options.get(i);
            renderer.renderMessage("  " + (i + 1) + ") (" + p.getRow() + "," + p.getCol() + ")");
        }
        renderer.renderMessage("  0) Back");
        int destChoice = input.readInt();
        if (destChoice == 0) return false;
        destChoice--;
        if (destChoice < 0 || destChoice >= options.size()) {
            renderer.renderMessage("Invalid destination.");
            return false;
        }

        Position dest = options.get(destChoice);
        heroPositions.put(hero, dest);
        applyTerrainEffects(hero, heroPos, dest);
        renderer.renderMessage(hero.getName() + " teleports to (" + dest.getRow() + ", " + dest.getCol() + ")");
        logAction(hero.getName() + " teleported to (" + dest.getRow() + ", " + dest.getCol() + ").");
        return true;
    }

    private boolean handleRemoveObstacle(Hero hero) {
        Position pos = heroPositions.get(hero);
        if (pos == null) return false;
        int targetRow = pos.getRow() - 1; // forward toward monster nexus
        int targetCol = pos.getCol();
        if (!isInsideBoard(targetRow, targetCol)) {
            renderer.renderMessage("No obstacle in front (out of board).");
            return false;
        }
        Tile tile = world.getTile(targetRow, targetCol);
        if (tile.getType() != TileType.OBSTACLE) {
            renderer.renderMessage("Front tile is not an obstacle.");
            return false;
        }
        world.setTile(targetRow, targetCol, new CommonTile(TileType.COMMON));
        renderer.renderMessage(hero.getName() + " cleared the obstacle ahead.");
        logAction(hero.getName() + " cleared an obstacle at (" + targetRow + ", " + targetCol + ").");
        return true;
    }

    private void renderHeroTurnMenu(Hero hero, boolean showBoard) {
        if (showBoard) {
            renderer.renderWorld(world, heroPositions, monsterPositions, heroCodes, monsterCodes);
        }
        String code = heroCodes.get(hero);
        if (code == null) {
            code = "h?";
        }
        StringBuilder title = new StringBuilder();
        title.append(hero.getName()).append(" (").append(colorize(code, "\u001B[36m")).append(")");
        String terrainDesc = terrainBuffDescription(hero);
        if (!terrainDesc.isEmpty()) {
            title.append(", ").append(terrainDesc);
        }
        title.append(", choose your action:");
        renderer.renderMessage(title.toString());
        renderer.renderMessage("  1) Move");
        renderer.renderMessage("  2) Attack");
        renderer.renderMessage("  3) Cast Spell");
        renderer.renderMessage("  4) Inventory");
        renderer.renderMessage("  5) Recall");
        renderer.renderMessage("  6) Shop (free, if at Hero Nexus)");
        renderer.renderMessage("  7) Teleport");
        renderer.renderMessage("  8) Remove Obstacle");
        renderer.renderMessage("  9) View Party/Monsters (free)");
        renderer.renderMessage("  10) Skip");
    }

    private void logAction(String msg) {
        roundLog.add(msg);
    }

    private void assignHeroCodes() {
        heroCodes.clear();
        int idx = 1;
        for (Hero h : heroes) {
            heroCodes.put(h, "h" + idx++);
        }
    }

    private void assignMonsterCodes() {
        monsterCodes.clear();
        int idx = 1;
        for (Monster m : monsters) {
            monsterCodes.put(m, "m" + idx++);
        }
    }

    private String terrainBuffDescription(Hero hero) {
        Position pos = heroPositions.get(hero);
        if (pos == null) return "";
        Tile tile = world.getTile(pos.getRow(), pos.getCol());
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

    private String colorize(String text, String color) {
        return color + text + "\u001B[0m";
    }

    // --------------------------------------------------------
    // Monster phase + pathfinding
    // --------------------------------------------------------

    private void monsterPhase() {
        for (Monster monster : monsters) {
            if (monster.isFainted()) continue;
            Position mPos = monsterPositions.get(monster);
            if (mPos == null) continue;

            // 1. If any hero in adjacent tile -> attack
            Hero targetInRange = findHeroInRange(mPos);
            if (targetInRange != null) {
                attackHero(monster, targetInRange);
                if (gameOver) return;
                continue;
            }

            // 2. Otherwise, move one row closer to hero nexus (DOWN in its lane)
            int row = mPos.getRow();
            int col = mPos.getCol();
            int newRow = row + 1;
            int newCol = col;

            if (isInsideBoard(newRow, newCol)) {
                Tile destTile = world.getTile(newRow, newCol);
                Position dest = new Position(newRow, newCol);

                if (destTile.isAccessible() &&
                        !isOccupiedByMonster(dest) &&
                        !wouldMovePastEnemy(mPos, dest, false)) {
                    monsterPositions.put(monster, dest);
                    renderer.renderMessage(monster.getName() + " moves to (" +
                            newRow + ", " + newCol + ").");
                    logAction(monster.getName() + " moves to (" + newRow + ", " + newCol + ").");
                }
            }

            // Otherwise, try to pathfind toward hero Nexus in lane
            Position next = computeMonsterStep(m);
            if (next != null) {
                monsterPositions.put(m, next);
            }
        }
    }

    private Hero findHeroInRange(Position monsterPos) {
        for (Hero h : heroes) {
            if (h.isFainted()) continue;
            Position hp = heroPositions.get(h);
            if (hp == null) continue;
            if (isInRange(monsterPos, hp)) {
                return h;
            }
        }
        return null;
    }

    private void attackHero(Monster monster, Hero target) {
        int rawDamage = monster.getDamage();
        int reducedDamage = rawDamage - target.getArmorReduction();
        if (reducedDamage < 0) reducedDamage = 0;

        if (target.tryDodge()) {
            renderer.renderMessage(target.getName() +
                    " dodged the attack from " + monster.getName() + "!");
            logAction(target.getName() + " dodged the attack from " + monster.getName() + ".");
            return;
        }

        target.takeDamage(reducedDamage);
        renderer.renderMessage(monster.getName() + " attacked " +
                target.getName() + " for " + reducedDamage + " damage.");
        logAction(monster.getName() + " attacked " + target.getName() + " for " + reducedDamage + " damage.");

        if (target.isFainted()) {
            renderer.renderMessage(target.getName() + " has fallen!");
        }
    }

    /**
     * Monster pathfinding:
     *  - stays in its lane (2 columns)
     *  - BFS toward its lane's hero Nexus
     *  - cannot move onto heroes or monsters
     *  - cannot move behind a hero in that lane (cannot go further toward hero Nexus)
     */
    private Position computeMonsterStep(Monster m) {
        Position start = monsterPositions.get(m);
        if (start == null) return null;

        int lane = laneIndex(start);
        if (lane == -1) return null;

        // Monsters move "down" (toward hero Nexus), then maybe sidestep in-lane
        int[][] candidates = {
                { 1, 0 },  // straight down
                { 0, -1 }, // left within lane
                { 0, 1 }   // right within lane
        };

        for (int[] d : candidates) {
            int nr = start.getRow() + d[0];
            int nc = start.getCol() + d[1];
            if (isValidMonsterStep(m, start, nr, nc)) {
                return new Position(nr, nc);
            }
        }

        // No valid move
        return null;
    }

    private Position heroNexusForLane(int lane) {
        int size = world.getSize();
        switch (lane) {
            case 0: return new Position(size - 1, 0);
            case 1: return new Position(size - 1, 3);
            case 2: return new Position(size - 1, 6);
        }
        return new Position(size - 1, 0);
    }

    private boolean isValidMonsterStep(Monster m, Position from, int nr, int nc) {
        int size = world.getSize();
        if (nr < 0 || nr >= size || nc < 0 || nc >= size) return false;

    private void cleanupPhase() {
        // Remove dead monsters from list + position map
        Iterator<Monster> it = monsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            if (m.isFainted()) {
                it.remove();
                monsterPositions.remove(m);
                monsterCodes.remove(m);
            }
        }
    }

    private void endOfRound() {
        // Respawn fainted heroes at their nexus
        for (Hero h : heroes) {
            if (h.isFainted()) {
                Position spawn = heroSpawnPositions.get(h);
                if (spawn != null) {
                    Position dest = spawn;
                    if (isOccupiedByHero(spawn, h)) {
                        int lane = world.laneIndexForCol(spawn.getCol());
                        Position alt = findAvailableHeroNexusSlot(lane, h);
                        if (alt != null) {
                            dest = alt;
                        }
                    }
                    dest = new Position(dest.getRow(), dest.getCol());
                    heroPositions.put(h, dest);
                    applyTerrainEffects(h, null, dest);
                }
                // Fully restore HP & Mana
                h.heal(h.getMaxHP());
                h.restoreMana(h.getMaxMana());
                renderer.renderMessage(h.getName() + " is revived at their Hero Nexus!");
                logAction(h.getName() + " revived at (" + heroPositions.get(h).getRow() + ", " + heroPositions.get(h).getCol() + ").");
            }
        }

        // End-of-round recovery for living heroes
        BattleSupport.recoverHeroesEndOfRound(heroes, GameBalance.END_OF_ROUND_RECOVER_FRACTION);

        // Simple recovery for monsters mirrors heroes' fraction
        for (Monster m : monsters) {
            if (!m.isFainted()) {
                int healAmount = (int) (m.getMaxHP() * GameBalance.END_OF_ROUND_RECOVER_FRACTION);
                m.heal(healAmount);
            }
        }
    }

        // New monster wave every MONSTER_WAVE_PERIOD rounds
        if (roundCount > 0 && roundCount % MONSTER_WAVE_PERIOD == 0) {
            spawnMonsterWave();
            logAction("A new wave of monsters appeared.");
        }

        // Round summary
        renderer.renderMessage("------------------------------------------------------------");
        renderer.renderMessage("Round Info");
        renderer.renderMessage("------------------------------------------------------------");
        for (String line : roundLog) {
            renderer.renderMessage(line);
        }
        renderer.renderMessage("------------------------------------------------------------");

        // Check win/lose at the end of the round as well
        checkWinLoseConditions();
    }

    private void spawnMonsterWave() {
        renderer.renderMessage("A new wave of monsters appears!");

        int level = maxHeroLevel();
        List<Monster> spawned = monsterFactory.spawnMonstersForBattle(3, level);

        int[][] laneCols = {{0,1},{3,4},{6,7}};
        int idx = 0;

        for (int lane = 0; lane < laneCols.length && idx < spawned.size(); lane++) {
            for (int c = 0; c < laneCols[lane].length && idx < spawned.size(); c++) {
                int col = laneCols[lane][c];
                Position nexusPos = new Position(0, col);

                // Only spawn if nexus is not currently occupied by a monster
                if (isOccupiedByMonster(nexusPos)) {
                    continue;
                }

                Monster m = spawned.get(idx++);
                monsters.add(m);
                monsterPositions.put(m, nexusPos);
                break;
            }
        }
        assignMonsterCodes();
    }

    private void checkWinLose() {
        // If any hero reaches a Monster Nexus tile -> heroes win.
        for (Map.Entry<Hero, Position> e : heroPositions.entrySet()) {
            Hero h = e.getKey();
            Position p = e.getValue();
            if (p == null) continue;
            Tile t = world.getTile(p.getRow(), p.getCol());
            if (t.getType() == TileType.MONSTER_NEXUS) {
                renderer.renderMessage("Heroes reach the Monster Nexus! Heroes win!");
                gameOver = true;
                return;
            }
        }

        // If any monster reaches a Hero Nexus tile -> monsters win.
        for (Map.Entry<Monster, Position> e : monsterPositions.entrySet()) {
            Monster m = e.getKey();
            Position p = e.getValue();
            if (p == null) continue;
            Tile t = world.getTile(p.getRow(), p.getCol());
            if (t.getType() == TileType.HERO_NEXUS) {
                renderer.renderMessage("Monsters reach the Hero Nexus! Monsters win!");
                gameOver = true;
                return;
            }
        }

        if (!hasLivingHeroes()) {
            renderer.renderMessage("All heroes are dead. Monsters win!");
            gameOver = true;
        }
    }

    private boolean equipWeaponForHero(Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Weapon> weapons = new ArrayList<Weapon>();
        for (Item item : items) {
            if (item instanceof Weapon) {
                weapons.add((Weapon) item);
            }
        }
        if (weapons.isEmpty()) {
            renderer.renderMessage("No weapons available to equip.");
            return false;
        }

        renderer.renderMessage("Choose a weapon to equip:");
        for (int i = 0; i < weapons.size(); i++) {
            Weapon w = weapons.get(i);
            String handsText = (w.getHandsRequired() == 2 ? "2H" : "1H");
            renderer.renderMessage("  " + (i + 1) + ") " + w.getName() +
                    " (Damage " + w.getDamage() +
                    ", Req Lv " + w.getRequiredLevel() +
                    ", " + handsText + ")");
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= weapons.size()) {
            renderer.renderMessage("Invalid weapon choice.");
            return false;
        }

        Weapon selected = weapons.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            renderer.renderMessage("Hero level too low to equip this weapon.");
            return false;
        }

        boolean useTwoHands = false;
        if (selected.getHandsRequired() == 2) {
            // Must use two hands
            useTwoHands = true;
            renderer.renderMessage("Equipping " + selected.getName() + " (2H required).");
        } else {
            // One-handed weapon: ask how many hands to use
            renderer.renderMessage("Use this one-handed weapon with:");
            renderer.renderMessage("  1) One hand (normal damage)");
            renderer.renderMessage("  2) Two hands (increased damage)");
            int handChoice = input.readInt();
            useTwoHands = (handChoice == 2);
        }

        hero.equipWeapon(selected, useTwoHands);
        renderer.renderMessage(hero.getName() + " equipped weapon: " +
                selected.getName() + (useTwoHands ? " (using both hands)" : ""));
        return true;
    }

    private boolean equipArmorForHero(Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Armor> armors = new ArrayList<Armor>();
        for (Item item : items) {
            if (item instanceof Armor) {
                armors.add((Armor) item);
            }
        }
        if (armors.isEmpty()) {
            renderer.renderMessage("No armor available to equip.");
            return false;
        }

        renderer.renderMessage("Choose armor to equip:");
        for (int i = 0; i < armors.size(); i++) {
            Armor a = armors.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " + a.getName() +
                    " (Reduction " + a.getDamageReduction() +
                    ", Req Lv " + a.getRequiredLevel() + ")");
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= armors.size()) {
            renderer.renderMessage("Invalid armor choice.");
            return false;
        }

        Armor selected = armors.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            renderer.renderMessage("Hero level too low to equip this armor.");
            return false;
        }

        hero.equipArmor(selected);
        renderer.renderMessage(hero.getName() + " equipped armor: " + selected.getName());
        return true;
    }

    private boolean usePotionForHero(Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Potion> potions = new ArrayList<Potion>();
        for (Item item : items) {
            if (item instanceof Potion) {
                potions.add((Potion) item);
            }
        }
        if (potions.isEmpty()) {
            renderer.renderMessage("No potions available.");
            return false;
        }

        renderer.renderMessage("Choose a potion to use:");
        for (int i = 0; i < potions.size(); i++) {
            Potion p = potions.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " + p.getName() +
                    " (Effect " + p.getAmount() +
                    " on " + p.getStatType() +
                    ", Req Lv " + p.getRequiredLevel() + ")");
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return false;
        choice--;
        if (choice < 0 || choice >= potions.size()) {
            renderer.renderMessage("Invalid potion choice.");
            return false;
        }

        Potion selected = potions.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            renderer.renderMessage("Hero level too low to use this potion.");
            return false;
        }

        selected.consume(hero);
        hero.getInventory().remove(selected);
        renderer.renderMessage(hero.getName() + " used potion: " + selected.getName());
        return true;
    }

    private int averageHeroLevel() {
        if (heroes.isEmpty()) return 1;
        int sum = 0;
        for (Hero h : heroes) sum += h.getLevel();
        return sum / heroes.size();
    }

    private int maxHeroLevel() {
        int max = 1;
        for (Hero h : heroes) {
            if (h.getLevel() > max) {
                max = h.getLevel();
            }
        }
        return max;
    }

    private boolean isInsideBoard(int r, int c) {
        int size = world.getSize();
        return r >= 0 && r < size && c >= 0 && c < size;
    }

    // In range = same tile or 8 neighbors, and in the same lane
    private boolean isInRange(Position a, Position b) {
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getCol() - b.getCol());
        return dr <= 1 && dc <= 1 && world.sameLane(a, b);
    }

    private boolean isOccupiedByHero(Position p) {
        return isOccupiedByHero(p, null);
    }

    private boolean isOccupiedByHero(Position p, Hero ignore) {
        for (Map.Entry<Hero, Position> entry : heroPositions.entrySet()) {
            if (ignore != null && entry.getKey() == ignore) continue;
            Position hp = entry.getValue();
            if (hp != null && hp.getRow() == p.getRow() && hp.getCol() == p.getCol()) {
                return true;
            }
        }
        return false;
    }

    private boolean isOccupiedByMonster(Position p) {
        return isOccupiedByMonster(p, null);
    }

    private boolean isOccupiedByMonster(Position p, Monster ignore) {
        for (Map.Entry<Monster, Position> entry : monsterPositions.entrySet()) {
            if (ignore != null && entry.getKey() == ignore) continue;
            Position mp = entry.getValue();
            if (mp != null && mp.getRow() == p.getRow() && mp.getCol() == p.getCol()) {
                return true;
            }
        }
        return false;
    }

    // Heroes advance upward and cannot jump ahead of the foremost monster; monsters advance downward with the same rule
    private boolean wouldMovePastEnemy(Position from, Position to, boolean isHero) {
        if (!world.sameLane(from, to)) {
            return false;
        }
        if (isHero) {
            if (to.getRow() >= from.getRow()) {
                return false; // side/back move not checked
            }
            for (Map.Entry<Monster, Position> entry : monsterPositions.entrySet()) {
                Position mp = entry.getValue();
                if (entry.getKey().isFainted()) continue;
                if (mp == null) continue;
                if (!world.sameLane(from, mp)) continue;
                if (to.getRow() < mp.getRow()) {
                    return true;
                }
            }
        } else {
            if (to.getRow() <= from.getRow()) {
                return false;
            }
            for (Map.Entry<Hero, Position> entry : heroPositions.entrySet()) {
                Position hp = entry.getValue();
                if (entry.getKey().isFainted()) continue;
                if (hp == null) continue;
                if (!world.sameLane(from, hp)) continue;
                if (to.getRow() > hp.getRow()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBehindEnemyInDestination(Position dest, boolean isHero) {
        if (isHero) {
            for (Map.Entry<Monster, Position> entry : monsterPositions.entrySet()) {
                if (entry.getKey().isFainted()) continue;
                Position mp = entry.getValue();
                if (mp == null) continue;
                if (!world.sameLane(dest, mp)) continue;
                if (dest.getRow() < mp.getRow()) {
                    return true;
                }
            }
        } else {
            for (Map.Entry<Hero, Position> entry : heroPositions.entrySet()) {
                if (entry.getKey().isFainted()) continue;
                Position hp = entry.getValue();
                if (hp == null) continue;
                if (!world.sameLane(dest, hp)) continue;
                if (dest.getRow() > hp.getRow()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Position findAvailableHeroNexusSlot(int laneIndex, Hero hero) {
        Position[] slots = world.getHeroNexusColumnsForLane(laneIndex);
        if (slots == null) return null;
        for (Position p : slots) {
            if (!isOccupiedByHero(p, hero)) {
                return p;
            }
        }
        return null;
    }

    private void clearTerrainEffects(Hero hero) {
        TileType prevType = activeTerrainBuff.remove(hero);
        int[] delta = terrainBuffDeltas.remove(hero);
        if (prevType != null && delta != null) {
            hero.adjustStrength(-delta[0]);
            hero.adjustDexterity(-delta[1]);
            hero.adjustAgility(-delta[2]);
        }
    }

    private void applyTerrainEffects(Hero hero, Position from, Position to) {
        clearTerrainEffects(hero);
        if (to == null) {
            return;
        }
        TileType type = world.getTile(to.getRow(), to.getCol()).getType();
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
        activeTerrainBuff.put(hero, type);
        terrainBuffDeltas.put(hero, new int[]{str, dex, agi});
    }
}
