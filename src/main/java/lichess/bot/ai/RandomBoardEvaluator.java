package lichess.bot.ai;

import chesslib.Board;
import chesslib.Side;

import java.security.SecureRandom;
import java.util.Random;

public class RandomBoardEvaluator implements BoardEvaluator {
    private final Random random = new SecureRandom();

    @Override
    public double evaluate(Board board, Side mySide) {
        return random.nextDouble();
    }
}
