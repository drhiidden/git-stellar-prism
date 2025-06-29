/**
 * ================================
 * GITSTELLARPRISM - DASHBOARD JS
 * ================================
 * 
 * Controlador JavaScript para el dashboard de repositorios
 * Funcionalidades:
 * - Gestión de repositorios del usuario
 * - Filtrado por tecnologías
 * - Navegación hacia análisis
 */

// Variables específicas del dashboard
let allRepos = [];
let filteredRepos = [];

// Inicialización del dashboard
document.addEventListener('DOMContentLoaded', function() {
    initDashboard();
});

async function initDashboard() {
    console.log('Inicializando dashboard...');
    try {
        // Verificar si el usuario está autenticado
        const authResponse = await fetch('/api/user/auth-status');
        const authData = await authResponse.json();
        
        if (authData.authenticated) {
            await loadDashboardUserInfo();
            setupDashboardEventListeners();
            // Para usuarios autenticados, cargar repositorios automáticamente
            await loadInitialRepositories();
            console.log('✅ Dashboard completo para usuario autenticado');
        } else {
            // Para usuarios no autenticados, solo configurar listeners básicos
            setupDashboardEventListeners();
            console.log('📄 Dashboard público para usuario no autenticado');
        }
    } catch (error) {
        console.error('Error inicializando dashboard:', error);
        showNotification('Error cargando el dashboard', 'error');
    }
}

async function loadDashboardUserInfo() {
    try {
        const response = await fetch('/api/user/info');
        if (response.ok) {
            const userInfo = await response.json();
            updateDashboardUserDisplay(userInfo);
        }
    } catch (error) {
        console.error('Error cargando información del usuario:', error);
    }
}

function updateDashboardUserDisplay(userInfo) {
    if (!userInfo) return;
    
    const welcomeUserName = document.getElementById('welcomeUserName');
    const userName = document.getElementById('userName');
    const userAvatar = document.getElementById('userAvatar');
    const defaultAvatar = document.getElementById('defaultAvatar');
    
    const displayName = userInfo.name || userInfo.login || 'Usuario';
    
    if (welcomeUserName) welcomeUserName.textContent = displayName;
    if (userName) userName.textContent = displayName;
    
    if (userAvatar && userInfo.avatar_url) {
        userAvatar.src = userInfo.avatar_url;
        userAvatar.style.display = 'block';
        if (defaultAvatar) defaultAvatar.style.display = 'none';
    }
    
    const totalRepos = document.getElementById('totalRepos');
    if (totalRepos && userInfo.public_repos !== undefined) {
        const total = (userInfo.public_repos || 0) + (userInfo.total_private_repos || 0);
        totalRepos.textContent = total;
    }
}

function setupDashboardEventListeners() {
    const analyzeForm = document.getElementById('analyzeForm');
    if (analyzeForm) {
        analyzeForm.addEventListener('submit', handleAnalyzeSubmit);
    }
    
    // Listener para botón de actualizar repositorios
    const refreshBtn = document.getElementById('refreshBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', refreshRepositories);
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
    const modal = new bootstrap.Modal(document.getElementById('analyzeModal'));
    modal.show();
}

// Función para cargar repositorios inicialmente (usuarios autenticados)
async function loadInitialRepositories() {
    const initialLoading = document.getElementById('initialLoading');
    console.log('🔄 Iniciando carga de repositorios...');
    
    try {
        const response = await fetch('/api/user/repositories/detailed');
        if (response.ok) {
            allRepos = await response.json();
            filteredRepos = [...allRepos];
            
            displayRepositories();
            generateTechFilters();
            updateRepoCount();
            
            console.log(`✅ ${allRepos.length} repositorios cargados automáticamente`);
            showNotification(`${allRepos.length} repositorios cargados exitosamente`, 'success');
        } else {
            throw new Error('Error cargando repositorios');
        }
    } catch (error) {
        console.error('❌ Error cargando repositorios:', error);
        showNotification('Error cargando repositorios', 'error');
        
        // Mostrar mensaje de error en lugar de loading
        const reposList = document.getElementById('reposList');
        if (reposList) {
            reposList.innerHTML = `
                <div class="col-12 text-center py-4">
                    <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3"></i>
                    <h5 class="text-muted">Error cargando repositorios</h5>
                    <p class="text-muted">Verifica tu conexión e inténtalo de nuevo.</p>
                    <button class="btn btn-primary" onclick="refreshRepositories()">
                        <i class="fas fa-sync me-1"></i> Reintentar
                    </button>
                </div>
            `;
        }
    } finally {
        // Asegurar que el loading desaparezca SIEMPRE
        if (initialLoading) {
            console.log('🔄 Ocultando indicador de carga');
            initialLoading.style.display = 'none';
        }
    }
}

// Función para mostrar repositorios (compatibilidad con código anterior)
async function showMyRepos() {
    await loadInitialRepositories();
}

// Función para actualizar repositorios (botón refresh)
async function refreshRepositories() {
    const initialLoading = document.getElementById('initialLoading');
    const refreshBtn = document.querySelector('button[onclick="refreshRepositories()"]');
    
    // Mostrar loading y deshabilitar botón
    if (initialLoading) {
        initialLoading.style.display = 'block';
    }
    if (refreshBtn) {
        refreshBtn.disabled = true;
        refreshBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Actualizando...';
    }
    
    try {
        const response = await fetch('/api/user/repositories/refresh');
        if (response.ok) {
            allRepos = await response.json();
            filteredRepos = [...allRepos];
            
            displayRepositories();
            generateTechFilters();
            updateRepoCount();
            
            showNotification(`${allRepos.length} repositorios actualizados exitosamente`, 'success');
            console.log(`🔄 ${allRepos.length} repositorios actualizados desde GitHub`);
        } else {
            throw new Error('Error actualizando repositorios');
        }
    } catch (error) {
        console.error('❌ Error actualizando repositorios:', error);
        showNotification('Error actualizando repositorios', 'error');
    } finally {
        // Restaurar estado del botón y ocultar loading
        if (initialLoading) {
            initialLoading.style.display = 'none';
        }
        if (refreshBtn) {
            refreshBtn.disabled = false;
            refreshBtn.innerHTML = '<i class="fas fa-sync me-1"></i> Actualizar';
        }
    }
}

