package com.checkers.logic;

import com.checkers.model.Board;
import com.checkers.model.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.checkers.testutil.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Undo / End / Restart behavior")
public class GameServiceUndoEndRestartTest {

    private GameService game;
    private Board b;

    @BeforeEach
    void setup() {
        game = new GameService();
        b = new Board();
        setBoard(game, b);
        clear(b);
        ensureActiveGame(game, b);
    }

    @Test
    @DisplayName("Undo reverts last move when available")
    void undo_reverts_last_move_when_available() throws Exception {
        // Given white c3 and it's white's turn
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        // When white moves c3->d4 and then undoes
        game.moveFromCommand("c3", 'r', false);
        game.undo();

        // Then the piece is back on c3
        Board after = game.getBoard();
        assertNull(after.getPiece(x("d4"), y("d4")));
        assertNotNull(after.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("Undo works multiple times until history empty")
    void undo_multiple_times_until_history_empty() throws Exception {
        // Given white c3, black e6; white to move
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("e6"), y("e6"));
        setTurn(game, true);

        // When white c3->d4; black e6->d5; then undo twice
        game.moveFromCommand("c3", 'r', false);
        game.moveFromCommand("e6", 'l', false);

        game.undo(); // undo black
        game.undo(); // undo white

        // Then both are at original squares
        Board after = game.getBoard();
        assertNull(after.getPiece(x("d5"), y("d5")));
        assertNull(after.getPiece(x("d4"), y("d4")));
        assertNotNull(after.getPiece(x("e6"), y("e6")));
        assertNotNull(after.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("Undo has no effect after user-ended game")
    void undo_has_no_effect_after_user_end() throws Exception {
        // Given a valid move has been made
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);
        game.moveFromCommand("c3", 'r', false);

        // When user ends game and tries undo
        game.endGame();
        assertTrue(game.isGameOver());
        game.undo();

        // Then position is unchanged
        Board after = game.getBoard();
        assertNotNull(after.getPiece(x("d4"), y("d4")));
        assertNull(after.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("End game sets over flag and result text mentions ended")
    void endGame_marks_over_and_message_mentions_ended() {
        // Given active game (ensured in setup)
        assertFalse(game.isGameOver());

        // When ending the game
        game.endGame();

        // Then over and message
        assertTrue(game.isGameOver());
        assertTrue(game.resultText().toLowerCase().contains("ended"));
    }

    @Test
    @DisplayName("Restart resets board, white to move, and clears history")
    void restartGame_resets_board_turn_and_clears_history() throws Exception {
        // Given a move has been made
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);
        game.moveFromCommand("c3", 'r', false); // d4

        // When restarting
        game.restartGame();

        // Then we have fresh initial layout and white to move
        Board fresh = game.getBoard();
        assertNotNull(fresh.getPiece(x("a3"), y("a3")), "initial setup should have a3 occupied by white");
        assertTrue(game.isWhiteToMove());

        // And undo after restart has no effect (history cleared)
        game.undo();
        assertNotNull(fresh.getPiece(x("a3"), y("a3")));
    }
}
