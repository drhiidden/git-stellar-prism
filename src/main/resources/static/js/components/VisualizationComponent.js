/**
 * VisualizationComponent - Componente UI para visualización 3D
 * SOLID: Single Responsibility - Solo maneja UI de la visualización 3D
 * Utiliza VisualizationService para la lógica de Three.js
 */

class VisualizationComponent extends BaseComponent {
    constructor(selector, options = {}) {
        super(selector, options);
        this.visualizationService = VisualizationService.getInstance();
        this.subscriptions = [];
        this.isActive = false;
    }

    getDefaultOptions() {
        return {
            containerId: 'visualization-container',
            showControls: true,
            showStats: true,
            enableInteraction: true,
            autoResize: true
        };
    }

    async beforeInit() {
        this.setupServiceSubscriptions();
    }

    async render() {
        if (!this.element) return;

        this.element.innerHTML = `
            <div class="visualization-wrapper h-100">
                ${this.options.showControls ? this.renderControls() : ''}
                <div id="${this.options.containerId}" class="visualization-container h-100"></div>
                ${this.options.showStats ? this.renderStats() : ''}
            </div>
        `;

        // Inicializar visualización 3D
        await this.initializeVisualization();
    }

    /**
     * Inicializa el sistema de visualización 3D
     */
    async initializeVisualization() {
        try {
            await this.visualizationService.initialize(this.options.containerId);
            this.isActive = true;
            this.emit('visualization:initialized');
        } catch (error) {
            console.error('Error inicializando visualización:', error);
            this.showError('Error al inicializar la visualización 3D');
        }
    }

