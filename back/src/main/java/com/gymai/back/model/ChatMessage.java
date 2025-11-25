package com.gymai.back.model;

/**
 * Representa un mensaje simple en el historial (usuario o bot).
 */
public class ChatMessage {

	private String sender;
	private String content;
	private String timestamp;

	public ChatMessage() {}

	public ChatMessage(String sender, String content) {
		this.sender = sender;
		this.content = content;
	}

	public String getSender() { return sender; }
	public void setSender(String sender) { this.sender = sender; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public String getTimestamp() { return timestamp; }
	public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
