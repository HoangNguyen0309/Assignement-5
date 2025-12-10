package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battle.Battle;
import battle.StandardBattle;
import characters.Hero;
import characters.Monster;
import config.GameBalance;
import data.ItemFactory;
import data.MonsterFactory;
import events.DefaultEventFactory;
import events.EventManager;
import io.InputHandler;
import io.Renderer;
import items.Armor;
import items.Item;
import items.Potion;
import items.Weapon;
import market.Market;
import market.MarketController;
import world.MarketTile;
import world.Tile;
import world.TileType;
import world.World;

/**
 * Coordinates the main game loop, switching between exploration and battles,
 * and delegating to World, Market, and Battle components.
 */
public class GameEngine {

    private enum GameState {
        EXPLORATION,
        BATTLE,
        GAME_OVER
    }

    private World world;
    private List<Hero> party;
    private Renderer renderer;
    private InputHandler input;

    private EventManager eventManager;
    private MonsterFactory monsterFactory;
    private ItemFactory itemFactory;
    private MarketController marketController;

    private GameState state;
    private Random random;

    public GameEngine(World world,
                      List<Hero> party,
                      Renderer renderer,
                      InputHandler input) {
        this.world = world;
        this.party = party;
        this.renderer = renderer;
        this.input = input;

        this.eventManager = new EventManager(new DefaultEventFactory(), renderer, input);
        this.monsterFactory = new MonsterFactory();
        this.itemFactory = new ItemFactory();
        this.marketController = new MarketController(renderer, input);

        this.state = GameState.EXPLORATION;
        this.random = new Random();
    }

    public void run() {
        boolean done = false;

        while (!done) {
            switch (state) {
                case EXPLORATION:
                    explorationStep();
                    break;
                case BATTLE:
                    battleStep();
                    break;
                case GAME_OVER:
                    done = true;
                    break;
            }
        }

        renderer.renderMessage("Game over. Thanks for playing!");
    }

    // ------------------------------------------------------------
    // EXPLORATION LOOP
    // ------------------------------------------------------------

    private void explorationStep() {
        // Always show world + heroes summary for context
        renderer.renderWorld(world);
        renderer.renderHeroStats(party);

        boolean onMarket = (world.getCurrentTile().getType() == TileType.MARKET);

        renderer.renderMessage("Choose action:");
        renderer.renderMessage("  1) Move");
        renderer.renderMessage("  2) Manage inventory / equipment");
        renderer.renderMessage("  3) Show map");
        renderer.renderMessage("  4) Quit");
        if (onMarket) {
            renderer.renderMessage("  5) Open market");
        }

        int choice = input.readInt();

        switch (choice) {
            case 1:
                handleMovement();
                break;
            case 2:
                handleInventoryMenu();
                break;
            case 3:
                renderer.renderWorld(world);
                break;
            case 4:
                state = GameState.GAME_OVER;
                break;
            case 5:
                if (onMarket) {
                    openMarketOnCurrentTile();
                } else {
                    renderer.renderMessage("You are not standing on a market tile.");
                }
                break;
            default:
                renderer.renderMessage("Invalid choice.");
                break;
        }
    }

    private void handleMovement() {
        // Let InputHandler decide how to read movement (WASD, number, etc.)
        // and World.move(Direction) or similar handle it.
        boolean moved = world.move(input.readMovement());

        if (!moved) {
            renderer.renderMessage("You cannot move there.");
            return;
        }

        // Heal heroes slightly after a successful move (NOT in battle)
        healHeroesAfterMove();

        // Trigger random event (good/bad) if applicable
        eventManager.maybeTriggerEvent(world, party);

        Tile current = world.getCurrentTile();

        // Possibly trigger a random battle on a common tile
        if (current.getType() == TileType.COMMON) {
            int roll = random.nextInt(100);
            if (roll < GameBalance.BATTLE_CHANCE_COMMON_TILE) {
                state = GameState.BATTLE;
                return;
            }
        }

        // Prepare market object for this tile (lazy initialization)
        if (current.getType() == TileType.MARKET) {
            if (current instanceof MarketTile) {
                MarketTile mTile = (MarketTile) current;
                if (mTile.getMarket() == null) {
                    int avgLevel = averageHeroLevel();
                    mTile.setMarket(new Market(itemFactory, avgLevel));
                }
                // We no longer auto-open here; player can choose option 5 in exploration.
            }
        }
    }

    private void healHeroesAfterMove() {
        for (Hero h : party) {
            if (!h.isFainted()) {
                h.heal(GameBalance.MOVE_HEAL_AMOUNT);
            }
        }
    }

    // ------------------------------------------------------------
    // MARKET ACCESS
    // ------------------------------------------------------------

    private void openMarketOnCurrentTile() {
        Tile tile = world.getCurrentTile();
        if (!(tile instanceof MarketTile)) {
            renderer.renderMessage("This is not a market tile.");
            return;
        }

        MarketTile mTile = (MarketTile) tile;
        if (mTile.getMarket() == null) {
            int avgLevel = averageHeroLevel();
            mTile.setMarket(new Market(itemFactory, avgLevel));
        }

        marketController.openMarket(mTile.getMarket(), party);
    }

    private int averageHeroLevel() {
        if (party.isEmpty()) return 1;
        int sum = 0;
        for (Hero h : party) {
            sum += h.getLevel();
        }
        return sum / party.size();
    }

    // ------------------------------------------------------------
    // INVENTORY / EQUIPMENT MENU (OUTSIDE BATTLES)
    // ------------------------------------------------------------

    private void handleInventoryMenu() {
        if (party.isEmpty()) {
            renderer.renderMessage("There are no heroes in your party.");
            return;
        }

        renderer.renderMessage("Choose a hero:");
        for (int i = 0; i < party.size(); i++) {
            Hero h = party.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " + h.getName());
        }
        renderer.renderMessage("  0) Back");

        int idx = input.readInt();
        if (idx == 0) return;
        idx--;

        if (idx < 0 || idx >= party.size()) {
            renderer.renderMessage("Invalid hero choice.");
            return;
        }

        Hero hero = party.get(idx);
        boolean done = false;

        while (!done) {
            renderer.renderHeroStats(party); // show full info including equipped
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
                    break;
            }
        }
    }

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

        selected.consume(hero); // Potion implements Consumable
        hero.getInventory().remove(selected);
        renderer.renderMessage(hero.getName() + " used potion: " + selected.getName());
    }

    // ------------------------------------------------------------
    // BATTLE TRANSITION
    // ------------------------------------------------------------

    private void battleStep() {
        int avgLevel = averageHeroLevel();
        List<Monster> monsters = monsterFactory.spawnMonstersForBattle(party.size(), avgLevel);

        Battle battle = new StandardBattle(party, monsters, renderer, input, itemFactory);
        battle.start();

        boolean hasLivingHero = false;
        for (Hero h : party) {
            if (!h.isFainted()) {
                hasLivingHero = true;
                break;
            }
        }
        if (!hasLivingHero) {
            state = GameState.GAME_OVER;
        } else {
            state = GameState.EXPLORATION;
        }
    }
}
