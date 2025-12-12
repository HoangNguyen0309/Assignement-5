import java.util.List;

import characters.Hero;
import core.GameEngine;
import core.ValorGameEngine;
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

        renderer.renderMessage("Welcome to Legends!");
        renderer.renderMessage("Choose a game mode:");
        renderer.renderMessage("  1) Legends: Monsters and Heroes");
        renderer.renderMessage("  2) Legends of Valor");

        int mode = 0;
        while (mode < 1 || mode > 2) {
            mode = input.readInt();
            if (mode < 1 || mode > 2) {
                renderer.renderMessage("Please enter 1 or 2.");
            }
        }

        HeroFactory heroFactory = new HeroFactory();
        List<Hero> party;

        if (mode == 1) {
            // Classic game: player chooses 1–3 heroes
            party = PartyBuilder.buildParty(renderer, input, heroFactory);
        } else {
            // Legends of Valor: exactly 3 heroes, one per lane
            party = PartyBuilder.buildValorParty(renderer, input, heroFactory);
        }

        if (party.isEmpty()) {
            renderer.renderMessage("No heroes selected. Exiting game.");
            return;
        }

        if (mode == 1) {
            World world = new World(8, "Hero and Monster");
            GameEngine engine = new GameEngine(world, party, renderer, input);
            engine.run();
        } else {
            World world = World.createValorWorld();
            ValorGameEngine valorEngine = new ValorGameEngine(world, party, renderer, input);
            valorEngine.run();
        }
    }
}
