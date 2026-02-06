import PromoOption from "./PromoOption";

export default function PromoModal({ gameState, onPromoOptionClick }) {
  return (
    <div className="promo-overlay">
      <div className="promo-options-container">
        {gameState.promoOptionNames.map((promoOptionName) => (
          <PromoOption
            key={promoOptionName}
            pieceData={{
              pieceName: promoOptionName,
              color: gameState.colorToMove,
            }}
            onMouseClick={onPromoOptionClick}
          />
        ))}
      </div>
    </div>
  );
}
