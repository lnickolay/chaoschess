package com.chaoschess.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameStateMessagingController {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameStateMessagingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushGameState(GameStateDTO gameStateDTO) {
        messagingTemplate.convertAndSend("/topic/game-state", gameStateDTO);
    }
}
