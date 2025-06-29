/**
 * TimelineComponent - Componente UI para mostrar timeline de commits
 * SOLID: Single Responsibility - Solo maneja UI del timeline
 * Utiliza TimelineService para la lógica de datos
 */

class TimelineComponent extends BaseComponent {
    constructor(selector, options = {}) {
        super(selector, options);
        this.timelineService = TimelineService.getInstance();
        this.templates = new Map();
        this.subscriptions = [];
    }

    getDefaultOptions() {
        return {
            showHeader: true,
            showPagination: true,
            showFilters: true,
            autoOptimize: true,
            theme: 'dark'
        };
    }

    async beforeInit() {
        this.initializeTemplates();
        this.setupServiceSubscriptions();
    }

    async render() {
        if (!this.element) return;

        this.element.innerHTML = this.templates.get('main')({
            showHeader: this.options.showHeader,
            showFilters: this.options.showFilters,
            theme: this.options.theme
        });

        await this.renderCommits();
        if (this.options.showPagination) {
            this.renderPagination();
        }
    }

    /**
     * Renderiza la lista de commits actual
     */
    async renderCommits() {
        const commitsContainer = this.element.querySelector('.timeline-commits-container');
        if (!commitsContainer) return;

        const commits = this.timelineService.getCurrentPageCommits();
        
        if (commits.length === 0) {
            commitsContainer.innerHTML = this.templates.get('empty')();
            return;
        }

        const commitsHTML = commits.map((commit, index) => 
            this.templates.get('commit')({
                ...commit,
                index: ((this.timelineService.currentPage - 1) * this.timelineService.commitsPerPage) + index + 1,
                relativeTime: this.getRelativeTime(commit.timestamp),
                shortMessage: this.truncateMessage(commit.message, 80)
            })
        ).join('');

        commitsContainer.innerHTML = commitsHTML;
        this.emit('commits:rendered', { count: commits.length });
    }

    /**
     * Renderiza los controles de paginación
     */
    renderPagination() {
        const paginationContainer = this.element.querySelector('.timeline-pagination');
        if (!paginationContainer) return;

        const paginationInfo = this.timelineService.getPaginationInfo();
        paginationContainer.innerHTML = this.templates.get('pagination')(paginationInfo);
    }

    /**
     * Configura subscripciones a eventos del servicio
     */
    setupServiceSubscriptions() {
        this.subscriptions.push(
            this.timelineService.eventBus.subscribe('timeline:commits:updated', () => {
                this.renderCommits();
                this.renderPagination();
            }),

            this.timelineService.eventBus.subscribe('timeline:page:changed', () => {
                this.renderCommits();
                this.renderPagination();
                this.scrollToTop();
            }),

            this.timelineService.eventBus.subscribe('timeline:filters:applied', () => {
                this.renderCommits();
                this.renderPagination();
            }),

            this.timelineService.eventBus.subscribe('timeline:pagination:optimized', () => {
                this.renderPagination();
            })
        );
    }

    bindEvents() {
        // Paginación
        this.element.addEventListener('click', (e) => {
            if (e.target.matches('.timeline-page-btn')) {
                const page = parseInt(e.target.dataset.page);
                this.timelineService.goToPage(page);
            }

            if (e.target.matches('.timeline-prev-btn')) {
                this.timelineService.goToPage(this.timelineService.currentPage - 1);
            }

            if (e.target.matches('.timeline-next-btn')) {
                this.timelineService.goToPage(this.timelineService.currentPage + 1);
            }

            // Click en commit
            if (e.target.closest('.timeline-commit-item')) {
                const commitItem = e.target.closest('.timeline-commit-item');
                const commitHash = commitItem.dataset.hash;
                this.handleCommitClick(commitHash);
            }
        });

        // Input de página directa
        this.element.addEventListener('keypress', (e) => {
            if (e.target.matches('.timeline-page-input') && e.key === 'Enter') {
                const page = parseInt(e.target.value);
                if (page && page > 0) {
                    this.timelineService.goToPage(page);
                }
            }
        });

        // Filtros
        this.element.addEventListener('input', (e) => {
            if (e.target.matches('.timeline-filter-input')) {
                this.handleFilterChange();
            }
        });

        // Auto-optimización en resize si está habilitada
        if (this.options.autoOptimize) {
            window.addEventListener('resize', this.debounce(() => {
                this.timelineService.optimizeCommitsPerPage();
            }, 250));
        }
    }

    /**
     * Maneja el click en un commit
     */
    handleCommitClick(commitHash) {
        const commits = this.timelineService.getCurrentPageCommits();
        const commit = commits.find(c => c.hash === commitHash);
        
        if (commit) {
            this.emit('commit:selected', { commit });
            
            // Resaltar visualmente
            this.element.querySelectorAll('.timeline-commit-item').forEach(item => {
                item.classList.remove('selected');
            });
            
            const selectedItem = this.element.querySelector(`[data-hash="${commitHash}"]`);
            if (selectedItem) {
                selectedItem.classList.add('selected');
            }
        }
    }

    /**
     * Maneja cambios en filtros
     */
    handleFilterChange() {
        const filters = {
            author: this.element.querySelector('.filter-author')?.value || '',
            message: this.element.querySelector('.filter-message')?.value || '',
            dateRange: this.element.querySelector('.filter-date')?.value || ''
        };

        this.timelineService.applyFilters(filters);
    }

