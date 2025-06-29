/**
 * Sistema de Componentes Frontend - Arquitectura Reactiva
 * Implementa principios SOLID, DRY y POO para el frontend
 * Inspirado en Angular/React pero usando WebFlux como backend
 */

/**
 * Clase base abstracta para todos los componentes
 * SOLID: Single Responsibility - Maneja el ciclo de vida base
 */
class BaseComponent {
    constructor(selector, options = {}) {
        this.selector = selector;
        this.element = document.querySelector(selector);
        this.options = { ...this.getDefaultOptions(), ...options };
        this.state = new Proxy({}, {
            set: (target, property, value) => {
                const oldValue = target[property];
                target[property] = value;
                this.onStateChange(property, value, oldValue);
                return true;
            }
        });
        this.eventBus = EventBus.getInstance();
        this.isInitialized = false;
        this.isDestroyed = false;
    }

    // Método Template - Define el ciclo de vida
    async init() {
        if (this.isInitialized) return;
        
        try {
            await this.beforeInit();
            await this.render();
            this.bindEvents();
            await this.afterInit();
            this.isInitialized = true;
            console.log(`✅ Componente ${this.constructor.name} inicializado`);
        } catch (error) {
            console.error(`❌ Error inicializando ${this.constructor.name}:`, error);
            throw error;
        }
    }

    // Métodos abstractos - deben ser implementados por subclases
    getDefaultOptions() { return {}; }
    async beforeInit() {}
    async render() { 
        throw new Error(`${this.constructor.name} debe implementar render()`);
    }
    bindEvents() {}
    async afterInit() {}

    // Gestión de estado reactivo
    setState(newState) {
        Object.assign(this.state, newState);
    }

    onStateChange(property, newValue, oldValue) {
        // Trigger re-render si es necesario
        if (this.shouldUpdate(property, newValue, oldValue)) {
            this.update();
        }
    }

    shouldUpdate(property, newValue, oldValue) {
        return newValue !== oldValue;
    }

    async update() {
        if (!this.isInitialized) return;
        await this.render();
    }

    // Gestión de eventos
    emit(eventName, data = {}) {
        this.eventBus.emit(eventName, { 
            source: this.constructor.name, 
            data 
        });
    }

    subscribe(eventName, callback) {
        return this.eventBus.subscribe(eventName, callback);
    }

    // Cleanup
    destroy() {
        if (this.isDestroyed) return;
        
        this.beforeDestroy();
        this.eventBus.unsubscribeAll(this);
        this.isDestroyed = true;
        console.log(`🗑️ Componente ${this.constructor.name} destruido`);
    }

    beforeDestroy() {}
}

/**
 * Event Bus singleton para comunicación entre componentes
 * SOLID: Single Responsibility - Solo maneja eventos
 */
class EventBus {
    static instance = null;

    constructor() {
        if (EventBus.instance) {
            return EventBus.instance;
        }
        this.events = {};
        EventBus.instance = this;
    }

    static getInstance() {
        if (!EventBus.instance) {
            EventBus.instance = new EventBus();
        }
        return EventBus.instance;
    }

    emit(eventName, data) {
        if (!this.events[eventName]) return;
        
        this.events[eventName].forEach(callback => {
            try {
                callback(data);
            } catch (error) {
                console.error(`Error en evento ${eventName}:`, error);
            }
        });
    }

    subscribe(eventName, callback) {
        if (!this.events[eventName]) {
            this.events[eventName] = [];
        }
        this.events[eventName].push(callback);
        
        // Retorna función de unsubscribe
        return () => this.unsubscribe(eventName, callback);
    }

    unsubscribe(eventName, callback) {
        if (!this.events[eventName]) return;
        
        const index = this.events[eventName].indexOf(callback);
        if (index > -1) {
            this.events[eventName].splice(index, 1);
        }
    }

    unsubscribeAll(component) {
        // Cleanup automático cuando un componente se destruye
        Object.keys(this.events).forEach(eventName => {
            this.events[eventName] = this.events[eventName].filter(
                callback => !callback.component || callback.component !== component
            );
        });
    }
}

/**
 * Sistema de routing client-side
 * SOLID: Single Responsibility - Solo maneja navegación
 */
class Router {
    static instance = null;

    constructor() {
        if (Router.instance) return Router.instance;
        
        this.routes = new Map();
        this.currentRoute = null;
        this.eventBus = EventBus.getInstance();
        Router.instance = this;
    }

    static getInstance() {
        if (!Router.instance) {
            Router.instance = new Router();
        }
        return Router.instance;
    }

    addRoute(path, component, options = {}) {
        this.routes.set(path, { component, options });
    }

    navigate(path, data = {}) {
        const route = this.routes.get(path);
        if (!route) {
            console.warn(`Ruta no encontrada: ${path}`);
            return;
        }

        this.currentRoute = path;
        this.eventBus.emit('route:change', { path, data, route });
    }

    getCurrentRoute() {
        return this.currentRoute;
    }
}

/**
 * Factory para crear componentes
 * SOLID: Open/Closed - Extensible sin modificar código existente
 */
class ComponentFactory {
    static components = new Map();

    static register(name, componentClass) {
        if (!componentClass.prototype instanceof BaseComponent) {
            throw new Error(`${name} debe extender BaseComponent`);
        }
        this.components.set(name, componentClass);
    }

    static create(name, selector, options = {}) {
        const ComponentClass = this.components.get(name);
        if (!ComponentClass) {
            throw new Error(`Componente no registrado: ${name}`);
        }
        return new ComponentClass(selector, options);
    }

    static getRegistered() {
        return Array.from(this.components.keys());
    }
}

/**
 * Gestor de estado global de la aplicación
 * Patrón Singleton + Observer
 */
class AppStateManager {
    static instance = null;

    constructor() {
        if (AppStateManager.instance) return AppStateManager.instance;
        
        this.state = {
            user: null,
            currentRepo: null,
            repositories: [],
            isLoading: false,
            notifications: [],
            theme: 'light'
        };
        
        this.subscribers = new Map();
        this.eventBus = EventBus.getInstance();
        AppStateManager.instance = this;
    }

    static getInstance() {
        if (!AppStateManager.instance) {
            AppStateManager.instance = new AppStateManager();
        }
        return AppStateManager.instance;
    }

    // Getter inmutable del estado
    getState() {
        return { ...this.state };
    }

    // Setter que notifica cambios
    setState(updates) {
        const oldState = { ...this.state };
        Object.assign(this.state, updates);
        
        // Notificar a subscribers específicos
        Object.keys(updates).forEach(key => {
            if (this.subscribers.has(key)) {
                this.subscribers.get(key).forEach(callback => {
                    callback(this.state[key], oldState[key]);
                });
            }
        });

        // Emitir evento global
        this.eventBus.emit('state:change', { 
            updates, 
            newState: this.getState(), 
            oldState 
        });
    }

    // Suscribirse a cambios de una propiedad específica
    subscribe(property, callback) {
        if (!this.subscribers.has(property)) {
            this.subscribers.set(property, []);
        }
        this.subscribers.get(property).push(callback);

        // Retornar función de cleanup
        return () => {
            const callbacks = this.subscribers.get(property);
            const index = callbacks.indexOf(callback);
            if (index > -1) callbacks.splice(index, 1);
        };
    }
}

// Exportar para uso global
window.BaseComponent = BaseComponent;
window.EventBus = EventBus;
window.Router = Router;
window.ComponentFactory = ComponentFactory;
window.AppStateManager = AppStateManager;

// Inicialización automática
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Sistema de Componentes inicializado');
}); 