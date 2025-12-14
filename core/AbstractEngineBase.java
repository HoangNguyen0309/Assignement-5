package core;

import java.util.Random;

import data.ItemFactory;
import data.MonsterFactory;
import io.InputHandler;
import io.Renderer;
import market.MarketController;

/**
 * Shared wiring base for engines.
 * IMPORTANT: This does NOT own a game loop. It only centralizes dependencies.
 *
 * Both GameEngine and ValorGameEngine can extend this to avoid repeated setup.
 */
public abstract class AbstractEngineBase {

    protected final Renderer renderer;
    protected final InputHandler input;

    protected final ItemFactory itemFactory;
    protected final MonsterFactory monsterFactory;

    // If you already have one, keep it. Otherwise it's safe to share here.
    protected final MarketController marketController;

    protected final Random rng;

    protected AbstractEngineBase(Renderer renderer, InputHandler input) {
        this.renderer = renderer;
        this.input = input;

        this.itemFactory = new ItemFactory();
        this.monsterFactory = new MonsterFactory();
        this.marketController = new MarketController(renderer, input);

        this.rng = new Random();
    }
}
