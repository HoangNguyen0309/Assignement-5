package core.valor.services;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import core.Direction;
import core.Position;
import core.valor.ValorContext;
import world.CommonTile;
import world.Tile;
import world.TileType;

public class HeroMovementService {

    private final TerrainSystem terrain;

    public HeroMovementService(TerrainSystem terrain) {
        this.terrain = terrain;
    }

    public boolean move(ValorContext ctx, Hero hero) {
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

        if (!ValorRules.isInsideBoard(ctx, newRow, newCol)) {
            ctx.renderer.renderMessage("Cannot move outside the board.");
            return false;
        }

        Tile tile = ctx.world.getTile(newRow, newCol);
        if (!tile.isAccessible()) {
            ctx.renderer.renderMessage("That tile is not accessible.");
            return false;
        }

        Position dest = new Position(newRow, newCol);

        if (ValorRules.isOccupiedByHero(ctx, dest, hero)) {
            ctx.renderer.renderMessage("Another hero is already there.");
            return false;
        }

        if (ValorRules.wouldMovePastEnemy(ctx, pos, dest, true)) {
            ctx.renderer.renderMessage("You cannot move past a monster in your lane.");
            return false;
        }

        ctx.heroPositions.put(hero, dest);
        terrain.apply(ctx, hero, dest);
        ctx.renderer.renderMessage(hero.getName() + " moved to (" + newRow + ", " + newCol + ").");
        ctx.log(hero.getName() + " moved to (" + newRow + ", " + newCol + ").");
        return true;
    }

    public boolean recall(ValorContext ctx, Hero hero) {
        Position spawn = ctx.heroSpawnPositions.get(hero);
        if (spawn == null) {
            ctx.renderer.renderMessage("No recall position for " + hero.getName() + ".");
            return false;
        }

        Position dest = spawn;
        if (ValorRules.isOccupiedByHero(ctx, spawn, hero)) {
            int lane = ctx.world.laneIndexForCol(spawn.getCol());
            Position alt = findAvailableHeroNexusSlot(ctx, lane, hero);
            if (alt != null) dest = alt;
            else {
                ctx.renderer.renderMessage("Your lane nexus is fully occupied. Recall failed.");
                return false;
            }
        }

        ctx.heroPositions.put(hero, new Position(dest.getRow(), dest.getCol()));
        terrain.apply(ctx, hero, dest);
        ctx.renderer.renderMessage(hero.getName() + " recalls to their Hero Nexus.");
        ctx.log(hero.getName() + " recalls to their Hero Nexus.");
        return true;
    }

    public boolean teleport(ValorContext ctx, Hero hero) {
        Position heroPos = ctx.heroPositions.get(hero);
        if (heroPos == null) return false;

        List<Hero> candidates = new ArrayList<Hero>();
        for (Hero h : ctx.heroes) {
            if (h == hero) continue;
            if (h.isFainted()) continue;
            candidates.add(h);
        }
        if (candidates.isEmpty()) {
            ctx.renderer.renderMessage("No other heroes to teleport near.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a hero to teleport next to:");
        for (int i = 0; i < candidates.size(); i++) {
            Hero h = candidates.get(i);
            Position hp = ctx.heroPositions.get(h);
            ctx.renderer.renderMessage("  " + (i + 1) + ") " + h.getName()
                    + " @ (" + hp.getRow() + "," + hp.getCol() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int choice = ctx.input.readInt();
        if (choice == 0) return false;
        choice--;

        if (choice < 0 || choice >= candidates.size()) {
            ctx.renderer.renderMessage("Invalid choice.");
            return false;
        }

        Hero targetHero = candidates.get(choice);
        Position targetPos = ctx.heroPositions.get(targetHero);
        if (targetPos == null) return false;

        if (ctx.world.sameLane(heroPos, targetPos)) {
            ctx.renderer.renderMessage("Must teleport to a different lane.");
            return false;
        }

        List<Position> options = new ArrayList<Position>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = targetPos.getRow() + d[0];
            int nc = targetPos.getCol() + d[1];
            if (!ValorRules.isInsideBoard(ctx, nr, nc)) continue;

            Position dest = new Position(nr, nc);
            if (!ctx.world.isAccessible(dest)) continue;

            // must change lane
            if (ctx.world.sameLane(heroPos, dest)) continue;

            // cannot be ahead of target hero
            if (dest.getRow() < targetPos.getRow()) continue;

            if (ValorRules.isOccupiedByHero(ctx, dest, hero)) continue;

            // not behind a monster in destination lane
            if (ValorRules.isBehindEnemyInDestination(ctx, dest, true)) continue;

            options.add(dest);
        }

        if (options.isEmpty()) {
            ctx.renderer.renderMessage("No valid teleport destinations.");
            return false;
        }

        ctx.renderer.renderMessage("Choose a teleport destination:");
        for (int i = 0; i < options.size(); i++) {
            Position p = options.get(i);
            ctx.renderer.renderMessage("  " + (i + 1) + ") (" + p.getRow() + "," + p.getCol() + ")");
        }
        ctx.renderer.renderMessage("  0) Back");

        int dc = ctx.input.readInt();
        if (dc == 0) return false;
        dc--;

        if (dc < 0 || dc >= options.size()) {
            ctx.renderer.renderMessage("Invalid destination.");
            return false;
        }

        Position dest = options.get(dc);
        ctx.heroPositions.put(hero, dest);
        terrain.apply(ctx, hero, dest);
        ctx.renderer.renderMessage(hero.getName() + " teleports to (" + dest.getRow() + ", " + dest.getCol() + ")");
        return true;
    }

    public boolean removeObstacle(ValorContext ctx, Hero hero) {
        Position pos = ctx.heroPositions.get(hero);
        if (pos == null) return false;

        int targetRow = pos.getRow() - 1;
        int targetCol = pos.getCol();

        if (!ValorRules.isInsideBoard(ctx, targetRow, targetCol)) {
            ctx.renderer.renderMessage("No obstacle in front (out of board).");
            return false;
        }

        Tile tile = ctx.world.getTile(targetRow, targetCol);
        if (tile.getType() != TileType.OBSTACLE) {
            ctx.renderer.renderMessage("Front tile is not an obstacle.");
            return false;
        }

        ctx.world.setTile(targetRow, targetCol, new CommonTile(TileType.COMMON));
        ctx.renderer.renderMessage(hero.getName() + " cleared the obstacle ahead.");
        return true;
    }

    private Position findAvailableHeroNexusSlot(ValorContext ctx, int laneIndex, Hero hero) {
        Position[] slots = ctx.world.getHeroNexusColumnsForLane(laneIndex);
        if (slots == null) return null;
        for (Position p : slots) {
            if (!ValorRules.isOccupiedByHero(ctx, p, hero)) return p;
        }
        return null;
    }
}
