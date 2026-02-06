package com.chaoschess.backend.core.ai;

import com.chaoschess.backend.core.model.Color;

import java.util.Map;

public record MaterialValues(Map<Color, Integer> total, Map<Color, Integer> excludingPawns) {}
