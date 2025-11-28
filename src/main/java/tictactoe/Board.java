package tictactoe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a 3D grid board that stores pieces, frozen cells, and provides
 * convenience helpers for layer shifting and empowered captures.
 */
public class Board {
    public static final int SIZE = 3;

    private final Piece[][][] grid = new Piece[SIZE][SIZE][SIZE];
    private final int[][][] frozenTurns = new int[SIZE][SIZE][SIZE];
    private final List<List<Position>> allLines;

    public Board() {
        this.allLines = computeAllLines();
    }

    public boolean inBounds(Position pos) {
        return pos.getX() >= 0 && pos.getX() < SIZE
                && pos.getY() >= 0 && pos.getY() < SIZE
                && pos.getZ() >= 0 && pos.getZ() < SIZE;
    }

    public Piece getPiece(Position pos) {
        return grid[pos.getX()][pos.getY()][pos.getZ()];
    }

    public boolean isEmpty(Position pos) {
        return getPiece(pos) == null;
    }

    public boolean isFrozen(Position pos) {
        return frozenTurns[pos.getX()][pos.getY()][pos.getZ()] > 0;
    }

    public void freezeCell(Position pos, int turns) {
        if (!inBounds(pos)) {
            return;
        }
        frozenTurns[pos.getX()][pos.getY()][pos.getZ()] = Math.max(turns,
                frozenTurns[pos.getX()][pos.getY()][pos.getZ()]);
    }

    public void tickFreezes() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (frozenTurns[x][y][z] > 0) {
                        frozenTurns[x][y][z] -= 1;
                    }
                }
            }
        }
    }

    public void setPiece(Position pos, Piece piece) {
        if (!inBounds(pos)) {
            throw new IllegalArgumentException("Position out of bounds: " + pos);
        }
        grid[pos.getX()][pos.getY()][pos.getZ()] = piece;
        piece.setPosition(pos);
    }

    public Piece removePiece(Position pos) {
        if (!inBounds(pos)) {
            throw new IllegalArgumentException("Position out of bounds: " + pos);
        }
        Piece piece = grid[pos.getX()][pos.getY()][pos.getZ()];
        grid[pos.getX()][pos.getY()][pos.getZ()] = null;
        if (piece != null) {
            piece.setPosition(null);
        }
        return piece;
    }

    public void shiftLayerUp(int layer) {
        if (layer < 0 || layer >= SIZE - 1) {
            return; // cannot shift the topmost layer up
        }
        swapLayers(layer, layer + 1);
    }

    public void shiftLayerDown(int layer) {
        if (layer <= 0 || layer >= SIZE) {
            return; // cannot shift the bottommost layer down
        }
        swapLayers(layer, layer - 1);
    }

    private void swapLayers(int firstLayer, int secondLayer) {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                Piece tempPiece = grid[x][y][firstLayer];
                int tempFrozen = frozenTurns[x][y][firstLayer];

                grid[x][y][firstLayer] = grid[x][y][secondLayer];
                frozenTurns[x][y][firstLayer] = frozenTurns[x][y][secondLayer];
                if (grid[x][y][firstLayer] != null) {
                    grid[x][y][firstLayer].setPosition(new Position(x, y, firstLayer));
                }

                grid[x][y][secondLayer] = tempPiece;
                frozenTurns[x][y][secondLayer] = tempFrozen;
                if (grid[x][y][secondLayer] != null) {
                    grid[x][y][secondLayer].setPosition(new Position(x, y, secondLayer));
                }
            }
        }
    }

    public Piece empoweredCapture(Piece piece, Position target) {
        if (piece == null || !piece.isEmpowered() || piece.getPosition() == null) {
            return null;
        }
        Position current = piece.getPosition();
        if (!inBounds(target) || isFrozen(target)) {
            return null;
        }
        int dx = Math.abs(current.getX() - target.getX());
        int dy = Math.abs(current.getY() - target.getY());
        int dz = Math.abs(current.getZ() - target.getZ());
        if (dx + dy + dz != 1) {
            return null; // not adjacent
        }
        Piece occupant = getPiece(target);
        if (occupant == null || occupant.getOwner() == piece.getOwner()) {
            return null;
        }
        removePiece(current);
        removePiece(target);
        piece.setEmpowered(false);
        setPiece(target, piece);
        return occupant;
    }

    public List<List<Position>> listAllLines() {
        return allLines;
    }

    private List<List<Position>> computeAllLines() {
        List<List<Position>> lines = new ArrayList<>();
        List<int[]> directions = List.of(
                new int[]{1, 0, 0}, new int[]{0, 1, 0}, new int[]{0, 0, 1},
                new int[]{1, 1, 0}, new int[]{1, 0, 1}, new int[]{0, 1, 1},
                new int[]{1, -1, 0}, new int[]{1, 0, -1}, new int[]{0, 1, -1},
                new int[]{1, 1, 1}, new int[]{1, 1, -1}, new int[]{1, -1, 1},
                new int[]{1, -1, -1}
        );

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    Position start = new Position(x, y, z);
                    for (int[] dir : directions) {
                        if (!isCanonicalStart(start, dir)) {
                            continue;
                        }
                        List<Position> candidate = new ArrayList<>();
                        Position cursor = start;
                        for (int step = 0; step < SIZE; step++) {
                            if (!inBounds(cursor)) {
                                candidate.clear();
                                break;
                            }
                            candidate.add(cursor);
                            cursor = cursor.translate(dir[0], dir[1], dir[2]);
                        }
                        if (candidate.size() == SIZE) {
                            lines.add(candidate);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableList(lines);
    }

    private boolean isCanonicalStart(Position start, int[] dir) {
        int prevX = start.getX() - dir[0];
        int prevY = start.getY() - dir[1];
        int prevZ = start.getZ() - dir[2];
        return prevX < 0 || prevX >= SIZE || prevY < 0 || prevY >= SIZE || prevZ < 0 || prevZ >= SIZE;
    }

    public Set<Position> positionsOf(Player player) {
        Set<Position> owned = new HashSet<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    Piece piece = grid[x][y][z];
                    if (piece != null && piece.getOwner() == player) {
                        owned.add(new Position(x, y, z));
                    }
                }
            }
        }
        return owned;
    }
}
