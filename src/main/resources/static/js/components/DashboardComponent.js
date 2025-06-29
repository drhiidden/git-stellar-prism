/**
 * Componente Dashboard - Gestión de repositorios y filtros
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
            autoLoad: true
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
            this.updateFilters();
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
            sortOrder: 'desc',
            currentPage: 1,
            stats: {
                total: 0,
                public: 0,
                private: 0,
                languages: {}
            }
        });
    }

    async render() {
        if (!this.element) return;

        const { user, repositories, isLoading, stats } = this.state;
        const { showWelcome, showFilters } = this.options;

        this.element.innerHTML = `
            ${showWelcome && user ? this.renderWelcomeSection(user, stats) : ''}
            ${!user ? this.renderUnauthenticatedSection() : ''}
            ${user ? this.renderRepositoriesSection(repositories, isLoading) : ''}
            ${showFilters && user ? this.renderFiltersSection() : ''}
            ${this.renderModals()}
        `;
    }

    renderWelcomeSection(user, stats) {
        console.log('👤 DashboardComponent - Renderizando usuario:', user);
        console.log('👤 DashboardComponent - user.name:', user.name);
        console.log('👤 DashboardComponent - user.login:', user.login);
        
        const displayName = user.name || user.login || user.id || 'Usuario';
        console.log('👤 DashboardComponent - Nombre a mostrar:', displayName);
        
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
                            Crea grafos interactivos y obtén insights valiosos de tu código.
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

    renderRepositoriesSection(repositories, isLoading) {
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
                    ${isLoading ? this.renderLoadingState() : this.renderRepositoriesList(repositories)}
                </div>
            </div>
        `;
    }

    renderLoadingState() {
        return `
            <div class="d-flex justify-content-center mb-3">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Cargando repositorios...</span>
                </div>
            </div>
        `;
    }

    renderRepositoriesList(repositories) {
        if (!repositories || repositories.length === 0) {
            return `
                <div class="text-center py-4">
                    <i class="fas fa-inbox fa-3x text-muted mb-3"></i>
                    <h5 class="text-muted">No hay repositorios disponibles</h5>
                    <p class="text-muted">Haz clic en "Actualizar" para cargar tus repositorios</p>
                </div>
            `;
        }

        const { filteredRepositories, currentPage } = this.state;
        const { pageSize } = this.options;
        const startIndex = (currentPage - 1) * pageSize;
        const endIndex = startIndex + pageSize;
        const paginatedRepos = filteredRepositories.slice(startIndex, endIndex);

        return `
            <div class="row" id="repositories-grid">
                ${paginatedRepos.map(repo => this.renderRepositoryCard(repo)).join('')}
            </div>
            ${this.renderPagination(filteredRepositories.length, currentPage, pageSize)}
        `;
    }

    renderRepositoryCard(repo) {
        const languages = repo.languages || {};
        const primaryLanguage = Object.keys(languages)[0];
        const languageColor = this.getLanguageColor(primaryLanguage);

        return `
            <div class="col-md-6 col-lg-4 mb-3">
                <div class="card repo-card h-100" data-repo="${repo.full_name}">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h6 class="card-title mb-0">${repo.name}</h6>
                            <span class="badge ${repo.private ? 'bg-warning' : 'bg-success'} text-dark">
                                <i class="fas ${repo.private ? 'fa-lock' : 'fa-globe'} me-1"></i>
                                ${repo.private ? 'Privado' : 'Público'}
                            </span>
                        </div>
                        <p class="card-text text-muted small mb-2">
                            ${repo.description || 'Sin descripción'}
                        </p>
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div class="d-flex align-items-center">
                                ${primaryLanguage ? `
                                    <span class="language-dot" style="background-color: ${languageColor}"></span>
                                    <small class="text-muted">${primaryLanguage}</small>
                                ` : ''}
                            </div>
                            <small class="text-muted">
                                <i class="fas fa-star me-1"></i>${repo.stargazers_count || 0}
                            </small>
                        </div>
                        <div class="d-flex gap-2">
                            <button class="btn btn-primary btn-sm flex-fill" data-action="analyze" data-repo="${repo.full_name}">
                                <i class="fas fa-search me-1"></i>
                                Analizar
                            </button>
                            <button class="btn btn-outline-info btn-sm" data-action="graph" data-repo="${repo.full_name}">
                                <i class="fas fa-project-diagram me-1"></i>
                                Grafo
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    renderFiltersSection() {
        const { technologies, selectedTech, searchQuery } = this.state;

        return `
            <div class="card mb-4" id="filters-card">
                <div class="card-header">
                    <h5 class="mb-0">
                        <i class="fas fa-filter me-2"></i>
                        Filtros y Búsqueda
                    </h5>
                </div>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Buscar repositorios</label>
                            <input type="text" class="form-control" id="search-input" 
                                   placeholder="Nombre o descripción..." value="${searchQuery}">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Ordenar por</label>
                            <select class="form-select" id="sort-select">
                                <option value="updated">Última actualización</option>
                                <option value="created">Fecha de creación</option>
                                <option value="name">Nombre</option>
                                <option value="stars">Estrellas</option>
                            </select>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Filtrar por tecnología</label>
                        <div class="d-flex flex-wrap gap-2" id="tech-filters">
                            <button class="btn btn-outline-secondary btn-sm ${!selectedTech ? 'active' : ''}" 
                                    data-tech="">
                                Todas
                            </button>
                            ${Array.from(technologies).map(tech => `
                                <button class="btn btn-outline-secondary btn-sm ${selectedTech === tech ? 'active' : ''}" 
                                        data-tech="${tech}">
                                    ${tech}
                                </button>
                            `).join('')}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    renderPagination(total, currentPage, pageSize) {
        const totalPages = Math.ceil(total / pageSize);
        
        if (totalPages <= 1) return '';

        return `
            <nav aria-label="Paginación de repositorios">
                <ul class="pagination justify-content-center">
                    <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                        <a class="page-link" href="#" data-page="${currentPage - 1}">Anterior</a>
                    </li>
                    ${Array.from({ length: totalPages }, (_, i) => i + 1)
                        .map(page => `
                            <li class="page-item ${page === currentPage ? 'active' : ''}">
                                <a class="page-link" href="#" data-page="${page}">${page}</a>
                            </li>
                        `).join('')}
                    <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
                        <a class="page-link" href="#" data-page="${currentPage + 1}">Siguiente</a>
                    </li>
                </ul>
            </nav>
        `;
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
        });

        // Búsqueda en tiempo real
        this.element.addEventListener('input', (e) => {
            if (e.target.id === 'search-input') {
                this.setState({ searchQuery: e.target.value });
                this.applyFilters();
            }
        });

        // Cambio de ordenamiento
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
        } catch (error) {
            console.error('Error cargando repositorios:', error);
            this.emit('notification:show', {
                message: 'Error cargando repositorios',
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
                message: 'Repositorios actualizados',
                type: 'success'
            });
        } catch (error) {
            console.error('Error refrescando repositorios:', error);
            this.emit('notification:show', {
                message: 'Error refrescando repositorios',
                type: 'error'
            });
        } finally {
            this.stateManager.setState({ isLoading: false });
        }
    }

    calculateStats(repositories) {
        const stats = {
            total: repositories.length,
            public: repositories.filter(r => !r.private).length,
            private: repositories.filter(r => r.private).length,
            languages: {}
        };

        repositories.forEach(repo => {
            if (repo.languages) {
                Object.keys(repo.languages).forEach(lang => {
                    stats.languages[lang] = (stats.languages[lang] || 0) + 1;
                });
            }
        });

        this.setState({ stats });
    }

    updateFilters() {
        const { repositories } = this.state;
        const technologies = new Set();

        repositories.forEach(repo => {
            if (repo.languages) {
                Object.keys(repo.languages).forEach(lang => technologies.add(lang));
            }
        });

        this.setState({ technologies, filteredRepositories: repositories });
        this.applyFilters();
    }

    applyFilters() {
        const { repositories, searchQuery, selectedTech, sortBy, sortOrder } = this.state;
        
        let filtered = [...repositories];

        // Filtro por búsqueda
        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            filtered = filtered.filter(repo => 
                repo.name.toLowerCase().includes(query) ||
                (repo.description && repo.description.toLowerCase().includes(query))
            );
        }

        // Filtro por tecnología
        if (selectedTech) {
            filtered = filtered.filter(repo => 
                repo.languages && Object.keys(repo.languages).includes(selectedTech)
            );
        }

        // Ordenamiento
        filtered.sort((a, b) => {
            let aValue, bValue;
            
            switch (sortBy) {
                case 'name':
                    aValue = a.name.toLowerCase();
                    bValue = b.name.toLowerCase();
                    break;
                case 'stars':
                    aValue = a.stargazers_count || 0;
                    bValue = b.stargazers_count || 0;
                    break;
                case 'created':
                    aValue = new Date(a.created_at);
                    bValue = new Date(b.created_at);
                    break;
                default:
                    aValue = new Date(a.updated_at);
                    bValue = new Date(b.updated_at);
            }

            if (sortOrder === 'desc') {
                return aValue > bValue ? -1 : 1;
            }
            return aValue < bValue ? -1 : 1;
        });

        this.setState({ 
            filteredRepositories: filtered,
            currentPage: 1 
        });
    }

    filterByTechnology(tech) {
        this.setState({ selectedTech: tech || null });
        this.applyFilters();
    }

    goToPage(page) {
        this.setState({ currentPage: page });
    }

    navigateToAnalysis(repo) {
        // Establecer el repositorio actual en el estado global para el tiempo real
        this.stateManager.setState({ currentRepo: repo });
        
        this.router.navigate('/analysis', { repo });
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
            message: `Generando grafo para ${repo} (funcionalidad en desarrollo)`,
            type: 'info'
        });
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
            'Ruby': '#701516'
        };
        return colors[language] || '#858585';
    }

    updateRepository(data) {
        const { repositories } = this.state;
        const index = repositories.findIndex(r => r.full_name === data.full_name);
        
        if (index !== -1) {
            repositories[index] = { ...repositories[index], ...data };
            this.setState({ repositories });
            this.applyFilters();
        }
    }
}

// Registrar el componente
ComponentFactory.register('dashboard', DashboardComponent);

console.log('📊 DashboardComponent registrado'); 