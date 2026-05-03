# 🌟 GitStellarPrism - Visualizador de Repositorios GitHub

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

Aplicación Spring Boot WebFlux que transforma repositorios de GitHub en espectaculares visualizaciones 3D interactivas, con análisis de tecnologías y tiempo real.

## ✨ Características Principales

### 🎨 Visualización 3D Interactiva
- **Renderizado WebGL** con Three.js para máximo rendimiento
- **Navegación fluida** (rotar, zoom, desplazar)
- **Nodos de commits** con colores dinámicos según autor y fecha
- **Conexiones temporales** entre commits relacionados

### 🔐 Autenticación OAuth2 GitHub
- **Acceso a repositorios privados** con máxima seguridad
- **Scopes mínimos necesarios**: `repo`, `user:email`, `read:user`
- **Salvaguardas múltiples** contra operaciones peligrosas
- **Validación automática** de operaciones de solo lectura

### ⚡ Tiempo Real
- **Webhooks de GitHub** para actualizaciones instantáneas
- **Server-Sent Events (SSE)** para comunicación bidireccional
- **Filtrado inteligente** de eventos relevantes
- **Validación de firma HMAC-SHA256** opcional

### 📊 Análisis Avanzado
- **Detección automática de tecnologías** usadas
- **Análisis de contribuciones** por autor
- **Timeline interactivo** de actividad
- **Generación de resúmenes** para portfolios

## 🚀 Inicio Rápido

### 1. Configurar OAuth2 GitHub

