package com.checkers.logic;

import com.checkers.model.Board;
import com.checkers.model.Color;
import com.checkers.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.checkers.testutil.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint-4 features in GameService:
 *  - undo (unlimited until history empty; blocked after game over)
 *  - end game (user-ended)
 *  - restart game (fresh board, history cleared)
 * Ensure board is not "already over" by keeping at least one piece per side.
 */
public class GameServiceUndoEndRestartTest {

    private GameService game;
    private Board b;

    @BeforeEach
    void setup() {
        game = new GameService();
        b = new Board();
        setBoard(game, b);
        clear(b);

        // Keep game active initially
        add(b, Color.WHITE, x("h1"), y("h1"));
        add(b, Color.BLACK, x("a8"), y("a8"));
    }

    @Test
    void undo_reverts_last_move_when_available() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        game.moveFromCommand("c3", 'r', false); // to d4
        assertNotNull(b.getPiece(x("d4"), y("d4")));

        game.undo();

        Board after = game.getBoard();
        assertNull(after.getPiece(x("d4"), y("d4")));
        assertNotNull(after.getPiece(x("c3"), y("c3")));
    }

    @Test
    void undo_multiple_times_until_history_empty() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("e6"), y("e6"));
        setTurn(game, true);

        game.moveFromCommand("c3", 'r', false); // white d4
        setTurn(game, false);
        game.moveFromCommand("e6", 'l', false); // black d5

        game.undo(); // undo black
        game.undo(); // undo white

        Board after = game.getBoard();
        assertNull(after.getPiece(x("d5"), y("d5")));
        assertNull(after.getPiece(x("d4"), y("d4")));
        assertNotNull(after.getPiece(x("e6"), y("e6")));
        assertNotNull(after.getPiece(x("c3"), y("c3")));
    }

    @Test
    void undo_has_no_effect_after_user_end() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);
        game.moveFromCommand("c3", 'r', false); // to d4

        game.endGame();
        assertTrue(game.isGameOver());

        game.undo(); // should be ignored

        Board after = game.getBoard();
        assertNotNull(after.getPiece(x("d4"), y("d4")));
        assertNull(after.getPiece(x("c3"), y("c3")));
    }

    @Test
    void endGame_marks_over_and_message_mentions_ended() {
        // Game should NOT be over at start thanks to baseline pieces
        assertFalse(game.isGameOver(), "game should be active before ending");

        game.endGame();

        assertTrue(game.isGameOver());
        assertTrue(game.resultText().toLowerCase().contains("ended"), "result should state ended by user");
    }

    @Test
    void restartGame_resets_board_turn_and_clears_history() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);
        game.moveFromCommand("c3", 'r', false); // to d4

        game.restartGame();

        Board fresh = game.getBoard();
        // spot-check a standard start square (white at a3 is typical for your init pattern)
        assertNotNull(fresh.getPiece(x("a3"), y("a3")), "fresh board should have white at a3 (initial setup)");
        assertTrue(game.isWhiteToMove(), "white starts after restart");

        // Undo should not change anything (history cleared)
        game.undo();
        assertNotNull(fresh.getPiece(x("a3"), y("a3")));
    }
}
