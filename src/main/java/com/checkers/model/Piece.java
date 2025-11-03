package com.checkers.model;

public class Piece {

    private final Color color;
    private int x, y;
    private boolean king;

    public Piece(Color color, int x, int y) { this(color, false, x, y); }
    public Piece(Color color, boolean king, int x, int y) {
        this.color = color; this.king = king; this.x = x; this.y = y;
    }

    public Color getColor() { return color; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isKing() { return king; }
    public void crown() { this.king = true; }
    public void setPos(int x, int y) { this.x = x; this.y = y; }

    /** Is this direction allowed for this piece? 0:FL,1:FR,2:BL,3:BR */
    public boolean allowsDir(int dir) {
        if (king) return true;
        // White moves "up" (+y), Black moves "down" (-y)
        if (color == Color.WHITE) return dir == 0 || dir == 1;
        else return dir == 2 || dir == 3;
    }

    /** One-step destination for a direction; null if offboard or direction illegal. */
    public int[] oneStep(int dir) {
        if (!allowsDir(dir)) return null;
        int dx = (dir == 0 || dir == 2) ? -1 : +1;
        int dy = (dir == 0 || dir == 1) ? +1 : -1;
        int nx = x + dx, ny = y + dy;
        return Board.onBoard(nx, ny) ? new int[]{nx, ny} : null;
    }

    /** Landing square after skipping over the adjacent (two squares) in given direction. */
    public int[] skipOver(int adjX, int adjY) {
        int lx = x + (adjX - x) * 2;
        int ly = y + (adjY - y) * 2;
        return new int[]{lx, ly};
    }
}
