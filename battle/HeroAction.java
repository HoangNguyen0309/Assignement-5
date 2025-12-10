package battle;

import java.util.List;

import characters.Hero;
import characters.Monster;
import io.Renderer;
import io.InputHandler;

public interface HeroAction {
    String getName();
    void execute(Hero hero,
                 List<Hero> heroes,
                 List<Monster> monsters,
                 Renderer renderer,
                 InputHandler input,
                 StandardBattle.BattleContext ctx);
}
