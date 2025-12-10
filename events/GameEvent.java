package events;

import java.util.List;

import characters.Hero;
import io.InputHandler;
import io.Renderer;
import world.World;

public interface GameEvent {
    String getName();
    EventType getType();

    void start(List<Hero> party, World world, Renderer renderer, InputHandler input);

    EventResultType resolve(List<Hero> party, World world);
}