function hideMyRepos() {
    document.getElementById('reposCard').style.display = 'none';
    document.getElementById('filtersCard').style.display = 'none';
}

function displayRepositories() {
    const reposList = document.getElementById('reposList');
    
    if (filteredRepos.length === 0) {
        reposList.innerHTML = `
            <div class="col-12 text-center py-4">
                <i class="fas fa-folder-open fa-3x text-muted mb-3"></i>
                <h5 class="text-muted">No se encontraron repositorios</h5>
                <p class="text-muted">Intenta ajustar los filtros o actualizar la lista.</p>
                <button class="btn btn-outline-primary mt-2" onclick="refreshRepositories()">
                    <i class="fas fa-sync me-1"></i> Actualizar Lista
                </button>
            </div>
        `;
        return;
    }
    
    reposList.innerHTML = filteredRepos.map(repo => `
        <div class="col-md-6 col-lg-4 mb-3">
            <div class="card repo-card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <h6 class="card-title mb-0">
                            <i class="fas fa-${repo.isPrivate ? 'lock' : 'unlock'} me-1"></i>
                            ${repo.name}
                        </h6>
                        <div class="d-flex gap-1">
                            <span class="badge bg-secondary">${repo.stargazersCount || 0} ⭐</span>
                            <span class="badge bg-info">${repo.forksCount || 0} 🍴</span>
                        </div>
                    </div>
                    <p class="card-text small text-muted mb-2">
                        ${repo.description || 'Sin descripción'}
                    </p>
                    <div class="d-flex flex-wrap gap-1 mb-3">
                        ${repo.languageDistribution ? Object.keys(repo.languageDistribution)
                            .slice(0, 3)
                            .map(lang => `<span class="badge bg-primary tech-badge">${lang}</span>`)
                            .join('') : ''}
                        ${repo.topics ? repo.topics.slice(0, 2).map(topic => 
                            `<span class="badge bg-light text-dark tech-badge">${topic}</span>`
                        ).join('') : ''}
                    </div>
                    <div class="d-grid gap-2">
                        <button class="btn btn-primary btn-sm" onclick="analyzeRepository('${repo.owner}/${repo.name}')">
                            <i class="fas fa-chart-line me-1"></i>
                            Analizar
                        </button>
                        <button class="btn btn-outline-info btn-sm" onclick="generateGraph('${repo.owner}/${repo.name}')">
                            <i class="fas fa-project-diagram me-1"></i>
                            Generar Grafo
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function analyzeRepository(fullName) {
    window.location.href = `/analysis?repo=${encodeURIComponent(fullName)}`;
}

function generateTechFilters() {
    const allLanguages = new Set();
    allRepos.forEach(repo => {
        if (repo.languageDistribution) {
            Object.keys(repo.languageDistribution).forEach(lang => {
                if (lang && lang.trim()) {
                    allLanguages.add(lang);
                }
            });
        }
    });

    const techFilters = document.getElementById('techFilters');
    if (techFilters && allLanguages.size > 0) {
        const sortedLanguages = Array.from(allLanguages).sort();
        
        techFilters.innerHTML = `
            <button class="btn btn-outline-secondary btn-sm me-2 mb-2 active" onclick="filterByTech('')">
                Todos (${allRepos.length})
            </button>
            ${sortedLanguages.map(lang => {
                const count = allRepos.filter(repo => 
                    repo.languageDistribution && repo.languageDistribution[lang]
                ).length;
                return `<button class="btn btn-outline-primary btn-sm me-2 mb-2" onclick="filterByTech('${lang}')">
                    ${lang} (${count})
                </button>`;
            }).join('')}
        `;
    }
}

function filterByTech(tech) {
    if (!tech) {
        filteredRepos = [...allRepos];
    } else {
        filteredRepos = allRepos.filter(repo => 
            repo.languageDistribution && repo.languageDistribution[tech]
        );
    }
    
    displayRepositories();
    updateRepoCount();
    updateFilterButtons(tech);
}

function updateFilterButtons(activeTech) {
    const techFilters = document.getElementById('techFilters');
    if (techFilters) {
        const buttons = techFilters.querySelectorAll('button');
        buttons.forEach(btn => {
            btn.classList.remove('active');
            btn.classList.remove('btn-secondary');
            btn.classList.add('btn-outline-primary');
            
            if ((!activeTech && btn.textContent.includes('Todos')) || 
                (activeTech && btn.textContent.includes(activeTech))) {
                btn.classList.add('active');
                btn.classList.remove('btn-outline-primary');
                btn.classList.add('btn-secondary');
            }
        });
    }
}

function updateRepoCount() {
    const repoCount = document.getElementById('repoCount');
    if (repoCount) {
        repoCount.textContent = `${filteredRepos.length} repositorio(s)`;
    }
}

function showGraphFeatures() {
    showNotification('Funcionalidad de grafos en desarrollo. ¡Próximamente!', 'info');
}

function generateGraph(fullName) {
    // Funcionalidad para generar grafos - placeholder
    showNotification(`Generando grafo para ${fullName}... ¡Próximamente!`, 'info');
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