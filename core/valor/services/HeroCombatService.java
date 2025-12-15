package core.valor.services;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import characters.Monster;
import config.GameBalance;
import core.Position;
import core.valor.ValorContext;
import items.Item;
import items.Spell;

public class HeroCombatService {

    public boolean attack(ValorContext ctx, Hero hero) {
        Position heroPos = ctx.heroPositions.get(hero);
        if (heroPos == null) return false;

        List<Monster> targets = new ArrayList<Monster>();
        for (Monster m : ctx.monsters) {
            if (m.isFainted()) continue;
            Position mp = ctx.monsterPositions.get(m);
            if (mp != null && ValorRules.isInRange(ctx, heroPos, mp)) targets.add(m);
        }

        if (targets.isEmpty()) {
            ctx.renderer.renderMessage("No monsters in range (same tile or adjacent required).");
            return false;
        }

        ctx.renderer.renderMessage("Choose a monster to attack:");
        for (int i = 0; i < targets.size(); i++) {
            Monster m = targets.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + m.getName()
                    + " (Lv " + m.getLevel()
                    + ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int choice = ctx.input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= targets.size()) {
            ctx.renderer.renderMessage("Invalid target.");
            return false;
        }

        Monster target = targets.get(choice);
        int baseDamage = hero.basicAttackDamage();

        int before = target.getHP();
        target.takeDamage(baseDamage);
        int effective = before - target.getHP();
        if (effective < 0) effective = 0;

        ctx.renderer.renderMessage(hero.getName() + " attacked " + target.getName()
                + " for " + effective + " damage.");
        ctx.log(hero.getName() + " attacked " + target.getName() + " for " + effective + " damage.");

        if (target.isFainted()) {
            ctx.renderer.renderMessage(target.getName() + " has been defeated!");
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL_VALOR;
            int gold = target.getLevel() * 500;
            hero.gainExperience(xp);
            hero.addGold(gold);
            ctx.renderer.renderMessage(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
            ctx.log(target.getName() + " has been defeated.");
            ctx.log(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
        }

        return true;
    }

    public boolean castSpell(ValorContext ctx, Hero hero) {
        Position heroPos = ctx.heroPositions.get(hero);
        if (heroPos == null) return false;

        List<Item> items = hero.getInventory().getItems();
        List<Spell> spells = new ArrayList<Spell>();
        for (Item it : items) if (it instanceof Spell) spells.add((Spell) it);

        if (spells.isEmpty()) {
            ctx.renderer.renderMessage(hero.getName() + " has no spells.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a spell to cast:");
        for (int i = 0; i < spells.size(); i++) {
            Spell s = spells.get(i);
            String effect = (s.getEffect() != null) ? s.getEffect().describe() : "";
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + s.getName()
                    + " | Dmg: " + s.getBaseDamage()
                    + " | MP: " + s.getManaCost()
                    + (effect.isEmpty() ? "" : " | Effect: " + effect));
        }
        ctx.renderer.renderMessage("  0) Back");

        int sc = ctx.input.readInt();
        if (sc == 0) return false;
        sc--;

        if (sc < 0 || sc >= spells.size()) {
            ctx.renderer.renderMessage("Invalid spell choice.");
            return false;
        }

        Spell spell = spells.get(sc);
        if (hero.getMana() < spell.getManaCost()) {
            ctx.renderer.renderMessage("Not enough mana.");
            return false;
        }

        List<Monster> targets = new ArrayList<Monster>();
        for (Monster m : ctx.monsters) {
            if (m.isFainted()) continue;
            Position mp = ctx.monsterPositions.get(m);
            if (mp != null && ValorRules.isInRange(ctx, heroPos, mp)) targets.add(m);
        }

        if (targets.isEmpty()) {
            ctx.renderer.renderMessage("No monsters in range for the spell.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a monster target:");
        for (int i = 0; i < targets.size(); i++) {
            Monster m = targets.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + m.getName()
                    + " (HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int tc = ctx.input.readInt();
        if (tc == 0) return false;
        tc--;

        if (tc < 0 || tc >= targets.size()) {
            ctx.renderer.renderMessage("Invalid target choice.");
            return false;
        }

        Monster target = targets.get(tc);

        hero.restoreMana(-spell.getManaCost());
        int before = target.getHP();
        spell.cast(hero, target);
        int effective = before - target.getHP();
        if (effective < 0) effective = 0;

        ctx.renderer.renderMessage(hero.getName() + " casts " + spell.getName()
                + " on " + target.getName() + " for " + effective + " damage.");
        ctx.log(hero.getName() + " casts " + spell.getName()
                + " on " + target.getName() + " for " + effective + " damage.");

        if (target.isFainted()) {
            ctx.renderer.renderMessage(target.getName() + " has been defeated!");
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL_VALOR;
            int gold = target.getLevel() * 500;
            hero.gainExperience(xp);
            hero.addGold(gold);
            ctx.renderer.renderMessage(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
            ctx.log(target.getName() + " has been defeated.");
            ctx.log(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
        }

        return true;
    }
}
