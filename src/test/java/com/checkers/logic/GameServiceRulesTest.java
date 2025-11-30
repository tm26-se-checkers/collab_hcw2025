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
        ensureActiveGame(game, b); // keep the game “alive” (both colors present)
    }

    @Test
    @DisplayName("Valid move: white c3 -> d4 (diagonal)")
    void white_c3_to_d4_is_valid() throws Exception {
        // Given
        add(b, Color.WHITE, x("c3"), y("c3"));
        setTurn(game, true);

        // When
        game.moveFromCommand("c3", 'r', false);

        // Then
        assertNotNull(b.getPiece(x("d4"), y("d4")));
        assertNull(b.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("Invalid move: horizontal is rejected (bad direction token)")
    void invalid_horizontal_move_rejected() {
        // Given
        add(b, Color.WHITE, x("e3"), y("e3"));
        setTurn(game, true);

        // When/Then
        assertThrows(Exception.class, () -> game.moveFromCommand("e3", 'x', false));
    }

    @Test
    @DisplayName("Black forward uses plain l/r (no 'b' needed)")
    void black_forward_uses_plain_lr() throws Exception {
        // Given
        add(b, Color.BLACK, x("b6"), y("b6"));
        setTurn(game, false);

        // When (black forward-left is 'l' without 'b')
        game.moveFromCommand("b6", 'l', false);

        // Then (b6 -> a5)
        assertNotNull(b.getPiece(x("a5"), y("a5")));
        assertNull(b.getPiece(x("b6"), y("b6")));
    }

    @Test
    @DisplayName("Promotion: white piece at g7 to h8 becomes king")
    void promotion_to_king_on_back_rank() throws Exception {
        // Given
        add(b, Color.WHITE, x("g7"), y("g7"));
        setTurn(game, true);

        // When
        game.moveFromCommand("g7", 'r', false); // to h8

        // Then
        assertTrue(b.getPiece(x("h8"), y("h8")).isKing());
    }

    @Test
    @DisplayName("King can move backward")
    void king_can_move_backward() throws Exception {
        // Given
        addKing(b, Color.WHITE, x("d4"), y("d4"));
        setTurn(game, true);

        // When (backward-left)
        game.moveFromCommand("d4", 'l', true); // to c3

        // Then
        assertNotNull(b.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("Multiple capture chain continues with the same piece (black g7→e5→c3)")
    void multiple_capture_chain() throws Exception {
        // Given: black on g7; white on f6 and d4; e5 and c3 free
        add(b, Color.BLACK, x("g7"), y("g7"));
        add(b, Color.WHITE, x("f6"), y("f6"));
        add(b, Color.WHITE, x("d4"), y("d4"));
        setTurn(game, false); // black to move

        // When: g7 -> e5 (capture over f6), then e5 -> c3 (capture over d4)
        game.moveFromCommand("g7", 'l', false); // to e5
        game.moveFromCommand("e5", 'l', false); // to c3

        // Then: both whites removed, black ends on c3
        assertNull(b.getPiece(x("f6"), y("f6")));
        assertNull(b.getPiece(x("d4"), y("d4")));
        assertNotNull(b.getPiece(x("c3"), y("c3")));
    }

    @Test
    @DisplayName("No mandatory capture globally (outside a chain)")
    void may_ignore_available_capture_when_not_in_chain() throws Exception {
        // Given: capture available from c3 over d4 to e5, but we move a different white
        add(b, Color.WHITE, x("c3"), y("c3"));
        add(b, Color.BLACK, x("d4"), y("d4"));
        add(b, Color.WHITE, x("a3"), y("a3"));
        setTurn(game, true);

        // When: move a3 -> b4 (ignore capture with c3)
        game.moveFromCommand("a3", 'r', false);

        // Then: allowed
        assertNotNull(b.getPiece(x("b4"), y("b4")));
    }
}
