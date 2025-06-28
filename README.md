# GitStellarPrism

![Project Banner](URL)

**GitStellarPrism** es una aplicación web innovadora que transforma la actividad de un repositorio de GitHub en una experiencia visual dinámica e interactiva. No solo permite ver la evolución del código como una obra de arte generativo, sino que también analiza las tecnologías utilizadas y genera resúmenes técnicos automáticos, ideales para portfolios de desarrolladores.

---

## ✨ Características Principales

Este proyecto combina la visualización de datos con el análisis de código para ofrecer una herramienta multifacética:

### 1. 🎨 **Visualizador de Actividad del Repositorio**
- **Línea de Tiempo Dinámica**: Visualiza el historial completo de commits como una constelación de nodos en 3D.
- **Eventos en Tiempo Real**: Escucha webhooks de GitHub para mostrar nuevos commits, Pull Requests y Issues a medida que ocurren, con animaciones y efectos visuales.
- **Interactividad Total**: Navega por el espacio 3D, haz zoom, y haz clic en cualquier nodo para obtener información detallada sobre un commit o evento.
- **Filtros Personalizados**: Filtra la visualización por autor, rama o tipo de evento para centrarte en lo que más te interesa.

### 2. 🔬 **Analizador de Tecnologías**
- **Detección Automática**: Analiza el contenido del repositorio para identificar los lenguajes de programación, frameworks y librerías utilizadas.
- **Visualización de Datos**: Presenta la distribución de lenguajes en un gráfico interactivo.
- **Explorador de Estructura**: Muestra una vista de árbol de la estructura de carpetas y archivos del proyecto.

### 3. 📄 **Generador de Resúmenes para Portfolio**
- **Resúmenes Inteligentes**: Genera automáticamente un resumen técnico del proyecto, incluyendo su propósito, tecnologías principales y métricas de calidad.
- **Editor Integrado**: Permite a los desarrolladores editar y personalizar el resumen generado para ajustarlo a sus necesidades.
- **Exportación Múltiple**: Exporta el resumen final en formatos como Markdown, HTML o PDF, listo para ser incluido en un CV, portfolio o perfil de LinkedIn.

---

## 🔐 Características de Seguridad

### Arquitectura de Seguridad Profesional

Esta aplicación implementa las mejores prácticas de seguridad siguiendo la documentación oficial de Spring Security WebFlux:

#### 🛡️ OAuth2 Integration
- **Autenticación OAuth2 con GitHub**: Configuración completa siguiendo el patrón Authorization Code Grant
- **Manejo Automático de Tokens**: Gestión automática de refresh tokens y expiración
- **Alcance de Permisos**: Configurado con permisos mínimos necesarios (`repo`, `user:email`, `read:user`, `read:org`)

#### 🌐 WebClient Integration
- **WebClient con OAuth2**: Configuración profesional con filtros de autenticación automática
- **Múltiples Clientes**: 
  - `githubWebClient`: Para llamadas autenticadas a GitHub API
  - `webClient`: Para uso general
  - `oAuth2WebClient`: Para otras APIs que requieran OAuth2
- **Manejo de Errores**: Sistema centralizado de manejo de errores con rate limiting y retry logic

#### 🔒 Security Configuration
- **CSRF Protection**: Configuración granular que protege formularios pero permite APIs REST
- **Headers de Seguridad**: HSTS, X-Content-Type-Options, y Frame-Options configurados
- **Autorización por Rutas**: Control granular de acceso a diferentes endpoints
- **Session Management**: Configuración optimizada para aplicaciones reactivas

#### ⚡ Performance & Reliability
- **Connection Pooling**: Configuración optimizada de timeouts y pools de conexión
- **Memory Management**: Límites configurables para responses grandes (2MB por defecto)
- **Logging Estructurado**: Logs de debug y error con información útil para monitoring

## 🏗️ Arquitectura de Beans

### Separación de Responsabilidades

La aplicación sigue una arquitectura limpia con separación clara de responsabilidades:

