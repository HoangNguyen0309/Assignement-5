package battle;

import java.util.List;

import characters.Hero;
import characters.Monster;
import io.InputHandler;
import io.Renderer;

public class SkipAction implements HeroAction {

    @Override
    public String getName() {
        return "Skip";
    }

    @Override
    public void execute(Hero hero,
                        List<Hero> heroes,
                        List<Monster> monsters,
                        Renderer renderer,
                        InputHandler input,
                        StandardBattle.BattleContext ctx) {
        renderer.renderMessage(hero.getName() + " skips the turn.");
    }
}
