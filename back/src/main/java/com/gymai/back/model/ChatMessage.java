package com.gymai.back.model;

/**
 * Representa un mensaje simple en el historial (usuario o bot).
 */
public class ChatMessage {

	private String id;
	private String sender;
	private String content;
	private String timestamp;

	public ChatMessage() {
		this.id = java.util.UUID.randomUUID().toString();
	}

	public ChatMessage(String sender, String content) {
		this.id = java.util.UUID.randomUUID().toString();
		this.sender = sender;
		this.content = content;
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getSender() { return sender; }
	public void setSender(String sender) { this.sender = sender; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public String getTimestamp() { return timestamp; }
	public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
