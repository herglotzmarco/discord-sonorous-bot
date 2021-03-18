package de.herglotz;

import java.util.List;
import java.util.Objects;

public class BibleMessage {

	private final List<String> authors;
	private final List<String> messages;

	public BibleMessage(List<String> authors, List<String> messages) {
		this.authors = authors;
		this.messages = messages;
	}

	public String getAsText() {
		StringBuilder text = new StringBuilder("Und ").append(authors.get(0)).append(" sprach: ").append(messages.get(0));
		for (int i = 1; i < messages.size(); i++) {
			text.append(", ");
			text.append("und dann sprach ").append(authors.get(i)).append(": ").append(messages.get(i));
		}
		return text.toString();
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
		return "BibleMessage [authors=" + authors + ", messages=" + messages + "]";
	}

}