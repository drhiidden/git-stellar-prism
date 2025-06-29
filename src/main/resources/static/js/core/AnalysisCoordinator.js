/**
 * AnalysisCoordinator - Coordinador principal para la vista de análisis
 * SOLID: Single Responsibility - Coordina componentes de análisis
 * DRY: Centraliza la lógica de coordinación
 * Patrón Mediator: Facilita comunicación entre componentes
 */

class AnalysisCoordinator {
    constructor() {
        this.eventBus = EventBus.getInstance();
        this.timelineService = TimelineService.getInstance();
        this.visualizationService = VisualizationService.getInstance();
        
        this.components = new Map();
        this.currentView = 'timeline'; // 'timeline' | '3d'
        this.repository = null;
        this.commits = [];
        
        this.isInitialized = false;
        this.subscriptions = [];
    }

    /**
     * Inicializa el coordinador de análisis
     */
    async initialize(repository) {
        if (this.isInitialized) return;

        this.repository = repository;
        console.log(`🎯 Inicializando análisis para: ${repository}`);

        try {
            await this.initializeComponents();
            this.setupEventCoordination();
            await this.loadRepositoryData();
            
            this.isInitialized = true;
            this.emit('coordinator:initialized', { repository });
            
        } catch (error) {
            console.error('❌ Error inicializando coordinador:', error);
            throw error;
        }
    }

    /**
     * Inicializa los componentes necesarios
     */
    async initializeComponents() {
        // Timeline Component
        const timelineElement = document.getElementById('timeline-container-full');
        if (timelineElement) {
            const timelineComponent = new TimelineComponent('#timeline-container-full', {
                showHeader: true,
                showPagination: true,
                showFilters: true,
                autoOptimize: true
            });
            
            await timelineComponent.init();
            this.components.set('timeline', timelineComponent);
            console.log('✅ TimelineComponent inicializado');
        }

        // Visualization Component
        const visualizationElement = document.getElementById('visualization-3d-container');
        if (visualizationElement) {
            const visualizationComponent = new VisualizationComponent('#visualization-3d-container', {
                containerId: 'three-canvas-container',
                showControls: true,
                showStats: true,
                enableInteraction: true
            });
            
            await visualizationComponent.init();
            this.components.set('visualization', visualizationComponent);
            console.log('✅ VisualizationComponent inicializado');
        }
    }

    /**
     * Configura la coordinación de eventos entre componentes
     */
    setupEventCoordination() {
        // Comunicación Timeline <-> Visualization
        this.subscriptions.push(
            // Cuando se selecciona un commit en timeline, resaltarlo en 3D
            this.eventBus.subscribe('timeline:commit:selected', (data) => {
                const visualizationComponent = this.components.get('visualization');
                if (visualizationComponent && this.currentView === '3d') {
                    visualizationComponent.highlightCommit(data.commit.hash);
                }
                this.emit('commit:selected', data);
            }),

            // Cuando se hace click en un commit en 3D, sincronizar con timeline
            this.eventBus.subscribe('visualization:commit:clicked', (data) => {
                const timelineComponent = this.components.get('timeline');
                if (timelineComponent) {
                    // Buscar la página que contiene este commit
                    this.navigateToCommit(data.commit.hash);
                }
                this.emit('commit:selected', data);
            }),

            // Sincronizar datos cuando se actualizan
            this.eventBus.subscribe('commits:loaded', (data) => {
                this.handleCommitsUpdate(data.commits);
            })
        );

        // Navegación entre vistas
        this.setupViewNavigation();
    }

    /**
     * Configura la navegación entre vistas
     */
    setupViewNavigation() {
        // Botones de navegación
        document.addEventListener('click', (e) => {
            if (e.target.matches('[data-view]')) {
                const view = e.target.dataset.view;
                this.switchView(view);
            }
        });
    }

    /**
     * Cambia entre vistas (timeline/3d)
     */
    async switchView(view) {
        if (view === this.currentView) return;

        const previousView = this.currentView;
        this.currentView = view;

        // Ocultar vista anterior
        this.hideView(previousView);
        
        // Mostrar nueva vista
        await this.showView(view);
        
        // Actualizar UI
        this.updateViewControls();
        
        this.emit('view:changed', { 
            from: previousView, 
            to: view,
            repository: this.repository 
        });

        console.log(`🔄 Vista cambiada: ${previousView} → ${view}`);
    }

    /**
     * Muestra una vista específica
     */
    async showView(view) {
        const viewElement = document.getElementById(`view-${view}`);
        if (!viewElement) return;

        // Activar vista
        viewElement.classList.add('active');
        viewElement.style.display = 'flex';

        // Inicializar componente si es necesario
        const component = this.components.get(view);
        if (component && this.commits.length > 0) {
            component.setCommits(this.commits);
        }

        // Optimizaciones específicas por vista
        if (view === 'timeline') {
            this.timelineService.optimizeCommitsPerPage();
        }
    }

