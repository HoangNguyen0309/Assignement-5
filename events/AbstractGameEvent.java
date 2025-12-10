package events;

import java.util.List;

import characters.Hero;
import io.InputHandler;
import io.Renderer;
import world.World;

public abstract class AbstractGameEvent implements GameEvent {
    private String name;
    private EventType type;

    protected AbstractGameEvent(String name, EventType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public EventType getType() { return type; }

    public abstract void start(List<Hero> party, World world,
                               Renderer renderer, InputHandler input);

    public abstract EventResultType resolve(List<Hero> party, World world);
}
