# Guía de Despliegue - GymAI

Esta guía te ayudará a desplegar GymAI en producción.

## 📋 Requisitos Previos

- Cuenta en [Fly.io](https://fly.io) (para backend)
- Cuenta en Vercel/Netlify (para frontend) - opcional
- [Fly CLI](https://fly.io/docs/hands-on/install-flyctl/) instalado
- Git configurado

## 🚀 Paso 1: Preparar el Backend en Fly.io

### 1.1 Compilar localmente (opcional, para verificar)

```bash
cd back
./mvnw clean package -DskipTests
```

### 1.2 Inicializar Fly.io (si es primera vez)

```bash
cd back
fly launch --no-deploy
```

Cuando te pregunte:

- **App name**: `gymai-back` (o el nombre que prefieras)
- **Region**: Elige la más cercana (ej: `mad` para Madrid)
- **PostgreSQL**: `No` (usamos memoria por ahora)
- **Redis**: `No`

Esto creará un archivo `fly.toml` con la configuración.

### 1.3 Configurar secrets (API Key de Gemini)

```bash
fly secrets set GEMINI_API_KEY=TU_API_KEY_DE_GEMINI
```

### 1.4 Actualizar fly.toml si es necesario

Verifica que el archivo `fly.toml` tenga:

```toml
[env]
  PORT = "8080"

[http_service]
  internal_port = 8080
  force_https = true
```

### 1.5 Desplegar

```bash
fly deploy
```

### 1.6 Verificar despliegue

```bash
fly status
fly logs
```

Prueba el endpoint:

```bash
curl https://gymai-back.fly.dev/api/messages
```

### 1.7 Actualizar CORS para producción

Edita `back/src/main/java/com/gymai/back/config/WebConfig.java`:

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:4200",
                "https://tu-frontend.vercel.app"  // Añade tu dominio del frontend
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
}
```

Redesplegar:

```bash
fly deploy
```

## 🌐 Paso 2: Desplegar Frontend

### Opción A: Vercel

1. Instala Vercel CLI:

```bash
npm i -g vercel
```

2. Desde la carpeta del frontend:

```bash
cd front/TripAI
npm install
vercel
```

3. Sigue las instrucciones:

   - Setup and deploy: `Yes`
   - Scope: Tu cuenta
   - Link to existing project: `No`
   - Project name: `gym-ai`
   - Directory: `./` (actual)
   - Override settings: `No`

4. Vercel desplegará automáticamente. Copia la URL que te da.

### Opción B: Netlify

1. Construye el proyecto:

```bash
cd front/TripAI
npm install
npm run build
```

2. Instala Netlify CLI:

```bash
npm i -g netlify-cli
```

3. Despliega:

```bash
netlify deploy --prod --dir=dist/gym-ai
```

### Opción C: Manual (cualquier hosting estático)

1. Construye el proyecto:

```bash
cd front/TripAI
npm install
npm run build
```

2. Sube la carpeta `dist/gym-ai` a tu hosting (GitHub Pages, Firebase Hosting, etc.)

## 📦 Paso 3: Subir a GitHub

### 3.1 Crear repositorio en GitHub

1. Ve a https://github.com/new
2. Nombre: `GymAI` o `gym-ai`
3. Descripción: "Asistente de gimnasio y dietas con IA (Angular + Spring Boot + Gemini)"
4. Público o Privado según prefieras
5. NO inicializar con README (ya lo tenemos)

### 3.2 Configurar Git local

```bash
cd c:\Users\gusta\Desktop\Angular\TripAI

# Inicializar git si no está inicializado
git init

# Añadir archivos
git add .

# Commit inicial
git commit -m "Renombrado completo de TripAI a GymAI con funcionalidad completa"

# Conectar con GitHub (reemplaza con tu URL)
git remote add origin https://github.com/TU_USUARIO/gym-ai.git

# Subir a GitHub
git push -u origin main
```

### 3.3 Crear .gitignore si no existe

Asegúrate de tener un `.gitignore` en la raíz con:

```
# Backend
back/target/
back/.mvn/
back/mvnw
back/mvnw.cmd

# Frontend
front/TripAI/node_modules/
front/TripAI/dist/
front/TripAI/.angular/

# IDEs
.vscode/
.idea/
*.iml

# OS
.DS_Store
Thumbs.db

# Secrets (IMPORTANTE)
*.env
application-local.properties
```

**⚠️ IMPORTANTE**: No subir la API key de Gemini a GitHub. Usa variables de entorno.

## 🔄 Actualizaciones Futuras

### Actualizar Backend

```bash
cd back
git pull
fly deploy
```

### Actualizar Frontend

```bash
cd front/TripAI
git pull
npm install
npm run build
vercel --prod  # o netlify deploy --prod --dir=dist/gym-ai
```

## 🔒 Configuración de Seguridad

### Backend (Fly.io)

1. **Proteger API Key**: Ya configurada con `fly secrets`
2. **HTTPS**: Automático en Fly.io
3. **Variables de entorno**: Usar secrets en lugar de application.properties

### Frontend

1. **Environment variables**: Nunca exponer API keys en el frontend
2. **CORS**: Configurado en el backend para dominios específicos

## 📊 Monitoreo

### Logs del Backend

```bash
fly logs -a gymai-back
```

### Ver estado

```bash
fly status -a gymai-back
```

### Métricas

```bash
fly dashboard
```

## 🐛 Troubleshooting

### Backend no responde

```bash
fly logs -a gymai-back
fly ssh console -a gymai-back
```

### Error de CORS

- Verifica que el dominio del frontend esté en `WebConfig.java`
- Redesplega el backend después de cambios

### Error 500 en Gemini

- Verifica que el secret esté configurado: `fly secrets list`
- Revisa los logs: `fly logs`

## 📝 Notas Adicionales

- **Nombre de la app en Fly.io**: Si ya tienes `tripai-back` desplegado, puedes:
  - Opción 1: Renombrar la app existente con `fly apps rename`
  - Opción 2: Crear una nueva app `gymai-back` y eliminar la antigua
- **Base de datos**: Actualmente usa memoria. Para producción real, considera PostgreSQL o MongoDB.

- **Costos**: Fly.io tiene plan gratuito. Vercel y Netlify también tienen tiers gratuitos generosos.
