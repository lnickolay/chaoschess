package com.chaoschess.backend.core.ai;

import com.chaoschess.backend.core.model.Color;

import java.util.Map;

public record PositionalValues(Map<Color, Integer> total) {}
