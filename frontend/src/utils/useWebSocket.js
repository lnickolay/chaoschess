import { useEffect, useState } from "react";

import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { sendRequest } from "./sendRequest";

// This has to match the values in WebSocketConfig.java in the backend
const SOCKET_URL = "http://localhost:8080/ws";
const GAME_STATE_TOPIC = "/topic/game-state";

export const useWebSocket = () => {
  const [gameState, setGameState] = useState(null);

  useEffect(() => {
    // Initialize STOMP client and set SockJS as the WebSocket factory
    const client = new Client({
      // Use SockJS because Spring Boot's config uses withSockJS()
      webSocketFactory: () => new SockJS(SOCKET_URL),

      // Callback after successful connection
      onConnect: () => {
        console.log("Connected to WebSocket. Subscribing to topic.");

        // Subscribe to topic: wait for GameStateDTO updates
        client.subscribe(GAME_STATE_TOPIC, (message) => {
          // message.body is a JSON string of the GameStateDTO
          const newGameState = JSON.parse(message.body);
          console.log("New GameStateDTO received:", newGameState);
          setGameState(newGameState);
        });
      },

      // Error handling
      onStompError: (frame) => {
        console.error("STOMP error:", frame);
      },
    });

    // Start connection
    client.activate();

    // Initial fetch via REST
    sendRequest("status")
      .then((initialGameState) => {
        if (initialGameState) {
          setGameState(initialGameState);
          console.log("Initial game state successfully loaded via REST.");
        }
      })
      .catch((error) => {
        console.error("Error while loading initial game state.");
      });

    // Cleanup function (called when the component unmounts or before re-execution)
    return () => {
      if (client) {
        client.deactivate();
      }
    };
    // Empty array ensures single execution on mount
  }, []);

  return gameState;
};
