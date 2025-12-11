package world;

import market.Market;

class HeroNexusTile extends MarketTile {

    private Market market;

    public HeroNexusTile(Market market) {
        super(market);
    }

    public TileType getType() {
        return TileType.HERO_NEXUS;
    }

}