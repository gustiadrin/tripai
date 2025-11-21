# Arquitectura

## Componentes
- Backend (Spring Boot)
  - `ChatController`: expone `POST /api/chat` y `GET /api/messages`.
  - `GeminiChatService`: llama a la API REST de Gemini con API key.
  - `ChatService`: mantiene contexto simple en memoria (últimos 5 mensajes) para construir el prompt.
  - `ChatMessage`: modelo de mensaje.

- Frontend (Angular)
  - `ChatService` (Angular):
    - Base URL `http://localhost:8080/api`.
    - Señal `messages` con persistencia en `localStorage`.
    - Polling a `/api/messages` cada 2s.
  - `ChatAssistant` component: UI de chat, envío de mensajes y render del historial.

## Flujo de Datos
1. Usuario escribe mensaje en el front.
2. Front llama `POST /api/chat` y añade mensaje propio al estado local.
3. Backend guarda el mensaje, compone prompt con últimos 5 mensajes y consulta Gemini.
4. Backend guarda y devuelve la respuesta.
5. Front añade la respuesta y el polling mantiene el estado sincronizado.

## Decisiones
- Contexto en memoria (simple para demo) en lugar de BBDD.
- API REST de Gemini con API key para facilitar pruebas locales.
- Persistencia local en el navegador vía `localStorage`.




