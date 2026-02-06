package com.chaoschess.backend;

import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.service.ConfigLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class GameConfig {

    private final ConfigLoader configLoader;

    public GameConfig(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Bean
    public PieceTypes pieceTypes() {
        return new PieceTypes(configLoader.loadPieceTypes());
    }

    @Bean("aiSearchExecutor")
    @Scope("prototype")
    public ExecutorService aiSearchExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
