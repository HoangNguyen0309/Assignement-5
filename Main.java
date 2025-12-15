import java.util.List;

import characters.Hero;
import core.GameEngine;
import core.ValorGameEngine;
import data.HeroFactory;
import io.BannerPrinter;
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
        BannerPrinter bannerPrinter = new BannerPrinter(renderer);

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
            bannerPrinter.printValorBanner();
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
            renderer.renderMessage("Choose difficulty for monster waves:");
            renderer.renderMessage("  1) Easy (wave every 6 rounds)");
            renderer.renderMessage("  2) Normal (wave every 4 rounds)");
            renderer.renderMessage("  3) Hard (wave every 2 rounds)");
            int wavePeriod = 6;
            int diff = 0;
            while (diff < 1 || diff > 3) {
                diff = input.readInt();
                switch (diff) {
                    case 1: wavePeriod = 6; break;
                    case 2: wavePeriod = 4; break;
                    case 3: wavePeriod = 2; break;
                    default:
                        renderer.renderMessage("Please choose 1, 2, or 3.");
                }
            }
            ValorGameEngine valorEngine = new ValorGameEngine(world, party, renderer, input, wavePeriod);
            valorEngine.start();
        }
    }
}
