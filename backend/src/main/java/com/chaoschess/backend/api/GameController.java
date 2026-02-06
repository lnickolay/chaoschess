package com.chaoschess.backend.api;

import com.chaoschess.backend.core.service.GameManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:5173")
public class GameController {

    private final GameManager gameManager;

    public GameController(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @GetMapping("/status")
    public GameStateDTO getGameStatus() {
        return gameManager.getGameStateDTO();
    }

    @PostMapping("/new")
    public ResponseEntity<Void> startNewGame(@RequestBody GameSettingsDTO gameSettingsDTO) {
        gameManager.startNewGame(gameSettingsDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/move")
    public ResponseEntity<Void> makeMove(@RequestBody MoveRequestDTO moveRequestDTO) {
        gameManager.tryAndExecutePlayerMoveRequest(moveRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/config/bot")
    public ResponseEntity<Void> setBotConfig(@RequestBody BotConfigRequestDTO botConfigRequestDTO) {
        gameManager.setBotConfig(botConfigRequestDTO);
        return ResponseEntity.noContent().build();
    }
}
