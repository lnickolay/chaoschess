package com.chaoschess.backend.core.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum MovementModifier {
    ONLY_CAPTURES       ('x'),
    ONLY_NON_CAPTURES   ('m'),
    ONLY_UNMOVED        ('i'),
    CASTLING            ('c'),
    ENABLES_EN_PASSANT  ('e'),
    CAPTURES_EN_PASSANT ('p');

    private final char symbol;

    // Static map for fast lookup (O(1)) by symbol (initialized once during enum loading)
    private static final Map<Character, MovementModifier> SYMBOL_TO_MODIFIER = Arrays.stream(MovementModifier.values())
            .collect(Collectors.toMap(modifier -> modifier.symbol, Function.identity()));

    MovementModifier(char symbol) {
        this.symbol = symbol;
    }

    public static MovementModifier getBySymbol(char symbol) {
        return SYMBOL_TO_MODIFIER.get(symbol);
    }
}

