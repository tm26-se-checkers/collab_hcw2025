package com.checkers;

import com.checkers.ui.ConsoleUI;
import org.fusesource.jansi.AnsiConsole;

public class Main {
    public static void main(String[] args) {
        // enable ANSI colors
        AnsiConsole.systemInstall();
        try {
            new ConsoleUI().start();
        } finally {
            AnsiConsole.systemUninstall();
        }
    }
}
