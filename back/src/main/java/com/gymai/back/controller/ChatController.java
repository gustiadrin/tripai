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
import com.gymai.back.service.PromptBuilder;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ChatController {

	private final GeminiChatService geminiChatService;
	private final ChatService chatService;
	private final PdfService pdfService;
	private final PromptBuilder promptBuilder;

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
		return chatService.getAllMessages();
	}

	@GetMapping(path = "/export/last-plan.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> exportLastPlanPdf() {
		ChatMessage lastBot = chatService.getLastBotMessage();
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
		chatService.clearMessages();
	}
	

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    /**
     * Recibe el mensaje del usuario, lo guarda en el historial y contexto (solo la
     * parte de mensaje del usuario), construye un prompt con el contexto reciente
     * más el mensaje completo recibido (que puede incluir perfil) y consulta a Gemini.
     */
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String userText = request.message() == null ? "" : request.message();

        String storedUserText = promptBuilder.extractUserMessage(userText);

        ChatMessage userMsg = new ChatMessage("user", storedUserText);
        userMsg.setTimestamp(Instant.now().toString());
        chatService.addMessage(userMsg);

        String prompt = promptBuilder.buildPrompt(chatService.getLastContext(), userText);
        String reply = geminiChatService.getChatbotResponse(prompt);

        ChatMessage botMsg = new ChatMessage("bot", reply);
        botMsg.setTimestamp(Instant.now().toString());
        chatService.addMessage(botMsg);
        return new ChatResponse(reply);
    }

	@GetMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStream(@RequestParam("message") String message) {
		String userText = message == null ? "" : message;
		String storedUserText = promptBuilder.extractUserMessage(userText);

		ChatMessage userMsg = new ChatMessage("user", storedUserText);
		userMsg.setTimestamp(Instant.now().toString());
		chatService.addMessage(userMsg);

		String prompt = promptBuilder.buildPrompt(chatService.getLastContext(), userText);
		StringBuilder fullReplyBuilder = new StringBuilder();

		return geminiChatService.streamChatbotResponse(prompt)
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
					chatService.addMessage(botMsg);
				});
	}

}
