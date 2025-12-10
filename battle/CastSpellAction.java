package battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import characters.Hero;
import characters.Monster;
import io.InputHandler;
import io.Renderer;
import items.Item;
import items.Spell;

public class CastSpellAction implements HeroAction {

    private Random rand = new Random();

    @Override
    public String getName() {
        return "Cast Spell";
    }

    @Override
    public void execute(Hero hero,
                        List<Hero> heroes,
                        List<Monster> monsters,
                        Renderer renderer,
                        InputHandler input,
                        StandardBattle.BattleContext ctx) {

        List<Item> items = hero.getInventory().getItems();
        List<Spell> spells = new ArrayList<Spell>();
        for (Item item : items) {
            if (item instanceof Spell) {
                spells.add((Spell)item);
            }
        }

        if (spells.isEmpty()) {
            renderer.renderMessage(hero.getName() + " has no spells.");
            return;
        }

        renderer.renderMessage("Choose a spell:");
        for (int i = 0; i < spells.size(); i++) {
            Spell s = spells.get(i);
            renderer.renderMessage("  " + (i+1) + ") " + s.getName() +
                    " | Dmg: " + s.getBaseDamage() +
                    " | MP: " + s.getManaCost() +
                    " | Effect: " + (s.getEffect() != null ? s.getEffect().describe() : ""));
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return;
        choice--;

        if (choice < 0 || choice >= spells.size()) {
            renderer.renderMessage("Invalid choice.");
            return;
        }

        Spell spell = spells.get(choice);
        if (hero.getMana() < spell.getManaCost()) {
            renderer.renderMessage("Not enough mana.");
            return;
        }

        List<Monster> living = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (!m.isFainted()) living.add(m);
        }
        if (living.isEmpty()) {
            renderer.renderMessage("No monsters to target.");
            return;
        }

        Monster target = living.get(rand.nextInt(living.size()));

        hero.restoreMana(-spell.getManaCost());
        int dealt = spell.cast(hero, target);
        ctx.addDamageDealt(hero, dealt);

        renderer.renderMessage(hero.getName() + " casts " +
                spell.getName() + " on " + target.getName() +
                " for " + dealt + " damage!");

        hero.getInventory().remove(spell);
        ctx.removeDeadMonsters();
    }
}
