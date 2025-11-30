package com.checkers.model;

/**
 * Strategy for piece movement: which directions are allowed and how steps are computed.
 * Direction indices must match GameService: 0:FL, 1:FR, 2:BL, 3:BR.
 */
interface MovementPolicy {
    /**
     * @param color piece color
     * @param king  whether the piece is a king
     * @param dir   direction index (0..3)
     * @return true if movement/capture is allowed in that direction for this piece
     */
    boolean allowsDir(Color color, boolean king, int dir);

    /**
     * @param dir direction index (0..3)
     * @return a 2-int array {dx, dy} for the given direction
     */
    int[] delta(int dir);
}
