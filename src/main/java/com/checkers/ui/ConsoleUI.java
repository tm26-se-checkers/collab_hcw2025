package com.checkers.ui;

import com.checkers.logic.GameService;
import com.checkers.model.Board;
import com.checkers.model.IllegalMove;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;

public class ConsoleUI {

    private static final Logger log = LogManager.getLogger(ConsoleUI.class);

    private final Scanner in = new Scanner(System.in);
    private final GameService game = new GameService();

    public void start() {
        log.info("Console session started.");
        printBoard(game.getBoard());

        while (!game.isGameOver()) {
            System.out.println(game.isWhiteToMove() ? "White's turn." : "Black's turn.");
            System.out.println("Enter move: <square> <l|r> [b]  | commands: help, undo, end, restart");

            String line = in.nextLine();
            log.debug("User raw input: {}", line);

            if (StringUtils.isBlank(line)) {
                System.out.println("Invalid input.");
                continue;
            }

            String input = line.trim().toLowerCase();

            // ---- Commands (Sprint-2) ----
            if (input.equals("help")) {
                printHelp();
                continue;
            }

            if (input.equals("undo")) {
                if (game.isGameOver()) {
                    System.out.println("Game is over; cannot undo.");
                    continue;
                }
                if (!game.hasUndo()) {
                    System.out.println("Nothing to undo.");
                    continue;
                }
                game.undo();
                System.out.println("Undid last move.");
                printBoard(game.getBoard());
                continue;
            }

            if (input.equals("end")) {
                if (confirm("Are you sure you want to end the game? (yes/no) ")) {
                    game.endGame();
                    System.out.println("Game ended by user.");
                    break; // loop ends because isGameOver() now true
                } else {
                    System.out.println("Cancelled. Game continues.");
                }
                continue;
            }

            if (input.equals("restart")) {
                if (confirm("Are you sure you want to restart the game? (yes/no) ")) {
                    game.restartGame();
                    System.out.println("Game restarted.");
                    printBoard(game.getBoard());
                } else {
                    System.out.println("Cancelled. Game continues.");
                }
                continue;
            }

            // ---- Move parsing: "<square> <l|r> [b]" ----
            String[] parts = input.split("\\s+");
            if (parts.length < 2) {
                System.out.println("Invalid input. Example: a3 r   or   d4 l b");
                log.debug("Move rejected: not enough tokens.");
                continue;
            }

            String square = parts[0];
            char lr = parts[1].charAt(0);
            boolean back = parts.length >= 3 && parts[2].charAt(0) == 'b';

            try {
                log.debug("Parsed move command: square={}, dir={}, back={}", square, lr, back);
                game.moveFromCommand(square, lr, back);
                printBoard(game.getBoard());
            } catch (IllegalMove e) {
                System.out.println(e.getMessage() == null ? "Illegal move." : "Error: " + e.getMessage());
                log.debug("IllegalMove thrown: {}", e.getMessage());
            } catch (Exception e) {
                System.out.println("Invalid input.");
                log.warn("Unexpected exception while parsing/playing a move.", e);
            }
        }

        String result = game.resultText();
        System.out.println("Game over. " + result);
        log.info("Game over: {}", result);
    }

    private boolean confirm(String prompt) {
        System.out.print(prompt);
        String ans = in.nextLine();
        boolean yes = ans != null && ans.trim().equalsIgnoreCase("yes");
        log.debug("Confirmation '{}' → {}", prompt.trim(), yes ? "YES" : "NO");
        return yes;
    }

    private boolean confirm(String prompt) {
        System.out.print(prompt);
        String ans = in.nextLine();
        return ans != null && ans.trim().equalsIgnoreCase("yes");
    }

    private void printBoard(Board board) {
        // colored output enabled by Jansi in Main
        System.out.println(board.render(true));
    }

    private void printHelp() {
        System.out.println("""
                HOW TO PLAY (current features)
                --------------------------------
                Moves:
                  - Enter: <square> <(l)eft|(r)ight> [b]
                    Examples:
                      a3 r       (white forward-right)
                      b6 l       (black forward-left)
                      d4 r b     (KING only: backward-right)
                  - Only kings may move backward (use 'b').
                  - Captures (jump over adjacent opponent) are supported.
                  - Multi-capture: if you captured and can capture again, you must continue with the same piece.
                  - There is NO mandatory capture otherwise (you may ignore available captures).
                
                Turns & Ending:
                  - Two-player local play (White starts).
                  - The game ends automatically if a side has no pieces or the current side has no legal moves.
                  - Type 'end' to end the game (confirmation required).
                  - Type 'restart' to reset the game (confirmation required).
                  - Type 'undo' to take back the last move (unlimited, until history is empty).
                  - Type 'help' to show this help.
                """);
    }
}
