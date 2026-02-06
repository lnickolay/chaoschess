import { useEffect, useState } from "react";

import Board from "./components/Board";
import Controls from "./components/Controls";

import { sendRequest } from "./utils/sendRequest";
import { useWebSocket } from "./utils/useWebSocket";

import "./App.css";

export default function App() {
  const gameState = useWebSocket();

  const [chaosLevel, setChaosLevel] = useState(0);

  const [isDark, setIsDark] = useState(() => {
    return localStorage.getItem("theme") === "dark";
  });

  const [isPromoModalVisible, setIsPromoModalVisible] = useState(false);
  const [pendingPromoMove, setPendingPromoMove] = useState(null);

  useEffect(() => {
    const theme = isDark ? "dark" : "light";
    document.body.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
  }, [isDark]);

  const handleNewGameClick = () => {
    sendRequest("new", "POST", { chaosLevel }).catch((error) =>
      console.error("Error while starting new game: " + error),
    );
  };

  const executeMove = (fromSquareIndex, toSquareIndex, promoPieceName) => {
    const moveRequest = { fromSquareIndex, toSquareIndex, promoPieceName };
    return sendRequest("move", "POST", moveRequest).catch((error) => {
      console.error("Error during move: " + error);
    });
  };

  const handlePromoMove = (fromSquareIndex, toSquareIndex) => {
    setPendingPromoMove({ fromSquareIndex, toSquareIndex });
    setIsPromoModalVisible(true);
  };

  const handlePromoOptionClick = (promoPieceName) => {
    if (!pendingPromoMove) return;
    executeMove(pendingPromoMove.fromSquareIndex, pendingPromoMove.toSquareIndex, promoPieceName);
    setPendingPromoMove(null);
    setIsPromoModalVisible(false);
  };

  if (!gameState) {
    return <div>{"Error while loading."}</div>;
  } else {
    return (
      <div className="main-container">
        <Board
          gameState={gameState}
          executeMove={executeMove}
          handlePromoMove={handlePromoMove}
          isPromoModalVisible={isPromoModalVisible}
          handlePromoOptionClick={handlePromoOptionClick}
        />
        <Controls
          handleNewGameClick={handleNewGameClick}
          chaosLevel={chaosLevel}
          setChaosLevel={setChaosLevel}
          isDark={isDark}
          setIsDark={setIsDark}
        />
      </div>
    );
  }
}
