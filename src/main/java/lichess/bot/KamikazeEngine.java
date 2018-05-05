package lichess.bot;

import chesslib.*;
import chesslib.move.Move;
import lichess.bot.ai.MonteCarloTreeSearch;
import lichess.bot.ai.OpeningBook;
import lichess.bot.chat.ChatEngine;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

public class KamikazeEngine implements Engine {
    private final ChatroomHandle chatroomHandle;
    private final OpeningBook openingBook;
    private OpeningBook.BookNode openingBookNode;
    private Board board = new Board();
    private ChatEngine chatEngine = new ChatEngine();
    private MonteCarloTreeSearch mcts = new MonteCarloTreeSearch(new Board());
    private String initialFen;
    private String nextMove = null;
    private String movesPlayed = "";
    private Side mySide = Side.WHITE;
    private boolean ggSent = false;

    public KamikazeEngine(ChatroomHandle chatroomHandle, OpeningBook openingBook) {
        this.chatroomHandle = chatroomHandle;
        this.openingBook = openingBook;
        this.openingBookNode = openingBook.getRootNode();
    }

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

        return chatEngine.getReply(username, text);
    }

    @Override
    public void initializeBoardState(String initialFen, boolean white) {
        mySide = white ? Side.WHITE : Side.BLACK;

        if (initialFen.equals("startpos")) {
            board = new Board();
            this.initialFen = board.getFen();
            mcts = new MonteCarloTreeSearch(board);
            openingBookNode = openingBook.getRootNode();
        } else {
            this.initialFen = initialFen;
            board.loadFromFen(initialFen);
            mcts = new MonteCarloTreeSearch(board);
            openingBookNode = null;
        }

        try {
            chatroomHandle.sendMessage(chatEngine.getHello());
        } catch (IOException e) {
            e.printStackTrace();
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
                openingBookNode = openingBook.applyMove(openingBookNode, moveMade);
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

        Optional<Move> bestMove = openingBook.findBestMove(openingBookNode);
        if (bestMove.isPresent()) {
            System.out.println("Using opening book move " + bestMove.get());
            return bestMove.get().toString();
        }

        Move move = mcts.findBestMove(Duration.ofMillis(5000L), 25000);

        if (mcts.isGameGoingToEndSoon() && !ggSent) {
            try {
                chatroomHandle.sendMessage(chatEngine.getGG());
                ggSent = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return move == null ? null : move.toString();
    }
}
