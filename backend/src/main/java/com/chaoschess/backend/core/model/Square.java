package com.chaoschess.backend.core.model;

public record Square(int x, int y) {

    public static long instanceCounter = 0;

    public Square {
        instanceCounter++;
    }
}
