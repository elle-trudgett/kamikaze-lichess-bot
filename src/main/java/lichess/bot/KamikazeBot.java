package lichess.bot;

import lichess.bot.model.Event;

import java.io.IOException;

public class KamikazeBot extends LichessBot {
    public KamikazeBot(String apiToken) throws IOException {
        super(apiToken);
    }

    public KamikazeBot(String apiToken, boolean registerBot) throws IOException {
        super(apiToken, registerBot);
    }

    @Override
    protected Engine newEngineInstance(ChatroomHandle chatroomHandle) {
        return new KamikazeEngine(chatroomHandle);
    }

    @Override
    protected boolean acceptChallenge(Event.Challenge challenge) {
        return challenge.variant.key.equals("antichess");
    }
}
