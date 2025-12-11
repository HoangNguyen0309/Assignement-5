package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import battle.Battle;
import characters.Hero;
import characters.Monster;
import config.GameBalance;
import data.ItemFactory;
import data.MonsterFactory;
import io.InputHandler;
import io.Renderer;
import items.Armor;
import items.Item;
import items.Potion;
import items.Spell;
import items.Weapon;
import market.Market;
import market.MarketController;
import world.MarketTile;
import world.Tile;
import world.TileType;
import world.World;

/**
 * Legends of Valor game loop.
 * Uses World in "Valor" mode, heroes & monsters on lanes, and an
 * adjacent-tile (no diagonal) range rule.
 */
public class ValorGameEngine implements Battle {

    private static final int BOARD_SIZE = 8;
    // How often to spawn a new wave of monsters
    private static final int MONSTER_WAVE_PERIOD = 6;

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
    private final Random random;

    private int roundCount;
    private boolean gameOver;

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
        this.monsters = new ArrayList<Monster>();
        this.monsterPositions = new HashMap<Monster, Position>();

        this.marketController = new MarketController(renderer, input);
        this.monsterFactory = new MonsterFactory();
        this.itemFactory = new ItemFactory();
        this.random = new Random();

        this.roundCount = 1;
        this.gameOver = false;

