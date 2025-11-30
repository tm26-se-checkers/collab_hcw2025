package com.checkers.model;

/**
 * A single checkers piece (white/black, man/king) located on the 8×8 board.
 * <p>Uses the Strategy pattern via {@link MovementPolicy} to decide allowed directions and steps.
 * This keeps movement rules decoupled from the data object and makes promotions trivial.
 */
public class Piece {

    private final Color color;
    private int x;
    private int y;
    private boolean king;

    /** Strategy: men vs. kings. */
    private MovementPolicy movement = new ManMovementPolicy();

    /**
     * Create a new non-king (man) at (x,y).
     */
    public Piece(Color color, int x, int y) {
        this(color, false, x, y);
    }

    /**
     * Full constructor.
     * @param color piece color (WHITE/BLACK), not null
     * @param king  whether this piece starts crowned
     * @param x     file (1..8)
     * @param y     rank (1..8)
     */
    public Piece(Color color, boolean king, int x, int y) {
        this.color = color;
        this.king = king;
        this.x = x;
        this.y = y;
        if (king) {
            this.movement = new KingMovementPolicy();
        }
    }

    public Color getColor() { return color; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isKing() { return king; }

    /** Crown this piece and switch movement policy to king behavior. */
    public void crown() {
        this.king = true;
        this.movement = new KingMovementPolicy();
    }

    /** Update the internal position to (x,y). */
    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Whether this piece type can move/capture in a given direction index.
     * Direction indices: 0:FL, 1:FR, 2:BL, 3:BR.
     */
    public boolean allowsDir(int dir) {
        return movement.allowsDir(color, king, dir);
    }

    /**
     * Compute the one-square destination for a step in the given direction index.
     * @return 2-int {nx, ny} or null if off the board
     */
    public int[] oneStep(int dir) {
        int[] d = movement.delta(dir);
        int nx = x + d[0];
        int ny = y + d[1];
        return Board.onBoard(nx, ny) ? new int[]{nx, ny} : null;
    }

    /**
     * Given the coordinates of an adjacent "middle" square (typically an opponent),
     * return the landing square if this piece jumps over it (two steps in the same direction).
     * This method does not check occupancy; callers must verify board bounds and emptiness.
     */
    public int[] skipOver(int midX, int midY) {
        int lx = x + (midX > x ? +2 : -2);
        int ly = y + (midY > y ? +2 : -2);
        return new int[]{lx, ly};
    }

    @Override
    public String toString() {
        // Keep original console symbols: men = ⚪/⚫, kings = ♚/♔
        if (color == Color.WHITE) {
            return king ? "♚" : "⚪";
        } else {
            return king ? "♔" : "⚫";
        }
    }
}
