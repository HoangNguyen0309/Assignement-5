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
 * 1) If hero in range -> attack
 * 2) Else BFS pathfind to nearest hero in SAME lane (4-neighbor moves), respecting obstacles/monsters
 * 3) Else fallback: move forward (down 1)
 *
 * NOTE: BFS uses string keys "r,c" so it does NOT require Position.equals/hashCode.
 */
public class MonsterSystem {

    public void takeTurn(ValorContext ctx) {
        for (Monster monster : ctx.monsters) {
            if (monster.isFainted()) continue;

            Position start = ctx.monsterPositions.get(monster);
            if (start == null) continue;

            // 1) Attack hero in range
            Hero target = findHeroInRange(ctx, start);
            if (target != null) {
                attack(ctx, monster, target);
                return;
            }

            // 2) Pathfind
            Position step = nextStepTowardNearestHeroSameLane(ctx, monster, start);
            if (step != null
                    && ctx.world.isAccessible(step)
                    && !ValorRules.isOccupiedByMonster(ctx, step, monster)
                    && !ValorRules.wouldMovePastEnemy(ctx, start, step, false)) {

                ctx.monsterPositions.put(monster, step);
                ctx.renderer.renderMessage(monster.getName() + " moves to (" + step.getRow() + ", " + step.getCol() + ").");
                return;
            }

            // 3) Fallback forward
            Position forward = new Position(start.getRow() + 1, start.getCol());
            if (ValorRules.isInsideBoard(ctx, forward.getRow(), forward.getCol())
                    && ctx.world.isAccessible(forward)
                    && !ValorRules.isOccupiedByMonster(ctx, forward, monster)
                    && !ValorRules.wouldMovePastEnemy(ctx, start, forward, false)) {

                ctx.monsterPositions.put(monster, forward);
                ctx.renderer.renderMessage(monster.getName() + " moves to (" + forward.getRow() + ", " + forward.getCol() + ").");
            }
            return;
        }
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

    private Position nextStepTowardNearestHeroSameLane(ValorContext ctx, Monster monster, Position start) {
        // targets: heroes in same lane
        List<Position> targets = new ArrayList<Position>();
        for (Hero h : ctx.heroes) {
            if (h.isFainted()) continue;
            Position hp = ctx.heroPositions.get(h);
            if (hp != null && ctx.world.sameLane(start, hp)) targets.add(hp);
        }
        if (targets.isEmpty()) return null;

        // BFS
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

            for (Position nb : neighbors4(cur)) {
                if (!ValorRules.isInsideBoard(ctx, nb.getRow(), nb.getCol())) continue;
                if (!ctx.world.sameLane(start, nb)) continue;
                if (!ctx.world.isAccessible(nb)) continue;

                // block monsters
                if (!ValorRules.samePos(nb, start) && ValorRules.isOccupiedByMonster(ctx, nb, monster)) continue;

                // do not move through heroes (only allow hero tile as the goal)
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

        // reconstruct first step from start -> found
        String curKey = foundKey;
        String pKey = prev.get(curKey);
        if (pKey == null) return null;

        while (pKey != null && !pKey.equals(startKey)) {
            curKey = pKey;
            pKey = prev.get(curKey);
        }

        Position step = posByKey.get(curKey);
        if (step == null) return null;
        if (isHeroTile(ctx, step)) return null; // safety
        return step;
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

    private List<Position> neighbors4(Position p) {
        List<Position> n = new ArrayList<Position>(4);
        n.add(new Position(p.getRow() - 1, p.getCol()));
        n.add(new Position(p.getRow() + 1, p.getCol()));
        n.add(new Position(p.getRow(), p.getCol() - 1));
        n.add(new Position(p.getRow(), p.getCol() + 1));
        return n;
    }

    private String key(Position p) {
        return p.getRow() + "," + p.getCol();
    }
}