        initializeHeroPositions();
        spawnInitialMonsters();
    }

    @Override
    public void start() {
        run();
    }

    public void run() {
        renderer.renderMessage("Starting Legends of Valor!");

        while (!gameOver) {
            // 1. Render board + hero stats
            renderer.renderWorld(world, heroPositions, monsterPositions);
            renderer.renderHeroStats(heroes);

            // 2. Hero phase
            heroPhase();
            if (gameOver) break;

            // 3. Monster phase
            monsterPhase();
            if (gameOver) break;

            // 4. Cleanup
            cleanupPhase();

            // 5. End-of-round recovery, respawn, new waves, win/lose checks
            endOfRound();

            roundCount++;
        }

        renderer.renderMessage("Game over. Thanks for playing Legends of Valor!");
    }

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
                    new Position(BOARD_SIZE - 1, 0),
                    new Position(BOARD_SIZE - 1, 3),
                    new Position(BOARD_SIZE - 1, 6)
            };
        }

        int count = Math.min(heroes.size(), starts.length);
        for (int i = 0; i < count; i++) {
            Hero h = heroes.get(i);
            Position p = new Position(starts[i].getRow(), starts[i].getCol());
            heroPositions.put(h, p);
            heroSpawnPositions.put(h, new Position(p.getRow(), p.getCol()));
        }
    }

    private void spawnInitialMonsters() {
        int avgLevel = averageHeroLevel();
        List<Monster> spawned = monsterFactory.spawnMonstersForBattle(3, avgLevel);

        // Three lanes: columns 0, 3, 6 at row 0 (monster nexus row)
        int[] laneCols = {0, 3, 6};
        int idx = 0;
        for (int lane = 0; lane < laneCols.length && idx < spawned.size(); lane++) {
            int col = laneCols[lane];
            Position pos = new Position(0, col);
            Monster m = spawned.get(idx++);
            monsters.add(m);
            monsterPositions.put(m, pos);
        }
    }

    // ------------------------------------------------------------
    // Hero Phase
    // ------------------------------------------------------------

    private void heroPhase() {
        for (Hero hero : heroes) {
            if (hero.isFainted()) {
                continue;
            }
            if (!heroPositions.containsKey(hero)) {
                continue;
            }

            renderer.renderWorld(world, heroPositions, monsterPositions);
            renderer.renderHeroStats(heroes);

            renderer.renderMessage(hero.getName() + ", choose your action:");
            renderer.renderMessage("  1) Move");
            renderer.renderMessage("  2) Attack");
            renderer.renderMessage("  3) Cast Spell");
            renderer.renderMessage("  4) Inventory");
            renderer.renderMessage("  5) Recall");
            renderer.renderMessage("  6) Shop (if at Hero Nexus)");
            renderer.renderMessage("  7) Skip");

            int choice = input.readInt();
            switch (choice) {
                case 1:
                    handleMove(hero);
                    break;
                case 2:
                    handleAttack(hero);
                    break;
                case 3:
                    handleCastSpell(hero);
                    break;
                case 4:
                    handleInventory(hero);
                    break;
                case 5:
                    handleRecall(hero);
                    break;
                case 6:
                    openShopIfAtHeroNexus(hero);
                    break;
                case 7:
                    renderer.renderMessage(hero.getName() + " skips the turn.");
                    break;
                default:
                    renderer.renderMessage("Invalid choice. Skipping turn.");
                    break;
            }

            // After each hero action, check immediate win condition
            checkWinLoseConditions();
            if (gameOver) return;
        }
    }

    private void handleMove(Hero hero) {
        Position pos = heroPositions.get(hero);
        if (pos == null) {
            return;
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
            return;
        }

        Tile tile = world.getTile(newRow, newCol);
        if (!tile.isAccessible()) {
            renderer.renderMessage("That tile is not accessible.");
            return;
        }

        Position dest = new Position(newRow, newCol);

        if (isOccupiedByHero(dest)) {
            renderer.renderMessage("Another hero is already there.");
            return;
        }
        if (isOccupiedByMonster(dest)) {
            renderer.renderMessage("A monster blocks the way.");
            return;
        }

        heroPositions.put(hero, dest);
        renderer.renderMessage(hero.getName() + " moved to (" + newRow + ", " + newCol + ").");
    }

    private void handleAttack(Hero hero) {
        Position heroPos = heroPositions.get(hero);
        if (heroPos == null) {
            return;
        }

        List<Monster> targetsInRange = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (m.isFainted()) continue;
            Position mp = monsterPositions.get(m);
            if (mp == null) continue;
            if (isAdjacent(heroPos, mp)) {
                targetsInRange.add(m);
            }
        }

        if (targetsInRange.isEmpty()) {
            renderer.renderMessage("No monsters in range (must be on an adjacent tile).");
            return;
        }

        renderer.renderMessage("Choose a monster to attack:");
        for (int i = 0; i < targetsInRange.size(); i++) {
            Monster m = targetsInRange.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() +
                    " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return;
        choice--;

        if (choice < 0 || choice >= targetsInRange.size()) {
            renderer.renderMessage("Invalid target.");
            return;
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

        if (target.isFainted()) {
            renderer.renderMessage(target.getName() + " has been defeated!");
            // Simple XP reward: proportional to monster level
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL;
            hero.gainExperience(xp);
            renderer.renderMessage(hero.getName() + " gains " + xp + " XP.");
        }
    }

    private void handleCastSpell(Hero hero) {
        Position heroPos = heroPositions.get(hero);
        if (heroPos == null) {
            return;
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
            return;
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
        if (spellChoice == 0) return;
        spellChoice--;

        if (spellChoice < 0 || spellChoice >= spells.size()) {
            renderer.renderMessage("Invalid spell choice.");
            return;
        }

        Spell spell = spells.get(spellChoice);

        if (hero.getMana() < spell.getManaCost()) {
            renderer.renderMessage("Not enough mana.");
            return;
        }

        // Monsters in range (adjacent tiles only)
        List<Monster> targetsInRange = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (m.isFainted()) continue;
            Position mp = monsterPositions.get(m);
            if (mp == null) continue;
            if (isAdjacent(heroPos, mp)) {
                targetsInRange.add(m);
            }
        }

        if (targetsInRange.isEmpty()) {
            renderer.renderMessage("No monsters in range for the spell.");
            return;
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
        if (targetChoice == 0) return;
        targetChoice--;

        if (targetChoice < 0 || targetChoice >= targetsInRange.size()) {
            renderer.renderMessage("Invalid target choice.");
            return;
        }

        Monster target = targetsInRange.get(targetChoice);

        // Spend mana and cast
        hero.restoreMana(-spell.getManaCost());
        int hpBefore = target.getHP();
        int rawDealt = spell.cast(hero, target);
        int hpAfter = target.getHP();

        int effective = hpBefore - hpAfter;
        if (effective < 0) effective = 0;

        renderer.renderMessage(hero.getName() + " casts " +
                spell.getName() + " on " + target.getName() +
                " for " + effective + " damage (raw: " + rawDealt + ").");

        // Single-use spell
        hero.getInventory().remove(spell);

        if (target.isFainted()) {
            renderer.renderMessage(target.getName() + " has been defeated!");
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL;
            hero.gainExperience(xp);
            renderer.renderMessage(hero.getName() + " gains " + xp + " XP.");
        }
    }

    private void handleInventory(Hero hero) {
        boolean done = false;
        while (!done) {
            renderer.renderHeroStats(heroes);
            renderer.renderMessage("Manage " + hero.getName() + ":");
            renderer.renderMessage("  1) Equip weapon");
            renderer.renderMessage("  2) Equip armor");
            renderer.renderMessage("  3) Use potion");
            renderer.renderMessage("  4) View inventory");
            renderer.renderMessage("  5) Back");

            int choice = input.readInt();
            switch (choice) {
                case 1:
                    equipWeaponForHero(hero);
                    break;
                case 2:
                    equipArmorForHero(hero);
                    break;
                case 3:
                    usePotionForHero(hero);
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
    }

    private void handleRecall(Hero hero) {
        Position spawn = heroSpawnPositions.get(hero);
        if (spawn == null) {
            renderer.renderMessage("No recall position for " + hero.getName() + ".");
            return;
        }
        heroPositions.put(hero, new Position(spawn.getRow(), spawn.getCol()));
        renderer.renderMessage(hero.getName() + " recalls to their Hero Nexus.");
    }

    private void openShopIfAtHeroNexus(Hero hero) {
        Position pos = heroPositions.get(hero);
        if (pos == null) return;

        Tile tile = world.getTile(pos.getRow(), pos.getCol());
        if (tile.getType() != TileType.HERO_NEXUS) {
            renderer.renderMessage("You must be standing on a Hero Nexus to shop.");
            return;
        }

        if (!(tile instanceof MarketTile)) {
            renderer.renderMessage("This Hero Nexus cannot host a market.");
            return;
        }

        MarketTile mTile = (MarketTile) tile;
        if (mTile.getMarket() == null) {
            mTile.setMarket(new Market(itemFactory, hero.getLevel()));
        }

        // You can let the whole party shop, or just this hero. Reuse existing API:
        List<Hero> singleHeroList = new ArrayList<Hero>();
        singleHeroList.add(hero);
        marketController.openMarket(mTile.getMarket(), singleHeroList);
    }

    // ------------------------------------------------------------
    // Monster Phase
    // ------------------------------------------------------------

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
                        !isOccupiedByHero(dest) &&
                        !isOccupiedByMonster(dest)) {
                    monsterPositions.put(monster, dest);
                    renderer.renderMessage(monster.getName() + " moves to (" +
                            newRow + ", " + newCol + ").");
                }
            }

            // If monster steps on Hero Nexus, they may have just won
            checkWinLoseConditions();
            if (gameOver) return;
        }
    }

    private Hero findHeroInRange(Position monsterPos) {
        for (Hero h : heroes) {
            if (h.isFainted()) continue;
            Position hp = heroPositions.get(h);
            if (hp == null) continue;
            if (isAdjacent(monsterPos, hp)) {
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
            return;
        }

        target.takeDamage(reducedDamage);
        renderer.renderMessage(monster.getName() + " attacked " +
                target.getName() + " for " + reducedDamage + " damage.");

        if (target.isFainted()) {
            renderer.renderMessage(target.getName() + " has fallen in this round.");
        }
    }

    // ------------------------------------------------------------
    // Cleanup & end-of-round
    // ------------------------------------------------------------

    private void cleanupPhase() {
        // Remove dead monsters from list + position map
        Iterator<Monster> it = monsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            if (m.isFainted()) {
                it.remove();
                monsterPositions.remove(m);
            }
        }
    }

    private void endOfRound() {
        // Respawn fainted heroes at their nexus
        for (Hero h : heroes) {
            if (h.isFainted()) {
                Position spawn = heroSpawnPositions.get(h);
                if (spawn != null) {
                    heroPositions.put(h, new Position(spawn.getRow(), spawn.getCol()));
                }
                // Fully restore HP & Mana
                h.heal(h.getMaxHP());
                h.restoreMana(h.getMaxMana());
                renderer.renderMessage(h.getName() + " is revived at their Hero Nexus!");
            }
        }

        // Simple 10% HP/MP recovery for surviving heroes
        for (Hero h : heroes) {
            if (!h.isFainted()) {
                int healAmount = (int) (h.getMaxHP() * 0.1);
                int manaAmount = (int) (h.getMaxMana() * 0.1);
                h.heal(healAmount);
                h.restoreMana(manaAmount);
            }
        }

        // Simple 10% HP recovery for monsters
        for (Monster m : monsters) {
            if (!m.isFainted()) {
                int healAmount = (int) (m.getMaxHP() * 0.1);
                m.heal(healAmount);
            }
        }

        // New monster wave every MONSTER_WAVE_PERIOD rounds
        if (roundCount > 0 && roundCount % MONSTER_WAVE_PERIOD == 0) {
            spawnMonsterWave();
        }

        // Check win/lose at the end of the round as well
        checkWinLoseConditions();
    }

    private void spawnMonsterWave() {
        renderer.renderMessage("A new wave of monsters appears!");

        int avgLevel = averageHeroLevel();
        List<Monster> spawned = monsterFactory.spawnMonstersForBattle(3, avgLevel);

        int[] laneCols = {0, 3, 6};
        int idx = 0;

        for (int lane = 0; lane < laneCols.length && idx < spawned.size(); lane++) {
            int col = laneCols[lane];
            Position nexusPos = new Position(0, col);

            // Only spawn if nexus is not currently occupied by a monster
            if (isOccupiedByMonster(nexusPos)) {
                continue;
            }

            Monster m = spawned.get(idx++);
            monsters.add(m);
            monsterPositions.put(m, nexusPos);
        }
    }

    private void checkWinLoseConditions() {
        // Heroes win if any hero stands on a MONSTER_NEXUS
        for (Map.Entry<Hero, Position> e : heroPositions.entrySet()) {
            Hero h = e.getKey();
            if (h.isFainted()) continue;
            Position p = e.getValue();
            if (p == null) continue;
            TileType t = world.getTile(p.getRow(), p.getCol()).getType();
            if (t == TileType.MONSTER_NEXUS) {
                renderer.renderMessage(h.getName() +
                        " has reached the Monster Nexus! Heroes win!");
                gameOver = true;
                return;
            }
        }

        // Monsters win if any monster stands on a HERO_NEXUS
        for (Map.Entry<Monster, Position> e : monsterPositions.entrySet()) {
            Monster m = e.getKey();
            if (m.isFainted()) continue;
            Position p = e.getValue();
            if (p == null) continue;
            TileType t = world.getTile(p.getRow(), p.getCol()).getType();
            if (t == TileType.HERO_NEXUS) {
                renderer.renderMessage(m.getName() +
                        " has reached the Hero Nexus! Monsters win!");
                gameOver = true;
                return;
            }
        }
    }

    // ------------------------------------------------------------
    // Shared helpers (inventory, range, etc.)
    // ------------------------------------------------------------

    private void renderInventory(Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        if (items.isEmpty()) {
            renderer.renderMessage(hero.getName() + " has no items.");
            return;
        }
        renderer.renderMessage(hero.getName() + "'s inventory:");
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " +
                    it.getName() + " (Lv " + it.getRequiredLevel() +
                    ", Price " + it.getPrice() + ")");
        }
    }

    private void equipWeaponForHero(Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Weapon> weapons = new ArrayList<Weapon>();
        for (Item item : items) {
            if (item instanceof Weapon) {
                weapons.add((Weapon) item);
            }
        }
        if (weapons.isEmpty()) {
            renderer.renderMessage("No weapons available to equip.");
            return;
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
        if (choice == 0) return;
        choice--;

        if (choice < 0 || choice >= weapons.size()) {
            renderer.renderMessage("Invalid weapon choice.");
            return;
        }

        Weapon selected = weapons.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            renderer.renderMessage("Hero level too low to equip this weapon.");
            return;
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
    }

    private void equipArmorForHero(Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Armor> armors = new ArrayList<Armor>();
        for (Item item : items) {
            if (item instanceof Armor) {
                armors.add((Armor) item);
            }
        }
        if (armors.isEmpty()) {
            renderer.renderMessage("No armor available to equip.");
            return;
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
        if (choice == 0) return;
        choice--;

        if (choice < 0 || choice >= armors.size()) {
            renderer.renderMessage("Invalid armor choice.");
            return;
        }

        Armor selected = armors.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            renderer.renderMessage("Hero level too low to equip this armor.");
            return;
        }

        hero.equipArmor(selected);
        renderer.renderMessage(hero.getName() + " equipped armor: " + selected.getName());
    }

    private void usePotionForHero(Hero hero) {
        List<Item> items = hero.getInventory().getItems();
        List<Potion> potions = new ArrayList<Potion>();
        for (Item item : items) {
            if (item instanceof Potion) {
                potions.add((Potion) item);
            }
        }
        if (potions.isEmpty()) {
            renderer.renderMessage("No potions available.");
            return;
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
        if (choice == 0) return;
        choice--;
        if (choice < 0 || choice >= potions.size()) {
            renderer.renderMessage("Invalid potion choice.");
            return;
        }

        Potion selected = potions.get(choice);
        if (hero.getLevel() < selected.getRequiredLevel()) {
            renderer.renderMessage("Hero level too low to use this potion.");
            return;
        }

        selected.consume(hero);
        hero.getInventory().remove(selected);
        renderer.renderMessage(hero.getName() + " used potion: " + selected.getName());
    }

    private int averageHeroLevel() {
        if (heroes.isEmpty()) return 1;
        int sum = 0;
        for (Hero h : heroes) sum += h.getLevel();
        return sum / heroes.size();
    }

    private boolean isInsideBoard(int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
    }

    // "In range" = adjacent tile (no diagonal)
    private boolean isAdjacent(Position a, Position b) {
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getCol() - b.getCol());
        return (dr + dc) == 1;
    }

    private boolean isOccupiedByHero(Position p) {
        for (Position hp : heroPositions.values()) {
            if (hp != null && hp.getRow() == p.getRow() && hp.getCol() == p.getCol()) {
                return true;
            }
        }
        return false;
    }

    private boolean isOccupiedByMonster(Position p) {
        for (Position mp : monsterPositions.values()) {
            if (mp != null && mp.getRow() == p.getRow() && mp.getCol() == p.getCol()) {
                return true;
            }
        }
        return false;
    }
}
