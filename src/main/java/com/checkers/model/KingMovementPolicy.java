package com.checkers.model;

/** Kings can move and capture in all four diagonal directions. */
final class KingMovementPolicy implements MovementPolicy {

    private static final int[][] DIRS = {
            {-1, +1}, // 0: FL
            {+1, +1}, // 1: FR
            {-1, -1}, // 2: BL
            {+1, -1}  // 3: BR
    };

    @Override
    public boolean allowsDir(Color color, boolean king, int dir) {
        return true;
    }

    @Override
    public int[] delta(int dir) {
        return DIRS[dir];
    }
}
