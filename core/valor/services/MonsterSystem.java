package core.valor.services;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import characters.Hero;
import characters.Monster;
import core.Position;
import core.valor.ValorContext;

/**
 * Monster AI:
 * - All monsters act each monster phase.
 * - If hero in range -> attack
 * - Else BFS pathfind (DOWN/LEFT/RIGHT only) to nearest hero in SAME lane
 * - If no path: try DOWN, else sidestep LEFT/RIGHT to avoid obstacle, else do nothing
 *
 * BFS uses string keys "r,c" so it does NOT require Position.equals/hashCode.
 */
public class MonsterSystem {

    public void takeTurn(ValorContext ctx) {
        // IMPORTANT: do NOT return after first monster.
        for (Monster monster : ctx.monsters) {
            if (monster.isFainted()) continue;

            Position start = ctx.monsterPositions.get(monster);
            if (start == null) continue;

            // 1) Attack if any hero in range
            Hero target = findHeroInRange(ctx, start);
            if (target != null) {
                attack(ctx, monster, target);
                continue; // next monster
            }

            // 2) Pathfind (prefers forward progress; no "up" moves)
            Position step = nextStepTowardNearestHeroSameLane(ctx, monster, start);
            if (step != null && canMoveTo(ctx, monster, start, step)) {
                ctx.monsterPositions.put(monster, step);
                ctx.renderer.renderMessage(monster.getName() + " moves to (" + step.getRow() + ", " + step.getCol() + ").");
                ctx.log(monster.getName() + " moved to (" + step.getRow() + ", " + step.getCol() + ").");

                continue;
            }

            // 3) Fallback: try forward first
            Position down = new Position(start.getRow() + 1, start.getCol());
            if (canMoveTo(ctx, monster, start, down)) {
                ctx.monsterPositions.put(monster, down);
                ctx.renderer.renderMessage(monster.getName() + " moves to (" + down.getRow() + ", " + down.getCol() + ").");
                ctx.log(monster.getName() + " moves to (" + down.getRow() + ", " + down.getCol() + ").");

                continue;
            }

            // 4) If forward blocked by obstacle/monster, sidestep left/right within lane to go around
            Position left = new Position(start.getRow(), start.getCol() - 1);
            if (canMoveTo(ctx, monster, start, left) && ctx.world.sameLane(start, left)) {
                ctx.monsterPositions.put(monster, left);
                ctx.renderer.renderMessage(monster.getName() + " sidesteps to (" + left.getRow() + ", " + left.getCol() + ").");
                continue;
            }

            Position right = new Position(start.getRow(), start.getCol() + 1);
            if (canMoveTo(ctx, monster, start, right) && ctx.world.sameLane(start, right)) {
                ctx.monsterPositions.put(monster, right);
                ctx.renderer.renderMessage(monster.getName() + " sidesteps to (" + right.getRow() + ", " + right.getCol() + ").");
                continue;
            }

            // else: stuck
        }

        ctx.tickHeroImmunity();
    }

    private boolean canMoveTo(ValorContext ctx, Monster monster, Position from, Position to) {
        if (to == null) return false;
        if (!ValorRules.isInsideBoard(ctx, to.getRow(), to.getCol())) return false;
        if (!ctx.world.isAccessible(to)) return false;
        if (ValorRules.isOccupiedByMonster(ctx, to, monster)) return false;
        if (ValorRules.wouldMovePastEnemy(ctx, from, to, false)) return false;
        // do not move onto a hero tile
        if (isHeroTile(ctx, to)) return false;
        return true;
    }

    private Hero findHeroInRange(ValorContext ctx, Position monsterPos) {
        for (Hero h : ctx.heroes) {
            if (h.isFainted()) continue;
            Position hp = ctx.heroPositions.get(h);
            if (hp == null) continue;
            if (ValorRules.isInRange(ctx, monsterPos, hp)) return h;
        }
        return null;
    }

