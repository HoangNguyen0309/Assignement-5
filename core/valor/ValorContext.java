package core.valor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import characters.Hero;
import characters.Monster;
import data.ItemFactory;
import data.MonsterFactory;
import io.InputHandler;
import io.Renderer;
import market.MarketController;
import world.TileType;
import world.World;
import core.Position;

public class ValorContext {

    // Dependencies
    public final World world;
    public final List<Hero> heroes;
    public final Renderer renderer;
    public final InputHandler input;

    public final MarketController marketController;
    public final MonsterFactory monsterFactory;
    public final ItemFactory itemFactory;
    public final Random random;
    public final int monsterWavePeriod;

    // State maps
    public final Map<Hero, Position> heroPositions = new HashMap<Hero, Position>();
    public final Map<Hero, Position> heroSpawnPositions = new HashMap<Hero, Position>();
    public final Map<Hero, TileType> activeTerrainBuff = new HashMap<Hero, TileType>();
    public final Map<Hero, int[]> terrainBuffDeltas = new HashMap<Hero, int[]>();

    public final List<Monster> monsters = new ArrayList<Monster>();
    public final Map<Monster, Position> monsterPositions = new HashMap<Monster, Position>();
    public final Map<Monster, String> monsterCodes = new HashMap<Monster, String>();

    public final Map<Hero, String> heroCodes = new HashMap<Hero, String>();
    public final Map<Integer, Integer> laneMaxLevels = new HashMap<Integer, Integer>(); // laneIndex -> max hero level

    public final List<String> roundLog = new ArrayList<String>();

    public int roundCount = 1;
    public boolean gameOver = false;

    public ValorContext(World world,
                        List<Hero> heroes,
                        Renderer renderer,
                        InputHandler input,
                        int monsterWavePeriod) {
        this.world = world;
        this.heroes = heroes;
        this.renderer = renderer;
        this.input = input;

        this.marketController = new MarketController(renderer, input);
        this.monsterFactory = new MonsterFactory();
        this.itemFactory = new ItemFactory();
        this.random = new Random();
        this.monsterWavePeriod = monsterWavePeriod;
    }

    public void log(String msg) {
        if (roundLog != null) roundLog.add(msg);
    }
}
