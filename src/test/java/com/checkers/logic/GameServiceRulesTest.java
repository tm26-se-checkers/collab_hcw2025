package com.checkers.logic;

import com.checkers.model.Board;
import com.checkers.model.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.checkers.testutil.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint-1/2 rules (no mandatory capture; must continue mid-chain; kings move both ways).
 * Important: We keep at least one piece for EACH side on the board so the game isn't "already over".
 */
public class GameServiceRulesTest {

    private GameService game;
    private Board b;

    @BeforeEach
    void setup() {
        game = new GameService();
        b = new Board();
        setBoard(game, b);
        clear(b);

        // Ensure game is not over before each test: add a neutral piece for each color.
        // Pick far corners to avoid interfering with specific scenarios.
        add(b, Color.WHITE, x("h1"), y("h1"));
        add(b, Color.BLACK, x("a8"), y("a8"));
    }

    @Test
    void white_c3_to_d4_is_valid_and_turn_switches() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        game.moveFromCommand("c3", 'r', false);

        Piece p = b.getPiece(x("d4"), y("d4"));
        assertNotNull(p);
        assertEquals(Color.WHITE, p.getColor());
        assertFalse(game.isWhiteToMove(), "turn should switch to black");
    }

    @Test
    void non_king_cannot_move_backward() {
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        assertThrows(IllegalMove.class, () -> game.moveFromCommand("c3", 'r', true));
    }

    @Test
    void black_forward_uses_plain_lr() throws Exception {
        add(b, Color.BLACK, x("d6"), y("d6"));
        setTurn(game, false);

        game.moveFromCommand("d6", 'l', false); // black forward-left

        // Then the piece is on d4
        assertNotNull(b.getPiece(x("d4"), y("d4")));
        assertNull(b.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("Invalid move: horizontal is rejected")
    void invalid_horizontal_move_rejected() {
        // Given white on e3
        add(b, Color.WHITE, x("e3"), y("e3"));
        setTurn(game, true);

        game.moveFromCommand("c3", 'r', false);

        assertNull(b.getPiece(x("d4"), y("d4")), "captured piece removed");
        assertNotNull(b.getPiece(x("e5"), y("e5")), "white lands on e5");
        assertFalse(game.isWhiteToMove(), "turn passes if no further capture");
    }

    @Test
    void multi_capture_chain_continues_with_same_piece() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("d4"), y("d4"));
        add(b, Color.BLACK, x("f6"), y("f6"));
        setTurn(game, true);

        game.moveFromCommand("c3", 'r', false); // c3->e5 capture
        assertTrue(game.isWhiteToMove(), "same side continues mid-chain");

        game.moveFromCommand("e5", 'r', false); // e5->g7 capture
        assertNull(b.getPiece(x("d4"), y("d4")));
        assertNull(b.getPiece(x("f6"), y("f6")));
        assertNotNull(b.getPiece(x("g7"), y("g7")));
        assertFalse(game.isWhiteToMove(), "after chain ends, turn passes");
    }

    @Test
    @DisplayName("Promotion: white piece at g7 to h8 becomes king")
    void promotion_to_king_on_back_rank() throws Exception {
        // Given white on g7 and it's white's turn
        add(b, Color.WHITE, x("g7"), y("g7"));
        setTurn(game, true);

        // When moving to back rank
        game.moveFromCommand("g7", 'r', false); // to h8

        // Then it is crowned
        assertTrue(b.getPiece(x("h8"), y("h8")).isKing());
    }

    @Test
    @DisplayName("King can move backward")
    void king_can_move_backward() throws Exception {
        // Given a crowned piece at d4 (white king)
        addKing(b, Color.WHITE, x("d4"), y("d4"));
        setTurn(game, true);

        // When moving backward-left
        game.moveFromCommand("d4", 'l', true); // to c3

        // Then move is accepted
        assertNotNull(b.getPiece(x("c3"), y("c3")));
    }

    @Test
    void simple_move_allowed_even_if_another_capture_exists() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3")); // has capture over d4
        add(b, Color.BLACK, x("d4"), y("d4"));
        add(b, Color.WHITE, x("a3"), y("a3")); // we move this instead
        setTurn(game, true);

        // When black captures g7 -> e5 (over f6), then must continue e5 -> c3 (over d4)
        game.moveFromCommand("g7", 'l', false); // capture to e5 (forward-left for black)
        game.moveFromCommand("e5", 'l', false); // capture to c3 (continue chain with same piece)

        // Then both white pieces are removed and black ends on c3
        assertNull(b.getPiece(x("f6"), y("f6")));
        assertNull(b.getPiece(x("d4"), y("d4")));
        assertNotNull(b.getPiece(x("c3"), y("c3")));
    }
}
