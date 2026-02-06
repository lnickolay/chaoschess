import { useState } from "react";

import { sendRequest } from "../utils/sendRequest";

export default function BotConfigurator() {
  const [selectedBotColors, setSelectedBotColors] = useState(new Set(["WHITE", "BLACK"]));

  const handleCheckboxChange = (color) => {
    // Create a new Set instead of mutating the existing one to ensure React detects the state change (React uses
    // referential equality checks to determine if the state has been updated)
    const newBotColors = new Set(selectedBotColors);
    if (newBotColors.has(color)) {
      newBotColors.delete(color);
    } else {
      newBotColors.add(color);
    }
    setSelectedBotColors(newBotColors);

    const configDTO = { botColorNames: Array.from(newBotColors) };
    sendRequest("config/bot", "POST", configDTO).catch((error) =>
      console.error("Error while setting colors controlled by bot: " + error),
    );
  };

  return (
    <>
      <label className="bot-color-checkbox">
        <input
          type="checkbox"
          checked={selectedBotColors.has("WHITE")}
          onChange={() => handleCheckboxChange("WHITE")}
        />
        Bot controls WHITE
      </label>
      <label className="bot-color-checkbox">
        <input
          type="checkbox"
          checked={selectedBotColors.has("BLACK")}
          onChange={() => handleCheckboxChange("BLACK")}
        />
        Bot controls BLACK
      </label>
    </>
  );
}
