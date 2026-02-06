package com.chaoschess.backend.core.service;

import com.chaoschess.backend.core.model.Direction;
import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.model.MovementModifier;
import com.chaoschess.backend.core.model.MovementRule;
import com.chaoschess.backend.core.model.PieceRole;
import com.chaoschess.backend.core.model.PieceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ConfigLoader {

    private static final String CONFIG_PATH = "/config/piece_types.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MOVEMENT_STRING_REGEX_PATTERN = Pattern.compile(
            "([a-z]*)\\((-?\\d+),(-?\\d+)\\)([+*|-]?)([fblr]*)(\\d+,|)(\\d+|n|)");

    public Map<String, PieceType> loadPieceTypes() {
        InputStream inputStream = PieceTypes.class.getResourceAsStream(CONFIG_PATH);
        if (inputStream == null) {
            throw new RuntimeException("Piece types JSON config file not found: " + CONFIG_PATH);
        }

        try {
            JsonNode rootNode = MAPPER.readTree(inputStream);
            JsonNode pieceTypesNode = rootNode.get("pieceTypes");

            Map<String, PieceType> pieceTypes = new HashMap<>();

            int id = 0;

            for (Map.Entry<String, JsonNode> entry : pieceTypesNode.properties()) {
                String name = entry.getKey();
                JsonNode config = entry.getValue();

                String symbol = config.get("symbol").asText();
                String movementStr = config.get("movement").asText();
                String roleStr = config.get("role").asText();

                Set<MovementRule> movementRules = createMovementRulesFromString(movementStr);
                PieceRole role = PieceRole.valueOf(roleStr);

                PieceType pieceType = new PieceType(id, name, symbol, movementRules, role);
                pieceTypes.put(name, pieceType);

                id++;
            }

            return pieceTypes;
        } catch (IOException e) {
            throw new RuntimeException("Piece types JSON config parsing error.", e);
        }
    }

    private static Set<MovementRule> createMovementRulesFromString(String movementStr) {
        Set<MovementRule> movementRules = new HashSet<>();

        String[] movementStrElements = movementStr.split("/");

        for (String movementStrElement : movementStrElements) {
            Matcher matcher = MOVEMENT_STRING_REGEX_PATTERN.matcher(movementStrElement);

            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid movement rule string format: " + movementStrElement);
            }

            String modifiersStr = matcher.group(1);
            String dxStr = matcher.group(2);
            String dyStr = matcher.group(3);
            String directionsStr = matcher.group(4);
            String directionRestrictionsStr = matcher.group(5);
            String minStepsStr = matcher.group(6);
            String maxStepsStr = matcher.group(7);

            Set<MovementModifier> modifiers = EnumSet.noneOf(MovementModifier.class);
            for (char modifierSymbol : modifiersStr.toCharArray()) {
                modifiers.add(MovementModifier.getBySymbol(modifierSymbol));
            }

            int dx = Integer.parseInt(dxStr);
            int dy = Integer.parseInt(dyStr);

            int minSteps = 1;
            if (!minStepsStr.isEmpty()) {
                minSteps = Integer.parseInt(minStepsStr.replace(",", ""));
            }
            int maxSteps = 1;
            if ("n".equals(maxStepsStr)) {
                maxSteps = Integer.MAX_VALUE;
            } else if (!maxStepsStr.isEmpty()) {
                maxSteps = Integer.parseInt(maxStepsStr);
            }

            Set<Direction> directions = new HashSet<>();
            int maxAbs = Math.max(Math.abs(dx), Math.abs(dy));
            switch (directionsStr) {
                case "":
                    directions.add(new Direction(dx, dy));
                    break;
                case "|":
                    directions.add(new Direction(dx, dy));
                    directions.add(new Direction(-dx, dy));
                    break;
                case "-":
                    directions.add(new Direction(dx, dy));
                    directions.add(new Direction(dx, -dy));
                    break;
                case "+":
                    if ((dx != 0) && (dy != 0)) {
                        directions.add(new Direction(dx, dy));
                        directions.add(new Direction(-dx, dy));
                        directions.add(new Direction(dx, -dy));
                        directions.add(new Direction(-dx, -dy));
                    } else {
                        directions.add(new Direction(maxAbs, 0));
                        directions.add(new Direction(-maxAbs, 0));
                        directions.add(new Direction(0, maxAbs));
                        directions.add(new Direction(0, -maxAbs));
                    }
                    break;
                case "*":
                    if ((dx != 0) && (dy != 0) && (Math.abs(dx) != Math.abs(dy))) {
                        directions.add(new Direction(dx, dy));
                        directions.add(new Direction(-dx, dy));
                        directions.add(new Direction(dx, -dy));
                        directions.add(new Direction(-dx, -dy));
                        directions.add(new Direction(dy, dx));
                        directions.add(new Direction(-dy, dx));
                        directions.add(new Direction(dy, -dx));
                        directions.add(new Direction(-dy, -dx));
                    } else {
                        directions.add(new Direction(maxAbs, 0));
                        directions.add(new Direction(-maxAbs, 0));
                        directions.add(new Direction(0, maxAbs));
                        directions.add(new Direction(0, -maxAbs));
                        directions.add(new Direction(maxAbs, maxAbs));
                        directions.add(new Direction(-maxAbs, maxAbs));
                        directions.add(new Direction(maxAbs, -maxAbs));
                        directions.add(new Direction(-maxAbs, -maxAbs));
                    }
            }
            directions.removeIf(direction ->
                    (directionRestrictionsStr.contains("f") && direction.y() < 0) ||
                    (directionRestrictionsStr.contains("b") && direction.y() > 0) ||
                    (directionRestrictionsStr.contains("l") && direction.x() > 0) ||
                    (directionRestrictionsStr.contains("r") && direction.x() < 0)
            );
            for (Direction direction : directions) {
                movementRules.add(new MovementRule(direction.x(), direction.y(), minSteps, maxSteps, modifiers));
            }
        }
        return movementRules;
    }
}
