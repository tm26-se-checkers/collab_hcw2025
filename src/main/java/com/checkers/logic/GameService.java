package com.checkers.logic;

import com.checkers.model.*;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * GameService: business logic / rules, turn handling, move validation.
 * Sprint-4 adds:
 *  - undo last move (unlimited, until no history)
 *  - end game (user-triggered)
 *  - restart game (reset to fresh board)
 *
 * Notes:
 *  - No mandatory capture (except continuing an in-progress capture chain)
 *  - Only kings may move backward
 *  - Black "forward" uses plain l/r (no 'b')
 */
public class GameService {

    private static final Logger log = LogManager.getLogger(GameService.class);

    // direction indices used by Piece helpers
    public static final int FL = 0; // forward-left
    public static final int FR = 1; // forward-right
    public static final int BL = 2; // backward-left
    public static final int BR = 3; // backward-right

    private Board board = new Board();
    private boolean whiteToMove = true;
    private Piece forcedPieceInChain = null; // if a capture was made and can continue, same piece must move

    // Sprint-2: history for UNDO (snapshot before each move/chain starts)
    private static final class HistoryEntry {
        final Board boardCopy;
        final boolean whiteToMove;
        HistoryEntry(Board b, boolean turn) { this.boardCopy = b; this.whiteToMove = turn; }
    }
    private final Deque<HistoryEntry> history = new ArrayDeque<>();

    // Sprint-2: user-ended flag
    private boolean userEnded = false;

    public Board getBoard() { return board; }
    public boolean isWhiteToMove() { return whiteToMove; }
    public boolean hasUndo() { return !history.isEmpty(); }

    /** Undo the last completed move (or entire capture chain). No effect if history empty or game over. */
    public void undo() {
        if (history.isEmpty() || isGameOver()) {
            log.debug("Undo ignored (historyEmpty={}, gameOver={})", history.isEmpty(), isGameOver());
            return;
        }
        HistoryEntry e = history.pop();
        this.board = new Board(e.boardCopy);   // deep copy to avoid aliasing
        this.whiteToMove = e.whiteToMove;
        this.forcedPieceInChain = null;        // chain never spans across moves after undo
        log.info("Undo applied. Restored turn: {}", whiteToMove ? "WHITE" : "BLACK");
    }

    /** Mark the game as ended by user. */
    public void endGame() {
        userEnded = true;
        log.info("Game flagged as ended by user.");
    }

    /** Restart to a fresh board; clear history and flags. */
    public void restartGame() {
        log.info("Restarting game: fresh board, history cleared.");
        board = new Board();
        whiteToMove = true;
        forcedPieceInChain = null;
        history.clear();
        userEnded = false;
    }

    public boolean hasUndo() { return !history.isEmpty(); }

    /** Undo the last completed move (or capture chain). No effect if history empty. */
    public void undo() {
        if (history.isEmpty() || isGameOver()) return;
        HistoryEntry e = history.pop();
        this.board = new Board(e.boardCopy);     // deep copy to avoid aliasing
        this.whiteToMove = e.whiteToMove;
        this.forcedPieceInChain = null;          // chain never spans across moves after undo
    }

    /** Mark the game as ended by user. */
    public void endGame() {
        userEnded = true;
    }

    /** Restart to a fresh board, clear history and flags. */
    public void restartGame() {
        board = new Board();
        whiteToMove = true;
        forcedPieceInChain = null;
        history.clear();
        userEnded = false;
    }

    public boolean isGameOver() {
        if (userEnded) return true;
        int winner = board.getWinner();
        if (winner != 0) return true;
        return !board.anyLegalMove(whiteToMove ? Color.WHITE : Color.BLACK, forcedPieceInChain);
    }

    public String resultText() {
        if (userEnded) return "Game ended by user.";
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
        if (piece == null) {
            log.debug("Illegal: no piece at {}", square);
            throw new IllegalMove("No piece at that square.");
        }
        Color toMove = isWhiteToMove() ? Color.WHITE : Color.BLACK;
        if (piece.getColor() != toMove) {
            log.debug("Illegal: wrong side to move ({} tried to move {})", toMove, piece.getColor());
            throw new IllegalMove("It's not that side's turn.");
        }

        char d = Character.toLowerCase(lr);
        if (d != 'l' && d != 'r') {
            log.debug("Illegal: direction must be 'l' or 'r' (got {})", lr);
            throw new IllegalMove("Direction must be 'l' or 'r'.");
        }

        boolean backwardRequested = back;
        int dir;

        if (piece.getColor() == Color.WHITE) {
            if (!backwardRequested) dir = (d == 'l') ? FL : FR;
            else {
                if (!piece.isKing()) {
                    log.debug("Illegal: non-king white tried to move backward.");
                    throw new IllegalMove("Only kings can move backward.");
                }
                dir = (d == 'l') ? BL : BR;
            }
        } else {
            if (!backwardRequested) dir = (d == 'l') ? BL : BR; // black forward is down the board
            else {
                if (!piece.isKing()) {
                    log.debug("Illegal: non-king black tried to move backward.");
                    throw new IllegalMove("Only kings can move backward.");
                }
                dir = (d == 'l') ? FL : FR;
            }
        }

        moveByDirection(x, y, dir);
    }