    /**
     * Actualiza datos del timeline
     */
    setCommits(commits) {
        this.timelineService.setCommits(commits);
    }

    /**
     * Scroll suave al inicio
     */
    scrollToTop() {
        const container = this.element.querySelector('.timeline-commits-container');
        if (container) {
            container.scrollTo({ top: 0, behavior: 'smooth' });
        }
    }

    /**
     * Plantillas HTML
     */
    initializeTemplates() {
        this.templates.set('main', (data) => `
            <div class="timeline-wrapper ${data.theme}">
                ${data.showHeader ? this.templates.get('header')(data) : ''}
                ${data.showFilters ? this.templates.get('filters')(data) : ''}
                <div class="timeline-commits-container"></div>
                <div class="timeline-pagination"></div>
            </div>
        `);

        this.templates.set('header', () => `
            <div class="timeline-header d-flex justify-content-between align-items-center p-3 border-bottom">
                <h5 class="mb-0 text-light">
                    <i class="fas fa-code-branch me-2"></i>
                    Timeline de Commits
                </h5>
                <div class="timeline-actions">
                    <button class="btn btn-sm btn-outline-light me-2" data-action="refresh">
                        <i class="fas fa-sync-alt"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-light" data-action="fullscreen">
                        <i class="fas fa-expand"></i>
                    </button>
                </div>
            </div>
        `);

        this.templates.set('filters', () => `
            <div class="timeline-filters p-3 border-bottom">
                <div class="row g-2">
                    <div class="col-md-4">
                        <input type="text" class="form-control form-control-sm filter-author timeline-filter-input" 
                               placeholder="Filtrar por autor...">
                    </div>
                    <div class="col-md-4">
                        <input type="text" class="form-control form-control-sm filter-message timeline-filter-input" 
                               placeholder="Filtrar por mensaje...">
                    </div>
                    <div class="col-md-4">
                        <select class="form-select form-select-sm filter-date timeline-filter-input">
                            <option value="">Todos los commits</option>
                            <option value="week">Última semana</option>
                            <option value="month">Último mes</option>
                            <option value="quarter">Último trimestre</option>
                            <option value="year">Último año</option>
                        </select>
                    </div>
                </div>
            </div>
        `);

        this.templates.set('commit', (commit) => `
            <div class="timeline-commit-item d-flex p-2 mb-2 bg-dark bg-opacity-25 rounded" 
                 data-hash="${commit.hash}">
                <div class="timeline-commit-index d-flex align-items-center justify-content-center me-3" 
                     style="min-width: 35px; height: 35px; background: linear-gradient(135deg, #007bff, #0056b3); 
                            border-radius: 50%; color: white; font-weight: bold; font-size: 0.8rem;">
                    ${commit.index}
                </div>
                <div class="timeline-commit-content flex-grow-1">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="timeline-commit-message">
                            <strong class="d-block mb-1 text-light">${commit.shortMessage}</strong>
                            <div class="d-flex align-items-center gap-3 text-muted small">
                                <span><i class="fas fa-user me-1"></i>${commit.author || 'Desconocido'}</span>
                                <span><i class="fas fa-calendar me-1"></i>${commit.relativeTime}</span>
                                <span><i class="fas fa-code-branch me-1"></i>${commit.branch || 'main'}</span>
                            </div>
                        </div>
                        <div class="timeline-commit-actions">
                            <small class="text-muted font-monospace">${commit.hash.substring(0, 7)}</small>
                        </div>
                    </div>
                </div>
            </div>
        `);

        this.templates.set('pagination', (info) => `
            <div class="d-flex justify-content-between align-items-center p-2 bg-dark bg-opacity-10">
                <div class="timeline-page-info">
                    <small class="text-muted">
                        Página ${info.currentPage} de ${info.totalPages} 
                        (${info.totalCommits} commits)
                    </small>
                </div>
                <div class="timeline-nav-controls d-flex gap-1">
                    <button class="btn btn-outline-primary btn-sm timeline-prev-btn" 
                            ${info.currentPage === 1 ? 'disabled' : ''}>
                        <i class="fas fa-chevron-left"></i>
                    </button>
                    <input type="number" min="1" max="${info.totalPages}" value="${info.currentPage}" 
                           class="form-control form-control-sm timeline-page-input" style="width: 60px;">
                    <button class="btn btn-outline-primary btn-sm timeline-next-btn" 
                            ${info.currentPage === info.totalPages ? 'disabled' : ''}>
                        <i class="fas fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        `);

        this.templates.set('empty', () => `
            <div class="timeline-empty-state text-center p-5">
                <i class="fas fa-code-branch fa-3x text-muted mb-3"></i>
                <h6 class="text-muted">No hay commits para mostrar</h6>
                <p class="text-muted small">Los commits aparecerán aquí cuando se carguen los datos</p>
            </div>
        `);
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
        if (diffDays < 30) return `Hace ${Math.floor(diffDays / 7)} semanas`;
        if (diffDays < 365) return `Hace ${Math.floor(diffDays / 30)} meses`;
        return `Hace ${Math.floor(diffDays / 365)} años`;
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
    }
}

// Registrar el componente
ComponentFactory.register('TimelineComponent', TimelineComponent); 