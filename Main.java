
import java.util.List;

import characters.Hero;
import core.GameEngine;
import data.HeroFactory;
import io.ConsoleInputHandler;
import io.ConsoleRenderer;
import io.InputHandler;
import io.Renderer;
import party.PartyBuilder;
import world.World;

public class Main {
    public static void main(String[] args) {
        Renderer renderer = new ConsoleRenderer();
        InputHandler input = new ConsoleInputHandler();

        renderer.renderMessage("Welcome to Legends: Monsters and Heroes!");

        HeroFactory heroFactory = new HeroFactory();

        // Build the hero party from files + class selection
        List<Hero> party = PartyBuilder.buildParty(renderer, input, heroFactory);

        // If party ends up empty (user backed out everything), just exit
        if (party.isEmpty()) {
            renderer.renderMessage("No heroes selected. Exiting game.");
            return;
        }

        World world = new World(8);
        GameEngine engine = new GameEngine(world, party, renderer, input);
        engine.run();
    }
}
