package com.checkers.logic;

import com.checkers.model.Board;
import com.checkers.model.Color;
import com.checkers.model.IllegalMove;
import com.checkers.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.checkers.testutil.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests reflect Sprint-1 behavior:
 *  - No mandatory capture (except continuing an in-progress chain)
 *  - Only kings may move backward (via [b])
 *  - Black forward uses plain l/r (no 'b')
 *  - Multi-capture: if a capture was made and another is available, same piece must continue
 *  - Promotion to king on back rank
 *  - Two-player turn alternation
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
    }

    // 1) Valid Move (White forward one step)
    @Test
    void white_c3_to_d4_is_valid() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true); // white to move

        game.moveFromCommand("c3", 'r', false); // forward-right

        Piece p = b.getPiece(x("d4"), y("d4"));
        assertNotNull(p);
        assertEquals(Color.WHITE, p.getColor());
        assertFalse(game.isWhiteToMove(), "turn should switch to black");
    }

    // 2) Invalid: non-king cannot move backward
    @Test
    void non_king_cannot_move_backward() {
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        assertThrows(IllegalMove.class, () -> game.moveFromCommand("c3", 'r', true));
    }

    // 3) Black forward move does NOT require 'b'
    @Test
    void black_forward_uses_plain_lr() throws Exception {
        add(b, Color.BLACK, x("d6"), y("d6"));
        setTurn(game, false); // black to move

        game.moveFromCommand("d6", 'l', false); // BL for black forward-left

        assertNotNull(b.getPiece(x("c5"), y("c5")));
        assertNull(b.getPiece(x("d6"), y("d6")));
        assertTrue(game.isWhiteToMove(), "turn should switch to white");
    }

    // 4) Single capture (white c3 over black d4 to e5)
    @Test
    void single_capture_white_c3_over_d4_to_e5() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("d4"), y("d4"));
        setTurn(game, true);

        game.moveFromCommand("c3", 'r', false); // should capture to e5

        assertNull(b.getPiece(x("d4"), y("d4")), "captured piece removed");
        assertNotNull(b.getPiece(x("e5"), y("e5")), "white lands on e5");
        assertFalse(game.isWhiteToMove(), "if no further capture, turn should pass to black");
    }

    // 5) Multi-capture chain (white c3 -> e5 -> g7)
    @Test
    void multi_capture_chain_continues_with_same_piece() throws Exception {
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("d4"), y("d4"));
        add(b, Color.BLACK, x("f6"), y("f6"));
        setTurn(game, true);

        game.moveFromCommand("c3", 'r', false); // c3->e5 capture

        // still white to move (same piece must continue)
        assertTrue(game.isWhiteToMove(), "capture chain: same side continues");

        game.moveFromCommand("e5", 'r', false); // e5->g7 capture

        assertNull(b.getPiece(x("d4"), y("d4")));
        assertNull(b.getPiece(x("f6"), y("f6")));
        assertNotNull(b.getPiece(x("g7"), y("g7")));
        assertFalse(game.isWhiteToMove(), "after chain ends, turn passes");
    }

    // 6) Promotion to king (white g7 -> h8)
    @Test
    void promotion_to_king_on_back_rank() throws Exception {
        add(b, Color.WHITE, x("g7"), y("g7"));
        setTurn(game, true);

        game.moveFromCommand("g7", 'r', false);

        Piece p = b.getPiece(x("h8"), y("h8"));
        assertNotNull(p);
        assertTrue(p.isKing(), "should crown on reaching back rank");
    }

    // 7) King can move backward
    @Test
    void king_can_move_backward() throws Exception {
        addKing(b, Color.WHITE, x("d4"), y("d4"));
        setTurn(game, true);

        game.moveFromCommand("d4", 'l', true); // backward-left

        assertNotNull(b.getPiece(x("c3"), y("c3")));
    }

    // 8) No mandatory capture: simple move allowed even if a capture exists elsewhere
    @Test
    void simple_move_allowed_even_if_another_capture_exists() throws Exception {
        // White has a capture with c3 over d4 -> e5, but we move a3 instead.
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("d4"), y("d4"));
        add(b, Color.WHITE, x("a3"), y("a3"));
        setTurn(game, true);

        game.moveFromCommand("a3", 'r', false);

        assertNotNull(b.getPiece(x("b4"), y("b4")));
        assertNull(b.getPiece(x("a3"), y("a3")));
    }

    // 9) Game over when one side has no pieces (winner detection)
    @Test
    void game_over_when_one_side_has_no_pieces() throws Exception {
        // Remove all black pieces
        clear(b);
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        assertTrue(game.isGameOver(), "game should end if one side has no pieces");
        assertTrue(game.resultText().toLowerCase().contains("white"), "white should be declared winner");
    }
}
