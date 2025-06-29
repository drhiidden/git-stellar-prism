/**
 * Sistema de Servicios API - Aprovechando WebFlux Reactivo
 * Implementa principios SOLID y DRY para API calls
 */

/**
 * Clase base abstracta para servicios API
 * SOLID: Single Responsibility - Solo maneja comunicación HTTP
 */
class BaseApiService {
    constructor(baseUrl = '/api') {
        this.baseUrl = baseUrl;
        this.eventBus = EventBus.getInstance();
        this.interceptors = {
            request: [],
            response: []
        };
    }

    // Interceptors para request/response
    addRequestInterceptor(interceptor) {
        this.interceptors.request.push(interceptor);
    }

    addResponseInterceptor(interceptor) {
        this.interceptors.response.push(interceptor);
    }

    // Método base para hacer requests
    async request(url, options = {}) {
        const fullUrl = url.startsWith('http') ? url : `${this.baseUrl}${url}`;
        
        // Aplicar interceptors de request
        let finalOptions = { ...options };
        for (const interceptor of this.interceptors.request) {
            finalOptions = await interceptor(fullUrl, finalOptions);
        }

        try {
            let response = await fetch(fullUrl, {
                headers: {
                    'Content-Type': 'application/json',
                    ...finalOptions.headers
                },
                ...finalOptions
            });

            // Aplicar interceptors de response
            for (const interceptor of this.interceptors.response) {
                response = await interceptor(response);
            }

            if (!response.ok) {
                throw new HttpError(response.status, response.statusText, await response.text());
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            
            return await response.text();
        } catch (error) {
            this.handleError(error);
            throw error;
        }
    }

    handleError(error) {
        console.error(`API Error in ${this.constructor.name}:`, error);
        this.eventBus.emit('api:error', { error, service: this.constructor.name });
    }

    // Métodos HTTP convenience
    get(url, options = {}) {
        return this.request(url, { ...options, method: 'GET' });
    }

    post(url, data, options = {}) {
        return this.request(url, {
            ...options,
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    put(url, data, options = {}) {
        return this.request(url, {
            ...options,
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    delete(url, options = {}) {
        return this.request(url, { ...options, method: 'DELETE' });
    }
}

/**
 * Error personalizado para HTTP
 */
class HttpError extends Error {
    constructor(status, statusText, body) {
        super(`HTTP ${status}: ${statusText}`);
        this.status = status;
        this.statusText = statusText;
        this.body = body;
    }
}

/**
 * Servicio específico para GitHub API
 * SOLID: Single Responsibility - Solo maneja GitHub
 */
class GithubApiService extends BaseApiService {
    constructor() {
        super('/api');
        this.setupInterceptors();
    }

    setupInterceptors() {
        // Request interceptor para logging
        this.addRequestInterceptor(async (url, options) => {
            console.log(`🔄 GitHub API Request: ${options.method || 'GET'} ${url}`);
            return options;
        });

        // Response interceptor para rate limiting
        this.addResponseInterceptor(async (response) => {
            const remaining = response.headers.get('X-RateLimit-Remaining');
            if (remaining) {
                this.eventBus.emit('github:rateLimit', { remaining: parseInt(remaining) });
            }
            return response;
        });
    }

    // Métodos específicos de GitHub
    async getUserInfo() {
        return this.get('/user/info');
    }

    async getUserRepositories(useCache = true) {
        const url = useCache ? '/user/repositories' : '/user/repositories/refresh';
        return this.get(url);
    }

    async getUserRepositoriesDetailed() {
        return this.get('/user/repositories/detailed');
    }

    async getRepositoryCommits(owner, repo) {
        return this.get(`/repos/${owner}/${repo}/commits`);
    }

    async getRepositoryStructure(owner, repo) {
        return this.get(`/repos/${owner}/${repo}/structure`);
    }

    async analyzeRepository(owner, repo) {
        return this.post('/analysis/analyze', { owner, repo });
    }

    async exportAnalysis(owner, repo, type = 'all') {
        return this.get(`/cache/export/${owner}/${repo}?type=${type}`);
    }
}

/**
 * Servicio para manejo de caché
 * SOLID: Single Responsibility - Solo maneja caché
 */
class CacheApiService extends BaseApiService {
    constructor() {
        super('/api/cache');
    }

    async getStats() {
        return this.get('/stats');
    }

    async clearUserCache() {
        return this.delete('/user');
    }

    async clearRepositoryCache(owner, repo) {
        return this.delete(`/repo/${owner}/${repo}`);
    }

    async refreshCache() {
        return this.post('/refresh', {});
    }
}

/**
 * Servicio para manejo de eventos en tiempo real
 * Aprovecha WebFlux para SSE (Server-Sent Events)
 */
class RealtimeService {
    constructor() {
        this.eventSource = null;
        this.eventBus = EventBus.getInstance();
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.lastRepoParam = null;
    }

    connect(repoParam = null) {
        if (this.eventSource) {
            this.disconnect();
        }

        // El endpoint requiere el parámetro repo obligatoriamente
        if (!repoParam) {
            console.warn('⚠️ Parámetro repo requerido para conexión tiempo real');
            this.eventBus.emit('realtime:error', { message: 'Repositorio requerido' });
            return;
        }

        this.lastRepoParam = repoParam;
        const endpoint = `/api/stream/events?repo=${encodeURIComponent(repoParam)}`;
        
        console.log(`🔗 Intentando conectar a tiempo real: ${endpoint}`);

        try {
            this.eventSource = new EventSource(endpoint);
            
            this.eventSource.onopen = (event) => {
                console.log(`🔗 Conexión tiempo real establecida para: ${repoParam}`);
                this.isConnected = true;
                this.reconnectAttempts = 0;
                this.eventBus.emit('realtime:connected');
            };

            this.eventSource.onmessage = (event) => {
                console.log('📡 Mensaje SSE recibido:', event);
                console.log('📡 Datos raw:', event.data);
                console.log('📡 Tipo de evento:', event.type);
                
                try {
                    const data = JSON.parse(event.data);
                    console.log('📡 Evento tiempo real parseado:', data);
                    this.handleRealtimeEvent(data);
                } catch (error) {
                    console.error('Error parseando evento tiempo real:', error);
                    console.error('Datos que causaron error:', event.data);
                }
            };

            this.eventSource.onerror = (event) => {
                console.error('❌ Error en conexión tiempo real:', event);
                console.error('ReadyState:', this.eventSource?.readyState);
                this.isConnected = false;
                this.eventBus.emit('realtime:error', event);
                
                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.scheduleReconnect();
                } else {
                    console.error('❌ Máximo de intentos de reconexión alcanzado');
                }
            };

        } catch (error) {
            console.error('Error iniciando conexión tiempo real:', error);
            this.eventBus.emit('realtime:error', error);
        }
    }

    handleRealtimeEvent(data) {
        const { type, payload } = data;
        
        switch (type) {
            case 'CONNECTION_ESTABLISHED':
                console.log('✅ Conexión de tiempo real confirmada por servidor');
                this.eventBus.emit('realtime:confirmed', data);
                break;
            case 'HEARTBEAT':
                console.log('💓 Heartbeat recibido');
                this.eventBus.emit('realtime:heartbeat', data);
                break;
            case 'TEST_EVENT':
                console.log('🧪 Evento de prueba recibido:', data);
                this.eventBus.emit('realtime:test', data);
                break;
            case 'COMMIT_ANALYZED':
                this.eventBus.emit('analysis:commit', payload || data.payload);
                break;
            case 'REPOSITORY_UPDATED':
                this.eventBus.emit('repository:updated', payload || data.payload);
                break;
            case 'CACHE_INVALIDATED':
                this.eventBus.emit('cache:invalidated', payload || data.payload);
                break;
            default:
                console.log('📡 Evento tiempo real:', { type, data });
                this.eventBus.emit('realtime:event', { type, payload: payload || data.payload });
        }
    }

    scheduleReconnect() {
        this.reconnectAttempts++;
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
        
        console.log(`🔄 Reconectando en ${delay}ms (intento ${this.reconnectAttempts})`);
        
        setTimeout(() => {
            if (!this.isConnected) {
                this.connect(this.lastRepoParam);
            }
        }, delay);
    }

    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        this.isConnected = false;
        this.eventBus.emit('realtime:disconnected');
    }

    isConnected() {
        return this.isConnected;
    }

    getConnectionStatus() {
        return this.isConnected;
    }
}

/**
 * Factory de servicios - Patrón Singleton para cada servicio
 * SOLID: Dependency Inversion - Depende de abstracciones
 */
class ServiceFactory {
    static services = new Map();

    static register(name, serviceClass) {
        this.services.set(name, serviceClass);
    }

    static getInstance(name) {
        if (!this.services.has(name)) {
            throw new Error(`Servicio no registrado: ${name}`);
        }

        const ServiceClass = this.services.get(name);
        const instanceKey = `${name}_instance`;
        
        if (!this[instanceKey]) {
            this[instanceKey] = new ServiceClass();
        }
        
        return this[instanceKey];
    }

    static getGithub() {
        return this.getInstance('github');
    }

    static getCache() {
        return this.getInstance('cache');
    }

    static getRealtime() {
        return this.getInstance('realtime');
    }
}

// Registro automático de servicios
ServiceFactory.register('github', GithubApiService);
ServiceFactory.register('cache', CacheApiService);
ServiceFactory.register('realtime', RealtimeService);

// Exportar para uso global
window.BaseApiService = BaseApiService;
window.GithubApiService = GithubApiService;
window.CacheApiService = CacheApiService;
window.RealtimeService = RealtimeService;
window.ServiceFactory = ServiceFactory;
window.HttpError = HttpError;

console.log('🔧 Sistema de Servicios API cargado'); 