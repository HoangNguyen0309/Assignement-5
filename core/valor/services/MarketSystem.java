package core.valor.services;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import core.Position;
import core.valor.ValorContext;
import market.Market;
import world.MarketTile;
import world.Tile;
import world.TileType;

public class MarketSystem {

    public void openShopIfAtHeroNexus(ValorContext ctx, Hero hero) {
        Position pos = ctx.heroPositions.get(hero);
        if (pos == null) return;

        Tile tile = ctx.world.getTile(pos.getRow(), pos.getCol());
        if (tile.getType() != TileType.HERO_NEXUS) {
            ctx.renderer.renderMessage("You must be standing on a Hero Nexus to shop.");
            return;
        }

        if (!(tile instanceof MarketTile)) {
            ctx.renderer.renderMessage("This Hero Nexus cannot host a market.");
            return;
        }

        MarketTile mTile = (MarketTile) tile;

        int lane = laneIndexForHero(ctx, hero);
        int laneLevel = getLaneMaxLevel(ctx, lane);

        if (mTile.getMarket() == null) {
            mTile.setMarket(new Market(ctx.itemFactory, laneLevel));
        } else {
            Market market = mTile.getMarket();
            if (laneLevel > market.getBaseLevel()) {
                market.restock(ctx.itemFactory, laneLevel);
                ctx.renderer.renderMessage("The market has refreshed its stock for your lane.");
            }
        }

        List<Hero> single = new ArrayList<Hero>();
        single.add(hero);
        ctx.marketController.openMarket(mTile.getMarket(), single);
    }

    private int laneIndexForHero(ValorContext ctx, Hero hero) {
        Position spawn = ctx.heroSpawnPositions.get(hero);
        if (spawn == null) return -1;
        return ctx.world.laneIndexForCol(spawn.getCol());
    }

    private int getLaneMaxLevel(ValorContext ctx, int laneIndex) {
        Integer v = ctx.laneMaxLevels.get(laneIndex);
        return v == null ? 1 : v.intValue();
    }
}
