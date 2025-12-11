package battle;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import characters.Monster;
import io.InputHandler;
import io.Renderer;
import items.Item;
import items.Spell;

public class CastSpellAction implements HeroAction {

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

        // Collect all spells in hero inventory
        List<Item> items = hero.getInventory().getItems();
        List<Spell> spells = new ArrayList<Spell>();
        for (Item item : items) {
            if (item instanceof Spell) {
                spells.add((Spell) item);
            }
        }

        if (spells.isEmpty()) {
            renderer.renderMessage(hero.getName() + " has no spells.");
            return;
        }

        // Choose spell
        renderer.renderMessage("Choose a spell to cast:");
        for (int i = 0; i < spells.size(); i++) {
            Spell s = spells.get(i);
            String effectDesc = (s.getEffect() != null) ? s.getEffect().describe() : "";
            renderer.renderMessage("  " + (i + 1) + ") " + s.getName() +
                    " | Dmg: " + s.getBaseDamage() +
                    " | MP: " + s.getManaCost() +
                    " | Effect: " + effectDesc);
        }
        renderer.renderMessage("  0) Back");

        int spellChoice = input.readInt();
        if (spellChoice == 0) {
            return;
        }
        spellChoice--;

        if (spellChoice < 0 || spellChoice >= spells.size()) {
            renderer.renderMessage("Invalid spell choice.");
            return;
        }

        Spell spell = spells.get(spellChoice);

        if (hero.getMana() < spell.getManaCost()) {
            renderer.renderMessage("Not enough mana.");
            return;
        }

        // Build list of living monsters
        List<Monster> living = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (!m.isFainted()) {
                living.add(m);
            }
        }

        if (living.isEmpty()) {
            renderer.renderMessage("No monsters to target.");
            return;
        }

        // Choose target monster
        renderer.renderMessage("Choose a monster to target with " + spell.getName() + ":");
        for (int i = 0; i < living.size(); i++) {
            Monster m = living.get(i);
            renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() +
                    " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        renderer.renderMessage("  0) Back");

        int targetChoice = input.readInt();
        if (targetChoice == 0) {
            return;
        }
        targetChoice--;

        if (targetChoice < 0 || targetChoice >= living.size()) {
            renderer.renderMessage("Invalid target choice.");
            return;
        }

        Monster target = living.get(targetChoice);

        // Spend mana and cast
        hero.restoreMana(-spell.getManaCost());

        int hpBefore = target.getHP();
        int rawDealt = spell.cast(hero, target);
        int hpAfter = target.getHP();

        int effective = hpBefore - hpAfter;
        if (effective < 0) {
            effective = 0;
        }

        ctx.addDamageDealt(hero, effective);

        renderer.renderMessage(hero.getName() + " casts " +
                spell.getName() + " on " + target.getName() +
                " for " + effective + " damage (raw: " + rawDealt + ").");

        // Spell is single-use: remove from inventory
        hero.getInventory().remove(spell);

        ctx.removeDeadMonsters();
    }
}