1. Ve a [GitHub Developer Settings](https://github.com/settings/developers)
2. Crear nueva **OAuth App**:
   - **Application name**: `GitStellarPrism Local`
   - **Homepage URL**: `http://localhost:8080`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
3. Copiar **Client ID** y **Client Secret**

### 2. Configurar Variables de Entorno

#### **Opción A: Archivo `.env` (Recomendado)**

```bash
# 1. Copiar archivo de ejemplo
cp .env.example .env

# 2. Editar .env con tus credenciales
# Reemplazar GITHUB_CLIENT_ID y GITHUB_CLIENT_SECRET
```

#### **Opción B: Variables de Sistema**

```bash
# Windows (PowerShell)
$env:GITHUB_CLIENT_ID="tu_client_id_aquí"
$env:GITHUB_CLIENT_SECRET="tu_client_secret_aquí"

# Linux/macOS
export GITHUB_CLIENT_ID="tu_client_id_aquí"
export GITHUB_CLIENT_SECRET="tu_client_secret_aquí"
```

### 3. Ejecutar la Aplicación

```bash
# Con Maven Wrapper (recomendado)
./mvnw spring-boot:run

# O con Maven instalado
mvn spring-boot:run
```

### 4. Abrir en el Navegador

1. Ve a: http://localhost:8080
2. Haz clic en **"Iniciar Sesión"** 
3. Autoriza la aplicación en GitHub
4. ¡Explora tus repositorios!

### 5. Probar Generación de CV Técnico (NUEVO)

```bash
# Ejecutar script de pruebas automatizado

# Windows
.\test-cv-flow.ps1

# Linux/macOS
./test-cv-flow.sh
```

**Endpoints disponibles después de login:**
- `GET /api/user/repositories` - Lista de tus repositorios
- `GET /api/cv/summary` - Resumen técnico consolidado
- `GET /api/cv/preview` - Preview del CV en HTML
- `GET /api/cv/export/markdown` - Exportar CV a Markdown
- `GET /api/cv/export/json` - Exportar datos a JSON

> 📘 **Ver guía completa**: [SETUP.md](SETUP.md)

## 🎯 Cómo Usar

### Visualizar un Repositorio

1. **Inicia sesión** con tu cuenta de GitHub
2. **Ingresa el repositorio** en formato `owner/repo` (ej: `microsoft/vscode`)
3. **Haz clic en "Visualizar"** y espera a que carguen los datos
4. **Interactúa** con la visualización 3D:
   - 🖱️ **Click izquierdo + arrastrar**: Rotar vista
   - 🎢 **Rueda del mouse**: Zoom in/out
   - 🖱️ **Click derecho + arrastrar**: Mover vista
   - 🎯 **Click en commit**: Ver detalles

### Funciones Avanzadas

- **Filtros por fecha**: Última semana, mes, trimestre, año
- **Filtros por autor**: Buscar commits de desarrolladores específicos
- **Timeline interactivo**: Navegar por histórico de actividad
- **Análisis de tecnologías**: Ver distribución de lenguajes
- **Tiempo real**: Recibir notificaciones de nuevos commits

## 🛠️ Tecnologías Utilizadas

### Backend
- **Spring Boot 3.5.0** - Framework principal
- **Spring WebFlux** - Programación reactiva
- **Spring Security OAuth2** - Autenticación GitHub
- **WebClient** - Cliente HTTP reactivo
- **H2 Database** - Cache en memoria
- **Maven** - Gestión de dependencias

### Frontend
- **Three.js** - Renderizado 3D WebGL
- **Bootstrap 5** - UI responsivo
- **Vanilla JavaScript** - Lógica de interfaz
- **Server-Sent Events** - Comunicación tiempo real
- **FontAwesome** - Iconografía

## 📁 Estructura del Proyecto

```
ghvis/
├── src/main/java/com/drhdn/ghvis/
│   ├── config/          # Configuraciones (Security, WebClient, OAuth2)
│   ├── controller/      # Controladores REST y vistas
│   ├── service/         # Lógica de negocio
│   ├── model/          # Modelos de datos
│   ├── entity/         # Entidades JPA
│   └── repository/     # Repositorios de datos
├── src/main/resources/
│   ├── templates/      # Plantillas Thymeleaf
│   ├── static/         # Recursos estáticos (CSS, JS)
│   └── application*.properties  # Configuraciones
└── target/             # Artefactos compilados
```

## 🔧 Configuración Avanzada

### Perfiles de Ejecución

```bash
# Desarrollo (logging verbose)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Producción (logging mínimo)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Configurar Webhooks GitHub

1. En tu repositorio GitHub: **Settings** → **Webhooks** → **Add webhook**
2. **Payload URL**: `http://tu-dominio.com/webhook`
3. **Content type**: `application/json`
4. **Secret**: Tu `GITHUB_WEBHOOK_SECRET`
5. **Events**: `Push`, `Pull requests`, `Issues`

### Variables de Entorno Completas

```bash
# OAuth2 GitHub (REQUERIDO)
GITHUB_CLIENT_ID=tu_client_id_aquí
GITHUB_CLIENT_SECRET=tu_client_secret_aquí

# Webhook GitHub (OPCIONAL)
GITHUB_WEBHOOK_SECRET=tu_webhook_secret_aquí

# Base de datos (OPCIONAL - usa H2 por defecto)
SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=password

# Configuración de red (OPCIONAL)
APP_WEBCLIENT_TIMEOUT_CONNECTION=10000
APP_WEBCLIENT_TIMEOUT_READ=30000
APP_WEBCLIENT_MAX_MEMORY_SIZE=2097152
```

## 🔒 Política de Seguridad

Esta aplicación implementa **múltiples capas de seguridad**:

### ✅ Operaciones de Solo Lectura
- **Validación automática** de todas las operaciones
- **Lista blanca estricta** de métodos permitidos
- **Prohibición total** de operaciones de escritura (POST, PUT, DELETE)

### ✅ Autenticación Robusta
- **OAuth2 Authorization Code Flow** completo
- **Refresh automático** de tokens
- **CSRF Protection** granular
- **Headers de seguridad** optimizados

### ✅ Validación de Webhooks
- **Firma HMAC-SHA256** para validar origen
- **Filtrado de eventos** relevantes únicamente
- **Rate limiting** automático
- **Logging de seguridad** completo

## 🐛 Resolución de Problemas

### Error: "Access Denied"
```bash
# Verificar variables de entorno
echo $GITHUB_CLIENT_ID
echo $GITHUB_CLIENT_SECRET

# Verificar configuración OAuth2 GitHub
curl -v http://localhost:8080/oauth2/authorization/github
```

### Error: "Connection refused"
```bash
# Verificar conectividad a GitHub API
curl -v https://api.github.com/repos/microsoft/vscode

# Verificar logs de aplicación
tail -f logs/application.log
```

### Error: "Webhook signature invalid"
```bash
# Verificar secret de webhook
echo $GITHUB_WEBHOOK_SECRET

# Verificar payload en logs
grep "Webhook recibido" logs/application.log
```

## 📈 Rendimiento y Escalabilidad

- **Conexión HTTP/2** para máximo throughput
- **Connection pooling** optimizado para GitHub API
- **Cache inteligente** de commits en H2
- **Compresión automática** de respuestas
- **Timeouts configurables** por entorno

## 🤝 Contribuir

1. **Fork** el proyecto
2. **Crear rama** para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. **Commit** tus cambios (`git commit -m 'Agregar nueva funcionalidad'`)
4. **Push** a la rama (`git push origin feature/nueva-funcionalidad`)
5. **Crear Pull Request**

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 🗺️ Roadmap — Visión 2.0

El flujo actual (OAuth2 → visualización 3D → export Markdown) es una base sólida. La dirección natural de evolución:

### Fase actual: Visualizador 3D + CV Técnico
- Visualización 3D de commits con Three.js ✅
- Análisis de tecnologías y contribuciones ✅
- Generación de CV técnico en Markdown ✅
- Exportación con fecha de primer commit y URLs ✅

### Fase 2 — CV Visual Generado por IA (próxima)
El script de análisis existente, combinado con un LLM local (Ollama) o vía API, puede generar:
- **Imagen de CV técnico** renderizada automáticamente (formato tarjeta LinkedIn / portfolio)
- **Dashboard interactivo** con métricas reales de contribución
- **Análisis narrativo** del perfil técnico del desarrollador
- **Comparativa de tecnologías** por año / proyecto

La arquitectura reactiva de Spring WebFlux hace este paso natural: el análisis ya está, solo hay que conectar la generación visual.

### Contribuciones bienvenidas
Ideas, PRs y Issues para la Fase 2 son especialmente bienvenidos.

## 📚 Documentación Técnica

La documentación de arquitectura, decisiones y guías de setup está en `.cursor/`:

- [**INDICE_MAESTRO.md**](.cursor/INDICE_MAESTRO.md) - Índice completo
- [**ARQUITECTURA_Y_BUENAS_PRACTICAS.md**](.cursor/ARQUITECTURA_Y_BUENAS_PRACTICAS.md) - Arquitectura completa
- [**IMPLEMENTACIONES_Y_ESTRATEGIAS.md**](.cursor/IMPLEMENTACIONES_Y_ESTRATEGIAS.md) - CQRS, Rate Limit, CV
- [**GUIAS_SETUP_Y_TESTING.md**](.cursor/GUIAS_SETUP_Y_TESTING.md) - Setup y testing

## 🆘 Soporte

- **Issues**: [GitHub Issues](https://github.com/drhiidden/git-stellar-prism/issues)
- **Discusiones**: [GitHub Discussions](https://github.com/drhiidden/git-stellar-prism/discussions)

---

⭐ **¡Si te gusta el proyecto, dale una estrella!** ⭐ 