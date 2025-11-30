package com.checkers.logic;

import com.checkers.model.Board;
import com.checkers.model.Color;
import com.checkers.model.IllegalMove;
import com.checkers.model.Piece;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GameService: business rules, turn handling, move validation, capture chains, promotion,
 * and sprint-2 actions (undo, end, restart).
 *
 * Design patterns:
 *  - Facade: simple API for UI
 *  - Memento: undo via internal history snapshots
 */
public class GameService {

    private static final Logger log = LogManager.getLogger(GameService.class);

    // direction indices used consistently across model
    public static final int FL = 0; // forward-left
    public static final int FR = 1; // forward-right
    public static final int BL = 2; // backward-left
    public static final int BR = 3; // backward-right

    private Board board = new Board();
    private boolean whiteToMove = true;

    /** If a capture just happened and can continue, the same piece must play again. */
    private Piece forcedPieceInChain = null;

    /** Undo history (Memento): snapshot of board + whose turn. */
    private static final class HistoryEntry {
        final Board boardCopy;
        final boolean whiteToMove;
        HistoryEntry(Board b, boolean turn) { this.boardCopy = b; this.whiteToMove = turn; }
    }
    private final Deque<HistoryEntry> history = new ArrayDeque<>();

    /** User-requested end flag. */
    private boolean userEnded = false;

    // --- Query API ---

    public Board getBoard() { return board; }

    public boolean isWhiteToMove() { return whiteToMove; }

    public boolean hasUndo() { return !history.isEmpty(); }

    /** Game is over if: user ended; one side has no pieces; or side to move has no legal moves. */
    public boolean isGameOver() {
        if (userEnded) return true;
        int winner = board.getWinner();
        if (winner != 0) return true;
        return !board.anyLegalMove(whiteToMove ? Color.WHITE : Color.BLACK, forcedPieceInChain);
    }

    /** Human-readable result text. */
    public String resultText() {
        if (userEnded) return "Game ended by user.";
        int winner = board.getWinner();
        return switch (winner) {
            case 1 -> "White has captured all opponent pieces. White wins.";
            case -1 -> "Black has captured all opponent pieces. Black wins.";
            default -> (whiteToMove ? "White" : "Black") + " has no legal moves. Draw or stalemate by no-move.";
        };
    }

    // --- Sprint-2 features ---

    /** Undo the last completed move (or entire capture chain). No effect if history empty or game over. */
    public void undo() {
        if (history.isEmpty() || isGameOver()) {
            log.debug("Undo ignored (historyEmpty={}, gameOver={})", history.isEmpty(), isGameOver());
            return;
        }
        HistoryEntry e = history.pop();
        this.board = new Board(e.boardCopy); // deep copy to avoid aliasing
        this.whiteToMove = e.whiteToMove;
        this.forcedPieceInChain = null;      // chain does not persist across undo
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

    // --- Move API ---

    /**
     * Parse a console command: "&lt;square&gt; &lt;l|r&gt; [b]".
     * White forward is up (+y), Black forward is down (-y).
     * Only kings may move/capture backward (with [b]).
     * No global mandatory capture (player may ignore captures), BUT if a capture chain has started,
     * the same piece must continue capturing if possible.
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
        } else { // BLACK
            if (!backwardRequested) dir = (d == 'l') ? BL : BR; // forward for black is down the board
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

    /**
     * Execute a move by absolute direction index.
     * Simple steps are allowed (no global mandatory capture), except when in a capture chain.
     * Captures remove the opponent and may trigger chain continuation for the same piece.
     */
    public void moveByDirection(int x, int y, int dir) throws IllegalMove {
        if (isGameOver()) {
            log.debug("Move rejected: game already over.");
            throw new IllegalMove("Game is already over.");
        }

        Color toMove = whiteToMove ? Color.WHITE : Color.BLACK;

        Piece piece = board.getPiece(x, y);
        if (piece == null) {
            log.debug("Illegal: no piece at {}{}", (char) ('a' + x - 1), y);
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
            log.debug("Illegal: direction not allowed or off-board (dir={})", dir);
            throw new IllegalMove("That direction is not allowed.");
        }

        boolean destEmpty = board.getPiece(step[0], step[1]) == null;

        // --- Simple move (only when not in a capture chain) ---
        if (destEmpty) {
            if (forcedPieceInChain != null) {
                log.debug("Illegal: attempted simple move while in capture chain.");
                throw new IllegalMove("You must continue the capture chain with the same piece.");
            }
            snapshot(); // snapshot before the move to allow undo
            log.debug("Move {} {}{} -> {}{}", piece.getColor(),
                    (char) ('a' + x - 1), y, (char) ('a' + step[0] - 1), step[1]);
            board.movePieceTo(piece, step[0], step[1]);

            boolean wasKing = piece.isKing();
            board.applyPromotionIfEligible(piece);
            if (!wasKing && piece.isKing()) {
                log.info("Promotion: {} crowned at {}{}", piece.getColor(),
                        (char) ('a' + piece.getX() - 1), piece.getY());
            }
            endTurn();
            return;
        }

        // --- Attempt capture ---
        Piece victim = board.getPiece(step[0], step[1]);
        if (victim == null || victim.getColor() == piece.getColor()) {
            log.debug("Illegal: square blocked or same-color piece at {}{}", (char) ('a' + step[0] - 1), step[1]);
            throw new IllegalMove("Square blocked.");
        }

        int[] landing = piece.skipOver(step[0], step[1]);
        if (!Board.onBoard(landing[0], landing[1]) || board.getPiece(landing[0], landing[1]) != null) {
            log.debug("Illegal: no valid landing square after capture.");
            throw new IllegalMove("No landing square to complete the capture.");
        }

        if (forcedPieceInChain == null) snapshot(); // snapshot once at start of capture sequence

        log.debug("Capture {} {}{} x {}{} -> {}{}", piece.getColor(),
                (char) ('a' + x - 1), y,
                (char) ('a' + step[0] - 1), step[1],
                (char) ('a' + landing[0] - 1), landing[1]);

        board.removePiece(victim);
        board.movePieceTo(piece, landing[0], landing[1]);
        board.incrementCapture(toMove);

        boolean wasKing = piece.isKing();
        board.applyPromotionIfEligible(piece);
        if (!wasKing && piece.isKing()) {
            log.info("Promotion: {} crowned at {}{}", piece.getColor(),
                    (char) ('a' + piece.getX() - 1), piece.getY());
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

    // --- internals ---

    private void endTurn() {
        whiteToMove = !whiteToMove;
        log.debug("Turn ended. Next to move: {}", whiteToMove ? "WHITE" : "BLACK");
    }

    private void snapshot() {
        history.push(new HistoryEntry(new Board(board), whiteToMove));
        log.trace("Snapshot saved (history size = {}).", history.size());
    }
}
