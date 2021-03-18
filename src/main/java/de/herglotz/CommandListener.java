package de.herglotz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory.MessageRetrieveAction;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	private Map<String, List<BibleMessage>> messagesByReaction;

	public CommandListener() {
		messagesByReaction = new HashMap<>();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (shouldReactToMessage(event)) {
			String msg = event.getMessage().getContentRaw();
			if (isCommandKeyword(msg)) {
				if (msg.equals("!index")) {
					handleIndexEvent(event);
				} else if (msg.contains("!vers")) {
					handleFetchEvent(event, msg);
				}
				event.getMessage().delete().queueAfter(2, TimeUnit.SECONDS);
			}
		}

	}

	private void handleIndexEvent(MessageReceivedEvent event) {
		messagesByReaction.clear();
		MessageRetrieveAction history = event.getChannel().getHistoryFromBeginning(100);
		history.queue(h -> {
			h.getRetrievedHistory().forEach(this::indexQuote);
			while (h.getRetrievedHistory().size() % 100 == 0) {
				h.retrieveFuture(100).queue(m -> m.forEach(this::indexQuote));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		});
	}

	private void handleFetchEvent(MessageReceivedEvent event, String msg) {
		String[] split = msg.split(" ");
		List<BibleMessage> messages;
		if (split.length > 1) {
			String emote = split[1];
			messages = messagesByReaction.get(emote);
		} else {
			messages = messagesByReaction.values().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
		}
		int random = (int) (Math.random() * (messages.size() - 1));
		event.getChannel().sendMessage(messages.get(random).toString()).queue();
	}

	private void indexQuote(Message message) {
		if (message.getAuthor().isBot()) {
			message.delete().queue();
		} else if (!isCommandKeyword(message.getContentStripped())) {
			List<String> reactions = message.getReactions().stream()//
					.map(MessageReaction::getReactionEmote)//
					.map(ReactionEmote::getAsReactionCode)//
					.collect(Collectors.toList());

			String content = message.getContentStripped();
			String[] lines = content.split("\\n");
			List<String> authors = new ArrayList<>();
			List<String> messages = new ArrayList<>();
			for (int i = 0; i < lines.length; i = i + 2) {
				if (lines.length == 1) {
					authors.add("jemand");
					messages.add(lines[i]);
				} else {
					messages.add(lines[i]);
					authors.add(lines[i + 1].substring(2));
				}
			}

			if (reactions.isEmpty()) {
				messagesByReaction.computeIfAbsent("", r -> new ArrayList<>()).add(new BibleMessage(authors, messages));
			} else {
				for (String reaction : reactions) {
					messagesByReaction.computeIfAbsent(reaction, r -> new ArrayList<>()).add(new BibleMessage(authors, messages));
				}
			}
		}
	}

	private boolean shouldReactToMessage(MessageReceivedEvent event) {
		return event.getChannel().getName().contains("bible") && !event.getAuthor().isBot();
	}

	private boolean isCommandKeyword(String msg) {
		return msg.contains("!index") || msg.contains("!vers");
	}

	private static class BibleMessage {

		private final List<String> authors;
		private final List<String> messages;

		private BibleMessage(List<String> authors, List<String> messages) {
			this.authors = authors;
			this.messages = messages;
		}

		@Override
		public int hashCode() {
			return Objects.hash(authors, messages);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			BibleMessage other = (BibleMessage) obj;
			return Objects.equals(authors, other.authors) && Objects.equals(messages, other.messages);
		}

		@Override
		public String toString() {
			String text = "Und " + authors.get(0) + " sprach: " + messages.get(0);
			for (int i = 1; i < messages.size(); i++) {
				text = text + ", ";
				text = text + "und dann sprach " + authors.get(i) + ": " + messages.get(i);
			}
			return text;
		}

	}
}