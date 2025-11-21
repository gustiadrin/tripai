package com.tripai.back.service;

import com.tripai.back.model.ChatMessage;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Gestiona un contexto simple en memoria para construir prompts con las
 * últimas interacciones. No persistente y apto sólo para desarrollo/demo.
 */
@Service
public class ChatService {

    private final List<ChatMessage> context = new ArrayList<>();



    /**
     * Devuelve hasta los últimos 5 mensajes.
     */
    public List<ChatMessage> getLastContext() {
        int size = context.size();
        return context.subList(Math.max(0, size - 5), size);
    }

    /**
     * Añade un mensaje al contexto, manteniendo un máximo de 10.
     */
    public void addToContext(ChatMessage message) {
        context.add(message);
        if (context.size() > 10) context.remove(0);
    }

    /**
     * Limpia todo el contexto almacenado en memoria.
     */
    public void clearContext() {
        context.clear();
    }
}
