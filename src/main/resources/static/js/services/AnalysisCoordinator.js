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
     * Inicializa los componentes necesarios de forma simple
     */
    async initializeComponents() {
        // Inicializar Timeline si existe el contenedor
        const timelineContainer = document.getElementById('timeline-container-full');
        if (timelineContainer) {
            console.log('📊 Inicializando Timeline...');
            this.components.set('timeline', { 
                element: timelineContainer,
                type: 'timeline',
                active: true 
            });
        }

        // Inicializar Visualización 3D si existe el contenedor
        const visualizationContainer = document.getElementById('visualization-3d-container');
        if (visualizationContainer) {
            console.log('🎮 Inicializando Visualización 3D...');
            this.components.set('visualization', { 
                element: visualizationContainer,
                type: 'visualization',
                active: false 
            });
        }
    }

    /**
     * Carga y procesa datos del repositorio
     */
    async loadRepositoryData() {
        if (!this.repository) return;

        try {
            console.log(`📡 Cargando commits para: ${this.repository}`);
            
            // Intentar obtener commits desde el sistema existente
            if (window.commits && window.commits.length > 0) {
                await this.processCommits(window.commits);
                return;
            }

            // Fallback: cargar desde API
            const response = await fetch(`/api/repositories/${encodeURIComponent(this.repository)}/commits`);
            if (response.ok) {
                const commits = await response.json();
                await this.processCommits(commits);
            } else {
                console.warn('⚠️ No se pudieron cargar commits desde API');
            }
            
        } catch (error) {
            console.error('❌ Error cargando datos:', error);
        }
    }

    /**
     * Procesa commits de forma simplificada
     */
    async processCommits(commits) {
        if (!commits || commits.length === 0) return;

        this.commits = commits;
        console.log(`✅ Procesando ${commits.length} commits`);

        // Actualizar timeline service
        this.timelineService.setCommits(commits);

        // Renderizar en vista actual
        this.renderCurrentView();

        this.emit('commits:loaded', { count: commits.length });
    }

    /**
     * Renderiza la vista actual de forma simple
     */
    renderCurrentView() {
        if (this.currentView === 'timeline') {
            this.renderTimelineView();
        } else if (this.currentView === '3d') {
            this.renderVisualizationView();
        }
    }

    /**
     * Renderiza vista de timeline simplificada
     */
    renderTimelineView() {
        const timelineComponent = this.components.get('timeline');
        if (!timelineComponent) return;

        const container = timelineComponent.element;
        const currentCommits = this.timelineService.getCurrentPageCommits();
        
        if (currentCommits.length === 0) {
            container.innerHTML = this.getEmptyStateHTML();
            return;
        }

        // Renderizar commits de forma simple
        const commitsHTML = currentCommits.map((commit, index) => {
            const globalIndex = ((this.timelineService.currentPage - 1) * this.timelineService.commitsPerPage) + index + 1;
            return this.getCommitHTML(commit, globalIndex);
        }).join('');

        container.innerHTML = `
            <div class="timeline-header p-3 border-bottom">
                <h5 class="mb-0 text-light">
                    <i class="fas fa-code-branch me-2"></i>
                    Timeline - ${this.repository}
                </h5>
            </div>
            <div class="timeline-commits-container flex-grow-1 overflow-auto p-3">
                ${commitsHTML}
            </div>
            ${this.getPaginationHTML()}
        `;

        // Configurar eventos básicos
        this.setupTimelineEvents(container);
    }

    /**
     * Configura eventos básicos del timeline
     */
    setupTimelineEvents(container) {
        container.addEventListener('click', (e) => {
            // Paginación
            if (e.target.matches('[data-page]')) {
                const page = parseInt(e.target.dataset.page);
                this.timelineService.goToPage(page);
                this.renderTimelineView();
            }

            // Navegación anterior/siguiente
            if (e.target.matches('.timeline-prev')) {
                this.timelineService.goToPage(this.timelineService.currentPage - 1);
                this.renderTimelineView();
            }

            if (e.target.matches('.timeline-next')) {
                this.timelineService.goToPage(this.timelineService.currentPage + 1);
                this.renderTimelineView();
            }

            // Click en commit
            if (e.target.closest('[data-commit-hash]')) {
                const commitHash = e.target.closest('[data-commit-hash]').dataset.commitHash;
                this.handleCommitClick(commitHash);
            }
        });
    }

    /**
     * Maneja click en commit
     */
    handleCommitClick(commitHash) {
        const commit = this.commits.find(c => c.hash === commitHash);
        if (commit) {
            console.log(`📍 Commit seleccionado: ${commit.hash.substring(0, 8)}`);
            this.emit('commit:selected', { commit });
        }
    }

    /**
     * Genera HTML para un commit
     */
    getCommitHTML(commit, index) {
        const shortHash = commit.hash.substring(0, 8);
        const shortMessage = commit.message.length > 80 ? 
            commit.message.substring(0, 80) + '...' : commit.message;
        const relativeTime = this.getRelativeTime(commit.timestamp);

        return `
            <div class="timeline-commit-item d-flex p-2 mb-2 bg-dark bg-opacity-25 rounded cursor-pointer" 
                 data-commit-hash="${commit.hash}">
                <div class="timeline-commit-index d-flex align-items-center justify-content-center me-3" 
                     style="min-width: 35px; height: 35px; background: linear-gradient(135deg, #007bff, #0056b3); 
                            border-radius: 50%; color: white; font-weight: bold; font-size: 0.8rem;">
                    ${index}
                </div>
                <div class="timeline-commit-content flex-grow-1">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <strong class="d-block mb-1 text-light">${shortMessage}</strong>
                            <div class="d-flex align-items-center gap-3 text-muted small">
                                <span><i class="fas fa-user me-1"></i>${commit.author || 'Desconocido'}</span>
                                <span><i class="fas fa-calendar me-1"></i>${relativeTime}</span>
                                <span><i class="fas fa-code-branch me-1"></i>${commit.branch || 'main'}</span>
                            </div>
                        </div>
                        <div>
                            <small class="text-muted font-monospace">${shortHash}</small>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Genera HTML de paginación
     */
    getPaginationHTML() {
        const info = this.timelineService.getPaginationInfo();
        const { currentPage, totalPages } = info;

        return `
            <div class="timeline-pagination d-flex justify-content-between align-items-center p-2 border-top">
                <div>
                    <small class="text-muted">
                        Página ${currentPage} de ${totalPages} (${info.totalCommits} commits)
                    </small>
                </div>
                <div class="d-flex gap-1">
                    <button class="btn btn-outline-primary btn-sm timeline-prev" 
                            ${currentPage === 1 ? 'disabled' : ''}>
                        <i class="fas fa-chevron-left"></i>
                    </button>
                    <span class="d-flex align-items-center px-2">
                        <input type="number" min="1" max="${totalPages}" value="${currentPage}" 
                               class="form-control form-control-sm" style="width: 60px;"
                               onchange="this.dispatchEvent(new CustomEvent('page-change', {detail: this.value}))">
                    </span>
                    <button class="btn btn-outline-primary btn-sm timeline-next" 
                            ${currentPage === totalPages ? 'disabled' : ''}>
                        <i class="fas fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Genera HTML de estado vacío
     */
    getEmptyStateHTML() {
        return `
            <div class="timeline-empty-state d-flex align-items-center justify-content-center h-100 text-center">
                <div>
                    <i class="fas fa-code-branch fa-3x text-muted mb-3"></i>
                    <h6 class="text-muted">No hay commits para mostrar</h6>
                    <p class="text-muted small">Los commits aparecerán aquí cuando se carguen</p>
                </div>
            </div>
        `;
    }

    /**
     * Renderiza vista de visualización 3D
     */
    renderVisualizationView() {
        const visualizationComponent = this.components.get('visualization');
        if (!visualizationComponent || !this.commits.length) return;

        // Inicializar Three.js si no está inicializado
        if (!this.visualizationService.isInitialized) {
            this.visualizationService.initialize('visualization-3d-container');
        }

        // Renderizar commits
        this.visualizationService.renderCommits(this.commits);
    }

    /**
     * Cambia entre vistas
     */
    switchView(view) {
        if (view === this.currentView) return;

        console.log(`🔄 Cambiando vista: ${this.currentView} → ${view}`);
        
        // Ocultar vista actual
        this.hideView(this.currentView);
        
        // Mostrar nueva vista
        this.showView(view);
        
        this.currentView = view;
        this.renderCurrentView();
        
        this.emit('view:changed', { view });
    }

    /**
     * Muestra vista
     */
    showView(view) {
        const viewElement = document.getElementById(`view-${view}`);
        if (viewElement) {
            viewElement.classList.add('active');
            viewElement.style.display = 'flex';
        }
    }

    /**
     * Oculta vista
     */
    hideView(view) {
        const viewElement = document.getElementById(`view-${view}`);
        if (viewElement) {
            viewElement.classList.remove('active');
            viewElement.style.display = 'none';
        }
    }

    /**
     * Configura coordinación de eventos simplificada
     */
    setupEventCoordination() {
        // Navegación entre vistas
        document.addEventListener('click', (e) => {
            if (e.target.matches('[data-view]')) {
                const view = e.target.dataset.view;
                this.switchView(view);
            }
        });

        // Escuchar cambios de commits desde sistema global
        this.eventBus.subscribe('commits:updated', (data) => {
            if (data.commits) {
                this.processCommits(data.commits);
            }
        });
    }

    /**
     * Obtiene tiempo relativo
     */
    getRelativeTime(timestamp) {
        const now = new Date();
        const date = new Date(timestamp);
        const diffMs = now - date;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        
        if (diffDays === 0) return 'Hoy';
        if (diffDays === 1) return 'Ayer';
        if (diffDays < 7) return `Hace ${diffDays} días`;
        if (diffDays < 30) return `Hace ${Math.floor(diffDays / 7)} semanas`;
        return date.toLocaleDateString();
    }

    /**
     * Emite eventos
     */
    emit(eventName, data) {
        this.eventBus.emit(`analysis:${eventName}`, data);
    }

    /**
     * Destruye el coordinador
     */
    destroy() {
        this.subscriptions.forEach(unsubscribe => unsubscribe?.());
        this.subscriptions = [];
        this.isInitialized = false;
        console.log('🗑️ AnalysisCoordinator destruido');
    }

    /**
     * Factory method singleton
     */
    static getInstance() {
        if (!AnalysisCoordinator.instance) {
            AnalysisCoordinator.instance = new AnalysisCoordinator();
        }
        return AnalysisCoordinator.instance;
    }
}

// Registrar globalmente
window.AnalysisCoordinator = AnalysisCoordinator; 