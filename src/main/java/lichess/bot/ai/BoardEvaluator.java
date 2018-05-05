package lichess.bot.ai;

import chesslib.Board;
import chesslib.Side;

public interface BoardEvaluator {
    double evaluate(Board board, Side mySide);
}
