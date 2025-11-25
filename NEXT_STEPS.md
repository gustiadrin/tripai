# 🚀 Próximos Pasos - GymAI

## ✅ Cambios Completados

Se han realizado todos los cambios de renombrado de **TripAI → GymAI**:

### Backend

- ✅ Packages Java: `com.tripai.back` → `com.gymai.back`
- ✅ Maven groupId actualizado
- ✅ Nombre de aplicación: `gymai-back`
- ✅ Todos los imports y referencias actualizadas

### Frontend

- ✅ Nombre del proyecto: `gym-ai`
- ✅ localStorage keys: `gymai_messages`, `gymai_theme`
- ✅ Configuración de producción: `https://gymai-back.fly.dev/api`
- ✅ Todos los archivos de configuración actualizados

### Documentación

- ✅ README.md actualizado con funcionalidad de GymAI
- ✅ ARCHITECTURE.md con flujos completos documentados
- ✅ DEPLOYMENT.md con guía completa de despliegue

## 🔥 IMPORTANTE: Proteger API Key

**⚠️ ANTES DE SUBIR A GITHUB:**

Tu API key de Gemini está actualmente en `back/src/main/resources/application.properties`.

### Opción 1: Usar variables de entorno (Recomendado)

1. Edita `application.properties` y elimina la API key:

```properties
spring.application.name=gymai-back
gemini.api-key=${GEMINI_API_KEY}
```

2. Crea un archivo `application-local.properties` (ignorado por git):

```properties
gemini.api-key=TU_API_KEY_REAL
```

3. Añade a `.gitignore`:

```
application-local.properties
```

4. En Fly.io, la key ya está configurada como secret.

### Opción 2: Quitar la key del código

Simplemente borra la línea con la key antes de hacer commit. La configurarás solo en Fly.io.

## 📦 1. Probar Localmente

Antes de desplegar, verifica que todo funciona:

### Backend:

```bash
cd back
./mvnw spring-boot:run
```

Debería iniciar sin errores en `http://localhost:8080`

### Frontend:

```bash
cd front/TripAI
npm install
npm start
```

Debería abrir en `http://localhost:4200`

Prueba:

- Completar perfil
- Enviar mensajes
- Generar rutina PDF
- Generar dieta PDF
- Cambiar tema claro/oscuro

## 📤 2. Subir a GitHub

```bash
cd c:\Users\gusta\Desktop\Angular\TripAI

# Ver qué archivos se van a subir
git status

# Añadir todos los archivos
git add .

# Commit
git commit -m "feat: Renombrado completo TripAI → GymAI con funcionalidad completa de entrenamiento y dietas"

# Si aún no has creado el repositorio en GitHub:
# 1. Ve a https://github.com/new
# 2. Crea un repo llamado "GymAI" o "gym-ai"
# 3. Conecta con el remote:
git remote add origin https://github.com/TU_USUARIO/gym-ai.git

# Subir
git push -u origin main
```

## 🚀 3. Desplegar Backend a Fly.io

### Si ya tienes tripai-back desplegado:

#### Opción A: Renombrar la app existente

```bash
cd back
fly apps rename tripai-back gymai-back
fly deploy
```

#### Opción B: Crear nueva app

```bash
cd back
fly launch --name gymai-back
fly secrets set GEMINI_API_KEY=TU_API_KEY
fly deploy
```

### Verificar despliegue:

```bash
fly status
curl https://gymai-back.fly.dev/api/messages
```

## 🌐 4. Desplegar Frontend

### Vercel (Recomendado):

```bash
cd front/TripAI
npm install
vercel
```

### O manual:

```bash
npm run build
# Sube la carpeta dist/gym-ai a tu hosting
```

## 🔧 5. Actualizar CORS

Una vez tengas la URL del frontend desplegado, actualiza el backend:

`back/src/main/java/com/gymai/back/config/WebConfig.java`:

```java
.allowedOrigins(
    "http://localhost:4200",
    "https://tu-app.vercel.app"  // Tu URL real
)
```

Redesplegar:

```bash
cd back
fly deploy
```

## ✨ 6. Probar en Producción

1. Abre tu frontend en producción
2. Completa el formulario de perfil
3. Prueba el chat
4. Genera PDFs
5. Verifica que todo funcione

## 📊 Monitoreo

```bash
# Ver logs del backend
fly logs -a gymai-back

# Ver estado
fly status -a gymai-back

# Dashboard
fly dashboard
```

## 🎉 ¡Listo!

Tu app GymAI debería estar completamente funcional en producción.

### URLs finales:

- **Backend**: https://gymai-back.fly.dev
- **Frontend**: https://tu-app.vercel.app (o el dominio que uses)

---

## 💡 Mejoras Futuras Sugeridas

- [ ] Agregar base de datos (PostgreSQL/MongoDB) para persistencia real
- [ ] Implementar autenticación de usuarios
- [ ] Guardar rutinas y dietas por usuario
- [ ] Agregar seguimiento de progreso
- [ ] Integrar con APIs de fitness (Apple Health, Google Fit)
- [ ] Modo offline con PWA
- [ ] Analytics y métricas
