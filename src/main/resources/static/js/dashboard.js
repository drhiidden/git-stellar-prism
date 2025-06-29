/**
 * ================================
 * GITSTELLARPRISM - DASHBOARD JS
 * ================================
 * 
 * Controlador JavaScript para el dashboard de repositorios
 * Sistema de fallback para cuando el DashboardComponent no esté disponible
 */

// Variables específicas del dashboard
let allRepos = [];
let filteredRepos = [];
let currentTechnologies = new Set();
let showAllTechnologies = false;

// Inicialización del dashboard
document.addEventListener('DOMContentLoaded', function() {
    // Solo inicializar si no hay componentes modernos disponibles
    if (typeof ComponentFactory === 'undefined') {
    initDashboard();
    }
});

async function initDashboard() {
    console.log('🔄 Inicializando dashboard legacy...');
    
    try {
        // Verificar si el usuario está autenticado
        const authResponse = await fetch('/api/user/auth-status');
        const authData = await authResponse.json();
        
        if (authData.authenticated) {
            await loadDashboardUserInfo();
            await renderAuthenticatedDashboard();
            console.log('✅ Dashboard legacy completo para usuario autenticado');
        } else {
            renderPublicDashboard();
            console.log('📄 Dashboard legacy público para usuario no autenticado');
        }
        
        setupDashboardEventListeners();
        
    } catch (error) {
        console.error('❌ Error inicializando dashboard legacy:', error);
        showNotification('Error cargando el dashboard', 'error');
        renderErrorDashboard();
    }
}

async function renderAuthenticatedDashboard() {
    const container = document.getElementById('dashboard-container');
    const loadingElement = document.getElementById('initial-loading');
    
    if (loadingElement) loadingElement.style.display = 'block';
    
    try {
        // Cargar información del usuario
        const userResponse = await fetch('/api/user/info');
        const userInfo = userResponse.ok ? await userResponse.json() : null;
        
        // Cargar repositorios
        const reposResponse = await fetch('/api/user/repositories/detailed');
        if (reposResponse.ok) {
            allRepos = await reposResponse.json();
            filteredRepos = [...allRepos];
            updateTechnologiesFromRepos();
        }
        
        // Renderizar dashboard unificado
        container.innerHTML = renderUnifiedDashboard(userInfo, allRepos);
        
        // Configurar eventos específicos
        setupUnifiedEvents();
        
        showNotification(`Dashboard cargado: ${allRepos.length} repositorios`, 'success');
        
    } catch (error) {
        console.error('❌ Error cargando dashboard autenticado:', error);
        renderErrorDashboard();
    } finally {
        if (loadingElement) loadingElement.style.display = 'none';
    }
}

function renderPublicDashboard() {
    const container = document.getElementById('dashboard-container');
    const loadingElement = document.getElementById('initial-loading');
    
    if (loadingElement) loadingElement.style.display = 'none';
    
    container.innerHTML = `
        <div class="public-dashboard">
            <div class="row mb-4">
                <div class="col-md-6 mb-3">
                    <div class="card dashboard-card h-100" onclick="showAnalyzeModal()">
                        <div class="card-body text-center">
                            <i class="fas fa-search fa-3x text-primary mb-3"></i>
                            <h5 class="card-title">Analizar Repositorio</h5>
                            <p class="card-text">Introduce la URL de cualquier repositorio público para crear visualizaciones 3D.</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-3">
                    <div class="card dashboard-card h-100" onclick="showGraphFeatures()">
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
        </div>
    `;
}

function renderErrorDashboard() {
    const container = document.getElementById('dashboard-container');
    const loadingElement = document.getElementById('initial-loading');
    
    if (loadingElement) loadingElement.style.display = 'none';
    
    container.innerHTML = `
        <div class="text-center py-5">
            <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3"></i>
            <h4 class="text-muted">Error cargando dashboard</h4>
            <p class="text-muted">Hubo un problema cargando la información. Inténtalo de nuevo.</p>
            <button class="btn btn-primary" onclick="location.reload()">
                <i class="fas fa-sync me-1"></i>
                Recargar Página
            </button>
        </div>
    `;
}

