package lichess.bot.ai;

import com.github.bhlangonijr.chesslib.*;

import java.util.List;

/**
 * Evaluates board position based on material only.
 */
public class SimpleSuicideBoardEvaluator implements BoardEvaluator {
    @Override
    public double evaluate(Board board, Side mySide) {
        double myMaterialTotal = 0.01;
        double theirMaterialTotal = 0.01;

        for (PieceType pieceType : PieceType.values()) {
            if (pieceType.equals(PieceType.NONE)) {
                continue;
            }

            List<Square> myPieces = board.getPieceLocation(Piece.make(mySide, pieceType));
            List<Square> theirPieces = board.getPieceLocation(Piece.make(mySide.flip(), pieceType));
            myMaterialTotal += valueOf(pieceType) * myPieces.size();
            theirMaterialTotal += valueOf(pieceType) * theirPieces.size();
        }

        return theirMaterialTotal / myMaterialTotal;
    }

    private double valueOf(PieceType pieceType) {
        if (pieceType == null) {
            // This is a hack, but we know if the target is missing, it's en passant. It's the only capture where you
            // don't end on where the enemy piece is.
            pieceType = PieceType.PAWN;
        }

        switch (pieceType) {
            case KING:
                return 1.1;
            case PAWN:
                return 1;
            case KNIGHT:
                return 2;
            case BISHOP:
                return 6;
            case ROOK:
                return 5;
            case QUEEN:
                return 4;
        }

        return 0;
    }
}