    /**
     * Renderiza controles de la visualización
     */
    renderControls() {
        return `
            <div class="visualization-controls d-flex justify-content-between align-items-center p-2 border-bottom">
                <div class="control-group">
                    <button class="btn btn-sm btn-outline-light me-2" data-action="reset-camera">
                        <i class="fas fa-home"></i> Reset Vista
                    </button>
                    <button class="btn btn-sm btn-outline-light me-2" data-action="auto-fit">
                        <i class="fas fa-expand-arrows-alt"></i> Ajustar
                    </button>
                </div>
                <div class="control-group">
                    <button class="btn btn-sm btn-outline-light me-2" data-action="toggle-stats">
                        <i class="fas fa-chart-bar"></i> Stats
                    </button>
                    <button class="btn btn-sm btn-outline-light" data-action="fullscreen">
                        <i class="fas fa-expand"></i>
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Renderiza estadísticas de la visualización
     */
    renderStats() {
        return `
            <div class="visualization-stats position-absolute top-0 end-0 p-2 m-2 bg-dark bg-opacity-75 rounded">
                <div class="stats-content text-light small">
                    <div class="stat-item mb-1">
                        <i class="fas fa-cube me-1"></i>
                        <span class="stat-nodes">0</span> nodos
                    </div>
                    <div class="stat-item mb-1">
                        <i class="fas fa-project-diagram me-1"></i>
                        <span class="stat-connections">0</span> conexiones
                    </div>
                    <div class="stat-item">
                        <i class="fas fa-eye me-1"></i>
                        <span class="stat-fps">60</span> FPS
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Configura subscripciones a eventos del servicio
     */
    setupServiceSubscriptions() {
        this.subscriptions.push(
            this.visualizationService.eventBus.subscribe('visualization:initialized', (data) => {
                this.updateStats();
                this.emit('ready', data);
            }),

            this.visualizationService.eventBus.subscribe('visualization:render:complete', (data) => {
                this.updateStats(data);
                this.emit('render:complete', data);
            }),

            this.visualizationService.eventBus.subscribe('visualization:commit:clicked', (data) => {
                this.handleCommitInteraction(data.commit);
            }),

            this.visualizationService.eventBus.subscribe('visualization:commit:highlighted', (data) => {
                this.emit('commit:highlighted', data);
            })
        );
    }

    bindEvents() {
        // Controles de la visualización
        this.element.addEventListener('click', (e) => {
            const action = e.target.closest('[data-action]')?.dataset.action;
            if (!action) return;

            switch (action) {
                case 'reset-camera':
                    this.resetCamera();
                    break;
                case 'auto-fit':
                    this.autoFitView();
                    break;
                case 'toggle-stats':
                    this.toggleStats();
                    break;
                case 'fullscreen':
                    this.toggleFullscreen();
                    break;
            }
        });

        // Auto-resize si está habilitado
        if (this.options.autoResize) {
            window.addEventListener('resize', this.debounce(() => {
                this.handleResize();
            }, 250));
        }
    }

    /**
     * Actualiza la visualización con nuevos commits
     */
    setCommits(commits) {
        if (!this.isActive) {
            console.warn('Visualización no inicializada');
            return;
        }

        this.visualizationService.renderCommits(commits);
        this.emit('commits:updated', { count: commits.length });
    }

    /**
     * Resalta un commit específico
     */
    highlightCommit(commitHash) {
        if (!this.isActive) return;
        this.visualizationService.highlightCommit(commitHash);
    }

    /**
     * Maneja interacción con commits
     */
    handleCommitInteraction(commit) {
        this.emit('commit:selected', { commit });
        
        // Mostrar información del commit
        this.showCommitInfo(commit);
    }

    /**
     * Muestra información del commit seleccionado
     */
    showCommitInfo(commit) {
        const infoPanel = this.element.querySelector('.commit-info-panel');
        if (!infoPanel) {
            // Crear panel si no existe
            const panel = document.createElement('div');
            panel.className = 'commit-info-panel position-absolute bottom-0 start-0 p-3 m-2 bg-dark bg-opacity-90 rounded';
            panel.style.maxWidth = '300px';
            this.element.querySelector('.visualization-wrapper').appendChild(panel);
        }

        const panel = this.element.querySelector('.commit-info-panel');
        panel.innerHTML = `
            <div class="commit-info text-light small">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <strong class="text-primary">${commit.hash.substring(0, 8)}</strong>
                    <button class="btn btn-sm btn-outline-light ms-2" onclick="this.parentElement.parentElement.parentElement.remove()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="mb-2">
                    <strong>${this.truncateMessage(commit.message, 50)}</strong>
                </div>
                <div class="commit-meta text-muted">
                    <div><i class="fas fa-user me-1"></i>${commit.author || 'Desconocido'}</div>
                    <div><i class="fas fa-calendar me-1"></i>${this.getRelativeTime(commit.timestamp)}</div>
                    <div><i class="fas fa-code-branch me-1"></i>${commit.branch || 'main'}</div>
                </div>
            </div>
        `;
    }

    /**
     * Actualiza estadísticas en tiempo real
     */
    updateStats(data = {}) {
        if (!this.options.showStats) return;

        const statsContainer = this.element.querySelector('.stats-content');
        if (!statsContainer) return;

        const nodesCount = data.nodesCount || this.visualizationService.nodes.length;
        const connectionsCount = data.connectionsCount || this.visualizationService.connections.length;

        statsContainer.querySelector('.stat-nodes').textContent = nodesCount;
        statsContainer.querySelector('.stat-connections').textContent = connectionsCount;
    }

    /**
     * Acciones de control
     */
    resetCamera() {
        if (!this.isActive) return;
        this.visualizationService.adjustCameraToFitScene();
        this.emit('camera:reset');
    }

    autoFitView() {
        if (!this.isActive) return;
        this.visualizationService.adjustCameraToFitScene();
        this.emit('view:auto-fit');
    }

    toggleStats() {
        const statsPanel = this.element.querySelector('.visualization-stats');
        if (statsPanel) {
            statsPanel.style.display = statsPanel.style.display === 'none' ? 'block' : 'none';
        }
    }

    toggleFullscreen() {
        const container = this.element.querySelector('.visualization-container');
        if (!container) return;

        if (document.fullscreenElement) {
            document.exitFullscreen();
        } else {
            container.requestFullscreen?.() || 
            container.webkitRequestFullscreen?.() || 
            container.msRequestFullscreen?.();
        }
    }

    handleResize() {
        if (!this.isActive) return;
        // El VisualizationService maneja el resize automáticamente
        this.emit('resized');
    }

    /**
     * Muestra error en la visualización
     */
    showError(message) {
        this.element.innerHTML = `
            <div class="visualization-error d-flex align-items-center justify-content-center h-100 text-center">
                <div>
                    <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3"></i>
                    <h6 class="text-light">${message}</h6>
                    <button class="btn btn-outline-primary btn-sm mt-2" onclick="location.reload()">
                        Reintentar
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Métodos de utilidad
     */
    getRelativeTime(timestamp) {
        const now = new Date();
        const date = new Date(timestamp);
        const diffMs = now - date;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        
        if (diffDays === 0) return 'Hoy';
        if (diffDays === 1) return 'Ayer';
        if (diffDays < 7) return `Hace ${diffDays} días`;
        return date.toLocaleDateString();
    }

    truncateMessage(message, maxLength) {
        if (!message || message.length <= maxLength) return message;
        return message.substring(0, maxLength) + '...';
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * Cleanup al destruir el componente
     */
    beforeDestroy() {
        this.subscriptions.forEach(unsubscribe => unsubscribe?.());
        this.subscriptions = [];
        this.isActive = false;
    }
}

// Registrar el componente
ComponentFactory.register('VisualizationComponent', VisualizationComponent); 