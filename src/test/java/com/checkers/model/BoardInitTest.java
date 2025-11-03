package com.checkers.model;

import com.checkers.testutil.TestHelpers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BoardInitTest {

    @Test
    void starts_with_12_white_and_12_black_on_correct_rows() {
        Board b = new Board();
        List<Piece> ps = TestHelpers.pieces(b);

        long whites = ps.stream().filter(p -> p.getColor() == Color.WHITE).count();
        long blacks = ps.stream().filter(p -> p.getColor() == Color.BLACK).count();

        assertEquals(12, whites, "White should start with 12 pieces");
        assertEquals(12, blacks, "Black should start with 12 pieces");

        // Orientation: White rows 1..3, Black rows 6..8
        assertTrue(ps.stream().filter(p -> p.getColor() == Color.WHITE).allMatch(p -> p.getY() <= 3));
        assertTrue(ps.stream().filter(p -> p.getColor() == Color.BLACK).allMatch(p -> p.getY() >= 6));
    }

    @Test
    void board_render_has_axes_and_grid() {
        Board b = new Board();
        String out = b.render(false);
        // smoke checks (robust against minor formatting tweaks)
        assertTrue(out.contains("-"), "should include horizontal separators");
        assertTrue(out.contains(" a"), "should show file labels a..h");
        assertTrue(out.contains(" h"), "should show file labels a..h");
        assertTrue(out.contains("|"), "should show columns");
    }
}
