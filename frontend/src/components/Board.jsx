import { useEffect, useMemo, useState } from "react";

import PromoModal from "./PromoModal";
import Square from "./Square";

export default function Board({
  gameState,
  executeMove,
  handlePromoMove,
  isPromoModalVisible,
  handlePromoOptionClick,
}) {
  const [hoveredSquareIndex, setHoveredSquareIndex] = useState(null);
  const [selectedSquareIndex, setSelectedSquareIndex] = useState(null);
  const [pseudolegalMoveOptionsMap, setPseudolegalMoveOptionsMap] = useState({});

  useEffect(() => {
    // Always clear the selected square when the game state changes
    setSelectedSquareIndex(null);
  }, [gameState]);

  const { width, height, colorToMove, pieceGrid, pseudolegalMoveOptions, botColors } = gameState;

  useEffect(() => {
    const newPseudolegalMoveOptionsMap = pseudolegalMoveOptions.reduce((acc, move) => {
      if (!acc[move.fromSquareIndex]) {
        acc[move.fromSquareIndex] = [];
      }
      acc[move.fromSquareIndex].push(move);
      return acc;
    }, {});
    setPseudolegalMoveOptionsMap(newPseudolegalMoveOptionsMap);
  }, [gameState]);

  const staticSquareData = useMemo(() => {
    if (!width || !height) {
      return [];
    }
    const data = [];
    for (let index = 0; index < width * height; index++) {
      const x = index % width;
      const y = height - 1 - Math.floor(index / width);
      const color = (x + y) % 2 === 0 ? "dark" : "light";
      data.push({ x, y, color });
    }
    return data;
  }, [width, height]);

  const selectedMoveOptions = pseudolegalMoveOptionsMap[selectedSquareIndex] || [];
  const hoveredMoveOptions = pseudolegalMoveOptionsMap[hoveredSquareIndex] || [];

  const selectedLegalMoveTargets = new Set(
    selectedMoveOptions.filter((moveOption) => moveOption.isLegal).map((moveOption) => moveOption.toSquareIndex),
  );
  const hoveredMoveTargets = new Set(hoveredMoveOptions.map((moveOption) => moveOption.toSquareIndex));

  const handleMouseSquareEnter = (enteredSquareIndex) => {
    setHoveredSquareIndex(enteredSquareIndex);
  };
  const handleMouseBoardLeave = () => setHoveredSquareIndex(null);

  const handleMouseSquareClick = (clickedSquareIndex) => {
    if (botColors.includes(colorToMove)) {
      console.log("Click on square ignored because it is the bot's turn.");
      return;
    }

    if (selectedSquareIndex === null) {
      if (pieceGrid[clickedSquareIndex] !== null && pieceGrid[clickedSquareIndex].color === colorToMove) {
        setSelectedSquareIndex(clickedSquareIndex);
      }
    } else {
      if (selectedLegalMoveTargets.has(clickedSquareIndex)) {
        const executedMoveOption = selectedMoveOptions.find(
          (moveOption) => moveOption.toSquareIndex === clickedSquareIndex,
        );

        if (!executedMoveOption.isPromo) {
          executeMove(selectedSquareIndex, clickedSquareIndex, null);
          setSelectedSquareIndex(null);
        } else {
          handlePromoMove(selectedSquareIndex, clickedSquareIndex);
        }
      } else if (clickedSquareIndex === selectedSquareIndex) {
        setSelectedSquareIndex(null);
      } else if (pieceGrid[clickedSquareIndex] !== null && pieceGrid[clickedSquareIndex].color === colorToMove) {
        setSelectedSquareIndex(clickedSquareIndex);
      } else {
        setSelectedSquareIndex(null);
      }
    }
  };

  const renderedSquares = pieceGrid.map((_, gridIndex) => {
    // Don't flip board when it's black's turn
    const index = gridIndex;

    // // Flip board when it's black's turn
    // const index =
    //   colorToMove === "WHITE" ? gridIndex : pieceGrid.length - 1 - gridIndex;

    const color = staticSquareData[index].color;
    const isSelected = index === selectedSquareIndex;
    const isHovered = index === hoveredSquareIndex && pieceGrid[index] !== null;
    const isHoveredMoveTarget = hoveredMoveTargets.has(index);
    const isSelectedLegalMoveTarget = selectedLegalMoveTargets.has(index);

    return (
      <Square
        key={index}
        index={index}
        color={color}
        pieceData={pieceGrid[index]}
        onMouseEnter={handleMouseSquareEnter}
        onMouseClick={handleMouseSquareClick}
        isSelected={isSelected}
        isHovered={isHovered}
        isHoveredMoveTarget={isHoveredMoveTarget}
        isSelectedLegalMoveTarget={isSelectedLegalMoveTarget}
      />
    );
  });

  return (
    <div className="board-container">
      <div
        className="board"
        onMouseLeave={handleMouseBoardLeave}
        style={{ "--grid-width": width, "--grid-height": height }}
      >
        {renderedSquares}
        {isPromoModalVisible && <PromoModal gameState={gameState} onPromoOptionClick={handlePromoOptionClick} />}
      </div>
    </div>
  );
}
