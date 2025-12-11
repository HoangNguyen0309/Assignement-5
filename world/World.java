package world;

import java.util.Random;
import java.util.ArrayDeque;
import java.util.Queue;
import world.MarketTile;
import core.Direction;
import core.Position;

public class World {
    private Tile[][] tiles;
    private Position partyPosition=null;
    private Position[] ValorHeroPosition=null;
    private Random random;
    private String type;

    // How much of the accessible area should be reachable from the start
    private static final double MIN_REACHABLE_FRACTION = 0.7;
    private static final int MAX_GENERATION_ATTEMPTS = 50;

    public World(int size, String type) {
        this.random = new Random();
        this.tiles = new Tile[size][size];
        this.type = type;
        if (type.equals("Hero and Monster")){
            this.partyPosition = new Position(size - 1, 0); // bottom-left start
            generateDefaultWorld(size);
        }
        if (type.equals("Valor")){
            generateValorWorld(size);
            this.ValorHeroPosition = new Position[]{new Position(size - 1, 0), new Position(size - 1, 3), new Position(size -1, 6)};
        }
    }

    public int getSize() {
        return tiles.length;
    }

    public Position getPartyPosition() {
        return partyPosition;
    }

    public Tile getCurrentTile() {
        return tiles[partyPosition.getRow()][partyPosition.getCol()];
    }

    public Tile getTile(int row, int col) {
        return tiles[row][col];
    }

    public boolean move(Direction direction) {
        int row = partyPosition.getRow();
        int col = partyPosition.getCol();
        switch (direction) {
            case UP:    row--; break;
            case DOWN:  row++; break;
            case LEFT:  col--; break;
            case RIGHT: col++; break;
            default: break;
        }
        if (row < 0 || row >= tiles.length || col < 0 || col >= tiles.length) {
            return false;
        }
        Tile tile = tiles[row][col];
        if (!tile.isAccessible()) {
            return false;
        }
        partyPosition.setRow(row);
        partyPosition.setCol(col);
        return true;
    }

    // ---------------------------------------------------------------------
    // Map generation with BFS connectivity check
    // ---------------------------------------------------------------------

    private void generateDefaultWorld(int size) {
        // Try several times to generate a "good" map
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            randomFill(size);

            // Ensure start tile is accessible
            tiles[size - 1][0] = new CommonTile();

            if (hasGoodConnectivity(size)) {
                // BFS says we can reach most of the accessible area — accept this map
                return;
            }
        }

        // Fallback: fully accessible map with some markets, guaranteed connected
        fallbackOpenWorld(size);
    }

    /**
     * Randomly fill the world with Common / Market / Inaccessible tiles.
     */
    private void randomFill(int size) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int roll = random.nextInt(100);
                if (roll < 20) {
                    tiles[r][c] = new InaccessibleTile();
                } else if (roll < 30) {
                    tiles[r][c] = new MarketTile(null); // markets can be wired later
                } else {
                    tiles[r][c] = new CommonTile();
                }
            }
        }
    }

    /**
     * Use BFS to see how many accessible tiles we can reach from the start.
     * If we can reach at least MIN_REACHABLE_FRACTION of them, we consider
     * this a "good" map.
     */
    private boolean hasGoodConnectivity(int size) {
        // Count total accessible tiles
        int totalAccessible = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (tiles[r][c].isAccessible()) {
                    totalAccessible++;
                }
            }
        }
        if (totalAccessible == 0) {
            return false;
        }

        int startRow = partyPosition.getRow();
        int startCol = partyPosition.getCol();
        if (!tiles[startRow][startCol].isAccessible()) {
            return false;
        }

        boolean[][] visited = new boolean[size][size];
        Queue<Position> queue = new ArrayDeque<Position>();

        visited[startRow][startCol] = true;
        queue.add(new Position(startRow, startCol));

        int reachable = 0;

        // BFS over accessible neighbors
        int[] dr = { -1, 1, 0, 0 };
        int[] dc = { 0, 0, -1, 1 };

        while (!queue.isEmpty()) {
            Position p = queue.remove();
            reachable++;

            int r = p.getRow();
            int c = p.getCol();

            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];

                if (nr < 0 || nr >= size || nc < 0 || nc >= size) {
                    continue;
                }
                if (visited[nr][nc]) {
                    continue;
                }
                if (!tiles[nr][nc].isAccessible()) {
                    continue;
                }

                visited[nr][nc] = true;
                queue.add(new Position(nr, nc));
            }
        }

        double fraction = (double) reachable / (double) totalAccessible;
        return fraction >= MIN_REACHABLE_FRACTION;
    }

    /**
     * Fallback map: all common tiles, with some markets along the main diagonal.
     * This guarantees that the hero can move everywhere.
     */
    private void fallbackOpenWorld(int size) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (r == c && r % 2 == 0) {
                    tiles[r][c] = new MarketTile(null);
                } else {
                    tiles[r][c] = new CommonTile();
                }
            }
        }
        tiles[size - 1][0] = new CommonTile();
    }

    /*
    *
    */
    private void generateValorWorld(int size) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (r == 0 && c != 2 && c != 5) {
                    tiles[r][c] = new MonsterNexusTile();
                }
                else if (r == 7 && c != 2 && c != 5) {
                    tiles[r][c] = new HeroNexusTile(null);
                }
                else if (c == 2 || c == 5) {
                    tiles[r][c] = new InaccessibleTile();
                }
                else
                    tiles[r][c] = new CommonTile();
                

            }
        }
    }
    public Position[] getValorHeroPosition() {
        return ValorHeroPosition;
    }
    public String getType() {
        return type;
    }
}
