package com.checkers.model;

@SuppressWarnings("serial")
public class IllegalMove extends Exception {
    public IllegalMove() {}
    public IllegalMove(String msg) { super(msg); }
}
