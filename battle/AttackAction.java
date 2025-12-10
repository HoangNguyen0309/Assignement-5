package battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import characters.Hero;
import characters.Monster;
import io.InputHandler;
import io.Renderer;

public class AttackAction implements HeroAction {

    private Random rand = new Random();

    @Override
    public String getName() {
        return "Attack";
    }

    @Override
    public void execute(Hero hero,
                        List<Hero> heroes,
                        List<Monster> monsters,
                        Renderer renderer,
                        InputHandler input,
                        StandardBattle.BattleContext ctx) {

        List<Monster> living = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (!m.isFainted()) living.add(m);
        }
        if (living.isEmpty()) {
            renderer.renderMessage("No monsters to attack.");
            return;
        }

        Monster target = living.get(rand.nextInt(living.size()));
        int baseDamage = hero.basicAttackDamage();
        int effective = baseDamage - target.getDefense();
        if (effective < 0) effective = 0;

        target.takeDamage(baseDamage); // Monster takes defense into account internally
        ctx.addDamageDealt(hero, effective);

        renderer.renderMessage(hero.getName() + " attacked " +
                target.getName() + " for " + effective + " damage.");
        ctx.removeDeadMonsters();
    }
}
