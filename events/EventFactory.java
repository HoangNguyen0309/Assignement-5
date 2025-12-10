package events;

import world.Tile;

public interface EventFactory {
    GameEvent randomEventForTile(Tile tile, int partyLevel);
}
