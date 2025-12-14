package controllers;

import java.util.List;

import characters.Hero;
import io.InputHandler;
import io.Renderer;
import market.Market;
import market.MarketController;
import world.Tile;
import world.TileType;

/**
 * Shared shop logic for both GameEngine and ValorGameEngine.
 */
public class ShopController {

    /**
     * Returns true if the tile is a place where shopping is allowed.
     * Adjust to your real rules/types.
     */
    public boolean canShopOnTile(Tile tile) {
        if (tile == null) return false;

        try {
            Object type = tile.getClass().getMethod("getType").invoke(tile);
            if (type instanceof TileType) {
                TileType t = (TileType) type;
                return t == TileType.MARKET || t == TileType.HERO_NEXUS;
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Opens the market UI.
     * Your MarketController expects (Market, List<Hero>).
     */
    public void openShop(Market market,
                         List<Hero> heroes,
                         MarketController marketController,
                         Renderer renderer,
                         InputHandler input) {
        if (market == null || heroes == null || heroes.isEmpty()) return;
        marketController.openMarket(market, heroes);
    }
}
