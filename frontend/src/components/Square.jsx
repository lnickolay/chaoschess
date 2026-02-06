import { getPieceImgPath } from "../utils/getPieceImgPath";

export default function Square({
  index,
  color,
  pieceData,
  onMouseEnter,
  onMouseClick,
  isSelected,
  isHovered,
  isHoveredMoveTarget,
  isSelectedLegalMoveTarget,
}) {
  const pieceImgPath = pieceData ? getPieceImgPath(pieceData) : null;

  let className = `square ${color}`;
  if (isSelected) className += " selected";
  if (isHovered) className += " hovered";
  if (isHoveredMoveTarget) className += " hovered-move-target";
  if (isSelectedLegalMoveTarget) className += " selected-legal-move-target";

  return (
    <div className={className} onMouseEnter={() => onMouseEnter(index)} onClick={() => onMouseClick(index)}>
      {pieceImgPath && <img src={pieceImgPath} className="piece-img"></img>}
      {/* {index} */}
    </div>
  );
}
