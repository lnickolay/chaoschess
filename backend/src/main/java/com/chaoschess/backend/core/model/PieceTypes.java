package com.chaoschess.backend.core.model;

import org.springframework.stereotype.Component;

import java.util.Map;

// TODO: work internally with an array using pieceIDs as indices and provide an O(1) function for getPieceTypeByName()
//  using a dedicated map
@Component
public record PieceTypes(Map<String, PieceType> pieceTypesMap) {

    public PieceTypes {
        pieceTypesMap = Map.copyOf(pieceTypesMap);
    }

    public PieceType getPieceTypeByName(String name) {
        if (name == null) {
            return null;
        }
        return this.pieceTypesMap.get(name);
    }

    public PieceType getPieceTypeBySymbol(String symbol) {
        return this.pieceTypesMap.values().stream()
                .filter(pieceType -> pieceType.symbol().equals(symbol))
                .findFirst()
                .orElse(null);
    }
}
