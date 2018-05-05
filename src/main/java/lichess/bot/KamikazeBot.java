package lichess.bot;

import lichess.bot.ai.OpeningBook;
import lichess.bot.model.Event;

import java.io.IOException;

public class KamikazeBot extends LichessBot {
    private final OpeningBook openingBook = new OpeningBook();

    public KamikazeBot(String apiToken) throws IOException {
        super(apiToken);
    }

    public KamikazeBot(String apiToken, boolean registerBot) throws IOException {
        super(apiToken, registerBot);
    }

    @Override
    protected Engine newEngineInstance(ChatroomHandle chatroomHandle) {
        return new KamikazeEngine(chatroomHandle, openingBook);
    }

    @Override
    protected boolean acceptChallenge(Event.Challenge challenge) {
        return challenge.variant.key.equals("antichess");
    }
}
