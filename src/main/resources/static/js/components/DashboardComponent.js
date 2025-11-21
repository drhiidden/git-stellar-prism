/**
 * Componente Dashboard Unificado - Gestión de repositorios y filtros integrados
 * Implementa principios SOLID y manejo de estado reactivo
 */

class DashboardComponent extends BaseComponent {
    constructor(selector, options = {}) {
        super(selector, options);
        this.githubService = ServiceFactory.getGithub();
        this.cacheService = ServiceFactory.getCache();
        this.stateManager = AppStateManager.getInstance();
        this.router = Router.getInstance();
    }

    getDefaultOptions() {
        return {
            showWelcome: true,
            showFilters: true,
            showStats: true,
            pageSize: 12,
            autoLoad: true,
            unifiedMode: false
        };
    }

    async beforeInit() {
        // Suscribirse a cambios de estado
        this.stateManager.subscribe('user', (user) => {
            this.setState({ user });
            if (user && this.options.autoLoad) {
                this.loadRepositories();
            }
        });

        this.stateManager.subscribe('repositories', (repositories) => {
            this.setState({ repositories });
            this.updateTechnologies();
            this.applyFilters();
        });

        this.stateManager.subscribe('isLoading', (isLoading) => {
            this.setState({ isLoading });
        });

        // Suscribirse a eventos de caché
        this.subscribe('cache:invalidated', () => {
            this.refreshRepositories();
        });

        // Suscribirse a eventos de repositorio
        this.subscribe('repository:updated', (data) => {
            this.updateRepository(data);
        });

        // Estado inicial del componente
        this.setState({
            repositories: [],
            filteredRepositories: [],
            technologies: new Set(),
            selectedTech: null,
            searchQuery: '',
            sortBy: 'updated',
            currentPage: 1,
            showAllTechnologies: false,
            stats: {
                total: 0,
                public: 0,
                private: 0,
                languages: {},
                totalStars: 0,
                totalForks: 0
            }
        });
    }

    async render() {
        if (!this.element) return;

        const { user, repositories, isLoading, stats } = this.state;
        const { showWelcome, unifiedMode } = this.options;

        if (unifiedMode) {
            this.renderUnifiedMode(user, repositories, isLoading, stats);
        } else {
            this.renderLegacyMode(user, repositories, isLoading, stats);
        }
    }

    renderUnifiedMode(user, repositories, isLoading, stats) {
        const { showWelcome } = this.options;
        this.element.innerHTML = `
            ${showWelcome && user ? this.renderWelcomeSection(user, stats) : ''}
            ${!user ? this.renderUnauthenticatedSection() : ''}
            ${user ? this.renderUnifiedRepositoriesSection(repositories, isLoading) : ''}
            ${this.renderModals()}
        `;
    }

