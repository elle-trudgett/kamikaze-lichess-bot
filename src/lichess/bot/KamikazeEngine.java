package lichess.bot;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class KamikazeEngine implements Engine {
    private Board board = new Board();
    private String initialFen;
    private String nextMove = null;

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
        } else {
            this.initialFen = initialFen;
            board.loadFromFen(initialFen);
        }
    }

    @Override
    public void updateGameState(String moves, long wtime, long btime, long winc, long binc) {
        board.loadFromFen(initialFen);
        if (moves != null && moves.length() > 0) {
            for (String move : moves.split(" ")) {
                Square from = Square.fromValue(move.substring(0, 2).toUpperCase());
                Square to = Square.fromValue(move.substring(2, 4).toUpperCase());
                board.doMove(new Move(from, to));
            }
        }
        System.out.println(board.toString());
    }

    @Override
    public String makeMove() {
        if (nextMove != null) {
            String moveToMake = nextMove;
            nextMove = null;
            return moveToMake;
        }
        return "d5e4";
    }
}
