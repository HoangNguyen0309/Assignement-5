package world;

import market.Market;

public class MarketTile implements Tile {

    private Market market;

    public MarketTile(Market market) {
        this.market = market;
    }

    @Override
    public boolean isAccessible() {
        return true;
    }

    @Override
    public TileType getType() {
        return TileType.MARKET;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }
}
