package com.checkers.model;

/** Forward-only movement policy for men (non-king pieces). */
final class ManMovementPolicy implements MovementPolicy {

    private static final int[][] DIRS = {
            {-1, +1}, // 0: FL
            {+1, +1}, // 1: FR
            {-1, -1}, // 2: BL
            {+1, -1}  // 3: BR
    };

    @Override
    public boolean allowsDir(Color color, boolean king, int dir) {
        if (king) return true; // not used for kings, but safe
        return (color == Color.WHITE && (dir == 0 || dir == 1))
                || (color == Color.BLACK && (dir == 2 || dir == 3));
    }

    @Override
    public int[] delta(int dir) {
        return DIRS[dir];
    }
}
