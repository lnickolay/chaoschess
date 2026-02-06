package com.chaoschess.backend.core.model;

public enum Color {
    WHITE,
    BLACK;

    public Color getOpponent() {
        return (this == WHITE) ? BLACK : WHITE;
    }
}