    public void moveByDirection(int x, int y, int dir) throws IllegalMove {
        if (isGameOver()) throw new IllegalMove("Game is already over.");

        Color toMove = whiteToMove ? Color.WHITE : Color.BLACK;

        Piece piece = board.getPiece(x, y);
        if (piece == null) {
            log.debug("Illegal: no piece at {}{}", (char)('a' + x - 1), y);
            throw new IllegalMove("No piece at that square.");
        }
        if (piece.getColor() != toMove) {
            log.debug("Illegal: it's {}'s turn, but {} piece selected.", toMove, piece.getColor());
            throw new IllegalMove("It's not that side's turn.");
        }

        if (forcedPieceInChain != null && piece != forcedPieceInChain) {
            log.debug("Illegal: must continue capture chain with same piece.");
            throw new IllegalMove("You must continue the capture chain with the same piece.");
        }

        int[] step = piece.oneStep(dir);
        if (step == null) {
            log.debug("Illegal: direction not allowed for this piece (dir={})", dir);
            throw new IllegalMove("That direction is not allowed.");
        }

        boolean destEmpty = board.getPiece(step[0], step[1]) == null;

        // ---- Snapshot BEFORE making any move (so undo reverts the whole move/chain) ----
        if (destEmpty) {
            if (forcedPieceInChain != null) {
                log.debug("Illegal: attempted simple move while in capture chain.");
                throw new IllegalMove("You must continue the capture chain with the same piece.");
            snapshot();
            board.movePieceTo(piece, step[0], step[1]);
            boolean wasKing = piece.isKing();
            board.applyPromotionIfEligible(piece);
            if (!wasKing && piece.isKing()) {
                log.info("Promotion: {} crowned at {}{}", piece.getColor(),
                        (char)('a' + piece.getX() - 1), piece.getY());
            }
            endTurn();
            return;
        }

        // ---- Attempt capture: adjacent must be enemy; landing must be empty and on board ----
        Piece victim = board.getPiece(step[0], step[1]);
        if (victim == null || victim.getColor() == piece.getColor()) {
            log.debug("Illegal: square blocked or same-color piece at {}{}", (char)('a' + step[0] - 1), step[1]);
            throw new IllegalMove("Square blocked.");
        }

        int[] landing = piece.skipOver(step[0], step[1]);
        if (!Board.onBoard(landing[0], landing[1]) || board.getPiece(landing[0], landing[1]) != null) {
            log.debug("Illegal: no valid landing square after capture.");
            throw new IllegalMove("No landing square to complete the capture.");
        }

        if (forcedPieceInChain == null) snapshot(); // snapshot at start of capture sequence

        log.debug("Capture {} {}{} x {}{} -> {}{}", piece.getColor(),
                (char)('a' + x - 1), y,
                (char)('a' + step[0] - 1), step[1],
                (char)('a' + landing[0] - 1), landing[1]);

        // Snapshot once at the start of a capture sequence
        if (forcedPieceInChain == null) snapshot();

        // Execute capture
        board.removePiece(victim);
        board.movePieceTo(piece, landing[0], landing[1]);
        board.incrementCapture(toMove);

        boolean wasKing = piece.isKing();
        board.applyPromotionIfEligible(piece);
        if (!wasKing && piece.isKing()) {
            log.info("Promotion: {} crowned at {}{}", piece.getColor(),
                    (char)('a' + piece.getX() - 1), piece.getY());
        }

        // Continue chain if another capture is available for the same piece
        if (board.canPieceCapture(piece)) {
            forcedPieceInChain = piece;
            log.debug("Capture chain continues for the same piece.");
        } else {
            forcedPieceInChain = null;
            endTurn();
        }
    }

    private void endTurn() {
        whiteToMove = !whiteToMove;
        log.debug("Turn ended. Next to move: {}", whiteToMove ? "WHITE" : "BLACK");
    }

    private void snapshot() {
        history.push(new HistoryEntry(new Board(board), whiteToMove));
        log.trace("Snapshot saved (history size = {}).", history.size());
    }

    private void snapshot() {
        // store deep copy + turn
        history.push(new HistoryEntry(new Board(board), whiteToMove));
    }
}
