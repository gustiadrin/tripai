package com.gymai.back.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gymai.back.model.ChatMessage;
import com.gymai.back.service.ChatService;
import com.gymai.back.service.GeminiChatService;
import com.gymai.back.service.PdfService;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ChatController {

	private final GeminiChatService geminiChatService;
	private final ChatService chatService;
	private final List<ChatMessage> messages = Collections.synchronizedList(new ArrayList<>());
	private final PdfService pdfService;

	private static final String SYSTEM_PROMPT = "Eres GymAI, un asistente experto en rutina de entrenamientos y dietas. Mantén el tema en rutinas de gimnasio y dietas y guía al usuario si se desvía. Elabora respuestas cortas y concisas que mantengan la conversación fluida.\n";

	/**
	 * Mensaje de entrada enviado por el front.
	 */
	public static record ChatRequest(String message) {}

	/**
	 * Respuesta mínima devuelta al front.
	 */
	public static record ChatResponse(String reply) {}

	@GetMapping("/messages")
	/**
	 * Devuelve el historial simple en memoria (sólo para desarrollo/demo).
	 */
	public List<ChatMessage> getMessages() {
		return messages;
	}

	private ChatMessage getLastBotMessage() {
		for (int i = messages.size() - 1; i >= 0; i--) {
			ChatMessage m = messages.get(i);
			if ("bot".equals(m.getSender())) {
				return m;
			}
		}
		return null;
	}

	@GetMapping(path = "/export/last-plan.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> exportLastPlanPdf() {
		ChatMessage lastBot = getLastBotMessage();
		if (lastBot == null) {
			return ResponseEntity.noContent().build();
		}
		String title = "Plan GymAI";
		byte[] pdfBytes = pdfService.generatePlanPdf(title, lastBot.getContent());
		return ResponseEntity
				.ok()
				.headers(h -> {
					h.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plan-gymai.pdf");
				})
				.body(pdfBytes);
	}

	@PostMapping("/messages/reset")
	public void resetMessages() {
		messages.clear();
		chatService.clearContext();
	}
	

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    /**
     * Recibe el mensaje del usuario, lo guarda en el historial y contexto (solo la
     * parte de mensaje del usuario), construye un prompt con el contexto reciente
     * más el mensaje completo recibido (que puede incluir perfil) y consulta a Gemini.
     */
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String userText = request.message() == null ? "" : request.message();

        String storedUserText = extractUserMessage(userText);

        ChatMessage userMsg = new ChatMessage("user", storedUserText);
        userMsg.setTimestamp(Instant.now().toString());
        messages.add(userMsg);
        chatService.addToContext(userMsg);

        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
        chatService.getLastContext().forEach(m ->
            prompt.append(m.getSender()).append(": ").append(m.getContent()).append("\n")
        );
        // Añadimos al final el mensaje completo tal y como lo envió el front
        prompt.append("user: ").append(userText).append("\n");

        String reply = geminiChatService.getChatbotResponse(prompt.toString());

        ChatMessage botMsg = new ChatMessage("bot", reply);
        botMsg.setTimestamp(Instant.now().toString());
        messages.add(botMsg);
        chatService.addToContext(botMsg);
        return new ChatResponse(reply);
    }

	@GetMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStream(@RequestParam("message") String message) {
		String userText = message == null ? "" : message;
		String storedUserText = extractUserMessage(userText);

		ChatMessage userMsg = new ChatMessage("user", storedUserText);
		userMsg.setTimestamp(Instant.now().toString());
		messages.add(userMsg);
		chatService.addToContext(userMsg);

		StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
		chatService.getLastContext().forEach(m ->
				prompt.append(m.getSender()).append(": ").append(m.getContent()).append("\n")
		);
		// Añadimos el mensaje completo enviado desde el front (con perfil, si lo hay)
		prompt.append("user: ").append(userText).append("\n");

		StringBuilder fullReplyBuilder = new StringBuilder();

		return geminiChatService.streamChatbotResponse(prompt.toString())
				.map(chunk -> {
					fullReplyBuilder.append(chunk);
					return ServerSentEvent.<String>builder()
							.data(chunk)
							.build();
				})
				.doOnComplete(() -> {
					String fullReply = fullReplyBuilder.toString();
					ChatMessage botMsg = new ChatMessage("bot", fullReply);
					botMsg.setTimestamp(Instant.now().toString());
					messages.add(botMsg);
					chatService.addToContext(botMsg);
				});
	}

	/**
	 * Si el texto viene en el formato
	 * "Perfil del usuario ... \n\nMensaje del usuario: <texto>", devuelve solo
	 * la parte de "Mensaje del usuario" para guardar en historial/contexto.
	 */
	private String extractUserMessage(String raw) {
		if (raw == null) return "";
		String marker = "Mensaje del usuario:";
		int idx = raw.indexOf(marker);
		if (idx == -1) {
			return raw;
		}
		return raw.substring(idx + marker.length()).trim();
	}
}
