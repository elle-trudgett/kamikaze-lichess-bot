package lichess.bot.ai;

import chesslib.Board;
import chesslib.Piece;
import chesslib.PieceType;
import chesslib.Side;
import chesslib.move.Move;
import chesslib.move.MoveGenerator;
import chesslib.move.MoveList;
import javafx.util.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Implements https://en.wikipedia.org/wiki/Monte_Carlo_tree_search#Pure_Monte_Carlo_game_search.
 *
 * For each legal move, play the game out to conclusion with random moves N times. The legal move that had the highest
 * winrate is chosen. No board evaluator is necessary. No tree data structure is necessary either- there is no
 * backpropagation necessary because only the immediately available moves are candidates for being "played out".
 */
public class PureMonteCarloGameSearch {
    private static final long MAX_PLAYOUTS = 500;
    private static final int MAX_MOVE_DEPTH = 100;
    private Board currentPosition;

    public PureMonteCarloGameSearch(Board position) {
        this.currentPosition = position;
    }

    public void updateState(Board board) {
        this.currentPosition = board;
    }

    public Move findBestMove() {
        // Enumerate all possible moves
        Collection<Move> moves = getAllPossibleMoves(currentPosition);

        // If there is only one possibility, return it (no need to evaluate)
        if (moves.size() == 1) {
            return moves.iterator().next();
        } else if (moves.size() == 0) {
            return null; // There are no possible moves .. (game over?)
        }

        // Iterate over each legal move round-robin style and make a playout.
        // We can stop whenever we feel like it and then choose the best option.

        Map<Move, PlayoutResult> moveScores = new HashMap<>();
        boolean searching = true;
        long playouts = 0;
        Instant before = Instant.now();
        while (searching) {
            for (Move move : moves) {
                Board nextPosition = currentPosition.clone();
                nextPosition.doMove(move);

                Pair<Side, Integer> winnerAndMovesTried = playout(nextPosition);

                if (winnerAndMovesTried != null && winnerAndMovesTried.getKey() == currentPosition.getSideToMove() && winnerAndMovesTried.getValue() == 1) {
                    // I can win next move. Do it!
                    moveScores = new HashMap<>();
                    PlayoutResult result = new PlayoutResult();
                    result.wins = 1;
                    result.games = 1;
                    moveScores.put(move, result);
                    searching = false;
                    break;
                }

                if (!moveScores.containsKey(move)) {
                    moveScores.put(move, new PlayoutResult());
                }

                moveScores.get(move).games++;
                if (winnerAndMovesTried != null && winnerAndMovesTried.getKey() == currentPosition.getSideToMove()) {
                    moveScores.get(move).wins += 1;
                } else if (winnerAndMovesTried == null) { // draw
                    moveScores.get(move).wins += 0.5;
                }
            }

            playouts++;

            if (playouts >= MAX_PLAYOUTS) {
                searching = false;
            }
        }
        Instant after = Instant.now();

        System.out.println("Ran " + playouts + " playouts of " + moves.size() + " moves in " + Duration.between(before, after).toMillis() + " ms");

        // Choose the move with the highest score
        List<Map.Entry<Move, PlayoutResult>> moveEntries = new ArrayList<>(moveScores.entrySet());
        Collections.shuffle(moveEntries);
        return Collections.max(moveEntries, Comparator.comparingDouble(x -> x.getValue().wins / x.getValue().games)).getKey();
    }

    private Pair<Side, Integer> playout(Board state) {
        int movesTested = 0;
        while (gameInProgress(state)) {
            makeRandomMove(state);
            if (gameIsDraw(state)) {
                return null;
            }
            movesTested++;
            if (movesTested > MAX_MOVE_DEPTH) {
                return null; // Assume draw if it goes this long.
            }
        }

        return new Pair<>(state.getSideToMove(), movesTested);
    }

    private boolean gameIsDraw(Board state) {
        List<Move> allPossibleMoves = getAllPossibleMoves(state);
        if (allPossibleMoves.size() == 0) {
            return false; // Someone can't move, it's game over
        }

        for (Move possibleMove : allPossibleMoves) {
            Piece pieceToMove = state.getPiece(possibleMove.getFrom());
            if (pieceToMove.getPieceType() != PieceType.BISHOP) {
                return false; // As long as you can move a non-bishop piece, it's not a draw (yet)
            }
        }

        // Now we know we only have bishop moves. It's only a draw if there's exactly 1 bishop on each side
        if (state.getPieceLocation(Piece.WHITE_BISHOP).size() != 1) {
            return false;
        }
        if (state.getPieceLocation(Piece.BLACK_BISHOP).size() != 1) {
            return false;
        }

        // And they're on different colored squares.
        return state.getPieceLocation(Piece.WHITE_BISHOP).get(0).isLightSquare() != state.getPieceLocation(Piece.BLACK_BISHOP).get(0).isLightSquare();
    }

    private void makeRandomMove(Board state) {
        List<Move> allPossibleMoves = getAllPossibleMoves(state);
        Collections.shuffle(allPossibleMoves);
        state.doMove(allPossibleMoves.get(0));
    }

    private boolean gameInProgress(Board state) {
        return getAllPossibleMoves(state).size() > 0;
    }

    private static class PlayoutResult {
        public double wins = 0;
        public long games = 0;
    }

    public List<Move> getAllPossibleMoves(Board state) {
        MoveList pseudoLegalMoves = MoveGenerator.generatePseudoLegalMoves(state);
        List<Move> captureMoves = new ArrayList<>();
        List<Move> nonCaptureMoves = new ArrayList<>();

        for (Move pseudoLegalMove : pseudoLegalMoves) {
            if (pseudoLegalMove.isCapture(state)) {
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
