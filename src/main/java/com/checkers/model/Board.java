package com.checkers.model;

import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class Board {

    public static final int MIN = 1, MAX = 8;

    private final List<Piece> pieces = new ArrayList<>();
    private int whiteCaptures = 0, blackCaptures = 0;

    public Board() {
        // Standard 8x8 setup: 12 per side on dark squares (rows 1..3 white, 6..8 black)
        for (int y = MAX; y >= MIN; y--) {
            for (int x = MIN; x <= MAX; x++) {
                if ((x + y) % 2 == 0) {
                    if (y >= 6) pieces.add(new Piece(Color.BLACK, x, y));
                    if (y <= 3) pieces.add(new Piece(Color.WHITE, x, y));
                }
            }
        }
    }

    // Deep copy (kept simple if you later reintroduce undo)
    public Board(Board other) {
        for (Piece p : other.pieces) {
            pieces.add(new Piece(p.getColor(), p.isKing(), p.getX(), p.getY()));
        }
        this.whiteCaptures = other.whiteCaptures;
        this.blackCaptures = other.blackCaptures;
    }

    public static boolean onBoard(int x, int y) {
        return x >= MIN && x <= MAX && y >= MIN && y <= MAX;
    }

    public Piece getPiece(int x, int y) {
        for (Piece p : pieces) if (p.getX() == x && p.getY() == y) return p;
        return null;
    }

    public void removePiece(Piece p) { pieces.remove(p); }

    public void movePieceTo(Piece p, int x, int y) { p.setPos(x, y); }

    public void incrementCapture(Color side) {
        if (side == Color.WHITE) whiteCaptures++; else blackCaptures++;
    }

    public void applyPromotionIfEligible(Piece p) {
        if (!p.isKing()) {
            if (p.getColor() == Color.WHITE && p.getY() == MAX) p.crown();
            if (p.getColor() == Color.BLACK && p.getY() == MIN) p.crown();
        }
    }

    public boolean canPieceCapture(Piece p) {
        int[][] dirs = {{-1, +1}, {+1, +1}, {-1, -1}, {+1, -1}};
        for (int i = 0; i < dirs.length; i++) {
            if (!p.allowsDir(i)) continue;
            int nx = p.getX() + dirs[i][0];
            int ny = p.getY() + dirs[i][1];
            if (!onBoard(nx, ny)) continue;
            Piece mid = getPiece(nx, ny);
            if (mid == null || mid.getColor() == p.getColor()) continue;
            int lx = p.getX() + 2 * dirs[i][0];
            int ly = p.getY() + 2 * dirs[i][1];
            if (onBoard(lx, ly) && getPiece(lx, ly) == null) return true;
        }
        return false;
    }

    public boolean anyLegalMove(Color side, Piece forcedPiece) {
        if (forcedPiece != null) return canPieceCapture(forcedPiece);
        // if any capture exists for any piece, that's a legal move (but not mandatory)
        for (Piece p : pieces) {
            if (p.getColor() != side) continue;
            if (canPieceCapture(p)) return true;
        }
        // otherwise check simple moves
        for (Piece p : pieces) {
            if (p.getColor() != side) continue;
            for (int i = 0; i < 4; i++) {
                if (!p.allowsDir(i)) continue;
                int[] step = p.oneStep(i);
                if (step != null && getPiece(step[0], step[1]) == null) return true;
            }
        }
        return false;
    }

    public int getWinner() {
        long whites = pieces.stream().filter(p -> p.getColor() == Color.WHITE).count();
        long blacks = pieces.stream().filter(p -> p.getColor() == Color.BLACK).count();
        if (blacks == 0) return 1;
        if (whites == 0) return -1;
        return 0;
    }

    /** Pretty print board; if colored=true, uses Jansi for white/black pieces. */
    public String render(boolean colored) {
        StringBuilder sb = new StringBuilder();
        for (int y = MAX; y >= MIN; y--) {
            sb.append("   ").append("-".repeat(MAX * 5)).append("\n");
            sb.append(String.format("%2d |", y));
            for (int x = MIN; x <= MAX; x++) {
                Piece p = getPiece(x, y);
                if (p != null) {
                    // Keep men as ⚪ / ⚫ ; keep your previous king symbols as you decided
                    String symbol = p.isKing()
                            ? (p.getColor() == Color.WHITE ? "♚" : "♔")  // your chosen king symbols
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
