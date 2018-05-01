package lichess.bot.ai;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import com.github.bhlangonijr.chesslib.move.MoveList;

import java.util.*;

public class MonteCarloTreeSearcher {
    private Board currentPosition;
    private final BoardEvaluator evaluator;

    public MonteCarloTreeSearcher(Board position, BoardEvaluator evaluator) {
        this.currentPosition = position;
        this.evaluator = evaluator;
    }

    public void updateState(Board board) {
        this.currentPosition = board;
    }

    public Move findBestMove() {
        // Enumerate all possible moves
        Collection<Move> moves = getAllPossibleMoves();

        // If there is only one possibility, return it (no need to evaluate)
        if (moves.size() == 1) {
            return moves.iterator().next();
        }

        // Create a board position representing the state after making the move
        Map<Move, Double> moveScores = new HashMap<>();
        for (Move move : moves) {
            Board nextPosition = currentPosition.clone();
            nextPosition.doMove(move);

            // Evaluate the board position
            double score = evaluator.evaluate(nextPosition, nextPosition.getSideToMove().flip());
            moveScores.put(move, score);
        }

        if (moveScores.isEmpty()) {
            // If there are no possibilities.. we can't return a move
            return null;
        } else {
            // Choose the move with the highest score
            return Collections.max(moveScores.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();
        }
    }

    public Collection<Move> getAllPossibleMoves() {
        MoveList pseudoLegalMoves = MoveGenerator.generatePseudoLegalMoves(currentPosition);
        List<Move> captureMoves = new ArrayList<>();
        List<Move> nonCaptureMoves = new ArrayList<>();

        for (Move pseudoLegalMove : pseudoLegalMoves) {
            if (pseudoLegalMove.isCapture(currentPosition)) {
                captureMoves.add(pseudoLegalMove);
            } else {
                nonCaptureMoves.add(pseudoLegalMove);
            }
        }

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        }

        return nonCaptureMoves;
    }
}
