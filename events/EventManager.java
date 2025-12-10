package events;

import java.util.List;

import characters.Hero;
import io.InputHandler;
import io.Renderer;
import world.Tile;
import world.World;

public class EventManager {
    private EventFactory factory;
    private Renderer renderer;
    private InputHandler input;

    public EventManager(EventFactory factory, Renderer renderer, InputHandler input) {
        this.factory = factory;
        this.renderer = renderer;
        this.input = input;
    }

    public void maybeTriggerEvent(World world, List<Hero> party) {
        Tile tile = world.getCurrentTile();
        int avgLevel = averageLevel(party);
        GameEvent event = factory.randomEventForTile(tile, avgLevel);
        if (event == null) {
            return;
        }
        renderer.renderMessage("Event: " + event.getName());
        event.start(party, world, renderer, input);
        EventResultType result = event.resolve(party, world);
        renderer.renderMessage("Event result: " + result);
    }

    private int averageLevel(List<Hero> party) {
        if (party.isEmpty()) return 1;
        int sum = 0;
        for (Hero h : party) {
            sum += h.getLevel();
        }
        return sum / party.size();
    }
}
