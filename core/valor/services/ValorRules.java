package core.valor.services;

import java.util.Map;

import characters.Hero;
import characters.Monster;
import core.Position;
import core.valor.ValorContext;

/**
 * Shared, pure-ish rule checks for Legends of Valor.
 * Intentionally avoids relying on Position.equals/hashCode by comparing row/col.
 */
public final class ValorRules {

    private ValorRules() {}

    // ------------------------------------------------------------
    // Basic geometry / board checks
    // ------------------------------------------------------------

    public static boolean isInsideBoard(ValorContext ctx, int r, int c) {
        int size = ctx.world.getSize();
        return r >= 0 && r < size && c >= 0 && c < size;
    }

    public static boolean samePos(Position a, Position b) {
        return a != null && b != null && a.getRow() == b.getRow() && a.getCol() == b.getCol();
    }

    /**
     * In range = same tile OR any of 8 neighbors, AND in the same lane.
     */
    public static boolean isInRange(ValorContext ctx, Position a, Position b) {
        if (a == null || b == null) return false;
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getCol() - b.getCol());
        return dr <= 1 && dc <= 1 && ctx.world.sameLane(a, b);
    }

    /**
     * A hero is considered "engaged" if any living monster in the same lane is within attack range.
     */
    public static boolean isHeroEngaged(ValorContext ctx, Hero hero) {
        if (hero == null || hero.isFainted()) return false;
        Position hp = ctx.heroPositions.get(hero);
        if (hp == null) return false;

        return getEngagedMonster(ctx, hero) != null;
    }

    /**
     * Returns one engaged monster (same lane and in attack range) for the given hero, or null if none.
     */
    public static Monster getEngagedMonster(ValorContext ctx, Hero hero) {
        if (hero == null || hero.isFainted()) return null;
        Position hp = ctx.heroPositions.get(hero);
        if (hp == null) return null;

        for (Map.Entry<Monster, Position> e : ctx.monsterPositions.entrySet()) {
            Monster m = e.getKey();
            if (m == null || m.isFainted()) continue;
            Position mp = e.getValue();
            if (mp == null) continue;
            if (!ctx.world.sameLane(hp, mp)) continue;
            if (isInRange(ctx, hp, mp)) return m;
        }
        return null;
    }

    // ------------------------------------------------------------
    // Occupancy checks
    // ------------------------------------------------------------

    public static boolean isOccupiedByHero(ValorContext ctx, Position p) {
        return isOccupiedByHero(ctx, p, null);
    }

    public static boolean isOccupiedByHero(ValorContext ctx, Position p, Hero ignore) {
        if (p == null) return false;
        for (Map.Entry<Hero, Position> e : ctx.heroPositions.entrySet()) {
            if (ignore != null && e.getKey() == ignore) continue;
            Position hp = e.getValue();
            if (samePos(hp, p)) return true;
        }
        return false;
    }

    public static boolean isOccupiedByMonster(ValorContext ctx, Position p) {
        return isOccupiedByMonster(ctx, p, null);
    }

    public static boolean isOccupiedByMonster(ValorContext ctx, Position p, Monster ignore) {
        if (p == null) return false;
        for (Map.Entry<Monster, Position> e : ctx.monsterPositions.entrySet()) {
            if (ignore != null && e.getKey() == ignore) continue;
            Position mp = e.getValue();
            if (samePos(mp, p)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------
    // Lane / movement constraints
    // ------------------------------------------------------------

    /**
     * Heroes advance upward (row decreases) and cannot move "past" a monster in their lane.
     * Monsters advance downward (row increases) and cannot move "past" a hero in their lane.
     *
     * This replicates your original rule: if the destination would end up beyond an opposing
     * unit's row in the same lane, the move is illegal.
     */
    public static boolean wouldMovePastEnemy(ValorContext ctx, Position from, Position to, boolean isHero) {
        if (from == null || to == null) return false;
        if (!ctx.world.sameLane(from, to)) return false;

        if (isHero) {
            // Only care about forward moves (toward monster nexus), i.e., row decreases
            if (to.getRow() >= from.getRow()) return false;

            for (Map.Entry<Monster, Position> e : ctx.monsterPositions.entrySet()) {
                Monster m = e.getKey();
                if (m == null || m.isFainted()) continue;

                Position mp = e.getValue();
                if (mp == null) continue;
                if (!ctx.world.sameLane(from, mp)) continue;

                // If hero tries to move to a row "above" the monster, that's past it
                if (to.getRow() < mp.getRow()) return true;
            }
        } else {
            // Monsters move forward toward hero nexus, i.e., row increases
            if (to.getRow() <= from.getRow()) return false;

            for (Map.Entry<Hero, Position> e : ctx.heroPositions.entrySet()) {
                Hero h = e.getKey();
                if (h == null || h.isFainted()) continue;

                Position hp = e.getValue();
                if (hp == null) continue;
                if (!ctx.world.sameLane(from, hp)) continue;

                // If monster tries to move to a row "below" the hero, that's past it
                if (to.getRow() > hp.getRow()) return true;
            }
        }

        return false;
    }

    /**
     * Teleport constraint helper:
     * - For hero teleports, destination must not be behind any monster in destination lane.
     *   ("behind" meaning closer to monster nexus: row smaller than monster row)
     * - Symmetric logic exists for monster if you ever need it.
     */
    public static boolean isBehindEnemyInDestination(ValorContext ctx, Position dest, boolean isHero) {
        if (dest == null) return false;

        if (isHero) {
            for (Map.Entry<Monster, Position> e : ctx.monsterPositions.entrySet()) {
                Monster m = e.getKey();
                if (m == null || m.isFainted()) continue;

                Position mp = e.getValue();
                if (mp == null) continue;
                if (!ctx.world.sameLane(dest, mp)) continue;

                // hero destination is "ahead" of monster (closer to monster nexus)
                if (dest.getRow() < mp.getRow()) return true;
            }
        } else {
            for (Map.Entry<Hero, Position> e : ctx.heroPositions.entrySet()) {
                Hero h = e.getKey();
                if (h == null || h.isFainted()) continue;

                Position hp = e.getValue();
                if (hp == null) continue;
                if (!ctx.world.sameLane(dest, hp)) continue;

                // monster destination is "ahead" of hero (closer to hero nexus)
                if (dest.getRow() > hp.getRow()) return true;
            }
        }

        return false;
    }
}
