/**
 * Aplicación Principal GitStellarPrism
 * Arquitectura de Componentes Reactiva con WebFlux
 * Implementa principios SOLID, DRY y POO
 */

class GitStellarPrismApp {
    constructor() {
        this.components = new Map();
        this.services = new Map();
        this.stateManager = AppStateManager.getInstance();
        this.eventBus = EventBus.getInstance();
        this.router = Router.getInstance();
        this.realtimeService = ServiceFactory.getRealtime();
        
        this.isInitialized = false;
        this.currentPage = this.detectCurrentPage();
    }

    /**
     * Inicialización principal de la aplicación
     * SOLID: Single Responsibility - Solo inicializa la app
     */
    async init() {
        if (this.isInitialized) return;

        try {
            console.log('🚀 Inicializando GitStellarPrism...');
            
            // 1. Configurar interceptors globales
            this.setupGlobalInterceptors();
            
            // 2. Configurar enrutamiento
            this.setupRouting();
            
            // 3. Inicializar componentes base
            await this.initializeBaseComponents();
            
            // 4. Inicializar componentes específicos de página
            await this.initializePageComponents();
            
            // 5. Configurar conexión en tiempo real
            this.setupRealtimeConnection();
            
            // 6. Configurar manejo global de errores
            this.setupErrorHandling();
            
            this.isInitialized = true;
            console.log('✅ GitStellarPrism inicializada correctamente');
            
            // Emitir evento de aplicación lista
            this.eventBus.emit('app:ready');
            
        } catch (error) {
            console.error('❌ Error inicializando aplicación:', error);
            this.handleInitializationError(error);
        }
    }

    /**
     * Detecta la página actual basada en la URL
     */
    detectCurrentPage() {
        const path = window.location.pathname;
        
        if (path === '/' || path === '/index') return 'index';
        if (path.startsWith('/dashboard')) return 'dashboard';
        if (path.startsWith('/analysis')) return 'analysis';
        if (path.startsWith('/summary')) return 'summary';
        
        return 'unknown';
    }

    /**
     * Configura interceptors globales para APIs
     */
    setupGlobalInterceptors() {
        const githubService = ServiceFactory.getGithub();
        
        // Interceptor de autenticación
        githubService.addRequestInterceptor(async (url, options) => {
            // Agregar headers de autenticación si es necesario
            return {
                ...options,
                headers: {
                    ...options.headers,
                    'X-Requested-With': 'XMLHttpRequest'
                }
            };
        });

        // Interceptor de logging
        githubService.addResponseInterceptor(async (response) => {
            if (!response.ok) {
                console.warn(`API Warning: ${response.status} ${response.url}`);
            }
            return response;
        });
    }

    /**
     * Configura el sistema de enrutamiento
     */
    setupRouting() {
        // Registrar rutas
        this.router.addRoute('/', 'index');
        this.router.addRoute('/dashboard', 'dashboard');
        this.router.addRoute('/analysis', 'analysis');
        this.router.addRoute('/summary', 'summary');

        // Escuchar cambios de ruta
        this.eventBus.subscribe('route:change', (data) => {
            this.handleRouteChange(data);
        });
    }

    /**
     * Inicializa componentes base que están en todas las páginas
     */
    async initializeBaseComponents() {
        // Componente de notificaciones (siempre presente)
        const notificationContainer = document.createElement('div');
        notificationContainer.id = 'notifications-container';
        document.body.appendChild(notificationContainer);
        
        const notificationComponent = ComponentFactory.create('notification', '#notifications-container');
        await notificationComponent.init();
        this.components.set('notification', notificationComponent);

        // Componente de header (si existe)
        const headerElement = document.querySelector('.navbar.modern-navbar');
        if (headerElement) {
            const headerOptions = this.getHeaderOptions();
            const headerComponent = ComponentFactory.create('header', '.navbar.modern-navbar', headerOptions);
            await headerComponent.init();
            this.components.set('header', headerComponent);
        }
    }

    /**
     * Inicializa componentes específicos según la página actual
     */
    async initializePageComponents() {
        switch (this.currentPage) {
            case 'dashboard':
                await this.initializeDashboardComponents();
                break;
            case 'analysis':
                await this.initializeAnalysisComponents();
                break;
            case 'summary':
                await this.initializeSummaryComponents();
                break;
            case 'index':
                await this.initializeIndexComponents();
                break;
        }
    }

    /**
     * Inicializa componentes específicos del dashboard
     */
    async initializeDashboardComponents() {
        const dashboardContainer = document.querySelector('.container-fluid.p-4');
        if (dashboardContainer) {
            const dashboardComponent = ComponentFactory.create('dashboard', '.container-fluid.p-4');
            await dashboardComponent.init();
            this.components.set('dashboard', dashboardComponent);
        }
    }

    /**
     * Inicializa componentes específicos del análisis
     */
    async initializeAnalysisComponents() {
        // Componente de visualización 3D
        const visualizationContainer = document.querySelector('#visualization-container');
        if (visualizationContainer) {
            // Aquí se inicializaría el componente de visualización
            console.log('🎮 Inicializando componente de visualización...');
        }

        // Componente de timeline
        const timelineContainer = document.querySelector('#timeline-container');
        if (timelineContainer) {
            console.log('📈 Inicializando componente de timeline...');
        }
    }

    /**
     * Inicializa componentes del resumen
     */
    async initializeSummaryComponents() {
        console.log('📄 Inicializando componentes de resumen...');
    }

    /**
     * Inicializa componentes de la página de inicio
     */
    async initializeIndexComponents() {
        console.log('🏠 Inicializando componentes de inicio...');
    }

