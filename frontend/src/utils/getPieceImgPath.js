export const getPieceImgPath = (pieceData) => {
  const { pieceName, color } = pieceData;
  const pieceTypeNormalized = pieceName.toLowerCase();
  const colorInitial = color.toLowerCase().charAt(0);
  return `/assets/images/pieces/${pieceTypeNormalized}_${colorInitial}.svg`;
};
