package com.chaoschess.backend.api;

public record MoveRequestDTO (int fromSquareIndex, int toSquareIndex, String promoPieceName) {}
