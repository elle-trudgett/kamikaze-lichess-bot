package lichess.bot;

import chesslib.*;
import chesslib.move.Move;
import lichess.bot.ai.MonteCarloTreeSearch;

import java.time.Duration;

public class KamikazeEngine implements Engine {
    private Board board = new Board();
    private MonteCarloTreeSearch mcts = new MonteCarloTreeSearch(new Board());
    private String initialFen;
    private String nextMove = null;
    private String movesPlayed = "";
    private Side mySide = Side.WHITE;

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

        if (text.startsWith("#eval")) {
            double evaluation = mcts.evaluation();
            if (board.getSideToMove() != mySide) {
                evaluation = 1 - evaluation;
            }
            return "I believe my chance of winning is " + String.format("%d percent", Math.round(evaluation * 100));
        }

        return "Hello!";
    }

    @Override
    public void initializeBoardState(String initialFen, boolean white) {
        mySide = white ? Side.WHITE : Side.BLACK;

        if (initialFen.equals("startpos")) {
            board = new Board();
            this.initialFen = board.getFen();
            mcts = new MonteCarloTreeSearch(board);
        } else {
            this.initialFen = initialFen;
            board.loadFromFen(initialFen);
            mcts = new MonteCarloTreeSearch(board);
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
            mcts = new MonteCarloTreeSearch(board);
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
                Move moveMade = new Move(from, to, promotion);
                board.doMove(moveMade);
                mcts.applyMove(moveMade);
            }
        }

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

        Move move = mcts.findBestMove(Duration.ofMillis(500L));
        return move == null ? null : move.toString();
    }
}
