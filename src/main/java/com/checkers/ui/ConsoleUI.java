package com.checkers.ui;

import com.checkers.logic.GameService;
import com.checkers.model.Board;
import com.checkers.model.IllegalMove;
import org.apache.commons.lang3.StringUtils;

import java.util.Scanner;

public class ConsoleUI {

    private final Scanner in = new Scanner(System.in);
    private final GameService game = new GameService();

    public void start() {
        printBoard(game.getBoard());

        while (!game.isGameOver()) {
            System.out.println(game.isWhiteToMove() ? "White's turn." : "Black's turn.");
            System.out.println("Enter move: <square> <l|r> [b]   e.g.  a3 r   or   d4 l b (king only)");

            String line = in.nextLine();
            if (StringUtils.isBlank(line)) {
                System.out.println("Invalid input.");
                continue;
            }

            String[] parts = line.trim().toLowerCase().split("\\s+");
            if (parts.length < 2) {
                System.out.println("Invalid input. Example: a3 r   or   d4 l b");
                continue;
            }

            String square = parts[0];
            char lr = parts[1].charAt(0);
            boolean back = parts.length >= 3 && parts[2].charAt(0) == 'b';

            try {
                game.moveFromCommand(square, lr, back);
                printBoard(game.getBoard());
            } catch (IllegalMove e) {
                System.out.println(e.getMessage() == null ? "Illegal move." : "Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Invalid input.");
            }
        }

        System.out.println("Game over. " + game.resultText());
    }

    private void printBoard(Board board) {
        // colored output enabled by Jansi in Main
        System.out.println(board.render(true));
    }
}