    renderWelcomeSection(user, stats) {
        const displayName = user.name || user.login || user.id || 'Usuario';
        
        return `
            <div class="welcome-section">
                <div class="row align-items-center">
                    <div class="col-md-8">
                        <h1 class="mb-3">
                            <i class="fas fa-star me-3"></i>
                            ¡Bienvenido, ${displayName}!
                        </h1>
                        <p class="lead mb-0">
                            Explora tus repositorios de GitHub con visualizaciones avanzadas y análisis técnico profundo.
                            Usa los filtros inteligentes para encontrar exactamente lo que buscas.
                        </p>
                    </div>
                    <div class="col-md-4">
                        <div class="stats-card">
                            <h3>${stats.total}</h3>
                            <p class="mb-0">Repositorios Disponibles</p>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    renderUnauthenticatedSection() {
        return `
            <div class="row mb-4">
                <div class="col-md-6 mb-3">
                    <div class="card dashboard-card h-100" data-action="analyze">
                        <div class="card-body text-center">
                            <i class="fas fa-search fa-3x text-primary mb-3"></i>
                            <h5 class="card-title">Analizar Repositorio</h5>
                            <p class="card-text">Introduce la URL de cualquier repositorio público para crear visualizaciones 3D.</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-3">
                    <div class="card dashboard-card h-100" data-action="graph">
                        <div class="card-body text-center">
                            <i class="fas fa-project-diagram fa-3x text-info mb-3"></i>
                            <h5 class="card-title">Crear Grafos</h5>
                            <p class="card-text">Genera grafos de dependencias y relaciones de código usando tecnología avanzada.</p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="text-center py-5">
                <i class="fas fa-github fa-4x text-muted mb-4"></i>
                <h4 class="text-muted mb-3">Inicia sesión con GitHub</h4>
                <p class="text-muted">Conecta tu cuenta para acceder a tus repositorios privados y obtener límites de API más altos.</p>
                <a class="btn btn-primary btn-lg" href="/oauth2/authorization/github">
                    <i class="fab fa-github me-2"></i>
                    Conectar con GitHub
                </a>
            </div>
        `;
    }

    renderUnifiedRepositoriesSection(repositories, isLoading) {
        const { filteredRepositories, technologies, selectedTech, searchQuery, currentPage, sortBy } = this.state;
        const { pageSize } = this.options;

        return `
            <div class="unified-section">
                <!-- Header de la sección unificada -->
                <div class="filters-header">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h3 class="mb-1">
                        <i class="fas fa-repository me-2"></i>
                                Explorar Repositorios
                            </h3>
                            <p class="mb-0 opacity-75">
                                ${repositories.length} repositorios encontrados
                                ${filteredRepositories.length !== repositories.length ? 
                                    ` • mostrando ${filteredRepositories.length} filtrados` : ''}
                            </p>
                        </div>
                    <div class="d-flex gap-2">
                            <button class="btn btn-light btn-sm" data-action="analyze" title="Analizar otro repositorio">
                                <i class="fas fa-plus me-1"></i>
                                Analizar Otro
                        </button>
                            <button class="btn btn-outline-light btn-sm" data-action="refresh" title="Actualizar repositorios">
                                <i class="fas fa-sync me-1 ${isLoading ? 'fa-spin' : ''}"></i>
                                ${isLoading ? 'Actualizando...' : 'Actualizar'}
                        </button>
                    </div>
                </div>
                </div>

                <!-- Contenido de filtros -->
                <div class="filters-content">
                    <div class="row">
                        <!-- Búsqueda principal -->
                        <div class="col-md-8 mb-3">
                            <div class="input-group">
                                <span class="input-group-text bg-white border-end-0">
                                    <i class="fas fa-search text-muted"></i>
                                </span>
                                <input type="text" class="form-control search-box border-start-0" 
                                       id="search-input" 
                                       placeholder="Buscar por nombre, descripción o tecnología..." 
                                       value="${searchQuery}">
                                ${searchQuery ? `
                                    <button class="btn btn-outline-secondary" type="button" id="clear-search">
                                        <i class="fas fa-times"></i>
                                    </button>
                                ` : ''}
                            </div>
                        </div>
                        
                        <!-- Ordenamiento -->
                        <div class="col-md-4 mb-3">
                            <select class="form-select" id="sort-select" value="${sortBy}">
                                <option value="updated" ${sortBy === 'updated' ? 'selected' : ''}>📅 Última actualización</option>
                                <option value="created" ${sortBy === 'created' ? 'selected' : ''}>🆕 Fecha de creación</option>
                                <option value="name" ${sortBy === 'name' ? 'selected' : ''}>🔤 Nombre (A-Z)</option>
                                <option value="stars" ${sortBy === 'stars' ? 'selected' : ''}>⭐ Más populares</option>
                                <option value="size" ${sortBy === 'size' ? 'selected' : ''}>📊 Tamaño</option>
                            </select>
                        </div>
                    </div>

                    <!-- Filtros de tecnologías -->
                    ${technologies.size > 0 ? `
                        <div class="mb-3">
                            <label class="form-label fw-bold mb-2">
                                <i class="fas fa-code me-1"></i>
                                Filtrar por Tecnología
                                <small class="text-muted">(${technologies.size} tecnologías encontradas)</small>
                            </label>
                            <div class="d-flex flex-wrap gap-2" id="tech-filters">
                                ${this.renderTechnologyFilters(technologies, selectedTech)}
                            </div>
                        </div>
                    ` : ''}

                    <!-- Estadísticas de filtros -->
                    <div class="d-flex justify-content-between align-items-center">
                        <small class="text-muted">
                            ${this.renderActiveFiltersInfo()}
                        </small>
                        ${(selectedTech || searchQuery) ? `
                            <button class="btn btn-outline-secondary btn-sm" id="reset-filters">
                                <i class="fas fa-undo me-1"></i>
                                Limpiar Filtros
                            </button>
                        ` : ''}
                    </div>
                </div>

                <!-- Contenido de repositorios -->
                <div class="repositories-content">
                    ${isLoading ? this.renderLoadingState() : this.renderRepositoriesList(filteredRepositories, currentPage, pageSize)}
                </div>
            </div>
        `;
    }

    renderTechnologyFilters(technologies, selectedTech) {
        if (technologies.size === 0) {
            return '<span class="text-muted">Cargando tecnologías...</span>';
        }

        const showAllTechs = this.state.showAllTechnologies || false;
        const showByCategory = this.state.showTechByCategory || false;
        
        // Si tenemos detalles de tecnologías (del detector inteligente)
        if (this.state.technologyDetails && this.state.technologyDetails.length > 0) {
            return this.renderHierarchicalTechFilters(selectedTech, showAllTechs, showByCategory);
        }

        // Fallback: renderizado tradicional
        const techArray = Array.from(technologies).map(tech => {
            const count = this.countRepositoriesWithTech(tech);
            return { tech, count };
        }).sort((a, b) => b.count - a.count);

        const maxVisible = 12;
        const visibleTechs = showAllTechs ? techArray : techArray.slice(0, maxVisible);
        const hiddenCount = techArray.length - maxVisible;

        const allButton = `
            <button class="btn tech-pill ${!selectedTech ? 'active btn-primary' : 'btn-outline-primary'}" 
                    data-tech="">
                <i class="fas fa-globe me-1"></i>
                Todas (${this.state.repositories.length})
            </button>
        `;

        const techButtons = visibleTechs.map(({ tech, count }) => `
            <button class="btn tech-pill ${selectedTech === tech ? 'active btn-primary' : 'btn-outline-primary'}" 
                    data-tech="${tech}">
                ${this.getTechIcon(tech)} ${tech} (${count})
            </button>
        `).join('');

        const toggleButton = hiddenCount > 0 ? `
            <button class="btn btn-outline-secondary tech-pill" id="toggle-technologies">
                <i class="fas fa-${showAllTechs ? 'chevron-up' : 'chevron-down'} me-1"></i>
                ${showAllTechs ? `Ver menos` : `Ver ${hiddenCount} más`}
            </button>
        ` : '';

        return allButton + techButtons + (hiddenCount > 0 ? toggleButton : '');
    }
    
    renderHierarchicalTechFilters(selectedTech, showAllTechs, showByCategory) {
        const { technologyDetails, repositories } = this.state;
        
        // Botón "Todas"
        let html = `
            <button class="btn tech-pill ${!selectedTech ? 'active btn-primary' : 'btn-outline-primary'}" 
                    data-tech="">
                <i class="fas fa-globe me-1"></i>
                Todas (${repositories.length})
            </button>
        `;
        
        // Botón para alternar vista jerárquica/plana
        html += `
            <button class="btn btn-outline-secondary tech-pill" id="toggle-category-view" title="Alternar vista por categorías">
                <i class="fas fa-${showByCategory ? 'list' : 'layer-group'} me-1"></i>
                ${showByCategory ? 'Vista Plana' : 'Por Categoría'}
            </button>
        `;
        
        if (showByCategory) {
            // Vista jerárquica por categorías
            html += this.renderCategoryView(technologyDetails, selectedTech, showAllTechs);
        } else {
            // Vista plana (las más populares)
            const maxVisible = 12;
            const visibleTechs = showAllTechs ? technologyDetails : technologyDetails.slice(0, maxVisible);
            const hiddenCount = technologyDetails.length - maxVisible;
            
            html += visibleTechs.map(tech => `
                <button class="btn tech-pill ${selectedTech === tech.name ? 'active btn-primary' : 'btn-outline-primary'}" 
                        data-tech="${tech.name}"
                        title="${tech.category} • Confianza: ${Math.round(tech.confidence * 100)}%">
                    ${tech.icon} ${tech.name} (${tech.count})
                </button>
            `).join('');
            
            if (hiddenCount > 0) {
                html += `
                    <button class="btn btn-outline-secondary tech-pill" id="toggle-technologies">
                        <i class="fas fa-${showAllTechs ? 'chevron-up' : 'chevron-down'} me-1"></i>
                        ${showAllTechs ? `Ver menos` : `Ver ${hiddenCount} más`}
                    </button>
                `;
            }
        }
        
        return html;
    }
    
    renderCategoryView(technologyDetails, selectedTech, showAllTechs) {
        // Agrupar por categoría
        const grouped = window.TechnologyDetector.groupByCategory(technologyDetails);
        
        let html = '<div class="w-100"></div>'; // Break line
        
        // Ordenar categorías por prioridad
        const sortedCategories = Object.entries(grouped).sort((a, b) => a[1].priority - b[1].priority);
        
        sortedCategories.forEach(([categoryName, categoryData]) => {
            const maxPerCategory = showAllTechs ? 999 : 5;
            const visibleTechs = categoryData.technologies.slice(0, maxPerCategory);
            const hiddenInCategory = categoryData.technologies.length - maxPerCategory;
            
            html += `
                <div class="tech-category-group w-100 mb-2">
                    <small class="text-muted fw-bold d-block mb-1">
                        ${categoryData.icon} ${categoryName} (${categoryData.technologies.length})
                    </small>
                    <div class="d-flex flex-wrap gap-1">
                        ${visibleTechs.map(tech => `
                            <button class="btn btn-sm tech-pill ${selectedTech === tech.name ? 'active btn-primary' : 'btn-outline-primary'}" 
                                    data-tech="${tech.name}"
                                    title="${tech.category} • ${tech.confidence ? Math.round(tech.confidence * 100) + '%' : ''}">
                                ${tech.icon} ${tech.name} (${tech.count})
                            </button>
                        `).join('')}
                        ${hiddenInCategory > 0 ? `
                            <small class="text-muted align-self-center">+${hiddenInCategory} más</small>
                        ` : ''}
                    </div>
                </div>
            `;
        });
        
        return html;
    }

    renderActiveFiltersInfo() {
        const { selectedTech, searchQuery, filteredRepositories, repositories } = this.state;
        let info = '';
        
        if (selectedTech || searchQuery) {
            info = `Filtros activos: `;
            const filters = [];
            if (searchQuery) filters.push(`Búsqueda: "${searchQuery}"`);
            if (selectedTech) filters.push(`Tecnología: ${selectedTech}`);
            info += filters.join(', ');
        } else {
            info = 'Mostrando todos los repositorios';
        }
        
        return info;
    }

    renderLoadingState() {
        return `
            <div class="d-flex justify-content-center py-5">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Cargando repositorios...</span>
                </div>
            </div>
        `;
    }

    renderRepositoriesList(repositories, currentPage, pageSize) {
        if (!repositories || repositories.length === 0) {
            return this.renderEmptyState();
        }

        const startIndex = (currentPage - 1) * pageSize;
        const endIndex = startIndex + pageSize;
        const paginatedRepos = repositories.slice(startIndex, endIndex);

        return `
            <div class="row" id="repositories-grid">
                ${paginatedRepos.map(repo => this.renderRepositoryCard(repo)).join('')}
            </div>
            ${this.renderPagination(repositories.length, currentPage, pageSize)}
        `;
    }

    renderEmptyState() {
        const { selectedTech, searchQuery } = this.state;

        return `
            <div class="empty-state">
                <i class="fas fa-search fa-4x mb-3"></i>
                <h4>No se encontraron repositorios</h4>
                <p class="text-muted">
                    ${selectedTech || searchQuery ? 
                        'Intenta ajustar los filtros o buscar con términos diferentes.' :
                        'Aún no tienes repositorios disponibles.'}
                </p>
                <div class="mt-3">
                    ${selectedTech || searchQuery ? `
                        <button class="btn btn-primary me-2" id="reset-filters">
                            <i class="fas fa-undo me-1"></i>
                            Limpiar Filtros
                        </button>
                                ` : ''}
                    <button class="btn btn-outline-primary" data-action="refresh">
                        <i class="fas fa-sync me-1"></i>
                        Actualizar Lista
                            </button>
                </div>
            </div>
        `;
    }

    renderRepositoryCard(repo) {
        const { sortBy } = this.state;
        const languages = repo.languages || repo.languageDistribution || {};
        const primaryLanguage = Object.keys(languages)[0];
        const languageColor = this.getLanguageColor(primaryLanguage);
        const description = repo.description || 'No hay descripción disponible';
        const stars = repo.stargazers_count || repo.stargazersCount || 0;
        const forks = repo.forks_count || repo.forksCount || 0;
        const isPrivate = repo.private || repo.isPrivate || false;

        // Determinar qué fecha mostrar según el ordenamiento
        let dateValue = repo.updated_at || repo.updatedAt;
        let dateLabel = 'Actualizado';
        let dateIcon = 'clock';

        if (sortBy === 'created') {
            dateValue = repo.created_at || repo.createdAt;
            dateLabel = 'Creado';
            dateIcon = 'calendar-plus';
        }

        return `
            <div class="col-md-6 col-lg-4 mb-4">
                <div class="card repo-card h-100">
                    <div class="repo-header">
                        <div class="d-flex justify-content-between align-items-start">
                            <h6 class="mb-1 fw-bold">
                                <i class="fas fa-${isPrivate ? 'lock' : 'book'} me-2 text-${isPrivate ? 'warning' : 'primary'}"></i>
                                ${repo.name}
                            </h6>
                            <span class="badge bg-${isPrivate ? 'warning' : 'success'} text-dark">
                                ${isPrivate ? 'Privado' : 'Público'}
                            </span>
                    </div>
                </div>
                <div class="card-body">
                        <p class="card-text text-muted small mb-3">
                            ${description.length > 100 ? description.substring(0, 100) + '...' : description}
                        </p>
                        
                        <!-- Información del lenguaje principal -->
                        ${primaryLanguage ? `
                            <div class="d-flex align-items-center mb-2">
                                <span class="language-dot" style="background-color: ${languageColor}"></span>
                                <small class="text-muted">${primaryLanguage}</small>
                            </div>
                        ` : ''}
                        
                        <!-- Topics del repositorio -->
                        ${repo.topics && repo.topics.length > 0 ? `
                            <div class="mb-2">
                                ${repo.topics.slice(0, 3).map(topic => `
                                    <span class="badge bg-light text-dark tech-badge me-1">${topic}</span>
                                `).join('')}
                                ${repo.topics.length > 3 ? `<small class="text-muted">+${repo.topics.length - 3} más</small>` : ''}
                            </div>
                        ` : ''}
                        
                        <!-- Estadísticas -->
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <div class="d-flex gap-3">
                                <small class="text-muted">
                                    <i class="fas fa-star text-warning me-1"></i>${stars}
                                </small>
                                <small class="text-muted">
                                    <i class="fas fa-code-branch text-info me-1"></i>${forks}
                                </small>
                                </div>
                            <small class="text-muted" title="${dateLabel}: ${new Date(dateValue).toLocaleDateString()}">
                                <i class="fas fa-${dateIcon} me-1"></i>
                                ${this.formatDate(dateValue)}
                            </small>
                                </div>
                                
                        <!-- Acciones -->
                        <div class="d-grid">
                            <button class="btn btn-primary btn-sm" data-action="analyze" data-repo="${repo.full_name || repo.owner + '/' + repo.name}">
                                <i class="fas fa-cube me-1"></i>
                                Ver Análisis y Grafo 3D
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    renderPagination(total, currentPage, pageSize) {
        const totalPages = Math.ceil(total / pageSize);
        
        if (totalPages <= 1) return '';

        let paginationHtml = `
            <nav aria-label="Paginación de repositorios">
                <ul class="pagination justify-content-center">
                    <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                        <a class="page-link" href="#" data-page="${currentPage - 1}">Anterior</a>
                    </li>
        `;

        // Mostrar páginas
        for (let i = 1; i <= totalPages; i++) {
            if (i === 1 || i === totalPages || (i >= currentPage - 2 && i <= currentPage + 2)) {
                paginationHtml += `
                    <li class="page-item ${i === currentPage ? 'active' : ''}">
                        <a class="page-link" href="#" data-page="${i}">${i}</a>
                            </li>
                `;
            } else if (i === currentPage - 3 || i === currentPage + 3) {
                paginationHtml += '<li class="page-item disabled"><span class="page-link">...</span></li>';
            }
        }

        paginationHtml += `
                    <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
                        <a class="page-link" href="#" data-page="${currentPage + 1}">Siguiente</a>
                    </li>
                </ul>
            </nav>
        `;

        return paginationHtml;
    }

    renderModals() {
        return `
            <!-- Modal para analizar repositorio -->
            <div class="modal fade" id="analyze-modal" tabindex="-1">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-search me-2"></i>
                                Analizar Repositorio
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <form id="analyze-form">
                                <div class="mb-3">
                                    <label for="repo-url" class="form-label">Repositorio</label>
                                    <input type="text" class="form-control" id="repo-url" 
                                           placeholder="owner/repository" required>
                                    <div class="form-text">Ejemplo: microsoft/vscode, facebook/react</div>
                                </div>
                                <div class="d-grid">
                                    <button type="submit" class="btn btn-primary">
                                        <i class="fas fa-rocket me-2"></i>
                                        Comenzar Análisis
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    bindEvents() {
        // Event delegation para botones dinámicos
        this.element.addEventListener('click', (e) => {
            const button = e.target.closest('[data-action]');
            if (button) {
                e.preventDefault();
                this.handleAction(button.dataset.action, button.dataset.repo);
            }

            const pageLink = e.target.closest('.page-link');
            if (pageLink && pageLink.dataset.page) {
                e.preventDefault();
                this.goToPage(parseInt(pageLink.dataset.page));
            }

            const techFilter = e.target.closest('[data-tech]');
            if (techFilter) {
                this.filterByTechnology(techFilter.dataset.tech);
            }

            // Limpiar búsqueda
            if (e.target.closest('#clear-search')) {
                this.setState({ searchQuery: '' });
                const searchInput = this.element.querySelector('#search-input');
                if (searchInput) searchInput.value = '';
                this.applyFilters();
            }

            // Reset filters
            if (e.target.closest('#reset-filters')) {
                this.resetFilters();
            }

            // Toggle technologies visibility
            if (e.target.closest('#toggle-technologies')) {
                this.setState({ showAllTechnologies: !this.state.showAllTechnologies });
                this.render();
            }
            
            // Toggle category view
            if (e.target.closest('#toggle-category-view')) {
                this.setState({ showTechByCategory: !this.state.showTechByCategory });
                this.render();
            }
        });

        // Búsqueda en tiempo real
        this.element.addEventListener('input', (e) => {
            if (e.target.id === 'search-input') {
                this.setState({ searchQuery: e.target.value });
                this.applyFilters();
            }
        });

        // Cambios en ordenamiento
        this.element.addEventListener('change', (e) => {
            if (e.target.id === 'sort-select') {
                    this.setState({ sortBy: e.target.value });
                    this.applyFilters();
            }
        });

        // Formulario de análisis
        this.element.addEventListener('submit', (e) => {
            if (e.target.id === 'analyze-form') {
                e.preventDefault();
                this.handleAnalyzeSubmit(e.target);
            }
        });
    }

    async handleAction(action, repo) {
        switch (action) {
            case 'analyze':
                if (repo) {
                    this.navigateToAnalysis(repo);
                } else {
                    this.showAnalyzeModal();
                }
                break;
            case 'graph':
                this.generateGraph(repo);
                break;
            case 'refresh':
                await this.refreshRepositories();
                break;
        }
    }

    async loadRepositories() {
        this.stateManager.setState({ isLoading: true });

        try {
            const repositories = await this.githubService.getUserRepositoriesDetailed();
            this.stateManager.setState({ repositories });
            this.calculateStats(repositories);
            this.emit('notification:show', {
                message: `${repositories.length} repositorios cargados exitosamente`,
                type: 'success'
            });
        } catch (error) {
            console.error('Error cargando repositorios:', error);
            this.emit('notification:show', {
                message: 'Error cargando repositorios. Verifique su conexión.',
                type: 'error'
            });
        } finally {
            this.stateManager.setState({ isLoading: false });
        }
    }

    async refreshRepositories() {
        this.stateManager.setState({ isLoading: true });

        try {
            const repositories = await this.githubService.getUserRepositories(false);
            this.stateManager.setState({ repositories });
            this.calculateStats(repositories);
            this.emit('notification:show', {
                message: `${repositories.length} repositorios actualizados exitosamente`,
                type: 'success'
            });
        } catch (error) {
            console.error('Error refrescando repositorios:', error);
            this.emit('notification:show', {
                message: 'Error actualizando repositorios',
                type: 'error'
            });
        } finally {
            this.stateManager.setState({ isLoading: false });
        }
    }

    calculateStats(repositories) {
        const languages = {};
        let totalStars = 0;
        let totalForks = 0;

        repositories.forEach(repo => {
            totalStars += repo.stargazers_count || repo.stargazersCount || 0;
            totalForks += repo.forks_count || repo.forksCount || 0;
            
            // Contar lenguajes
            const repoLanguages = repo.languages || repo.languageDistribution || {};
            Object.keys(repoLanguages).forEach(lang => {
                languages[lang] = (languages[lang] || 0) + 1;
            });
        });

        const stats = {
            total: repositories.length,
            public: repositories.filter(r => !(r.private || r.isPrivate)).length,
            private: repositories.filter(r => r.private || r.isPrivate).length,
            languages,
            totalStars,
            totalForks
        };

        this.setState({ stats });
    }

    async updateTechnologies() {
        const { repositories } = this.state;
        
        // ✅ NUEVO: Obtener metadata desde backend (fuente de verdad)
        try {
            const response = await fetch('/api/metadata/technologies', {
                credentials: 'include'
            });
            
            if (response.ok) {
                const metadata = await response.json();
                console.log('✅ Metadata desde backend:', metadata);
                
                // Convertir a formato esperado por el dashboard
                const technologies = new Set();
                const technologyDetails = [];
                
                // Lenguajes (ya filtrados por backend)
                metadata.languages.forEach(lang => {
                    technologies.add(lang.name);
                    // Enriquecer con metadatos visuales del detector
                    const techInfo = window.TechnologyDetector ? 
                        window.TechnologyDetector.findTechnologyInfo(lang.name, 'Programming Languages') : 
                        { icon: '💻', categoryIcon: '💻', priority: 1 };

                    technologyDetails.push({
                        name: lang.name,
                        count: lang.count,
                        category: 'Programming Languages',
                        icon: techInfo.icon,
                        categoryIcon: techInfo.categoryIcon,
                        priority: techInfo.priority,
                        repos: Array.from(lang.repositories || [])
                    });
                });
                
                // Frameworks (ya validados por backend)
                metadata.frameworks.forEach(fw => {
                    technologies.add(fw.name);
                    const techInfo = window.TechnologyDetector ? 
                        window.TechnologyDetector.findTechnologyInfo(fw.name, 'Frameworks') : 
                        { icon: '⚡', categoryIcon: '⚡', priority: 2 };

                    technologyDetails.push({
                        name: fw.name,
                        count: fw.count,
                        category: 'Frameworks & Libraries',
                        icon: techInfo.icon,
                        categoryIcon: techInfo.categoryIcon,
                        priority: techInfo.priority,
                        repos: Array.from(fw.repositories || [])
                    });
                });
                
                // CI/CD Tools
                metadata.cicdTools.forEach(tool => {
                    technologies.add(tool);
                    const techInfo = window.TechnologyDetector ? 
                        window.TechnologyDetector.findTechnologyInfo(tool, 'DevOps') : 
                        { icon: '🛠️', categoryIcon: '🛠️', priority: 3 };

                    technologyDetails.push({
                        name: tool,
                        count: 1, 
                        category: 'DevOps & Tools',
                        icon: techInfo.icon,
                        categoryIcon: techInfo.categoryIcon,
                        priority: techInfo.priority,
                        repos: []
                    });
                });
                
                this.setState({ 
                    technologies,
                    technologyDetails,
                    filteredRepositories: repositories 
                });
                
                console.log(`✅ Tecnologías actualizadas desde backend: ${technologies.size} encontradas`);
                return;
            }
        } catch (error) {
            console.warn('⚠️ Error obteniendo metadata desde backend, usando fallback:', error);
        }
        
        // ❌ FALLBACK: Usar el detector del frontend (viejo comportamiento)
        if (window.TechnologyDetector) {
            const stats = window.TechnologyDetector.getStatistics(repositories);
            const technologies = new Set(stats.technologies.map(t => t.name));
            this.setState({ 
                technologies,
                technologyDetails: stats.technologies,
                technologyCategories: stats.categories,
                filteredRepositories: repositories 
            });
        } else {
            // Fallback más básico
            const technologies = new Set();
            repositories.forEach(repo => {
                const languages = repo.languages || repo.languageDistribution || {};
                Object.keys(languages).forEach(lang => {
                    if (lang && lang.trim()) technologies.add(lang);
                });
            });
            this.setState({ technologies, filteredRepositories: repositories });
        }
    }

    countRepositoriesWithTech(tech) {
        const { repositories } = this.state;
        return repositories.filter(repo => {
            // Buscar en lenguajes
            const languages = repo.languages || repo.languageDistribution || {};
            const hasLanguage = Object.keys(languages).includes(tech);
            
            // Buscar en topics (considerando formato capitalizado)
            const topics = repo.topics || [];
            const hasTopicExact = topics.includes(tech);
            const hasTopicFormatted = topics.some(topic => {
                const formattedTopic = topic.charAt(0).toUpperCase() + topic.slice(1).toLowerCase();
                return formattedTopic === tech;
            });
            
            return hasLanguage || hasTopicExact || hasTopicFormatted;
        }).length;
    }

    filterByTechnology(tech) {
        this.setState({ selectedTech: tech || null, currentPage: 1 });
        this.applyFilters();
    }

    resetFilters() {
        this.setState({ 
            selectedTech: null,
            searchQuery: '',
            currentPage: 1
        });
        
        const searchInput = this.element.querySelector('#search-input');
        if (searchInput) searchInput.value = '';
        
        this.applyFilters();
        this.emit('notification:show', {
            message: 'Filtros reiniciados',
            type: 'info'
        });
    }

    applyFilters() {
        const { repositories, searchQuery, selectedTech, sortBy } = this.state;
        let filtered = [...repositories];

        // Filtro por búsqueda
        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            filtered = filtered.filter(repo => 
                repo.name.toLowerCase().includes(query) ||
                (repo.description && repo.description.toLowerCase().includes(query)) ||
                (repo.language && repo.language.toLowerCase().includes(query)) ||
                (repo.languages && Object.keys(repo.languages).some(lang => lang.toLowerCase().includes(query))) ||
                (repo.languageDistribution && Object.keys(repo.languageDistribution).some(lang => lang.toLowerCase().includes(query))) ||
                (repo.topics && repo.topics.some(topic => topic.toLowerCase().includes(query)))
            );
        }

        // Filtro por tecnología
        if (selectedTech) {
            filtered = filtered.filter(repo => {
                // Buscar en lenguajes
                const languages = repo.languages || repo.languageDistribution || {};
                const hasLanguage = Object.keys(languages).includes(selectedTech);
                
                // Buscar en topics (considerando formato capitalizado)
                const topics = repo.topics || [];
                const hasTopicExact = topics.includes(selectedTech);
                const hasTopicFormatted = topics.some(topic => {
                    const formattedTopic = topic.charAt(0).toUpperCase() + topic.slice(1).toLowerCase();
                    return formattedTopic === selectedTech;
                });
                
                return hasLanguage || hasTopicExact || hasTopicFormatted;
            });
        }

        // Ordenamiento
        filtered.sort((a, b) => {
            let aValue, bValue;
            
            switch (sortBy) {
                case 'name':
                    aValue = a.name.toLowerCase();
                    bValue = b.name.toLowerCase();
                    return aValue < bValue ? -1 : 1;
                case 'stars':
                    aValue = a.stargazers_count || a.stargazersCount || 0;
                    bValue = b.stargazers_count || b.stargazersCount || 0;
                    return bValue - aValue;
                case 'size':
                    aValue = a.size || 0;
                    bValue = b.size || 0;
                    return bValue - aValue;
                case 'created':
                    aValue = new Date(a.created_at || a.createdAt);
                    bValue = new Date(b.created_at || b.createdAt);
                    return bValue - aValue;
                default:
                    aValue = new Date(a.updated_at || a.updatedAt);
                    bValue = new Date(b.updated_at || b.updatedAt);
                    return bValue - aValue;
            }
        });

        this.setState({ filteredRepositories: filtered, currentPage: 1 });
        this.render();
    }

    goToPage(page) {
        this.setState({ currentPage: page });
        this.render();
    }

    navigateToAnalysis(repo) {
        this.stateManager.setState({ currentRepo: repo });
        window.location.href = `/analysis?repo=${encodeURIComponent(repo)}`;
    }

    showAnalyzeModal() {
        const modal = new bootstrap.Modal(document.getElementById('analyze-modal'));
        modal.show();
    }

    handleAnalyzeSubmit(form) {
        const repoUrl = form.querySelector('#repo-url').value.trim();
        if (repoUrl) {
            this.navigateToAnalysis(repoUrl);
        }
    }

    generateGraph(repo) {
        this.emit('notification:show', {
            message: `Generando grafo para ${repo}... ¡Próximamente!`,
            type: 'info'
        });
    }

    getTechIcon(tech) {
        const icons = {
            // Lenguajes de programación
            'JavaScript': '🟨',
            'TypeScript': '🔷',
            'Python': '🐍',
            'Java': '☕',
            'C++': '⚡',
            'C#': '🔷',
            'Go': '🐹',
            'Rust': '🦀',
            'PHP': '🐘',
            'Ruby': '💎',
            'HTML': '🌐',
            'CSS': '🎨',
            'Shell': '🐚',
            'Dockerfile': '🐳',
            
            // Frameworks y librerías
            'Vue': '💚',
            'React': '⚛️',
            'Angular': '🅰️',
            'Node.js': '💚',
            'Express': '🚂',
            'Spring': '🍃',
            'Django': '🎸',
            'Flask': '🧪',
            'Laravel': '🔺',
            
            // Herramientas y tecnologías
            'Docker': '🐳',
            'Kubernetes': '☸️',
            'Git': '📊',
            'Github': '🐙',
            'Gitlab': '🦊',
            'Jenkins': '👷',
            'Terraform': '🌍',
            'Aws': '☁️',
            'Azure': '☁️',
            'Google-cloud': '☁️',
            
            // Tipos de proyecto (topics comunes)
            'Web': '🌐',
            'Api': '🔌',
            'Backend': '⚙️',
            'Frontend': '🎨',
            'Mobile': '📱',
            'Desktop': '🖥️',
            'Game': '🎮',
            'Bot': '🤖',
            'Cli': '⌨️',
            'Library': '📚',
            'Framework': '🏗️',
            'Tool': '🔧',
            'Automation': '⚡',
            'Machine-learning': '🧠',
            'Ai': '🤖',
            'Data': '📊',
            'Database': '🗄️',
            'Security': '🔒',
            'Testing': '🧪',
            'Documentation': '📖',
            'Tutorial': '📚',
            'Example': '💡',
            'Template': '📄',
            'Boilerplate': '🏗️'
        };
        return icons[tech] || '🏷️';
    }

    getLanguageColor(language) {
        const colors = {
            'JavaScript': '#f1e05a',
            'TypeScript': '#2b7489',
            'Python': '#3572A5',
            'Java': '#b07219',
            'C++': '#f34b7d',
            'C#': '#239120',
            'Go': '#00ADD8',
            'Rust': '#dea584',
            'PHP': '#4F5D95',
            'Ruby': '#701516',
            'HTML': '#e34c26',
            'CSS': '#563d7c',
            'Vue': '#4FC08D',
            'Dockerfile': '#384d54',
            'Shell': '#89e051'
        };
        return colors[language] || '#858585';
    }

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        const now = new Date();
        const diffTime = Math.abs(now - date);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        if (diffDays === 1) return 'Ayer';
        if (diffDays < 7) return `Hace ${diffDays} días`;
        if (diffDays < 30) return `Hace ${Math.ceil(diffDays / 7)} semanas`;
        if (diffDays < 365) return `Hace ${Math.ceil(diffDays / 30)} meses`;
        return `Hace ${Math.ceil(diffDays / 365)} años`;
    }

    updateRepository(data) {
        const { repositories } = this.state;
        const index = repositories.findIndex(r => r.full_name === data.full_name);
        
        if (index !== -1) {
            const updatedRepos = [...repositories];
            updatedRepos[index] = { ...updatedRepos[index], ...data };
            
            this.setState({ repositories: updatedRepos });
            this.updateTechnologies();
            this.applyFilters();
            
            this.emit('repository:updated', { 
                repository: data.full_name, 
                changes: data,
                timestamp: Date.now()
            });
        }
    }

    // Modo legacy para compatibilidad
    renderLegacyMode(user, repositories, isLoading, stats) {
        const { showWelcome, showFilters } = this.options;
        this.element.innerHTML = `
            ${showWelcome && user ? this.renderWelcomeSection(user, stats) : ''}
            ${!user ? this.renderUnauthenticatedSection() : ''}
            ${user ? this.renderRepositoriesSection(repositories, isLoading) : ''}
            ${showFilters && user ? this.renderFiltersSection() : ''}
            ${this.renderModals()}
        `;
    }

    renderRepositoriesSection(repositories, isLoading) {
        const { currentPage } = this.state;
        const { pageSize } = this.options;
        
        return `
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">
                        <i class="fas fa-repository me-2"></i>
                        Tus Repositorios
                    </h5>
                    <div class="d-flex gap-2">
                        <button class="btn btn-outline-primary btn-sm" data-action="analyze">
                            <i class="fas fa-search me-1"></i>
                            Analizar Otro Repo
                        </button>
                        <button class="btn btn-outline-secondary btn-sm" data-action="refresh">
                            <i class="fas fa-sync me-1"></i>
                            Actualizar
                        </button>
                    </div>
                </div>
                <div class="card-body">
                    ${isLoading ? this.renderLoadingState() : this.renderRepositoriesList(repositories, currentPage, pageSize)}
                </div>
            </div>
        `;
    }

    renderFiltersSection() {
        // Implementación de filtros legacy si es necesario
        return '';
    }

    destroy() {
        this.emit('dashboard:destroyed', {
            timestamp: Date.now(),
            cleanupComplete: true
        });
        
        super.destroy();
    }
}

// Registrar el componente
ComponentFactory.register('dashboard', DashboardComponent);

console.log('📊 DashboardComponent Unificado registrado'); 