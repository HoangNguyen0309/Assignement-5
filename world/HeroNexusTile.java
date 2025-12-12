package world;

import market.Market;

public class HeroNexusTile extends MarketTile {

    public HeroNexusTile(Market market) {
        super(market);
    }

    public TileType getType() {
        return TileType.HERO_NEXUS;
    }

}