    /**
     * Oculta una vista específica
     */
    hideView(view) {
        const viewElement = document.getElementById(`view-${view}`);
        if (!viewElement) return;

        viewElement.classList.remove('active');
        viewElement.style.display = 'none';
    }

    /**
     * Actualiza controles de navegación de vistas
     */
    updateViewControls() {
        document.querySelectorAll('[data-view]').forEach(btn => {
            const view = btn.dataset.view;
            btn.classList.toggle('active', view === this.currentView);
        });
    }

    /**
     * Carga datos del repositorio
     */
    async loadRepositoryData() {
        if (!this.repository) return;

        try {
            console.log(`📡 Cargando datos para: ${this.repository}`);
            
            // Usar el servicio API existente
            const response = await fetch(`/api/repositories/${encodeURIComponent(this.repository)}/commits`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const commits = await response.json();
            await this.handleCommitsUpdate(commits);
            
            console.log(`✅ ${commits.length} commits cargados`);
            
        } catch (error) {
            console.error('❌ Error cargando datos del repositorio:', error);
            this.handleLoadError(error);
        }
    }

    /**
     * Maneja actualización de commits
     */
    async handleCommitsUpdate(commits) {
        if (!commits || commits.length === 0) {
            console.warn('⚠️ No hay commits para procesar');
            return;
        }

        this.commits = commits;

        // Actualizar servicios
        this.timelineService.setCommits(commits);

        // Actualizar componentes activos
        const activeComponent = this.components.get(this.currentView);
        if (activeComponent) {
            activeComponent.setCommits(commits);
        }

        this.emit('commits:updated', { 
            count: commits.length,
            repository: this.repository 
        });
    }

    /**
     * Navega al commit específico en el timeline
     */
    navigateToCommit(commitHash) {
        const commitIndex = this.commits.findIndex(c => c.hash === commitHash);
        if (commitIndex === -1) return;

        const commitsPerPage = this.timelineService.commitsPerPage;
        const targetPage = Math.ceil((commitIndex + 1) / commitsPerPage);
        
        this.timelineService.goToPage(targetPage);
        
        // Cambiar a vista timeline si no está activa
        if (this.currentView !== 'timeline') {
            this.switchView('timeline');
        }
    }

    /**
     * Maneja errores de carga
     */
    handleLoadError(error) {
        const message = `Error cargando repositorio: ${error.message}`;
        
        // Mostrar notificación si existe el sistema
        if (window.showNotification) {
            window.showNotification(message, 'error');
        } else {
            console.error(message);
        }
        
        this.emit('load:error', { error, repository: this.repository });
    }

    /**
     * Obtiene estadísticas del repositorio
     */
    getRepositoryStats() {
        if (!this.commits.length) return null;

        const stats = {
            totalCommits: this.commits.length,
            authors: new Set(this.commits.map(c => c.author)).size,
            branches: new Set(this.commits.map(c => c.branch || 'main')).size,
            dateRange: {
                oldest: Math.min(...this.commits.map(c => new Date(c.timestamp))),
                newest: Math.max(...this.commits.map(c => new Date(c.timestamp)))
            }
        };

        return stats;
    }

    /**
     * Aplica filtros globales
     */
    applyFilters(filters) {
        this.timelineService.applyFilters(filters);
        this.emit('filters:applied', { filters });
    }

    /**
     * Exporta datos para análisis externo
     */
    exportData(format = 'json') {
        const data = {
            repository: this.repository,
            commits: this.commits,
            stats: this.getRepositoryStats(),
            exportDate: new Date().toISOString()
        };

        if (format === 'json') {
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `${this.repository.replace('/', '_')}_analysis.json`;
            a.click();
            URL.revokeObjectURL(url);
        }
    }

    /**
     * Limpia recursos y suscripciones
     */
    destroy() {
        this.subscriptions.forEach(unsubscribe => unsubscribe?.());
        this.subscriptions = [];
        
        this.components.forEach(component => component.destroy?.());
        this.components.clear();
        
        this.isInitialized = false;
        console.log('🗑️ AnalysisCoordinator destruido');
    }

    /**
     * Emite eventos
     */
    emit(eventName, data) {
        this.eventBus.emit(`analysis:${eventName}`, data);
    }

    /**
     * Factory method para crear instancia singleton
     */
    static getInstance() {
        if (!AnalysisCoordinator.instance) {
            AnalysisCoordinator.instance = new AnalysisCoordinator();
        }
        return AnalysisCoordinator.instance;
    }
}

// Registrar en el sistema global
window.AnalysisCoordinator = AnalysisCoordinator; 