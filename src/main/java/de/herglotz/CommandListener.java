package de.herglotz;

import java.util.ArrayDeque;
import java.util.Queue;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	private Queue<String> queue;

	public CommandListener() {
		this.queue = new ArrayDeque<>();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (shouldReactToMessage(event)) {
			String msg = event.getMessage().getContentRaw();
			if (isCommandKeyword(msg)) {
				if (msg.equals("!hand")) {
					queue.add(event.getAuthor().getAsMention());
				}
				if (msg.equals("!next") || queue.size() == 1) {
					MessageChannel channel = event.getChannel();
					String user = queue.poll();
					if (user == null) {
						channel.sendMessage("Keine weiteren Sprecher").queue();
					} else {
						channel.sendMessage(user + " darf jetzt sprechen!").queue();
					}
				}
			} else {
				event.getChannel().sendMessage("Schnauze! Nur !hand und !next funktionieren hier!");
			}
		}
	}

	private boolean shouldReactToMessage(MessageReceivedEvent event) {
		return event.getChannel().getName().equals("discussion") && !event.getAuthor().isBot();
	}

	private boolean isCommandKeyword(String msg) {
		return msg.equalsIgnoreCase("!hand") || msg.equalsIgnoreCase("!next");
	}
}