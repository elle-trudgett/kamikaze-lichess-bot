import lichess.bot.KamikazeBot;
import lichess.bot.LichessBot;

public class ExampleRunner {
    public static void main(String[] args) throws Exception {
        LichessBot myBot = new KamikazeBot("api-token-here");
        myBot.listen();
    }
}
