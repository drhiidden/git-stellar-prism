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

## 🛠️ Stack Tecnológico

El proyecto está construido sobre una arquitectura moderna y reactiva, utilizando las siguientes tecnologías:

| Capa      | Tecnología                                                              | Propósito                                             |
|-----------|-------------------------------------------------------------------------|-------------------------------------------------------|
| **Backend** | [**Spring Boot 3**](https://spring.io/projects/spring-boot) con [**WebFlux**](https://docs.spring.io/spring-framework/reference/web/webflux.html) | Servidor reactivo de alto rendimiento y bajo consumo.   |
| **Frontend**| [**Thymeleaf**](https://www.thymeleaf.org/)                             | Motor de plantillas para renderizar el HTML inicial.  |
|           | [**Three.js**](https://threejs.org/)                                    | Librería 3D para la visualización generativa.         |
|           | [**Chart.js**](https://www.chartjs.org/)                                | Creación de gráficos para el análisis de tecnologías. |
|           | [**Bootstrap 5**](https://getbootstrap.com/)                            | Framework CSS para una interfaz de usuario moderna.   |
| **Comunicación**| **Server-Sent Events (SSE)**                                            | Para la transmisión de eventos en tiempo real.        |
| **Base de Datos**| [**H2 Database**](https://www.h2database.com/html/main.html)      | Base de datos en memoria para almacenamiento temporal.|
| **Build**   | [**Maven**](https://maven.apache.org/)                                  | Gestión de dependencias y construcción del proyecto.  |

---

## 🚀 Cómo Empezar

Sigue estos pasos para poner en marcha el proyecto en tu entorno local.

### Prerrequisitos

- **JDK 21** o superior.
- **Maven 3.8** o superior.
- Un token de acceso personal de GitHub con permisos de `repo` (si quieres analizar repositorios privados).

### Instalación y Ejecución

1.  **Clona el repositorio:**
    ```bash
    git clone https://github.com/tu-usuario/ghvis.git
    cd ghvis
    ```

2.  **(Opcional) Configura tu token de GitHub:**
    Abre el archivo `src/main/resources/application.properties` y añade la siguiente línea con tu token:
    ```properties
    github.api.token=ghp_XXXXXXXXXXXXXXXXXXXX
    ```

3.  **Construye el proyecto con Maven:**
    ```bash
    mvn clean install
    ```

4.  **Ejecuta la aplicación:**
    ```bash
    mvn spring-boot:run
    ```

5.  **Abre tu navegador:**
    Accede a `http://localhost:8080` para ver la aplicación en funcionamiento.

---

## 🏗️ Estructura del Proyecto

La estructura sigue las convenciones de un proyecto Spring Boot estándar, separando claramente las responsabilidades:

```
/src
 ├── /main
 │   ├── /java/com/drhdn/ghvis/
 │   │    ├── controller/  # Endpoints REST y Web
 │   │    ├── service/     # Lógica de negocio
 │   │    ├── model/       # Entidades de datos
 │   │    ├── config/      # Configuración de la aplicación
 │   │    └── util/        # Clases de utilidad
 │   └── /resources/
 │        ├── /templates/  # Plantillas Thymeleaf (HTML)
 │        ├── /static/     # Archivos estáticos (CSS, JS)
 │        └── application.properties
 └── /test
      └── ...
```

---

## 🗺️ Roadmap y Futuras Mejoras

- [ ] **Integración con GitLab y Bitbucket.**
- [ ] **Análisis de calidad de código más profundo** (complejidad ciclomática, duplicación de código).
- [ ] **Integración con un LLM (Modelo de Lenguaje Grande)** para generar descripciones de proyectos más ricas y sugerir mejoras.
- [ ] **Más opciones de personalización visual** (temas, colores, formas de nodos).
- [ ] **Sincronización con audio** para una experiencia audiovisual completa.

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Si tienes ideas para mejorar el proyecto, por favor, abre un *issue* para discutirlo o envía un *pull request* directamente.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles. 