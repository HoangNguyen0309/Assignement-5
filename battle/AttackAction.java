package battle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import characters.Hero;
import characters.Monster;
import io.InputHandler;
import io.Renderer;

public class AttackAction implements HeroAction {

    // For callers (like Valor) that want to know what happened after execute()
    private Monster lastTarget = null;
    private int lastEffectiveDamage = 0;

    public Monster getLastTarget() {
        return lastTarget;
    }

    public int getLastEffectiveDamage() {
        return lastEffectiveDamage;
    }

    private void clearLast() {
        lastTarget = null;
        lastEffectiveDamage = 0;
    }

    @Override
    public String getName() {
        return "Attack";
    }

    /**
     * StandardBattle version (unchanged behavior).
     */
    @Override
    public void execute(Hero hero,
                        List<Hero> heroes,
                        List<Monster> monsters,
                        Renderer renderer,
                        InputHandler input,
                        StandardBattle.BattleContext ctx) {

        clearLast();

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
            return; // cancel
        }
        choice--;

        if (choice < 0 || choice >= living.size()) {
            renderer.renderMessage("Invalid target choice.");
            return;
        }

        Monster target = living.get(choice);

        int baseDamage = hero.basicAttackDamage();

        int hpBefore = target.getHP();
        target.takeDamage(baseDamage);
        int hpAfter = target.getHP();

        int effective = hpBefore - hpAfter;
        if (effective < 0) effective = 0;

        // store for external readers
        lastTarget = target;
        lastEffectiveDamage = effective;

        ctx.addDamageDealt(hero, effective);

        renderer.renderMessage(hero.getName() + " attacked " +
                target.getName() + " for " + effective + " damage.");

        ctx.removeDeadMonsters();
    }

    /**
     * Valor (and other modes) version: same UI + damage, but no BattleContext dependency.
     * Caller can read getLastTarget()/getLastEffectiveDamage() after execution.
     */
    public void execute(Hero hero,
                        List<Hero> heroes,
                        List<Monster> monsters,
                        Renderer renderer,
                        InputHandler input) {

        clearLast();

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
            return; // cancel
        }
        choice--;

        if (choice < 0 || choice >= living.size()) {
            renderer.renderMessage("Invalid target choice.");
            return;
        }

        Monster target = living.get(choice);

        int baseDamage = hero.basicAttackDamage();

        int hpBefore = target.getHP();
        target.takeDamage(baseDamage);
        int hpAfter = target.getHP();

        int effective = hpBefore - hpAfter;
        if (effective < 0) effective = 0;

        lastTarget = target;
        lastEffectiveDamage = effective;

        renderer.renderMessage(hero.getName() + " attacked " +
                target.getName() + " for " + effective + " damage.");

        // Optional: remove dead from the provided list (safe for filtered lists too)
        Iterator<Monster> it = monsters.iterator();
        while (it.hasNext()) {
            if (it.next().isFainted()) it.remove();
        }
    }
}
