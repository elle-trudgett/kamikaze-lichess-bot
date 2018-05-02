package lichess.bot;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Constants;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import lichess.bot.ai.BoardEvaluator;
import lichess.bot.ai.MonteCarloTreeSearcher;
import lichess.bot.ai.SimpleSuicideBoardEvaluator;

public class KamikazeEngine implements Engine {
    private Board board = new Board();
    private BoardEvaluator boardEvaluator = new SimpleSuicideBoardEvaluator(true);
    private MonteCarloTreeSearcher mcts = new MonteCarloTreeSearcher(new Board(), boardEvaluator);
    private String initialFen;
    private String nextMove = null;
    private String movesPlayed = "";

    @Override
    public String onChatMessage(String username, String text) {
        if (username.equals("tgfcoder") || username.equals("KamikazeBot")) {
            // Admin controls
            if (text.equals("#resign")) {
                nextMove = "resign";
                return "Okay, I will resign on my turn.";
            } else if (text.startsWith("#move")) {
                nextMove = text.trim().toLowerCase().split(" ")[1];
                return "Okay, my next move will be " + nextMove + ".";
            }
        }
        return "Hello!";
    }

    @Override
    public void initializeBoardState(String initialFen) {
        if (initialFen.equals("startpos")) {
            board = new Board();
            this.initialFen = board.getFen();
            mcts = new MonteCarloTreeSearcher(board, boardEvaluator);
        } else {
            this.initialFen = initialFen;
            board.loadFromFen(initialFen);
            mcts = new MonteCarloTreeSearcher(board, boardEvaluator);
        }
    }

    @Override
    public void updateGameState(String moves, long wtime, long btime, long winc, long binc) {
        if (moves == null) {
            return;
        }

        if (!moves.startsWith(movesPlayed)) {
            System.out.println("Warning, moves are inconsistent");
            movesPlayed = "";
            board.loadFromFen(initialFen);
        }

        String movesToApply = moves.substring(movesPlayed.length()).trim();

        if (movesToApply.length() > 0) {
            for (String move : movesToApply.split(" ")) {
                Square from = Square.fromValue(move.substring(0, 2).toUpperCase());
                Square to = Square.fromValue(move.substring(2, 4).toUpperCase());
                Piece promotion = Piece.NONE;
                if (move.length() == 5) {
                    promotion = Piece.make(board.getSideToMove(), Constants.getPieceByNotation(move.substring(4, 5)).getPieceType());
                }
                board.doMove(new Move(from, to, promotion));
            }
        }

        mcts.updateState(board);

        System.out.println(board.toString());
        movesPlayed = moves;
    }

    @Override
    public String makeMove() {
        if (nextMove != null) {
            String moveToMake = nextMove;
            nextMove = null;
            return moveToMake;
        }

        Move move = mcts.findBestMove();
        return move == null ? null : move.toString();
    }
}
