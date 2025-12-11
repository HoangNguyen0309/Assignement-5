package world;

public class ObstacleTile extends AbstractTile {

    public ObstacleTile() {
        super(TileType.OBSTACLE, false);
    }

    public void removeObstacle() {
        setAccessible(true);
    }
    
}
