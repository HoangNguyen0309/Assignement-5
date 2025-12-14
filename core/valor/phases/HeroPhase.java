package core.valor.phases;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import characters.Monster;
import config.GameBalance;
import core.Direction;
import core.Position;
import core.valor.ValorContext;
import core.valor.ValorSupport;
import items.Item;
import items.Spell;
import world.Tile;

public class HeroPhase implements Phase {

    @Override
    public void execute(ValorContext ctx) {
        ValorSupport.assignHeroCodes(ctx);

        for (Hero hero : ctx.heroes) {
            if (hero.isFainted()) continue;
            if (!ctx.heroPositions.containsKey(hero)) continue;

            boolean actionTaken = false;
            boolean showBoard = true;

            while (!actionTaken && !ctx.gameOver) {
                ValorSupport.renderHeroTurnMenu(ctx, hero, showBoard);
                int choice = ctx.input.readInt();

                switch (choice) {
                    case 1:
                        actionTaken = handleMove(ctx, hero);
                        break;
                    case 2:
                        actionTaken = handleAttack(ctx, hero);
                        break;
                    case 3:
                        actionTaken = handleCastSpell(ctx, hero);
                        break;
                    case 4:
                        actionTaken = ValorSupport.handleInventory(ctx, hero);
                        break;
                    case 5:
                        actionTaken = ValorSupport.handleRecall(ctx, hero);
                        break;
                    case 6:
                        ValorSupport.openShopIfAtHeroNexus(ctx, hero);
                        break;
                    case 7:
                        actionTaken = ValorSupport.handleTeleport(ctx, hero);
                        break;
                    case 8:
                        actionTaken = ValorSupport.handleRemoveObstacle(ctx, hero);
                        break;
                    case 9:
                        ctx.renderer.renderHeroStats(ctx.heroes, ctx.heroCodes);
                        ctx.renderer.renderMonsterStats(ctx.monsters, ctx.monsterCodes);
                        break;
                    case 10:
                        ctx.renderer.renderMessage(hero.getName() + " skips the turn.");
                        actionTaken = true;
                        break;
                    default:
                        ctx.renderer.renderMessage("Invalid choice.");
                        break;
                }

                ValorSupport.checkWinLoseConditions(ctx);
                if (!actionTaken) showBoard = false;
            }

            if (ctx.gameOver) return;
        }
    }

    private boolean handleMove(ValorContext ctx, Hero hero) {
        Position pos = ctx.heroPositions.get(hero);
        if (pos == null) return false;

        ctx.renderer.renderMessage("Use W/A/S/D to move.");
        Direction dir = ctx.input.readMovement();

        int newRow = pos.getRow();
        int newCol = pos.getCol();
        switch (dir) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
            default: break;
        }

        if (!ValorSupport.isInsideBoard(ctx, newRow, newCol)) {
            ctx.renderer.renderMessage("Cannot move outside the board.");
            return false;
        }

        Tile tile = ctx.world.getTile(newRow, newCol);
        if (!tile.isAccessible()) {
            ctx.renderer.renderMessage("That tile is not accessible.");
            return false;
        }

        Position dest = new Position(newRow, newCol);

        if (ValorSupport.isOccupiedByHero(ctx, dest, hero)) {
            ctx.renderer.renderMessage("Another hero is already there.");
            return false;
        }

        if (ValorSupport.wouldMovePastEnemy(ctx, pos, dest, true)) {
            ctx.renderer.renderMessage("You cannot move past a monster in your lane.");
            return false;
        }

        ctx.heroPositions.put(hero, dest);
        ValorSupport.applyTerrainEffects(ctx, hero, pos, dest);

