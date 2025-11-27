package com.gymai.back.service;

import com.gymai.back.model.ChatMessage;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Servicio centralizado para gestión de mensajes y contexto.
 * Única fuente de verdad para el historial de chat.
 */
@Service
public class ChatService {

    private static final int MAX_MESSAGES = 50;
    private static final int CONTEXT_SIZE = 5;
    
    private final List<ChatMessage> messages = Collections.synchronizedList(new ArrayList<>());

    /**
     * Devuelve todos los mensajes del historial.
     */
    public List<ChatMessage> getAllMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Devuelve hasta los últimos 5 mensajes para contexto.
     */
    public List<ChatMessage> getLastContext() {
        int size = messages.size();
        return messages.subList(Math.max(0, size - CONTEXT_SIZE), size);
    }

    /**
     * Añade un mensaje al historial, manteniendo un máximo de 50.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    /**
     * Busca el último mensaje del bot para generar PDFs.
     */
    public ChatMessage getLastBotMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if ("bot".equals(m.getSender())) {
                return m;
            }
        }
        return null;
    }

    /**
     * Limpia todo el historial de mensajes.
     */
    public void clearMessages() {
        messages.clear();
    }
}
