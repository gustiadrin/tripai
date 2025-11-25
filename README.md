# GymAI

Aplicación Angular + Spring Boot con integración a Gemini (API REST) para un asistente de rutinas de entrenamiento y dietas.

## Requisitos

- Node.js 18+
- Angular CLI (opcional para dev): `npm i -g @angular/cli`
- Java 21
- Maven Wrapper incluido (`mvnw`)

## Estructura

- `front/TripAI/` Frontend Angular (GymAI)
- `back/` Backend Spring Boot (com.gymai.back)

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

El front apunta por defecto a `http://localhost:8080/api` en desarrollo. En producción usa la URL configurada en `environment.prod.ts`.

## Endpoints Backend

- `POST /api/chat`

  - Body: `{ "message": "texto" }`
  - Respuesta: `{ "reply": "texto" }`

- `GET /api/messages` (demo)
  - Devuelve el historial simple en memoria.

## Flujo de la app

- El usuario completa su perfil (edad, peso, altura, objetivos) en la página inicial.
- El front envía mensajes a `/api/chat/stream` (streaming) o `/api/chat` (respuesta completa).
- El backend construye un prompt con el perfil del usuario + contexto de los últimos 5 mensajes y consulta a Gemini.
- La respuesta se devuelve mediante Server-Sent Events (streaming) para una experiencia en tiempo real.
- El front persiste localmente el historial en `localStorage` (`gymai_messages`).
- Se pueden generar PDFs de rutinas y dietas mediante `/api/export/last-plan.pdf`.

## Troubleshooting

- `ERR_CONNECTION_REFUSED` desde el front: asegúrate de que el backend corre en 8080.
- 401/403 de Gemini: revisa `gemini.api-key`.
- CORS: el backend permite `http://localhost:4200` en el controlador.
- Postman: prueba `POST http://localhost:8080/api/chat` con `Content-Type: application/json`.

## Despliegue

### Backend (Fly.io)

```bash
cd back
fly deploy
```

### Frontend (por ejemplo, Vercel/Netlify)

1. Configurar la variable de entorno con la URL del backend en producción
2. Ejecutar `npm run build`
3. Desplegar la carpeta `dist/gym-ai`

**Notas de producción:**

- El historial en memoria es sólo para desarrollo. Considerar una base de datos para persistencia.
- Configurar la API key de Gemini como secret en Fly.io: `fly secrets set GEMINI_API_KEY=tu_key`
- Actualizar CORS en el backend para permitir el dominio del frontend en producción.
