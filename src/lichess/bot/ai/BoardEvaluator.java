package lichess.bot.ai;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

public interface BoardEvaluator {
    double evaluate(Board board, Side mySide);
}
