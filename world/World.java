package world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import world.MarketTile;
import core.Direction;
import core.Position;

public class World {
    private Tile[][] tiles;
    private final int size;
    private Position partyPosition=null;
    private Position[] valorHeroPosition=null;
    private Random random;
    private String type;

    // How much of the accessible area should be reachable from the start
    private static final double MIN_REACHABLE_FRACTION = 0.7;
    private static final int MAX_GENERATION_ATTEMPTS = 50;

    public World(int size, String type) {
        this.size = size;
        this.random = new Random();
        this.tiles = new Tile[size][size];
        this.type = type;
        if (type.equals("Hero and Monster")){
            this.partyPosition = new Position(size - 1, 0); // bottom-left start
            generateDefaultWorld(size);
        }
        if (type.equals("Valor")){
            generateValorWorld();
            this.valorHeroPosition = new Position[]{new Position(size - 1, 0), new Position(size - 1, 3), new Position(size -1, 6)};
        }
    }

    public static World createValorWorld() {
        return new World(8, "Valor");
    }

    public int getSize() {
        return size;
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

    public void setTile(int row, int col, Tile tile) {
        tiles[row][col] = tile;
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
    private void generateValorWorld() {
        // Walls at columns 2 and 5 (0-based) separate three lanes
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (c == 2 || c == 5) {
                    tiles[r][c] = new InaccessibleTile();
                    continue;
                }

                // Monster Nexus on top row (one per lane, left column of each lane)
                if (r == 0 && (c == 0 || c == 1 ||c == 3 || c == 4 || c == 6 || c == 7)) {
                    tiles[r][c] = new MonsterNexusTile();
                    continue;
                }

                // Hero Nexus on bottom row (one per lane, left column of each lane)
                if (r == size - 1 && (c == 0 || c == 1 ||c == 3 || c == 4 || c == 6 || c == 7)) {
                    tiles[r][c] = new HeroNexusTile(null);
                    continue;
                }

                // Everything else will be filled later
                tiles[r][c] = null;
            }
        }

        // Collect interior coordinates to fill (rows 1..size-2, non-wall, non-nexus)
        List<Position> interior = new ArrayList<Position>();
        for (int r = 1; r < size - 1; r++) {
            for (int c = 0; c < size; c++) {
                if (c == 2 || c == 5) {
                    continue;
                }
                if (tiles[r][c] != null) {
                    continue;
                }
                interior.add(new Position(r, c));
            }
        }

        // Ensure each special terrain appears at least once, plus at least one plain
        List<TileType> mustPlace = new ArrayList<TileType>();
        mustPlace.add(TileType.BUSH);
        mustPlace.add(TileType.CAVE);
        mustPlace.add(TileType.KOULOU);
        mustPlace.add(TileType.OBSTACLE);
        mustPlace.add(TileType.COMMON); // guarantee not all buff/obstacle

        Collections.shuffle(interior, random);
        int idx = 0;
        for (TileType t : mustPlace) {
            if (idx >= interior.size()) break;
            Position p = interior.get(idx++);
            tiles[p.getRow()][p.getCol()] = new CommonTile(t);
        }

        // Fill remaining interior with a weighted random mix (bias toward plain)
        TileType[] pool = new TileType[] {
                TileType.COMMON, TileType.COMMON, TileType.COMMON,
                TileType.BUSH, TileType.CAVE, TileType.KOULOU,
                TileType.OBSTACLE
        };

        while (idx < interior.size()) {
            Position p = interior.get(idx++);
            TileType pick = pool[random.nextInt(pool.length)];
            tiles[p.getRow()][p.getCol()] = new CommonTile(pick);
        }

        // Fill any remaining nulls defensively as plain
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (tiles[r][c] == null) {
                    tiles[r][c] = new CommonTile();
                }
            }
        }
    }

    public Position[] getValorHeroPosition() {
        return valorHeroPosition;
    }
    public String getType() {
        return type;
    }

    public boolean isHeroNexus(Position p) {
        if (!isInside(p)) return false;
        return tiles[p.getRow()][p.getCol()].getType() == TileType.HERO_NEXUS;
    }

    public boolean isMonsterNexus(Position p) {
        if (!isInside(p)) return false;
        return tiles[p.getRow()][p.getCol()].getType() == TileType.MONSTER_NEXUS;
    }

    // -------------------------------------------------------------
    // Valor helpers for engine use
    // -------------------------------------------------------------

    public Position getHeroNexusForLane(int laneIndex) {
        Position[] pair = getHeroNexusColumnsForLane(laneIndex);
        return pair == null ? null : pair[0];
    }

    public Position getMonsterNexusForLane(int laneIndex) {
        Position[] pair = getMonsterNexusColumnsForLane(laneIndex);
        return pair == null ? null : pair[0];
    }

    /**
     * Return both Nexus tiles for a lane (two columns per lane).
     */
    public Position[] getHeroNexusColumnsForLane(int laneIndex) {
        switch (laneIndex) {
            case 0: return new Position[]{ new Position(size - 1, 0), new Position(size - 1, 1) };
            case 1: return new Position[]{ new Position(size - 1, 3), new Position(size - 1, 4) };
            case 2: return new Position[]{ new Position(size - 1, 6), new Position(size - 1, 7) };
            default: return null;
        }
    }

    public Position[] getMonsterNexusColumnsForLane(int laneIndex) {
        switch (laneIndex) {
            case 0: return new Position[]{ new Position(0, 0), new Position(0, 1) };
            case 1: return new Position[]{ new Position(0, 3), new Position(0, 4) };
            case 2: return new Position[]{ new Position(0, 6), new Position(0, 7) };
            default: return null;
        }
    }

    public boolean isInside(Position p) {
        if (p == null) return false;
        return p.getRow() >= 0 && p.getRow() < size
                && p.getCol() >= 0 && p.getCol() < size;
    }

    public boolean isAccessible(Position p) {
        if (!isInside(p)) return false;
        return tiles[p.getRow()][p.getCol()].isAccessible();
    }

    public boolean isLaneWall(int col) {
        return col == 2 || col == 5;
    }

    public boolean sameLane(Position a, Position b) {
        if (!isInside(a) || !isInside(b)) return false;
        return laneIndexForCol(a.getCol()) == laneIndexForCol(b.getCol());
    }

    public int laneIndexForCol(int col) {
        if (col == 0 || col == 1) return 0;
        if (col == 3 || col == 4) return 1;
        if (col == 6 || col == 7) return 2;
        return -1; // walls or invalid
    }
}
