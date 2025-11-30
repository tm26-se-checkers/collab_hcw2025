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
@DisplayName("Game rules: movement, capture, promotion")
public class GameServiceRulesTest {

    private GameService game;
    private Board b;

    @BeforeEach
    void setup() {
        game = new GameService();
        b = new Board();
        setBoard(game, b);
        clear(b);
        ensureActiveGame(game, b); // keep game “alive”
    }

    @Test
    @DisplayName("Valid move: white c3 -> d4 (diagonal)")
    void white_c3_to_d4_is_valid() throws Exception {
        // Given white on c3 and it's white's turn
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        // When moving right forward
        game.moveFromCommand("c3", 'r', false);

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

        // When trying to move 'horizontally' (simulated by bad input)
        var ex = assertThrows(Exception.class, () -> game.moveFromCommand("e3", 'x', false));

        // Then an error occurs (direction must be l|r)
        assertNotNull(ex);
    }

    @Test
    @DisplayName("Black forward uses plain l/r (no 'b' needed)")
    void black_forward_uses_plain_lr() throws Exception {
        // Given black on b6 and it's black's turn
        add(b, Color.BLACK, x("b6"), y("b6"));
        setTurn(game, false);

        // When moving forward-left (down-left for black)
        game.moveFromCommand("b6", 'l', false);

        // Then the piece moved to a5 (b6 -> a5)
        assertNotNull(b.getPiece(x("a5"), y("a5")));
        assertNull(b.getPiece(x("b6"), y("b6")));
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
    @DisplayName("Multiple capture chain continues with the same piece")
    void multiple_capture_chain() throws Exception {
        // Given black on g7, whites on f6 and d4, landing squares e5 and c3 are free
        add(b, Color.BLACK, x("g7"), y("g7"));
        add(b, Color.WHITE, x("f6"), y("f6"));
        add(b, Color.WHITE, x("d4"), y("d4"));
        setTurn(game, false); // black to move

        // When black captures g7 -> e5 (over f6), then must continue e5 -> c3 (over d4)
        game.moveFromCommand("g7", 'l', false); // capture to e5 (forward-left for black)
        game.moveFromCommand("e5", 'l', false); // capture to c3 (continue chain with same piece)

        // Then both white pieces are removed and black ends on c3
        assertNull(b.getPiece(x("f6"), y("f6")));
        assertNull(b.getPiece(x("d4"), y("d4")));
        assertNotNull(b.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("No mandatory capture globally (outside a chain)")
    void may_ignore_available_capture_when_not_in_chain() throws Exception {
        // Given white has a capture available elsewhere
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("d4"), y("d4")); // capture available c3->e5
        add(b, Color.WHITE, x("a3"), y("a3")); // white chooses to move this instead
        setTurn(game, true);

        // When white moves a3->b4 (ignoring capture with c3)
        game.moveFromCommand("a3", 'r', false);

        // Then move is accepted (no global mandatory capture)
        assertNotNull(b.getPiece(x("b4"), y("b4")));
    }
}
