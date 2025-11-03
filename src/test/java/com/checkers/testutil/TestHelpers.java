package com.checkers.testutil;

import com.checkers.logic.GameService;
import com.checkers.model.Board;
import com.checkers.model.Color;
import com.checkers.model.Piece;

import java.lang.reflect.Field;
import java.util.List;

public final class TestHelpers {

    private TestHelpers() {}

    // ===== Board access via reflection (test-only) =====
    @SuppressWarnings("unchecked")
    public static List<Piece> pieces(Board b) {
        try {
            Field f = Board.class.getDeclaredField("pieces");
            f.setAccessible(true);
            return (List<Piece>) f.get(b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void clear(Board b) {
        pieces(b).clear();
        setInt(b, "whiteCaptures", 0);
        setInt(b, "blackCaptures", 0);
    }

    public static void add(Board b, Color c, int x, int y) {
        pieces(b).add(new Piece(c, x, y));
    }

    public static void addKing(Board b, Color c, int x, int y) {
        pieces(b).add(new Piece(c, true, x, y));
    }

    // ===== GameService wiring =====
    public static void setBoard(GameService g, Board b) {
        setObj(g, "board", b);
    }

    public static void setTurn(GameService g, boolean whiteToMove) {
        setObj(g, "whiteToMove", whiteToMove);
    }

    // ===== small helpers =====
    public static int x(String sq) { return (sq.toLowerCase().charAt(0) - 'a') + 1; }
    public static int y(String sq) { return Integer.parseInt(sq.substring(1)); }

    // ===== reflection utils =====
    private static void setObj(Object target, String field, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static void setInt(Object target, String field, int value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.setInt(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
