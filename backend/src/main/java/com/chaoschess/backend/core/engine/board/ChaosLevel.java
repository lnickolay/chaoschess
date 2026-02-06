package com.chaoschess.backend.core.engine.board;

public enum ChaosLevel {
    DULL    (0),
    ODD     (1),
    WEIRD   (2);
//    UNHINGED(3); // TODO

    private final int value;

    ChaosLevel(int value) {
        this.value = value;
    }

    public static ChaosLevel getByInt(int i) {
        for (ChaosLevel level : ChaosLevel.values()) {
            if (level.value == i) return level;
        }
        return DULL;
    }
}
