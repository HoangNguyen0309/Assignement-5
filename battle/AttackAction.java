package battle;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import characters.Monster;
import io.InputHandler;
import io.Renderer;

public class AttackAction implements HeroAction {

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

        // Build list of living monsters
        List<Monster> living = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (!m.isFainted()) {
                living.add(m);
            }
        }

        if (living.isEmpty()) {
            renderer.renderMessage("No monsters to attack.");
            return;
        }

        // Let the player choose which monster to attack
        renderer.renderMessage("Choose a monster to attack:");
        for (int i = 0; i < living.size(); i++) {
            Monster m = living.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() +
                    " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) {
            // cancel attack
            return;
        }
        choice--;

        if (choice < 0 || choice >= living.size()) {
            renderer.renderMessage("Invalid target choice.");
            return;
        }

        Monster target = living.get(choice);

        int baseDamage = hero.basicAttackDamage();

        // Monster handles its own defense internally in takeDamage(...)
        int hpBefore = target.getHP();
        target.takeDamage(baseDamage);
        int hpAfter = target.getHP();

        int effective = hpBefore - hpAfter;
        if (effective < 0) {
            effective = 0;
        }

        ctx.addDamageDealt(hero, effective);

        renderer.renderMessage(hero.getName() + " attacked " +
                target.getName() + " for " + effective + " damage.");

        ctx.removeDeadMonsters();
    }
}