function renderUnifiedDashboard(userInfo, repositories) {
    const displayName = userInfo ? (userInfo.name || userInfo.login || 'Usuario') : 'Usuario';
    
    return `
        <!-- Sección de bienvenida -->
        ${userInfo ? `
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
                        <div class="stat-number">${repositories.length}</div>
                        <p class="mb-0">Repositorios Disponibles</p>
                    </div>
                </div>
            </div>
        </div>
        ` : ''}

        <!-- Sección unificada de repositorios -->
        <div class="unified-section">
            <!-- Header -->
            <div class="filters-header">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h3 class="mb-1">
                            <i class="fas fa-repository me-2"></i>
                            Explorar Repositorios
                        </h3>
                        <p class="mb-0 opacity-75" id="repo-status">
                            ${repositories.length} repositorios encontrados
                        </p>
                    </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-light btn-sm" onclick="showAnalyzeModal()" title="Analizar otro repositorio">
                            <i class="fas fa-plus me-1"></i>
                            Analizar Otro
                        </button>
                        <button class="btn btn-outline-light btn-sm" onclick="refreshRepositories()" title="Actualizar repositorios">
                            <i class="fas fa-sync me-1"></i>
                            Actualizar
                        </button>
                    </div>
                </div>
            </div>

            <!-- Filtros -->
            <div class="filters-content">
                <div class="row">
                    <div class="col-md-8 mb-3">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0">
                                <i class="fas fa-search text-muted"></i>
                            </span>
                            <input type="text" class="form-control search-box border-start-0" 
                                   id="search-input" 
                                   placeholder="Buscar por nombre, descripción o tecnología...">
                            <button class="btn btn-outline-secondary d-none" type="button" id="clear-search">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    </div>
                    <div class="col-md-4 mb-3">
                        <select class="form-select" id="sort-select">
                            <option value="updated">📅 Última actualización</option>
                            <option value="created">🆕 Fecha de creación</option>
                            <option value="name">🔤 Nombre (A-Z)</option>
                            <option value="stars">⭐ Más populares</option>
                            <option value="size">📊 Tamaño</option>
                        </select>
                    </div>
                </div>

                <!-- Filtros de tecnología -->
                <div class="mb-3" id="tech-filter-section" style="display: none;">
                    <label class="form-label fw-bold mb-2">
                        <i class="fas fa-code me-1"></i>
                        Filtrar por Tecnología
                        <small class="text-muted" id="tech-count"></small>
                    </label>
                    <div class="d-flex flex-wrap gap-2" id="tech-filters"></div>
                </div>

                <!-- Estado de filtros -->
                <div class="d-flex justify-content-between align-items-center">
                    <small class="text-muted" id="filter-status">Mostrando todos los repositorios</small>
                    <button class="btn btn-outline-secondary btn-sm d-none" id="reset-filters">
                        <i class="fas fa-undo me-1"></i>
                        Limpiar Filtros
                    </button>
                </div>
            </div>

            <!-- Lista de repositorios -->
            <div class="repositories-content">
                <div class="row" id="repositories-list">
                    ${renderRepositoriesList(repositories)}
                </div>
            </div>
        </div>
    `;
}

