package com.gymai.back.service;

import com.gymai.back.model.ChatMessage;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Builder para construir prompts de manera consistente.
 */
@Component
public class PromptBuilder {
    
    private static final String SYSTEM_PROMPT = "Eres GymAI, un asistente experto en rutina de entrenamientos y dietas. Mantén el tema en rutinas de gimnasio y dietas y guía al usuario si se desvía. Elabora respuestas cortas y concisas que mantengan la conversación fluida.\n";
    
    /**
     * Construye un prompt completo con contexto y mensaje del usuario.
     * 
     * @param context Lista de mensajes de contexto
     * @param userMessage Mensaje completo del usuario (puede incluir perfil)
     * @return Prompt formateado para Gemini
     */
    public String buildPrompt(List<ChatMessage> context, String userMessage) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
        
        // Añadir contexto de conversación
        context.forEach(message ->
            prompt.append(message.getSender())
                  .append(": ")
                  .append(message.getContent())
                  .append("\n")
        );
        
        // Añadir mensaje actual del usuario
        prompt.append("user: ").append(userMessage).append("\n");
        
        return prompt.toString();
    }
    
    /**
     * Extrae solo la parte del mensaje del usuario del texto completo.
     * Si el texto viene en formato "Perfil del usuario ... \n\nMensaje del usuario: <texto>",
     * devuelve solo la parte después de "Mensaje del usuario:".
     * 
     * @param rawMessage Mensaje completo que puede incluir perfil
     * @return Solo la parte del mensaje del usuario
     */
    public String extractUserMessage(String rawMessage) {
        if (rawMessage == null) return "";
        
        String marker = "Mensaje del usuario:";
        int idx = rawMessage.indexOf(marker);
        
        if (idx == -1) {
            return rawMessage;
        }
        
        return rawMessage.substring(idx + marker.length()).trim();
    }
}
