package world;

public abstract class AbstractTile implements Tile {
    private TileType type;
    private boolean accessible;

    protected AbstractTile(TileType type, boolean accessible) {
        this.type = type;
        this.accessible = accessible;
    }

    public boolean isAccessible() { return accessible; }
    public TileType getType() { return type; }
    public void setAccessible(boolean accessible) { this.accessible = accessible; }
}
