package core;

import java.util.*;

import characters.Hero;
import characters.Monster;
import config.GameBalance;
import data.MonsterFactory;
import data.ItemFactory;
import io.InputHandler;
import io.Renderer;
import market.Market;
import market.MarketController;
import world.Tile;
import world.TileType;
import world.World;

public class ValorGameEngine {

    private final World world;
    private final List<Hero> heroes;
    private final Map<Hero, Position> heroPositions;
    private final Map<Hero, Position> heroSpawnPositions;

    private final List<Monster> monsters;
    private final Map<Monster, Position> monsterPositions;

    private final Renderer renderer;
    private final InputHandler input;
    private final MarketController marketController;
    private final MonsterFactory monsterFactory;
    private final ItemFactory itemFactory;

    private int roundCount = 0;
    private boolean gameOver = false;

    // Regen 10% HP/MP at end of round (alive heroes)
    private static final double ROUND_REGEN_FRACTION = 0.10;

    // Difficulty → spawn every N rounds
    private int spawnFrequencyRounds = 6; // default easy

    public ValorGameEngine(World world,
                           List<Hero> heroes,
                           Renderer renderer,
                           InputHandler input) {
        this.world = world;
        this.heroes = heroes;
        this.renderer = renderer;
        this.input = input;
        this.marketController = new MarketController(renderer, input);
        this.monsterFactory = new MonsterFactory();
        this.itemFactory = new ItemFactory();

        this.heroPositions = new HashMap<Hero, Position>();
        this.heroSpawnPositions = new HashMap<Hero, Position>();
        this.monsters = new ArrayList<Monster>();
        this.monsterPositions = new HashMap<Monster, Position>();

        initHeroPositions();
        initDifficulty();
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

    private void initDifficulty() {
        renderer.renderMessage("Choose difficulty:");
        renderer.renderMessage("  1) Easy   (spawn every 6 rounds)");
        renderer.renderMessage("  2) Medium (spawn every 4 rounds)");
        renderer.renderMessage("  3) Hard   (spawn every 2 rounds)");

        int choice = 0;
        while (choice < 1 || choice > 3) {
            choice = input.readInt();
            if (choice < 1 || choice > 3) {
                renderer.renderMessage("Please choose 1, 2, or 3.");
            }
        }

        switch (choice) {
            case 1:
                spawnFrequencyRounds = 6;
                break;
            case 2:
                spawnFrequencyRounds = 4;
                break;
            case 3:
                spawnFrequencyRounds = 2;
                break;
        }

        renderer.renderMessage("Spawn frequency set to every " +
                spawnFrequencyRounds + " rounds.");
    }

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

            monsters.add(m);
            monsterPositions.put(m, new Position(spawnPos.getRow(), spawnPos.getCol()));

            renderer.renderMessage("DEBUG: Spawned " + m.getName() +
                    " at Monster Nexus (" + spawnPos.getRow() + ", " + spawnPos.getCol() + ") in lane " + i);
        }
    }

    // --------------------------------------------------------
    // Main loop
    // --------------------------------------------------------

    public void run() {
        while (!gameOver) {
            roundCount++;
            //renderer.renderWorld(world, heroPositions, monsterPositions);
            //renderer.renderHeroStats(heroes);

            heroPhase();
            if (gameOver || !hasLivingHeroes()) break;

            monsterPhase();
            if (gameOver || !hasLivingHeroes()) break;

            cleanupDeadMonsters();
            respawnDeadHeroesAtStartOfNextRound();
            endOfRoundRegen();

            spawnWaveIfNeeded();
            checkWinLose();
        }

        renderer.renderMessage("Legends of Valor game over. Thanks for playing!");
    }

    // --------------------------------------------------------
    // Hero phase
    // --------------------------------------------------------

    private void heroPhase() {
        for (Hero h : heroes) {
            if (h.isFainted()) continue;

            renderer.renderWorld(world, heroPositions, monsterPositions);
            renderer.renderHeroStats(heroes);

            renderer.renderMessage("Hero: " + h.getName());
            renderer.renderMessage("Choose action:");
            renderer.renderMessage("  1) Move");
            renderer.renderMessage("  2) Attack");
            renderer.renderMessage("  3) Cast Spell");
            renderer.renderMessage("  4) Inventory");
            renderer.renderMessage("  5) Teleport");
            renderer.renderMessage("  6) Recall");
            renderer.renderMessage("  7) Shop (if at Nexus)");
            renderer.renderMessage("  8) Skip");

            int choice = input.readInt();
            switch (choice) {
                case 1:
                    handleMove(h);
                    break;
                case 2:
                    handleAttack(h);
                    break;
                case 3:
                    handleCastSpell(h);
                    break;
                case 4:
                    handleInventory(h);
                    break;
                case 5:
                    handleTeleport(h);
                    break;
                case 6:
                    handleRecall(h);
                    break;
                case 7:
                    openShopIfAtHeroNexus(h);
                    break;
                case 8:
                default:
                    renderer.renderMessage(h.getName() + " skips the turn.");
                    break;
            }

            if (gameOver) return;
            checkWinLose();
            if (gameOver) return;
        }
    }

    // Basic adjacency (no diagonals)
    private boolean isAdjacent(Position a, Position b) {
        if (a == null || b == null) return false;
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getCol() - b.getCol());
        return (dr + dc == 1);
    }

    // Lane indexes: 0 for cols 0–1, 1 for 3–4, 2 for 6–7, -1 otherwise
    private int laneIndex(Position p) {
        if (p == null) return -1;
        int c = p.getCol();
        if (c == 0 || c == 1) return 0;
        if (c == 3 || c == 4) return 1;
        if (c == 6 || c == 7) return 2;
        return -1;
    }

    private void handleMove(Hero h) {
        Position cur = heroPositions.get(h);
        if (cur == null) {
            renderer.renderMessage("Hero has no position.");
            return;
        }

        Direction dir = input.readMovement();
        int nr = cur.getRow();
        int nc = cur.getCol();

        switch (dir) {
            case UP:    nr--; break;
            case DOWN:  nr++; break;
            case LEFT:  nc--; break;
            case RIGHT: nc++; break;
        }

        if (!canHeroMoveTo(h, nr, nc)) {
            renderer.renderMessage("Invalid move for " + h.getName() + ".");
            return;
        }

        heroPositions.put(h, new Position(nr, nc));
    }

    /**
     * Hero move rules:
     *  - within bounds & accessible
     *  - no diagonal (already handled by Direction)
     *  - cannot move into a tile occupied by another hero
     *  - cannot move into a tile occupied by a monster
     *  - cannot move "behind" a monster in the same lane
     *    (cannot go further toward the monster Nexus than the front-most monster)
     */
    private boolean canHeroMoveTo(Hero h, int nr, int nc) {
        int size = world.getSize();
        if (nr < 0 || nr >= size || nc < 0 || nc >= size) return false;

        Tile tile = world.getTile(nr, nc);
        if (!tile.isAccessible()) return false;

        // Cannot move onto another hero
        for (Map.Entry<Hero, Position> e : heroPositions.entrySet()) {
            if (e.getKey() == h) continue;
            Position p = e.getValue();
            if (p != null && p.getRow() == nr && p.getCol() == nc) {
                return false;
            }
        }

        // Cannot move onto a monster
        for (Position p : monsterPositions.values()) {
            if (p != null && p.getRow() == nr && p.getCol() == nc) {
                return false;
            }
        }

        // Cannot move ahead of the front-most monster in the lane
        Position cur = heroPositions.get(h);
        int heroLane = laneIndex(cur);
        if (heroLane == -1) return false;
        int destLane = laneIndex(new Position(nr, nc));
        if (destLane != heroLane) return false; // heroes stay in their lane in normal "Move"

        Integer frontMostMonsterRow = null;
        for (Map.Entry<Monster, Position> e : monsterPositions.entrySet()) {
            Position mp = e.getValue();
            if (mp == null) continue;
            if (laneIndex(mp) != heroLane) continue;
            if (frontMostMonsterRow == null || mp.getRow() < frontMostMonsterRow) {
                frontMostMonsterRow = mp.getRow();
            }
        }

        if (frontMostMonsterRow != null) {
            // hero cannot move to row < frontMostMonsterRow (closer to monster Nexus)
            if (nr < frontMostMonsterRow) {
                return false;
            }
        }

        return true;
    }

    private void handleAttack(Hero h) {
        // basic: attack adjacent monster (no diagonals)
        Position hp = heroPositions.get(h);
        if (hp == null) return;

        List<Monster> adjacent = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (m.isFainted()) continue;
            Position mp = monsterPositions.get(m);
            if (mp == null) continue;
            if (isAdjacent(hp, mp)) {
                adjacent.add(m);
            }
        }

        if (adjacent.isEmpty()) {
            renderer.renderMessage("No adjacent monster to attack.");
            return;
        }

        renderer.renderMessage("Choose a monster to attack:");
        for (int i = 0; i < adjacent.size(); i++) {
            Monster m = adjacent.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() + " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        int choice = input.readInt();
        choice--;
        if (choice < 0 || choice >= adjacent.size()) {
            renderer.renderMessage("Invalid target.");
            return;
        }

        Monster target = adjacent.get(choice);
        int before = target.getHP();
        target.takeDamage(h.basicAttackDamage());
        int after = target.getHP();
        int dealt = before - after;
        if (dealt < 0) dealt = 0;

        renderer.renderMessage(h.getName() + " attacks " +
                target.getName() + " for " + dealt + " damage.");
    }

    private void handleCastSpell(Hero h) {
        // You can reuse your CastSpellAction logic here if you like.
        renderer.renderMessage("Casting spells in LoV not yet implemented here.");
    }

    private void handleInventory(Hero h) {
        // You can reuse parts of GameEngine.handleInventoryMenu for per-hero here.
        renderer.renderMessage("Inventory management in LoV not yet implemented here.");
    }

    // --------------------------------------------------------
    // Teleport & Recall
    // --------------------------------------------------------

    private void handleTeleport(Hero h) {
        Position from = heroPositions.get(h);
        if (from == null) {
            renderer.renderMessage("Hero has no position.");
            return;
        }
        int fromLane = laneIndex(from);
        if (fromLane == -1) {
            renderer.renderMessage("Hero is not in a valid lane.");
            return;
        }

        // Pick a target hero in a different lane
        List<Hero> candidates = new ArrayList<Hero>();
        for (Hero other : heroes) {
            if (other == h || other.isFainted()) continue;
            Position op = heroPositions.get(other);
            if (op == null) continue;
            int lane = laneIndex(op);
            if (lane != -1 && lane != fromLane) {
                candidates.add(other);
            }
        }

        if (candidates.isEmpty()) {
            renderer.renderMessage("No hero in a different lane to teleport to.");
            return;
        }

        renderer.renderMessage("Teleport to which hero (different lane)?");
        for (int i = 0; i < candidates.size(); i++) {
            Hero other = candidates.get(i);
            Position op = heroPositions.get(other);
            renderer.renderMessage("  " + (i + 1) + ") " + other.getName() +
                    " at (" + op.getRow() + ", " + op.getCol() + ")");
        }
        renderer.renderMessage("  0) Cancel");

        int choice = input.readInt();
        if (choice == 0) return;
        choice--;

        if (choice < 0 || choice >= candidates.size()) {
            renderer.renderMessage("Invalid choice.");
            return;
        }

        Hero targetHero = candidates.get(choice);
        Position tp = heroPositions.get(targetHero);
        int targetLane = laneIndex(tp);

        // Candidate dest squares: 4 neighbors of target hero
        List<Position> possible = new ArrayList<Position>();
        int[][] deltas = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int size = world.getSize();

        for (int[] d : deltas) {
            int nr = tp.getRow() + d[0];
            int nc = tp.getCol() + d[1];
            if (nr < 0 || nr >= size || nc < 0 || nc >= size) continue;

            Position dest = new Position(nr, nc);

            // Must be in the target hero's lane
            if (laneIndex(dest) != targetLane) continue;

            // Cannot be ahead of target hero (must not be closer to monster Nexus)
            if (nr < tp.getRow()) continue;

            Tile tile = world.getTile(nr, nc);
            if (!tile.isAccessible()) continue;

            // Cannot be occupied by hero
            boolean occupiedByHero = false;
            for (Position hp : heroPositions.values()) {
                if (hp != null && hp.getRow() == nr && hp.getCol() == nc) {
                    occupiedByHero = true;
                    break;
                }
            }
            if (occupiedByHero) continue;

            // Cannot be behind a monster in that lane (i.e. monster closer to hero Nexus)
            boolean behindMonster = false;
            for (Map.Entry<Monster, Position> e : monsterPositions.entrySet()) {
                Position mp = e.getValue();
                if (mp == null) continue;
                if (laneIndex(mp) != targetLane) continue;
                if (mp.getRow() > nr) { // monster is closer to hero Nexus
                    behindMonster = true;
                    break;
                }
            }
            if (behindMonster) continue;

            possible.add(dest);
        }

        if (possible.isEmpty()) {
            renderer.renderMessage("No valid teleport destination.");
            return;
        }

        renderer.renderMessage("Choose teleport destination:");
        for (int i = 0; i < possible.size(); i++) {
            Position p = possible.get(i);
            renderer.renderMessage("  " + (i + 1) + ") (" + p.getRow() + ", " + p.getCol() + ")");
        }
        renderer.renderMessage("  0) Cancel");

        int destChoice = input.readInt();
        if (destChoice == 0) return;
        destChoice--;

        if (destChoice < 0 || destChoice >= possible.size()) {
            renderer.renderMessage("Invalid choice.");
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

        // Build market for hero's level
        int level = h.getLevel();
        Market market = new Market(itemFactory, level);
        List<Hero> single = new ArrayList<Hero>();
        single.add(h);
        marketController.openMarket(market, single);
    }

    // --------------------------------------------------------
    // Monster phase + pathfinding
    // --------------------------------------------------------

    private void monsterPhase() {
        for (Monster m : new ArrayList<Monster>(monsters)) {
            if (m.isFainted()) continue;
            Position mp = monsterPositions.get(m);
            if (mp == null) continue;

            // If any hero in range (adjacent), attack instead of moving
            Hero target = findAdjacentHero(mp);
            if (target != null) {
                monsterAttack(m, target);
                if (!hasLivingHeroes()) {
                    gameOver = true;
                    return;
                }
                continue;
            }

            // Otherwise, try to pathfind toward hero Nexus in lane
            Position next = computeMonsterStep(m);
            if (next != null) {
                monsterPositions.put(m, next);
            }
        }
    }

    private Hero findAdjacentHero(Position mp) {
        for (Map.Entry<Hero, Position> e : heroPositions.entrySet()) {
            Hero h = e.getKey();
            if (h.isFainted()) continue;
            Position hp = e.getValue();
            if (hp == null) continue;
            if (isAdjacent(hp, mp)) {
                return h;
            }
        }
        return null;
    }

    private void monsterAttack(Monster m, Hero target) {
        int raw = m.getDamage();
        int reduced = raw - target.getArmorReduction();
        if (reduced < 0) reduced = 0;

        if (target.tryDodge()) {
            renderer.renderMessage(target.getName() + " dodges " + m.getName() + "'s attack!");
            return;
        }

        int before = target.getHP();
        target.takeDamage(reduced);
        int after = target.getHP();
        int dealt = before - after;
        if (dealt < 0) dealt = 0;

        renderer.renderMessage(m.getName() + " attacks " +
                target.getName() + " for " + dealt + " damage.");

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

        Position dest = new Position(nr, nc);
        int lane = laneIndex(from);
        if (laneIndex(dest) != lane) return false;

        Tile tile = world.getTile(nr, nc);
        if (!tile.isAccessible()) return false;

        // Cannot move onto hero
        for (Position hp : heroPositions.values()) {
            if (hp != null && hp.getRow() == nr && hp.getCol() == nc) {
                return false;
            }
        }

        // Cannot move onto another monster (except itself)
        for (Map.Entry<Monster, Position> e : monsterPositions.entrySet()) {
            Monster other = e.getKey();
            Position mp = e.getValue();
            if (other == m) continue;
            if (mp != null && mp.getRow() == nr && mp.getCol() == nc) {
                return false;
            }
        }

        // Cannot move behind the front-most hero in that lane (further toward hero Nexus)
        Integer frontHeroRow = null;
        for (Map.Entry<Hero, Position> e : heroPositions.entrySet()) {
            Position hp = e.getValue();
            if (hp == null) continue;
            if (laneIndex(hp) != lane) continue;
            if (frontHeroRow == null || hp.getRow() > frontHeroRow) {
                frontHeroRow = hp.getRow();
            }
        }
        if (frontHeroRow != null) {
            if (nr > frontHeroRow) { // nr closer to hero Nexus than front-most hero
                return false;
            }
        }

        return true;
    }

    // --------------------------------------------------------
    // Round end: regen, respawn, spawn waves, win/lose
    // --------------------------------------------------------

    private void cleanupDeadMonsters() {
        Iterator<Monster> it = monsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            if (m.isFainted()) {
                it.remove();
                monsterPositions.remove(m);
                renderer.renderMessage(m.getName() + " is slain and removed from the board.");
            }
        }
    }

    private void respawnDeadHeroesAtStartOfNextRound() {
        for (Hero h : heroes) {
            if (!h.isFainted()) continue;
            Position spawn = heroSpawnPositions.get(h);
            if (spawn == null) continue;

            h.heal(h.getMaxHP()); // fully restore HP
            h.restoreMana(h.getMaxMana()); // restore MP
            heroPositions.put(h, new Position(spawn.getRow(), spawn.getCol()));
            renderer.renderMessage(h.getName() +
                    " respawns at their Nexus at (" +
                    spawn.getRow() + ", " + spawn.getCol() + ").");
        }
    }

    private void endOfRoundRegen() {
        for (Hero h : heroes) {
            if (h.isFainted()) continue;
            int hpRegen = (int)(h.getMaxHP() * ROUND_REGEN_FRACTION);
            int mpRegen = (int)(h.getMaxMana() * ROUND_REGEN_FRACTION);
            h.heal(hpRegen);
            h.restoreMana(mpRegen);
        }
    }

    private void spawnWaveIfNeeded() {
        if (roundCount % spawnFrequencyRounds != 0) return;

        renderer.renderMessage("A new wave of monsters appears!");

        int avgLevel = averageHeroLevel();
        List<Monster> newOnes = monsterFactory.spawnMonstersForBattle(3, avgLevel);

        int[][] laneSpawn = {
                { 0, 0 },
                { 0, 3 },
                { 0, 6 }
        };

        for (int i = 0; i < newOnes.size() && i < laneSpawn.length; i++) {
            Monster m = newOnes.get(i);
            int r = laneSpawn[i][0];
            int c = laneSpawn[i][1];

            // If spawn tile is occupied by a monster, skip that lane
            boolean occupied = false;
            for (Position mp : monsterPositions.values()) {
                if (mp != null && mp.getRow() == r && mp.getCol() == c) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;

            monsters.add(m);
            monsterPositions.put(m, new Position(r, c));
        }
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

    private int averageHeroLevel() {
        if (heroes.isEmpty()) return 1;
        int sum = 0;
        for (Hero h : heroes) sum += h.getLevel();
        return sum / heroes.size();
    }

    private boolean hasLivingHeroes() {
        for (Hero h : heroes) {
            if (!h.isFainted()) return true;
        }
        return false;
    }
}
