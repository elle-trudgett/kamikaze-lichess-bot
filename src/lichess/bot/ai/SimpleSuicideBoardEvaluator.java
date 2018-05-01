package lichess.bot.ai;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.Collection;
import java.util.List;

public class SimpleSuicideBoardEvaluator implements BoardEvaluator {
    public final boolean useMCTS;

    public SimpleSuicideBoardEvaluator(boolean useMCTS) {
        this.useMCTS = useMCTS;
    }

    @Override
    public double evaluate(Board board, Side mySide) {
        double strength = 0;

        for (PieceType pieceType : PieceType.values()) {
            if (pieceType.equals(PieceType.NONE)) {
                continue;
            }

            List<Square> myPieces = board.getPieceLocation(Piece.make(mySide, pieceType));
            List<Square> theirPieces = board.getPieceLocation(Piece.make(mySide.flip(), pieceType));

            strength += valueOf(pieceType) * (myPieces.size() - theirPieces.size());
        }

        if (useMCTS) {
            MonteCarloTreeSearcher mcts = new MonteCarloTreeSearcher(board, new SimpleSuicideBoardEvaluator(false));
            Collection<Move> allPossibleMoves = mcts.getAllPossibleMoves();
            double guaranteedMaterialLoss = -1;
            for (Move possibleResponse : allPossibleMoves) {
                if (possibleResponse.isCapture(board)) {
                    if (guaranteedMaterialLoss < 0) {
                        guaranteedMaterialLoss = valueOf(board.getPiece(possibleResponse.getTo()).getPieceType());
                    } else {
                        guaranteedMaterialLoss = Math.min(guaranteedMaterialLoss, valueOf(board.getPiece(possibleResponse.getTo()).getPieceType()));
                    }
                }
            }

            if (guaranteedMaterialLoss > 0) {
                strength += guaranteedMaterialLoss;
            }
        }

        return strength;
    }

    private double valueOf(PieceType pieceType) {
        switch (pieceType) {
            case KING:
                return 0;
            case PAWN:
                return 1;
            case KNIGHT:
                return 3;
            case BISHOP:
                return 3;
            case ROOK:
                return 5;
            case QUEEN:
                return 9;
        }

        return 0;
    }
}
