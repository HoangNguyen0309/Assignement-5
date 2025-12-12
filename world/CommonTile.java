package world;

public class CommonTile extends AbstractTile {
    public CommonTile() {
        this(TileType.COMMON);
    }

    public CommonTile(TileType type) {
        super(type, type != TileType.OBSTACLE); // obstacles are not accessible
        if (!isSupported(type)) {
            throw new IllegalArgumentException("Unsupported TileType for CommonTile: " + type);
        }
    }

    private boolean isSupported(TileType type) {
        return type == TileType.COMMON
                || type == TileType.BUSH
                || type == TileType.CAVE
                || type == TileType.KOULOU
                || type == TileType.OBSTACLE;
    }
}
