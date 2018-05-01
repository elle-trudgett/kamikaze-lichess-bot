package lichess.bot;

public class KamikazeEngine implements Engine {
    @Override
    public String onChatMessage(String username, String text) {
        return null;
    }

    @Override
    public void initializeBoardState(String initialFen) {

    }

    @Override
    public void updateGameState(String moves, long wtime, long btime, long winc, long binc) {

    }

    @Override
    public String makeMove() {
        return "resign";
    }
}
