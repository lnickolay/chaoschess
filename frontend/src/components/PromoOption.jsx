import { getPieceImgPath } from "../utils/getPieceImgPath";

export default function PromoOption({ pieceData, onMouseClick }) {
  return (
    <div className="promo-option" onClick={() => onMouseClick(pieceData.pieceName)}>
      {<img src={getPieceImgPath(pieceData)} className="promo-option-img"></img>}
    </div>
  );
}
