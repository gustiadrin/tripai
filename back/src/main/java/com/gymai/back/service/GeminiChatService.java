package com.gymai.back.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Servicio que encapsula la llamada HTTP a la API REST de Gemini.
 * Usa API key desde application.properties y envía un único bloque de contenido
 * con el texto recibido (prompt ya construido por el controlador).
 */
@Service
public class GeminiChatService {

	@Value("${gemini.api-key}")
	private String apiKey;

	@Value("${gemini.model-name:gemini-2.0-flash}")
	private String modelName;

	private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
	private static final String STREAM_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?key=%s";

	private final WebClient webClient = WebClient.builder().build();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Envía el prompt a Gemini y devuelve el primer texto de la primera candidata.
	 */
	public String getChatbotResponse(String userMessage) {
		try {
			String url = String.format(BASE_URL, modelName, apiKey);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> textPart = new HashMap<>();
			textPart.put("text", userMessage);

			Map<String, Object> part = new HashMap<>();
			part.put("text", userMessage);

			Map<String, Object> content = new HashMap<>();
			content.put("parts", List.of(part));

			Map<String, Object> body = new HashMap<>();
			body.put("contents", List.of(content));

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
			RestTemplate restTemplate = new RestTemplate();

			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

			// Navegar la respuesta para extraer el texto:
			// candidates[0].content.parts[0].text
			if (response == null)
				return "Respuesta vacía de Gemini";
			List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
			if (candidates == null || candidates.isEmpty())
				return "Sin candidatos en la respuesta";
			Map<String, Object> firstCandidate = candidates.get(0);
			Map<String, Object> contentObj = (Map<String, Object>) firstCandidate.get("content");
			if (contentObj == null)
				return "Respuesta sin contenido";
			List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
			if (parts == null || parts.isEmpty())
				return "Respuesta sin partes";
			Object text = parts.get(0).get("text");
			return text != null ? text.toString() : "Respuesta sin texto";
		} catch (Exception e) {
			return "Error al conectar con la API de Gemini: " + e.getMessage();
		}
	}

	/**
	 * Streaming de respuesta desde Gemini: va emitiendo trozos de texto segan
	 * llegan.
	 */
	public Flux<String> streamChatbotResponse(String prompt) {
		String url = String.format(STREAM_URL_TEMPLATE, modelName, apiKey);

		Map<String, Object> part = new HashMap<>();
		part.put("text", prompt);

		Map<String, Object> content = new HashMap<>();
		content.put("parts", List.of(part));

		Map<String, Object> body = new HashMap<>();
		body.put("contents", List.of(content));

		return webClient.post()
				.uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToFlux(Map.class)
				.flatMap(chunk -> {
					try {
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> candidates = (List<Map<String, Object>>) chunk.get("candidates");
						if (candidates == null || candidates.isEmpty()) {
							return Flux.empty();
						}
						Map<String, Object> firstCandidate = candidates.get(0);
						@SuppressWarnings("unchecked")
						Map<String, Object> contentObj = (Map<String, Object>) firstCandidate.get("content");
						if (contentObj == null) {
							return Flux.empty();
						}
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
						if (parts == null || parts.isEmpty()) {
							return Flux.empty();
						}

						return Flux.fromIterable(parts)
								.map(p -> {
									Object text = p.get("text");
									return text != null ? text.toString() : "";
								})
								.filter(t -> !t.isEmpty());
					} catch (Exception e) {
						return Flux.just("Error al procesar chunk de Gemini: " + e.getMessage());
					}
				})
				.onErrorResume(WebClientResponseException.class, ex -> {
					String friendlyMessage = mapErrorToUserMessage(ex);
					return Flux.just(friendlyMessage);
				})
				.onErrorResume(Exception.class, ex -> Flux.just("❌ Error interno: " + ex.getMessage()));
	}

	private String mapErrorToUserMessage(WebClientResponseException ex) {
		try {
			String errorBody = ex.getResponseBodyAsString();
			// El cuerpo puede ser un objeto {...} o un array [{...}]
			JsonNode root = objectMapper.readTree(errorBody);

			if (root.isArray() && root.size() > 0) {
				root = root.get(0);
			}

			// Mensajes por defecto según código HTTP
			if (ex.getStatusCode().value() == 429) {
				return "⏳ Se ha superado la cuota gratuita de uso.\nEsta aplicación funciona actualmente con un plan gratuito, por lo que tiene límites de uso.\nPor favor, espera unos minutos antes de volver a intentarlo.";
			}
			if (ex.getStatusCode().value() == 400) {
				return "⚠️ Solicitud incorrecta. Verifica que tu API Key sea válida.";
			}
			if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
				return "🔒 Error de permisos. Tu API Key no es válida o ha expirado.";
			}

			// Si podemos extraer el mensaje original
			if (root != null && root.has("error")) {
				JsonNode errorObj = root.get("error");
				if (errorObj.has("message")) {
					String msg = errorObj.get("message").asText();
					return "❌ Error de Gemini (" + ex.getStatusCode().value() + "): " + msg;
				}
			}

			return "❌ Error de Gemini (" + ex.getStatusCode().value() + "): " + errorBody;
		} catch (Exception e) {
			return "❌ Error de conexión con Gemini (" + ex.getStatusCode().value() + ").";
		}
	}
}
