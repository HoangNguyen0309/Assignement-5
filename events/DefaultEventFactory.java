package events;

import java.util.Random;

import world.Tile;
import world.TileType;

public class DefaultEventFactory implements EventFactory {
    private Random random;

    public DefaultEventFactory() {
        this.random = new Random();
    }

    public GameEvent randomEventForTile(Tile tile, int partyLevel) {
        if (tile == null) return null;
        if (tile.getType() != TileType.COMMON) {
            return null;
        }
        int roll = random.nextInt(100);
        if (roll < 40) {
            return new TreasureChestEvent();
        } else if (roll < 80) {
            return new AmbushTrapEvent();
        } else {
            return null;
        }
    }
}

