package events;

import java.util.List;

import characters.Hero;
import io.InputHandler;
import io.Renderer;
import world.World;

public class AmbushTrapEvent extends AbstractGameEvent {

    public AmbushTrapEvent() {
        super("Ambush Trap", EventType.BAD);
    }

    public void start(List<Hero> party, World world,
                      Renderer renderer, InputHandler input) {
        renderer.renderMessage("A hidden trap is triggered! Poison darts shoot from the walls.");
    }

    public EventResultType resolve(List<Hero> party, World world) {
        for (Hero hero : party) {
            if (!hero.isFainted()) {
                int damage = (int) (hero.getMaxHP() * 0.1);
                hero.takeDamage(damage);
            }
        }
        return EventResultType.SUCCESS;
    }
}
