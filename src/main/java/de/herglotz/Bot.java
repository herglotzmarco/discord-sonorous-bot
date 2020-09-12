package de.herglotz;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot extends ListenerAdapter {

	public static void main(String[] args) throws LoginException {
		if (args.length == 0) {
			System.out.println("First arg must be login token for the bot!");
			System.exit(1);
		}

		// We only need 2 intents in this bot. We only respond to messages in guilds and
		// private channels.
		// All other events will be disabled.
		JDABuilder.createLight(args[0], GatewayIntent.GUILD_MESSAGES)//
				.addEventListeners(new CommandListener())//
				.build();
	}

}