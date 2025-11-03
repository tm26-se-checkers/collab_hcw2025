package com.checkers.logic;

import com.checkers.model.*;

public class GameService {

    // direction indices used by Piece helpers
    public static final int FL = 0; // forward-left
    public static final int FR = 1; // forward-right
    public static final int BL = 2; // backward-left
    public static final int BR = 3; // backward-right

    private Board board = new Board();
    private boolean whiteToMove = true;
    private Piece forcedPieceInChain = null; // if a capture was made and can continue, same piece must move

    public Board getBoard() { return board; }
    public boolean isWhiteToMove() { return whiteToMove; }

    public boolean isGameOver() {
        // game ends if one side has no pieces or current side has no legal move (simple sprint-1 end condition)
        int winner = board.getWinner();
        if (winner != 0) return true;
        return !board.anyLegalMove(whiteToMove ? Color.WHITE : Color.BLACK, forcedPieceInChain);
    }

    public String resultText() {
        int winner = board.getWinner();
        return switch (winner) {
            case 1 -> "White has captured all opponent pieces. White wins.";
            case -1 -> "Black has captured all opponent pieces. Black wins.";
            default -> (whiteToMove ? "White" : "Black") + " has no legal moves. Draw or stalemate by no-move.";
        };
    }

    /**
     * Parses a console command: "<square> <l|r> [b]".
     * - White forward = up (+y), Black forward = down (-y)
     * - Only kings can use [b] to move/capture backward
     * - No mandatory capture globally, but if a capture chain has started, it must continue with the same piece
     */
    public void moveFromCommand(String square, char lr, boolean back) throws IllegalMove {
        int x = (square.charAt(0) - 'a') + 1;
        int y = Integer.parseInt(square.substring(1));

        Piece piece = board.getPiece(x, y);
        if (piece == null) throw new IllegalMove("No piece at that square.");
        Color toMove = isWhiteToMove() ? Color.WHITE : Color.BLACK;
        if (piece.getColor() != toMove) throw new IllegalMove("It's not that side's turn.");

        char d = Character.toLowerCase(lr);
        if (d != 'l' && d != 'r') throw new IllegalMove("Direction must be 'l' or 'r'.");

        boolean backwardRequested = back;
        int dir;

        if (piece.getColor() == Color.WHITE) {
            if (!backwardRequested) dir = (d == 'l') ? FL : FR;
            else {
                if (!piece.isKing()) throw new IllegalMove("Only kings can move backward.");
                dir = (d == 'l') ? BL : BR;
            }
        } else {
            if (!backwardRequested) dir = (d == 'l') ? BL : BR; // black forward is down the board
            else {
                if (!piece.isKing()) throw new IllegalMove("Only kings can move backward.");
                dir = (d == 'l') ? FL : FR;
            }
        }

        moveByDirection(x, y, dir);
    }

    public void moveByDirection(int x, int y, int dir) throws IllegalMove {
        Color toMove = whiteToMove ? Color.WHITE : Color.BLACK;

        Piece piece = board.getPiece(x, y);
        if (piece == null) throw new IllegalMove("No piece at that square.");
        if (piece.getColor() != toMove) throw new IllegalMove("It's not that side's turn.");

        if (forcedPieceInChain != null && piece != forcedPieceInChain)
            throw new IllegalMove("You must continue the capture chain with the same piece.");

        int[] step = piece.oneStep(dir);
        if (step == null) throw new IllegalMove("That direction is not allowed.");

        boolean destEmpty = board.getPiece(step[0], step[1]) == null;

        // Simple move (allowed even if some other capture exists elsewhere)
        if (destEmpty) {
            if (forcedPieceInChain != null)
                throw new IllegalMove("You must continue the capture chain with the same piece.");
            board.movePieceTo(piece, step[0], step[1]);
            board.applyPromotionIfEligible(piece);
            endTurn();
            return;
        }

        // Attempt capture: adjacent must be enemy; landing must be empty and on board
        Piece victim = board.getPiece(step[0], step[1]);
        if (victim == null || victim.getColor() == piece.getColor())
            throw new IllegalMove("Square blocked.");

        int[] landing = piece.skipOver(step[0], step[1]);
        if (!Board.onBoard(landing[0], landing[1]) || board.getPiece(landing[0], landing[1]) != null)
            throw new IllegalMove("No landing square to complete the capture.");

        // Execute capture
        board.removePiece(victim);
        board.movePieceTo(piece, landing[0], landing[1]);
        board.incrementCapture(toMove);
        board.applyPromotionIfEligible(piece);

        // Continue chain if another capture is available for the same piece
        if (board.canPieceCapture(piece)) {
            forcedPieceInChain = piece;
        } else {
            forcedPieceInChain = null;
            endTurn();
        }
    }

    private void endTurn() {
        whiteToMove = !whiteToMove;
    }
}