function renderRepositoriesList(repositories) {
    if (!repositories || repositories.length === 0) {
        return `
            <div class="col-12">
                <div class="empty-state">
                    <i class="fas fa-folder-open fa-4x mb-3"></i>
                    <h4>No se encontraron repositorios</h4>
                    <p class="text-muted">No hay repositorios que coincidan with your search criteria.</p>
                </div>
            </div>
        `;
    }
    
    return repositories.map(repo => {
        const languages = repo.languageDistribution || {};
        const primaryLanguage = Object.keys(languages)[0];
        const description = repo.description || 'No hay descripción disponible';
        const stars = repo.stargazersCount || 0;
        const forks = repo.forksCount || 0;
        const isPrivate = repo.isPrivate || false;
        
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
                        
                        ${primaryLanguage ? `
                            <div class="d-flex align-items-center mb-2">
                                <span class="language-dot" style="background-color: ${getLanguageColor(primaryLanguage)}"></span>
                                <small class="text-muted">${primaryLanguage}</small>
                            </div>
                        ` : ''}
                        
                        ${repo.topics && repo.topics.length > 0 ? `
                            <div class="mb-2">
                                ${repo.topics.slice(0, 3).map(topic => `
                                    <span class="badge bg-light text-dark tech-badge me-1">${topic}</span>
                                `).join('')}
                                ${repo.topics.length > 3 ? `<small class="text-muted">+${repo.topics.length - 3} más</small>` : ''}
                            </div>
                        ` : ''}
                        
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <div class="d-flex gap-3">
                                <small class="text-muted">
                                    <i class="fas fa-star text-warning me-1"></i>${stars}
                                </small>
                                <small class="text-muted">
                                    <i class="fas fa-code-branch text-info me-1"></i>${forks}
                                </small>
                            </div>
                            <small class="text-muted">
                                <i class="fas fa-clock me-1"></i>
                                ${formatDate(repo.updatedAt)}
                            </small>
                        </div>
                        
                        <div class="d-grid gap-2">
                            <button class="btn btn-primary btn-sm" onclick="analyzeRepository('${repo.owner}/${repo.name}')">
                                <i class="fas fa-chart-line me-1"></i>
                                Analizar Repositorio
                            </button>
                            <button class="btn btn-outline-info btn-sm" onclick="generateGraph('${repo.owner}/${repo.name}')">
                                <i class="fas fa-project-diagram me-1"></i>
                                Generar Grafo
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function updateTechnologiesFromRepos() {
    currentTechnologies.clear();
    
    allRepos.forEach(repo => {
        // Incluir lenguajes detectados automáticamente
        const languages = repo.languageDistribution || {};
        Object.keys(languages).forEach(lang => {
            if (lang && lang.trim()) {
                currentTechnologies.add(lang);
            }
        });
        
        // Incluir topics/etiquetas del repositorio
        const topics = repo.topics || [];
        topics.forEach(topic => {
            if (topic && topic.trim()) {
                // Capitalizar primera letra para consistencia visual
                const formattedTopic = topic.charAt(0).toUpperCase() + topic.slice(1).toLowerCase();
                currentTechnologies.add(formattedTopic);
            }
        });
    });
    
    renderTechnologyFilters();
}

function renderTechnologyFilters() {
    const techFiltersContainer = document.getElementById('tech-filters');
    const techFilterSection = document.getElementById('tech-filter-section');
    const techCount = document.getElementById('tech-count');
    
    if (!techFiltersContainer || currentTechnologies.size === 0) {
        if (techFilterSection) techFilterSection.style.display = 'none';
        return;
    }
    
    techFilterSection.style.display = 'block';
    techCount.textContent = `(${currentTechnologies.size} tecnologías encontradas)`;
    
    // Convertir a array y ordenar por frecuencia
    const techArray = Array.from(currentTechnologies).map(tech => {
        const count = allRepos.filter(repo => {
            // Buscar en lenguajes
            const languages = repo.languageDistribution || {};
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
        return { tech, count };
    }).sort((a, b) => b.count - a.count);
    
    const maxVisible = 12; // Mostrar solo las 12 más populares por defecto
    const visibleTechs = showAllTechnologies ? techArray : techArray.slice(0, maxVisible);
    const hiddenCount = techArray.length - maxVisible;
    
    let filtersHTML = `
        <button class="btn tech-pill btn-primary active" data-tech="" onclick="filterByTechnology('')">
            <i class="fas fa-globe me-1"></i>
            Todas (${allRepos.length})
        </button>
    `;
    
    visibleTechs.forEach(({ tech, count }) => {
        filtersHTML += `
            <button class="btn tech-pill btn-outline-primary" data-tech="${tech}" onclick="filterByTechnology('${tech}')">
                ${getTechIcon(tech)} ${tech} (${count})
            </button>
        `;
    });
    
    // Botón toggle si hay tecnologías ocultas
    if (hiddenCount > 0) {
        filtersHTML += `
            <button class="btn btn-outline-secondary tech-pill" onclick="toggleTechnologiesVisibility()">
                <i class="fas fa-${showAllTechnologies ? 'chevron-up' : 'chevron-down'} me-1"></i>
                ${showAllTechnologies ? 'Ver menos' : `Ver ${hiddenCount} más`}
            </button>
        `;
    }
    
    techFiltersContainer.innerHTML = filtersHTML;
}

function setupUnifiedEvents() {
    const searchInput = document.getElementById('search-input');
    const sortSelect = document.getElementById('sort-select');
    const clearSearch = document.getElementById('clear-search');
    const resetFilters = document.getElementById('reset-filters');
    
    if (searchInput) {
        searchInput.addEventListener('input', handleSearchInput);
    }
    
    if (sortSelect) {
        sortSelect.addEventListener('change', handleSortChange);
    }
    
    if (clearSearch) {
        clearSearch.addEventListener('click', clearSearchInput);
    }
    
    if (resetFilters) {
        resetFilters.addEventListener('click', resetAllFilters);
    }
}

function handleSearchInput(e) {
    const query = e.target.value.trim();
    const clearBtn = document.getElementById('clear-search');
    
    if (query) {
        clearBtn.classList.remove('d-none');
    } else {
        clearBtn.classList.add('d-none');
    }
    
    applyFilters();
}

function handleSortChange() {
    applyFilters();
}

function clearSearchInput() {
    const searchInput = document.getElementById('search-input');
    const clearBtn = document.getElementById('clear-search');
    
    searchInput.value = '';
    clearBtn.classList.add('d-none');
    applyFilters();
}

function resetAllFilters() {
    const searchInput = document.getElementById('search-input');
    const sortSelect = document.getElementById('sort-select');
    const clearBtn = document.getElementById('clear-search');
    const resetBtn = document.getElementById('reset-filters');
    
    // Reset inputs
    searchInput.value = '';
    sortSelect.value = 'updated';
    
    // Hide buttons
    clearBtn.classList.add('d-none');
    resetBtn.classList.add('d-none');
    
    // Reset tech filters
    const techButtons = document.querySelectorAll('[data-tech]');
    techButtons.forEach(btn => {
        btn.classList.remove('active', 'btn-primary');
        btn.classList.add('btn-outline-primary');
    });
    
    const allButton = document.querySelector('[data-tech=""]');
    if (allButton) {
        allButton.classList.remove('btn-outline-primary');
        allButton.classList.add('btn-primary', 'active');
    }
    
    applyFilters();
    showNotification('Filtros reiniciados', 'info');
}

// Función para alternar la visibilidad de todas las tecnologías
function toggleTechnologiesVisibility() {
    showAllTechnologies = !showAllTechnologies;
    renderTechnologyFilters();
}

function filterByTechnology(tech) {
    // Update button states
    const techButtons = document.querySelectorAll('[data-tech]');
    techButtons.forEach(btn => {
        btn.classList.remove('active', 'btn-primary');
        btn.classList.add('btn-outline-primary');
    });
    
    const selectedButton = document.querySelector(`[data-tech="${tech}"]`);
    if (selectedButton) {
        selectedButton.classList.remove('btn-outline-primary');
        selectedButton.classList.add('btn-primary', 'active');
    }
    
    applyFilters();
}

function applyFilters() {
    const searchInput = document.getElementById('search-input');
    const sortSelect = document.getElementById('sort-select');
    const activeTech = document.querySelector('[data-tech].active');
    
    const searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const sortBy = sortSelect ? sortSelect.value : 'updated';
    const selectedTech = activeTech ? activeTech.dataset.tech : '';
    
    // Apply filters
    let filtered = [...allRepos];
    
    // Search filter
    if (searchQuery) {
        filtered = filtered.filter(repo => 
            repo.name.toLowerCase().includes(searchQuery) ||
            (repo.description && repo.description.toLowerCase().includes(searchQuery)) ||
            (repo.languageDistribution && Object.keys(repo.languageDistribution)
                .some(lang => lang.toLowerCase().includes(searchQuery))) ||
            (repo.topics && repo.topics.some(topic => topic.toLowerCase().includes(searchQuery)))
        );
    }
    
    // Technology filter
    if (selectedTech) {
        filtered = filtered.filter(repo => {
            // Buscar en lenguajes
            const languages = repo.languageDistribution || {};
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
    
    // Sort
    filtered.sort((a, b) => {
        switch (sortBy) {
            case 'name':
                return a.name.localeCompare(b.name);
            case 'stars':
                return (b.stargazersCount || 0) - (a.stargazersCount || 0);
            case 'size':
                return (b.size || 0) - (a.size || 0);
            case 'created':
                return new Date(b.createdAt) - new Date(a.createdAt);
            default:
                return new Date(b.updatedAt) - new Date(a.updatedAt);
        }
    });
    
    filteredRepos = filtered;
    
    // Update UI
    updateRepositoriesList();
    updateFilterStatus(searchQuery, selectedTech);
    updateResetButton(searchQuery || selectedTech);
}

function updateRepositoriesList() {
    const container = document.getElementById('repositories-list');
    if (container) {
        container.innerHTML = renderRepositoriesList(filteredRepos);
    }
}

function updateFilterStatus(searchQuery, selectedTech) {
    const statusElement = document.getElementById('filter-status');
    const repoStatus = document.getElementById('repo-status');
    
    let status = '';
    const filters = [];
    
    if (searchQuery) filters.push(`Búsqueda: "${searchQuery}"`);
    if (selectedTech) filters.push(`Tecnología: ${selectedTech}`);
    
    if (filters.length > 0) {
        status = `Filtros activos: ${filters.join(', ')}`;
    } else {
        status = 'Mostrando todos los repositorios';
    }
    
    if (statusElement) statusElement.textContent = status;
    if (repoStatus) {
        repoStatus.textContent = `${allRepos.length} repositorios encontrados${
            filteredRepos.length !== allRepos.length ? ` • mostrando ${filteredRepos.length} filtrados` : ''
        }`;
    }
}

function updateResetButton(hasFilters) {
    const resetBtn = document.getElementById('reset-filters');
    if (resetBtn) {
        if (hasFilters) {
            resetBtn.classList.remove('d-none');
        } else {
            resetBtn.classList.add('d-none');
        }
    }
}

// Legacy compatibility functions
async function loadDashboardUserInfo() {
    // Compatibility function - no longer needed in unified system
}

function setupDashboardEventListeners() {
    const analyzeForm = document.getElementById('analyzeForm');
    if (analyzeForm) {
        analyzeForm.addEventListener('submit', handleAnalyzeSubmit);
    }
    
    // Modal events
    const modal = document.getElementById('analyzeModal');
    if (modal) {
        modal.addEventListener('hidden.bs.modal', function() {
            const form = modal.querySelector('form');
            if (form) form.reset();
        });
    }
}

function handleAnalyzeSubmit(e) {
    e.preventDefault();
    const repoUrl = document.getElementById('repoUrl').value.trim();
    
    if (!repoUrl) {
        showNotification('Por favor ingresa una URL de repositorio', 'warning');
        return;
    }
    
    const repoPattern = /^[\w.-]+\/[\w.-]+$/;
    if (!repoPattern.test(repoUrl)) {
        showNotification('Formato inválido. Usa: owner/repository', 'error');
        return;
    }
    
    window.location.href = `/analysis?repo=${encodeURIComponent(repoUrl)}`;
}

function showAnalyzeModal() {
    const modal = document.getElementById('analyzeModal');
    if (modal) {
        const bootstrapModal = new bootstrap.Modal(modal);
        bootstrapModal.show();
    }
}

function analyzeRepository(fullName) {
    window.location.href = `/analysis?repo=${encodeURIComponent(fullName)}`;
}

function generateGraph(fullName) {
    showNotification(`Generando grafo para ${fullName}... ¡Próximamente!`, 'info');
}

function showGraphFeatures() {
    showNotification('Funcionalidad de grafos en desarrollo. ¡Próximamente!', 'info');
}

// Función para actualizar repositorios
async function refreshRepositories() {
    const loadingElement = document.getElementById('initial-loading');
    
    if (loadingElement) loadingElement.style.display = 'block';
    
    try {
        const response = await fetch('/api/user/repositories/refresh');
        if (response.ok) {
            allRepos = await response.json();
            filteredRepos = [...allRepos];
            
            updateTechnologiesFromRepos();
            updateRepositoriesList();
            
            showNotification(`${allRepos.length} repositorios actualizados exitosamente`, 'success');
        } else {
            throw new Error('Error actualizando repositorios');
        }
    } catch (error) {
        console.error('❌ Error actualizando repositorios:', error);
        showNotification('Error actualizando repositorios', 'error');
    } finally {
        if (loadingElement) loadingElement.style.display = 'none';
    }
}

function showNotification(message, type = 'info') {
    const alertClass = {
        success: 'alert-success',
        error: 'alert-danger',
        warning: 'alert-warning',
        info: 'alert-info'
    }[type] || 'alert-info';
    
    const notification = document.createElement('div');
    notification.className = `alert ${alertClass} alert-dismissible fade show position-fixed top-0 end-0 m-3`;
    notification.style.zIndex = '9999';
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
}

// Utility functions
function getTechIcon(tech) {
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

function getLanguageColor(language) {
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

function formatDate(dateString) {
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