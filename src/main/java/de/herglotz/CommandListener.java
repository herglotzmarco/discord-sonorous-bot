package de.herglotz;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.MessageHistory.MessageRetrieveAction;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	private static final String INDEX_KEYWORD = "!index";
	private static final String FETCH_KEYWORD = "!vers";
	private static final String DELETE_KEYWORD = "!cleanup";

	private final Map<String, List<BibleMessage>> messagesByReaction;
	private final Random random;

	public CommandListener() {
		this.messagesByReaction = new ConcurrentHashMap<>();
		this.random = new Random();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (shouldReactToMessage(event)) {
			String msg = event.getMessage().getContentStripped();
			if (containsCommandKeyword(msg)) {
				deleteIncomingMessage(event);
				handleCommand(event, msg);
			}
		}

	}

	private void deleteIncomingMessage(MessageReceivedEvent event) {
		event.getMessage().delete().queueAfter(2, TimeUnit.SECONDS);
	}

	private void handleCommand(MessageReceivedEvent event, String msg) {
		if (msg.contains(INDEX_KEYWORD)) {
			handleIndexCommand(event);
		} else if (msg.contains(FETCH_KEYWORD)) {
			handleFetchCommand(event, msg);
		} else if (msg.contains(DELETE_KEYWORD)) {
			handleDeleteCommand(event);
		}
	}

	private void handleIndexCommand(MessageReceivedEvent event) {
		messagesByReaction.clear();
		MessageRetrieveAction retrieveAction = event.getChannel().getHistoryFromBeginning(100);
		retrieveAction.queue(h -> traverseChannelHistory(h, this::indexQuote));
	}

	private void indexQuote(Message message) {
		String messageContent = message.getContentStripped();
		if (!containsCommandKeyword(messageContent)) {
			BibleMessage entry = parseMessage(messageContent);
			indexMessage(message, entry);
		}
	}

	private void indexMessage(Message message, BibleMessage entry) {
		List<String> reactions = message.getReactions().stream()//
				.map(MessageReaction::getReactionEmote)//
				.map(ReactionEmote::getAsReactionCode)//
				.collect(toList());
		if (reactions.isEmpty()) {
			messagesByReaction.computeIfAbsent("", r -> new ArrayList<>()).add(entry);
		} else {
			for (String reaction : reactions) {
				messagesByReaction.computeIfAbsent(reaction, r -> new ArrayList<>()).add(entry);
			}
		}
	}

	private BibleMessage parseMessage(String messageContent) {
		String[] lines = messageContent.split("\\n");
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
		return new BibleMessage(authors, messages);
	}

	private void handleFetchCommand(MessageReceivedEvent event, String msg) {
		List<BibleMessage> messages = selectMessages(msg);
		sendRandomMessage(event, messages);
	}

	private List<BibleMessage> selectMessages(String msg) {
		String[] split = msg.split(" ");
		if (split.length > 1) {
			String emote = split[1];
			return List.copyOf(messagesByReaction.get(emote));
		} else {
			return messagesByReaction.values().stream()//
					.flatMap(Collection::stream)//
					.distinct()//
					.collect(toUnmodifiableList());
		}
	}

	private void sendRandomMessage(MessageReceivedEvent event, List<BibleMessage> messages) {
		if (messages.size() == 1) {
			BibleMessage randomMessage = messages.get(0);
			event.getChannel().sendMessage(randomMessage.getAsText()).queue();
		} else if (messages.size() > 1) {
			int randomIndex = random.nextInt((messages.size() - 1));
			BibleMessage randomMessage = messages.get(randomIndex);
			event.getChannel().sendMessage(randomMessage.getAsText()).queue();
		}
	}

	private void handleDeleteCommand(MessageReceivedEvent event) {
		MessageRetrieveAction retrieveAction = event.getChannel().getHistoryFromBeginning(100);
		retrieveAction.queue(h -> traverseChannelHistory(h, this::deleteBotMessage));
	}

	private void deleteBotMessage(Message message) {
		if (message.getAuthor().isBot()) {
			message.delete().queue();
		}
	}

	private void traverseChannelHistory(MessageHistory history, Consumer<Message> action) {
		traverseChannelHistory(history, history.getRetrievedHistory(), action);
	}

	// potential StackOverflowException, but for now we will probably never fetch so
	// many messages that we hit this limit
	private void traverseChannelHistory(MessageHistory history, List<Message> retrieved, Consumer<Message> action) {
		retrieved.forEach(action);
		if (!retrieved.isEmpty() && history.getRetrievedHistory().size() % 100 == 0) {
			history.retrieveFuture(100).queue(messages -> this.traverseChannelHistory(history, messages, action));
		}
	}

	private boolean shouldReactToMessage(MessageReceivedEvent event) {
		return event.getChannel().getName().contains("bible") && !event.getAuthor().isBot();
	}

	private boolean containsCommandKeyword(String msg) {
		return msg.contains(INDEX_KEYWORD) || msg.contains(FETCH_KEYWORD) || msg.contains(DELETE_KEYWORD);
	}

}