        ctx.renderer.renderMessage(hero.getName() + " moved to (" + newRow + ", " + newCol + ").");
        ValorSupport.logAction(ctx, hero.getName() + " moved to (" + newRow + ", " + newCol + ").");
        return true;
    }

    private boolean handleAttack(ValorContext ctx, Hero hero) {
        Position heroPos = ctx.heroPositions.get(hero);
        if (heroPos == null) return false;

        List<Monster> targetsInRange = new ArrayList<Monster>();
        for (Monster m : ctx.monsters) {
            if (m.isFainted()) continue;
            Position mp = ctx.monsterPositions.get(m);
            if (mp == null) continue;
            if (ValorSupport.isInRange(ctx, heroPos, mp)) targetsInRange.add(m);
        }

        if (targetsInRange.isEmpty()) {
            ctx.renderer.renderMessage("No monsters in range (same tile or adjacent required).");
            return false;
        }

        ctx.renderer.renderMessage("Choose a monster to attack:");
        for (int i = 0; i < targetsInRange.size(); i++) {
            Monster m = targetsInRange.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() +
                    " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int choice = ctx.input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= targetsInRange.size()) {
            ctx.renderer.renderMessage("Invalid target.");
            return false;
        }

        Monster target = targetsInRange.get(choice);
        int baseDamage = hero.basicAttackDamage();

        int hpBefore = target.getHP();
        target.takeDamage(baseDamage);
        int hpAfter = target.getHP();
        int effective = hpBefore - hpAfter;
        if (effective < 0) effective = 0;

        ctx.renderer.renderMessage(hero.getName() +
                " attacked " + target.getName() +
                " for " + effective + " damage.");
        ValorSupport.logAction(ctx, hero.getName() + " attacked " + target.getName() + " for " + effective + " damage.");

        if (target.isFainted()) {
            ctx.renderer.renderMessage(target.getName() + " has been defeated!");
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL_VALOR;
            hero.gainExperience(xp);
            int gold = target.getLevel() * 500;
            hero.addGold(gold);
            ctx.renderer.renderMessage(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
            ValorSupport.logAction(ctx, target.getName() + " defeated by " + hero.getName() + " (+" + xp + " XP, +" + gold + " gold).");
            ValorSupport.updateLaneMaxLevels(ctx);
        }

        return true;
    }

    private boolean handleCastSpell(ValorContext ctx, Hero hero) {
        Position heroPos = ctx.heroPositions.get(hero);
        if (heroPos == null) return false;

        List<Item> items = hero.getInventory().getItems();
        List<Spell> spells = new ArrayList<Spell>();
        for (Item item : items) {
            if (item instanceof Spell) spells.add((Spell) item);
        }

        if (spells.isEmpty()) {
            ctx.renderer.renderMessage(hero.getName() + " has no spells.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a spell to cast:");
        for (int i = 0; i < spells.size(); i++) {
            Spell s = spells.get(i);
            String effectDesc = (s.getEffect() != null) ? s.getEffect().describe() : "";
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + s.getName() +
                    " | Dmg: " + s.getBaseDamage() +
                    " | MP: " + s.getManaCost() +
                    " | Effect: " + effectDesc);
        }
        ctx.renderer.renderMessage("  0) Back");

        int spellChoice = ctx.input.readInt();
        if (spellChoice == 0) return false;
        spellChoice--;

        if (spellChoice < 0 || spellChoice >= spells.size()) {
            ctx.renderer.renderMessage("Invalid spell choice.");
            return false;
        }

        Spell spell = spells.get(spellChoice);

        if (hero.getMana() < spell.getManaCost()) {
            ctx.renderer.renderMessage("Not enough mana.");
            return false;
        }

        List<Monster> targetsInRange = new ArrayList<Monster>();
        for (Monster m : ctx.monsters) {
            if (m.isFainted()) continue;
            Position mp = ctx.monsterPositions.get(m);
            if (mp == null) continue;
            if (ValorSupport.isInRange(ctx, heroPos, mp)) targetsInRange.add(m);
        }

        if (targetsInRange.isEmpty()) {
            ctx.renderer.renderMessage("No monsters in range for the spell (same tile or adjacent required).");
            return false;
        }

        ctx.renderer.renderMessage("Choose a monster to target with " + spell.getName() + ":");
        for (int i = 0; i < targetsInRange.size(); i++) {
            Monster m = targetsInRange.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " +
                    m.getName() +
                    " (Lv " + m.getLevel() +
                    ", HP " + m.getHP() + "/" + m.getMaxHP() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int targetChoice = ctx.input.readInt();
        if (targetChoice == 0) return false;
        targetChoice--;

        if (targetChoice < 0 || targetChoice >= targetsInRange.size()) {
            ctx.renderer.renderMessage("Invalid target choice.");
            return false;
        }

        Monster target = targetsInRange.get(targetChoice);

        hero.restoreMana(-spell.getManaCost());
        int hpBefore = target.getHP();
        int rawDealt = spell.cast(hero, target);
        int hpAfter = target.getHP();

        int effective = hpBefore - hpAfter;
        if (effective < 0) effective = 0;

        ctx.renderer.renderMessage(hero.getName() + " casts " +
                spell.getName() + " on " + target.getName() +
                " for " + effective + " damage (raw: " + rawDealt + ").");
        ValorSupport.logAction(ctx, hero.getName() + " casts " + spell.getName() +
                " on " + target.getName() + " for " + effective + " damage.");

        if (target.isFainted()) {
            ctx.renderer.renderMessage(target.getName() + " has been defeated!");
            int xp = target.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL_VALOR;
            hero.gainExperience(xp);
            int gold = target.getLevel() * 500;
            hero.addGold(gold);
            ctx.renderer.renderMessage(hero.getName() + " gains " + xp + " XP and " + gold + " gold.");
            ValorSupport.logAction(ctx, target.getName() + " defeated by " + hero.getName() + " (+" + xp + " XP, +" + gold + " gold).");
            ValorSupport.updateLaneMaxLevels(ctx);
        }

        return true;
    }
}
