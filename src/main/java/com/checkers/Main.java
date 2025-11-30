package com.checkers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        org.fusesource.jansi.AnsiConsole.systemInstall();
        try {
            log.info("Checkers starting…");
            new com.checkers.ui.ConsoleUI().start();
            log.info("Checkers exiting normally.");
        } finally {
            org.fusesource.jansi.AnsiConsole.systemUninstall();
        }
    }
}
