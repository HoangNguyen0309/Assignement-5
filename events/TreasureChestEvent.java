package events;

import java.util.List;
import java.util.Random;

import characters.Hero;
import io.InputHandler;
import io.Renderer;
import world.World;

public class TreasureChestEvent extends AbstractGameEvent {

    public TreasureChestEvent() {
        super("Treasure Chest", EventType.GOOD);
    }

    public void start(List<Hero> party, World world,
                      Renderer renderer, InputHandler input) {
        renderer.renderMessage("You find a mysterious treasure chest. Open it? (Y/N)");
        boolean open = input.readYesNo();
        if (!open) {
            renderer.renderMessage("You decide to leave the chest alone.");
        }
    }

    public EventResultType resolve(List<Hero> party, World world) {
        if (party.isEmpty()) {
            return EventResultType.FAILURE;
        }
        Random rand = new Random();
        Hero hero = party.get(rand.nextInt(party.size()));
        int gold = 100 + rand.nextInt(100);
        hero.addGold(gold);
        return EventResultType.SUCCESS;
    }
}