```
SecurityConfig
├── ReactiveOAuth2AuthorizedClientManager
├── WebClient Beans (githubWebClient, oAuth2WebClient, webClient)
├── Authentication Handlers (success/failure)
└── CSRF & Headers Configuration

WebClientConfig
├── Timeout Configuration
├── Connection Management
└── OAuth2 Integration

OAuth2ErrorConfig
├── Centralized Error Handling
├── Rate Limiting Management
└── Structured Logging
```

### Bean Management Best Practices

1. **@Qualifier Usage**: Diferenciación clara entre diferentes tipos de WebClient
2. **Configuration Properties**: Externalized configuration con valores por defecto sensatos
3. **Conditional Beans**: Configuración que se adapta al entorno (desarrollo/producción)
4. **Error Boundaries**: Manejo centralizado de errores con fallbacks apropiados

## 🚀 Configuración

### Variables de Entorno Requeridas

```bash
# OAuth2 Configuration
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# Optional: Fallback token for public APIs
GITHUB_API_TOKEN=your_github_token
```

### Propiedades de Configuración

```properties
# Security Configuration
app.security.success-url=/
app.security.logout-success-url=/

# WebClient Timeouts (milliseconds)
app.webclient.timeout.connection=10000
app.webclient.timeout.read=30000
app.webclient.timeout.write=30000
app.webclient.max-memory-size=2097152

# OAuth2 GitHub Configuration
spring.security.oauth2.client.registration.github.scope=repo,user:email,read:user,read:org
```

## 🔧 Componentes Principales

### SecurityConfig
Configuración principal de seguridad que incluye:
- OAuth2 authorization code flow
- CSRF protection granular
- Security headers optimization
- Route-based authorization

### WebClientConfig
Configuración de clientes HTTP con:
- OAuth2 automatic token injection
- Connection pooling optimization
- Error handling and retry logic
- Performance monitoring

### GithubService
Servicio optimizado para GitHub API con:
- Automatic OAuth2 authentication
- Rate limit handling
- Error recovery mechanisms
- Structured response mapping

## 🛠️ Instalación y Ejecución

### Prerrequisitos
- Java 21+
- Maven 3.9+
- Aplicación OAuth2 registrada en GitHub

### Pasos de Instalación

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd ghvis
```

2. **Configurar OAuth2 en GitHub**
   - Ir a GitHub Settings > Developer settings > OAuth Apps
   - Crear nueva OAuth App
   - Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`

3. **Configurar variables de entorno**
```bash
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
```

4. **Ejecutar la aplicación**
```bash
./mvnw spring-boot:run
```

5. **Acceder a la aplicación**
   - Navegador: `http://localhost:8080`
   - Console H2 (desarrollo): `http://localhost:8080/h2-console`

## 📊 Monitoring y Observabilidad

### Endpoints de Actuator
- `/actuator/health`: Estado de la aplicación
- `/actuator/info`: Información de la aplicación
- `/actuator/metrics`: Métricas de performance

### Logging Configuration
- Debug level para OAuth2 y security components
- Structured logging para análisis
- Performance monitoring para WebClient calls

## 🔍 Debugging y Troubleshooting

### Common Issues

1. **401 Unauthorized**: Verificar configuración OAuth2 y tokens
2. **Rate Limit Exceeded**: GitHub API tiene límites, considerar caché
3. **CORS Issues**: Verificar configuración de headers y origins

### Debug Configuration
```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web.reactive.function.client=DEBUG
logging.level.com.drhdn.ghvis=DEBUG
```

## 📚 Referencias

- [Spring Security WebFlux Documentation](https://docs.spring.io/spring-security/reference/reactive/oauth2/index.html)
- [Spring WebFlux Reference](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [GitHub OAuth2 Documentation](https://docs.github.com/en/developers/apps/building-oauth-apps)

## 🤝 Contribución

Este proyecto sigue las mejores prácticas de desarrollo con Spring Boot. Para contribuir:

1. Fork del repositorio
2. Crear feature branch
3. Seguir convenciones de código establecidas
4. Añadir tests apropiados
5. Crear Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles. 

---

**Nota**: Esta aplicación ha sido diseñada siguiendo las mejores prácticas de seguridad y arquitectura de Spring Boot WebFlux. Toda la configuración de seguridad está optimizada para entornos de producción con fallbacks apropiados para desarrollo. 