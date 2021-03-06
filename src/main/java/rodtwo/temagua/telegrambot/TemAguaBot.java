package rodtwo.temagua.telegrambot;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import rodtwo.temagua.services.bean.PeriodoRodizio;
import rodtwo.temagua.services.bean.PropertySource;
import rodtwo.temagua.util.MyLog;
import rodtwo.temagua.util.RodizioListUtil;
import rodtwo.temagua.util.MyLog.Group;

public class TemAguaBot extends TelegramLongPollingBot {

	public static void init(String botToken, String host, String endpoint) throws TelegramApiException {
        // Instantiate Telegram Bots API
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        // Register bot
        try {
        	MyLog.getInstance().add(Group.IMPORTANT, "Telegram bot will query: "+ host + endpoint);
            botsApi.registerBot(new TemAguaBot(botToken, host, endpoint));
        } catch (TelegramApiException e) {
        	MyLog.getInstance().add(Group.ERROR, e.toString());
        	e.printStackTrace();	// TODO pass stack to MyLog
        }
	}
	
	
	private String botToken;
	private String host;
	private String endpoint;
	
	public TemAguaBot(String botToken, String host, String endpoint) {
		this.botToken = botToken;
		this.host = host;
		this.endpoint = endpoint;
	}
	
	@Override
	public void onUpdateReceived(Update update) {
		MyLog.getInstance().add(Group.IMPORTANT, "Update received");
		if (update.hasMessage() && update.getMessage().hasText()) {
			MyLog.getInstance().add(Group.IMPORTANT, "\t" + update.getMessage().getText());
			// call jetty local API
			String rawUpdateText = update.getMessage().getText();
			String[] updateMsgs = rawUpdateText.split(" ");
			String msg = updateMsgs[0].contains(getBotUsername()) 
							? rawUpdateText.replace("@"+getBotUsername(), "") 
							: rawUpdateText; 
			msg = msg.replace(" ", "-");
			
			Client client = ClientBuilder.newClient();
			WebTarget webTarget 
			  = client.target(this.host + this.endpoint + "/json/" 
					  			+ msg );	// e.g. "2-saic"
			
			Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
			
			Response response = invocationBuilder.get(Response.class);
			List<PeriodoRodizio> rods = response.readEntity(new GenericType<List<PeriodoRodizio>>() {});
			
	        // Set variables
	        //String messageText = update.getMessage().getText();
	        long chatId = update.getMessage().getChatId();

	        SendMessage message = new SendMessage();
	        message.setChatId(""+chatId);
	        message.setText(RodizioListUtil.convertRodizioListToString(rods, "\n"));
	        
	        try {
	            execute(message); // Sending our message object to user
	        } catch (TelegramApiException e) {
	            e.printStackTrace();
	        }
	    }
	}

	@Override
	public String getBotUsername() {
		return "TemAguaRmcBot";
	}

	@Override
	public String getBotToken() {
		return botToken;
	}

}