    private void attack(ValorContext ctx, Monster monster, Hero target) {
        if (ctx.isHeroImmune(target)) {
            ctx.renderer.renderMessage(target.getName() + " is immune this turn and ignores " + monster.getName() + "'s attack.");
            return;
        }

        int rawDamage = monster.getDamage();
        int reduced = rawDamage - target.getArmorReduction();
        if (reduced < 0) reduced = 0;

        if (target.tryDodge()) {
            ctx.renderer.renderMessage(target.getName() + " dodged the attack from " + monster.getName() + "!");
            return;
        }

        target.takeDamage(reduced);
        ctx.renderer.renderMessage(monster.getName() + " attacked " + target.getName() + " for " + reduced + " damage.");

    }

    /**
     * BFS in the monster's lane to nearest hero.
     * Allowed moves for monsters: DOWN, LEFT, RIGHT (no UP), so they don't "backtrack".
     * Returns the next step (one move) or null.
     */
    private Position nextStepTowardNearestHeroSameLane(ValorContext ctx, Monster monster, Position start) {
        List<Position> targets = new ArrayList<Position>();
        for (Hero h : ctx.heroes) {
            if (h.isFainted()) continue;
            Position hp = ctx.heroPositions.get(h);
            if (hp != null && ctx.world.sameLane(start, hp)) targets.add(hp);
        }
        if (targets.isEmpty()) return null;

        Deque<Position> q = new ArrayDeque<Position>();
        Map<String, String> prev = new HashMap<String, String>();
        Map<String, Position> posByKey = new HashMap<String, Position>();
        Map<String, Boolean> seen = new HashMap<String, Boolean>();

        String startKey = key(start);
        q.addLast(start);
        seen.put(startKey, Boolean.TRUE);
        posByKey.put(startKey, start);

        String foundKey = null;

        while (!q.isEmpty()) {
            Position cur = q.removeFirst();
            String curKey = key(cur);

            if (isTarget(cur, targets)) {
                foundKey = curKey;
                break;
            }

            for (Position nb : monsterNeighbors(cur)) { // DOWN/LEFT/RIGHT only
                if (!ValorRules.isInsideBoard(ctx, nb.getRow(), nb.getCol())) continue;
                if (!ctx.world.sameLane(start, nb)) continue;
                if (!ctx.world.isAccessible(nb)) continue;

                // block other monsters
                if (!ValorRules.samePos(nb, start) && ValorRules.isOccupiedByMonster(ctx, nb, monster)) continue;

                // don't path "through" heroes (only allow hero tile as the goal)
                if (!isTarget(nb, targets) && isHeroTile(ctx, nb)) continue;

                String nbKey = key(nb);
                if (seen.containsKey(nbKey)) continue;

                seen.put(nbKey, Boolean.TRUE);
                prev.put(nbKey, curKey);
                posByKey.put(nbKey, nb);
                q.addLast(nb);
            }
        }

        if (foundKey == null) return null;

        // reconstruct first step from start -> foundKey
        String curKey = foundKey;
        String pKey = prev.get(curKey);
        if (pKey == null) return null;

        while (pKey != null && !pKey.equals(startKey)) {
            curKey = pKey;
            pKey = prev.get(curKey);
        }

        Position step = posByKey.get(curKey);
        if (step == null) return null;
        if (isHeroTile(ctx, step)) return null;
        return step;
    }

    private List<Position> monsterNeighbors(Position p) {
        // DOWN first so BFS naturally prefers progress
        List<Position> n = new ArrayList<Position>(3);
        n.add(new Position(p.getRow() + 1, p.getCol()));     // down
        n.add(new Position(p.getRow(), p.getCol() - 1));     // left
        n.add(new Position(p.getRow(), p.getCol() + 1));     // right
        return n;
    }

    private boolean isHeroTile(ValorContext ctx, Position p) {
        for (Map.Entry<Hero, Position> e : ctx.heroPositions.entrySet()) {
            if (e.getKey().isFainted()) continue;
            Position hp = e.getValue();
            if (ValorRules.samePos(hp, p)) return true;
        }
        return false;
    }

    private boolean isTarget(Position p, List<Position> targets) {
        for (Position t : targets) {
            if (ValorRules.samePos(t, p)) return true;
        }
        return false;
    }

    private String key(Position p) {
        return p.getRow() + "," + p.getCol();
    }
}
