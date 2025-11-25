# Arquitectura GymAI

## Componentes

- **Backend** (Spring Boot - `com.gymai.back`)

  - `ChatController`: expone `GET /api/chat/stream` (SSE), `POST /api/chat`, `GET /api/messages`, `POST /api/messages/reset`, `GET /api/export/last-plan.pdf`.
  - `GeminiChatService`: llama a la API REST de Gemini (streaming y normal) con API key.
  - `ChatService`: mantiene contexto en memoria (últimos 5 mensajes) para construir el prompt.
  - `PdfService`: genera PDFs de planes usando OpenPDF.
  - `ChatMessage`: modelo de mensaje.

- **Frontend** (Angular)
  - `ProfileService`: gestiona el perfil del usuario (edad, peso, altura, objetivos) en `localStorage`.
  - `ChatService` (Angular):
    - Base URL configurable por environment.
    - Señal `messages` con persistencia en `localStorage` (`gymai_messages`).
    - Streaming mediante EventSource (SSE).
  - `HomeComponent`: formulario de perfil inicial con tema claro/oscuro.
  - `ChatAssistant` component: UI de chat con streaming en tiempo real, generación de PDFs (rutinas/dietas), markdown rendering.

## Flujo de Datos

### Flujo inicial con perfil:

1. Usuario completa perfil en `HomeComponent` (edad, peso, altura, objetivo, actividad).
2. Al guardar, se resetea la conversación y navega a `/chat`.
3. `ChatAssistant` detecta que hay perfil y envía automáticamente un mensaje inicial a Gemini con el perfil.
4. Gemini responde con un análisis inicial y plan personalizado.

### Flujo de mensajes normales:

1. Usuario escribe mensaje en el chat.
2. Frontend construye mensaje completo: `Perfil del usuario: ... \n\nMensaje del usuario: [texto]`
3. Frontend llama `GET /api/chat/stream?message=...` (EventSource/SSE).
4. Backend extrae solo el texto del mensaje para guardar en historial.
5. Backend compone prompt: `SYSTEM_PROMPT + contexto últimos 5 mensajes + mensaje completo con perfil`.
6. Gemini responde en streaming (chunks de texto).
7. Frontend muestra chunks en tiempo real con efecto typewriter.
8. Al completar, guarda respuesta completa en historial.

### Generación de PDFs:

1. Usuario hace clic en "Generar Rutina" o "Generar Dieta".
2. Frontend envía comando especial con perfil + instrucción específica.
3. Backend genera respuesta de Gemini (no visible en chat).
4. Frontend descarga automáticamente `GET /api/export/last-plan.pdf`.

## Decisiones Técnicas

- **Contexto en memoria**: Simple para demo. Para producción considerar Redis o base de datos.
- **API REST de Gemini**: Uso de Gemini 2.0 Flash con streaming para respuestas rápidas.
- **Server-Sent Events (SSE)**: Para streaming en tiempo real sin WebSockets.
- **Persistencia local**: `localStorage` para perfil y mensajes (funciona sin backend).
- **PDF Generation**: OpenPDF para generar documentos descargables de rutinas/dietas.
- **Markdown rendering**: `ngx-markdown` para formato rico en respuestas de Gemini.
- **Theming**: Sistema de temas claro/oscuro usando CSS custom properties.
