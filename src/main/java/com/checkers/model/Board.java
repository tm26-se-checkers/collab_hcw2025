package com.checkers.model;

import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Board (8×8) holding current pieces and capture counters.
 * Responsibilities:
 *  - Initialize standard setup
 *  - Provide local queries/mutations (get/move/remove/promote)
 *  - Provide helper queries (can a piece capture? does a side have any move?)
 *
 * Game-wide rule orchestration and turn handling are in the logic layer (GameService).
 */
public class Board {

    public static final int MIN = 1;
    public static final int MAX = 8;

    /** Direction table used by local capture checks: 0:FL, 1:FR, 2:BL, 3:BR. */
    private static final int[][] DIRECTIONS = {
            {-1, +1}, // FL
            {+1, +1}, // FR
            {-1, -1}, // BL
            {+1, -1}  // BR
    };

    private final List<Piece> pieces = new ArrayList<>();
    private int whiteCaptures = 0;
    private int blackCaptures = 0;

    /**
     * Create a standard 8×8 start position:
     * - White pieces on ranks 1..3 on dark squares
     * - Black pieces on ranks 6..8 on dark squares
     * Dark squares are where (x+y) % 2 == 0.
     */
    public Board() {
        for (int y = MAX; y >= MIN; y--) {
            for (int x = MIN; x <= MAX; x++) {
                if ((x + y) % 2 == 0) {
                    if (y >= 6) pieces.add(new Piece(Color.BLACK, x, y));
                    if (y <= 3) pieces.add(new Piece(Color.WHITE, x, y));
                }
            }
        }
    }

    /**
     * Deep copy constructor.
     * Creates new Piece instances so the cloned Board is independent from {@code other}.
     */
    public Board(Board other) {
        for (Piece p : other.pieces) {
            pieces.add(new Piece(p.getColor(), p.isKing(), p.getX(), p.getY()));
        }
        this.whiteCaptures = other.whiteCaptures;
        this.blackCaptures = other.blackCaptures;
    }

    /** Is the coordinate inside the board bounds 1..8 × 1..8? */
    public static boolean onBoard(int x, int y) {
        return x >= MIN && x <= MAX && y >= MIN && y <= MAX;
    }

    /**
     * Return the piece at (x,y), or null if the square is empty.
     * Complexity is O(n), which is fine for a small board.
     */
    public Piece getPiece(int x, int y) {
        for (Piece p : pieces) {
            if (p.getX() == x && p.getY() == y) return p;
        }
        return null;
    }

    /** Remove a piece from the board. */
    public void removePiece(Piece p) {
        pieces.remove(p);
    }

    /** Move a piece to (x,y) without validating rules; logic layer handles that. */
    public void movePieceTo(Piece p, int x, int y) {
        p.setPos(x, y);
    }

    /**
     * Increase the capture counter for the side that performed a capture.
     * Used for winner detection based on captures.
     */
    public void incrementCapture(Color side) {
        if (side == Color.WHITE) {
            whiteCaptures++;
        } else {
            blackCaptures++;
        }
    }

    /**
     * Promote a man to king if it has reached the opponent’s back rank:
     * - White promotes on rank 8.
     * - Black promotes on rank 1.
     */
    public void applyPromotionIfEligible(Piece p) {
        if (!p.isKing()) {
            if (p.getColor() == Color.WHITE && p.getY() == MAX) p.crown();
            if (p.getColor() == Color.BLACK && p.getY() == MIN) p.crown();
        }
    }

    /**
     * Does the given piece have at least one capture available right now?
     * <ol>
     *   <li>For each direction the piece is allowed to move in, inspect the adjacent square.</li>
     *   <li>If there is an opponent there, compute the landing square two steps away.</li>
     *   <li>If that landing square is on board and empty, a capture exists.</li>
     * </ol>
     * Multi-jump chaining is coordinated by the logic layer.
     */
    public boolean canPieceCapture(Piece p) {
        for (int dir = 0; dir < DIRECTIONS.length; dir++) {
            if (!p.allowsDir(dir)) continue;

            int nx = p.getX() + DIRECTIONS[dir][0];
            int ny = p.getY() + DIRECTIONS[dir][1];
            if (!onBoard(nx, ny)) continue;

            Piece mid = getPiece(nx, ny);
            if (mid == null || mid.getColor() == p.getColor()) continue;

            int lx = p.getX() + 2 * DIRECTIONS[dir][0];
            int ly = p.getY() + 2 * DIRECTIONS[dir][1];
            if (onBoard(lx, ly) && getPiece(lx, ly) == null) {
                return true;
            }
        }
        return false;
    }

    /** Is there any capture available for the given side? */
    public boolean anyCaptureAvailable(Color side) {
        for (Piece p : pieces) {
            if (p.getColor() != side) continue;
            if (canPieceCapture(p)) return true;
        }
        return false;
    }

    /**
     * Does the side have any legal move at all?
     * If a capture chain is in progress (forcedPiece != null), only captures for that piece are considered.
     * Otherwise, either a capture or a simple diagonal step to an empty square makes a legal move.
     */
    public boolean anyLegalMove(Color side, Piece forcedPiece) {
        if (forcedPiece != null) return canPieceCapture(forcedPiece);

        if (anyCaptureAvailable(side)) return true;

        for (Piece p : pieces) {
            if (p.getColor() != side) continue;
            for (int dir = 0; dir < 4; dir++) {
                if (!p.allowsDir(dir)) continue;
                int[] step = p.oneStep(dir);
                if (step != null && getPiece(step[0], step[1]) == null) return true;
            }
        }
        return false;
    }

    /**
     * Winner detection by pieces remaining:
     * @return  1 if Black has no pieces left (White wins),
     *         -1 if White has no pieces left (Black wins),
     *          0 otherwise.
     * Stalemate/no-move is handled by the logic layer via anyLegalMove(...).
     */
    public int getWinner() {
        long whites = pieces.stream().filter(p -> p.getColor() == Color.WHITE).count();
        long blacks = pieces.stream().filter(p -> p.getColor() == Color.BLACK).count();
        if (blacks == 0) return 1;
        if (whites == 0) return -1;
        return 0;
    }

    /** Console rendering (with optional colors). */
    public String render(boolean colored) {
        StringBuilder sb = new StringBuilder();
        for (int y = MAX; y >= MIN; y--) {
            sb.append("   ").append("-".repeat(MAX * 5)).append("\n");
            sb.append(String.format("%2d |", y));
            for (int x = MIN; x <= MAX; x++) {
                Piece p = getPiece(x, y);
                if (p != null) {
                    String symbol = p.isKing()
                            ? (p.getColor() == Color.WHITE ? "♚" : "♔")
                            : (p.getColor() == Color.WHITE ? "⚪" : "⚫");
                    if (colored) {
                        sb.append(" ").append(
                                p.getColor() == Color.WHITE
                                        ? ansi().fgBright(Ansi.Color.WHITE).a(symbol).reset()
                                        : ansi().fg(Ansi.Color.BLACK).a(symbol).reset()
                        ).append(" |");
                    } else {
                        sb.append(" ").append(symbol).append(" |");
                    }
                } else {
                    sb.append("    |");
                }
            }
            if (y == MAX) sb.append(" ").append("⚪".repeat(blackCaptures));
            if (y == MIN) sb.append(" ").append("⚫".repeat(whiteCaptures));
            sb.append("\n");
        }
        sb.append("   ").append("-".repeat(MAX * 5)).append("\n ");
        for (int i = 0; i < MAX; i++) sb.append(String.format("%5c", (char) ('a' + i)));
        sb.append("\n");
        return sb.toString();
    }
}
