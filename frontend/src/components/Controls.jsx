import { MdLightMode, MdDarkMode } from "react-icons/md";

import BotConfigurator from "./BotConfigurator";

export default function Controls({ handleNewGameClick, chaosLevel, setChaosLevel, isDark, setIsDark }) {
  return (
    <div className="controls-container">
      <button onClick={handleNewGameClick}>Start New Game</button>
      <hr />
      <div className="chaos-slider-container">
        <input
          className="chaos-slider"
          type="range"
          min="0"
          max="2"
          step="1"
          value={chaosLevel}
          onChange={(e) => setChaosLevel(parseInt(e.target.value, 10))}
        />
        <div className="chaos-slider-labels">
          <span>Conformist Chess</span>
          <span>ChaosChess</span>
        </div>
      </div>

      <hr />
      <BotConfigurator />
      <hr />
      <button className="theme-toggle-btn" onClick={() => setIsDark(!isDark)}>
        {isDark ? <MdLightMode /> : <MdDarkMode />}
      </button>
    </div>
  );
}
