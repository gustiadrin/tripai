# TripAI

Aplicación Angular + Spring Boot con integración a Gemini (API REST) para un asistente de viajes.

## Requisitos

- Node.js 18+
- Angular CLI (opcional para dev): `npm i -g @angular/cli`
- Java 21
- Maven Wrapper incluido (`mvnw`)

## Estructura

- `front/TripAI/` Frontend Angular
- `back/` Backend Spring Boot

## Configuración Backend

1. Variables en `back/src/main/resources/application.properties`:

```
server.port=8080
# API key de Gemini (Google AI Studio)
gemini.api-key=TU_API_KEY
# Modelo (opcional)
gemini.model-name=gemini-2.0-flash
```

2. Ejecutar backend:

```bash
cd back
./mvnw spring-boot:run
```

## Configuración Frontend

1. Instalar dependencias:

```bash
cd front/TripAI
npm install
```

2. Ejecutar en dev:

```bash
npm start
# o
ng serve
```

El front apunta por defecto a `http://localhost:8080/api` en `ChatService`.

## Endpoints Backend

- `POST /api/chat`

  - Body: `{ "message": "texto" }`
  - Respuesta: `{ "reply": "texto" }`

- `GET /api/messages` (demo)
  - Devuelve el historial simple en memoria.

## Flujo de la app

- El front envía mensajes a `/api/chat`.
- El backend guarda el mensaje del usuario en memoria, construye un prompt con los últimos 5 mensajes y consulta a Gemini.
- La respuesta se guarda y se devuelve al front.
- El front también persiste localmente el historial en `localStorage` (`tripai_messages`).

## Troubleshooting

- `ERR_CONNECTION_REFUSED` desde el front: asegúrate de que el backend corre en 8080.
- 401/403 de Gemini: revisa `gemini.api-key`.
- CORS: el backend permite `http://localhost:4200` en el controlador.
- Postman: prueba `POST http://localhost:8080/api/chat` con `Content-Type: application/json`.

## Producción

- El historial en memoria es sólo para desarrollo. Sustituir por una base de datos si es necesario.
- Mover API key a variables de entorno/secret manager.