    /**
     * Obtiene las opciones del header basándose en atributos del template
     */
    getHeaderOptions() {
        const options = {};
        
        // Buscar en elementos del DOM o meta tags
        const breadcrumbElement = document.querySelector('[data-breadcrumb-title]');
        if (breadcrumbElement) {
            options.showBreadcrumb = true;
            options.breadcrumbTitle = breadcrumbElement.dataset.breadcrumbTitle;
            options.breadcrumbIcon = breadcrumbElement.dataset.breadcrumbIcon || 'fas fa-file';
        }

        const realtimeElement = document.querySelector('#realtime-status');
        if (realtimeElement) {
            options.showRealtimeStatus = true;
        }

        return options;
    }

    /**
     * Configura la conexión en tiempo real
     */
    setupRealtimeConnection() {
        if (this.currentPage === 'analysis' || this.currentPage === 'dashboard') {
            console.log('🔗 Estableciendo conexión en tiempo real...');
            this.realtimeService.connect('/stream/events');
        }
    }

    /**
     * Configura manejo global de errores
     */
    setupErrorHandling() {
        // Errores JavaScript no capturados
        window.addEventListener('error', (event) => {
            // Verificar si el error es relevante y no es null
            if (!event.error || 
                (event.error && event.error.message && event.error.message.includes('Script error')) ||
                event.filename && event.filename.includes('extensions/')) {
                // Ignorar errores de script cross-origin o extensiones del navegador
                return;
            }
            
            console.error('Error global JavaScript:', event.error);
            this.handleGlobalError('JavaScript Error', event.error);
        });

        // Promesas rechazadas no capturadas
        window.addEventListener('unhandledrejection', (event) => {
            // Verificar si la razón es relevante
            if (!event.reason) {
                return;
            }
            
            console.error('Promesa rechazada no capturada:', event.reason);
            this.handleGlobalError('Promise Rejection', event.reason);
        });

        // Errores de fetch
        this.eventBus.subscribe('api:error', (data) => {
            this.handleApiError(data.error, data.service);
        });
    }

    /**
     * Maneja cambios de ruta
     */
    handleRouteChange(data) {
        console.log(`🧭 Navegando a: ${data.path}`);
        
        // Aquí se puede implementar lógica de transiciones entre páginas
        // Por ahora, simplemente redirigimos
        if (data.path !== window.location.pathname) {
            window.location.href = data.path;
        }
    }

    /**
     * Maneja errores de inicialización
     */
    handleInitializationError(error) {
        const errorMessage = `Error crítico de inicialización: ${error.message}`;
        
        // Mostrar error en consola
        console.error(errorMessage, error);
        
        // Intentar mostrar notificación si el componente está disponible
        const notificationComponent = this.components.get('notification');
        if (notificationComponent) {
            notificationComponent.showError(errorMessage);
        } else {
            // Fallback a alert nativo
            alert(errorMessage);
        }
    }

    /**
     * Maneja errores globales de JavaScript
     */
    handleGlobalError(type, error) {
        const notificationComponent = this.components.get('notification');
        if (notificationComponent) {
            let errorMessage = type;
            
            if (error) {
                if (error.message) {
                    errorMessage += `: ${error.message}`;
                } else if (typeof error === 'string') {
                    errorMessage += `: ${error}`;
                } else {
                    errorMessage += `: Error desconocido`;
                }
            } else {
                errorMessage += ': Error sin detalles';
            }
            
            notificationComponent.showError(errorMessage);
        }
    }

    /**
     * Maneja errores de API
     */
    handleApiError(error, service) {
        const notificationComponent = this.components.get('notification');
        if (notificationComponent) {
            let message = `Error en ${service}`;
            
            if (error.status) {
                message += ` (${error.status})`;
            }
            
            if (error.message) {
                message += `: ${error.message}`;
            }
            
            notificationComponent.showError(message);
        }
    }

    /**
     * Método para obtener un componente por nombre
     */
    getComponent(name) {
        return this.components.get(name);
    }

    /**
     * Método para obtener el estado global
     */
    getState() {
        return this.stateManager.getState();
    }

    /**
     * Método para actualizar el estado global
     */
    setState(updates) {
        this.stateManager.setState(updates);
    }

    /**
     * Limpieza al cerrar la aplicación
     */
    destroy() {
        console.log('🧹 Limpiando aplicación...');
        
        // Destruir todos los componentes
        this.components.forEach(component => {
            if (component.destroy) {
                component.destroy();
            }
        });
        
        // Desconectar tiempo real
        this.realtimeService.disconnect();
        
        this.isInitialized = false;
    }
}

/**
 * Función de inicialización global
 * Se ejecuta cuando el DOM está listo
 */
function initializeApp() {
    // Verificar que todos los sistemas estén cargados
    if (typeof BaseComponent === 'undefined' || 
        typeof ServiceFactory === 'undefined' ||
        typeof ComponentFactory === 'undefined') {
        console.error('❌ Sistemas base no cargados. Reintentando en 500ms...');
        setTimeout(initializeApp, 500);
        return;
    }

    // Crear e inicializar la aplicación
    window.GitStellarPrism = new GitStellarPrismApp();
    window.GitStellarPrism.init();
}

// Auto-inicialización cuando el DOM esté listo
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeApp);
} else {
    // DOM ya está listo
    initializeApp();
}

// Limpieza antes de cerrar/recargar la página
window.addEventListener('beforeunload', () => {
    if (window.GitStellarPrism) {
        window.GitStellarPrism.destroy();
    }
});

console.log('🎯 Sistema de aplicación GitStellarPrism cargado'); 