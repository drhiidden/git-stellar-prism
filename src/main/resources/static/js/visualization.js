/**
 * Visualización de repositorios GitHub con Three.js
 * Sistema de Git Graph con timeline y visualización de ramas
 */

// Variables globales
let scene, camera, renderer;
let commits = [];
let nodes = [];
let connections = [];
let controls;
let raycaster, mouse;
let infoTooltip;

// Configuración
const config = {
    nodeSize: 8, // Nodos más grandes
    nodeColors: {
        commit: 0x28a745,      // Verde para commits normales
        merge: 0x007bff,       // Azul para merges
        branch: 0xffc107,      // Amarillo para branches
        tag: 0xdc3545,         // Rojo para tags
        head: 0x6f42c1,        // Púrpura para HEAD
        recent: 0x17a2b8,      // Cyan para commits recientes
        old: 0x6c757d          // Gris para commits antiguos
    },
    backgroundColor: 0x1a1a1a,
    gridColor: 0x333333,
    connectionColor: 0x00d4ff,  // Cyan más brillante para conexiones
    branchSpacing: 25,          // Más espaciado
    commitSpacing: 20,          // Más espaciado
    animationSpeed: 0.002,
    glowIntensity: 0.5         // Intensidad del glow
};

// Variables para git graph
let gitGraph = {
    branches: new Map(),
    commitPositions: new Map(),
    maxBranchLevel: 0
};

/**
 * ======================================
 * SISTEMA DE VISTAS ALTERNAS CON PAGINACIÓN
 * ======================================
 */

// Namespace para evitar conflictos de variables globales
window.ViewSystem = window.ViewSystem || {
    currentView: '3d',
    timelineCommits: [],
    currentPage: 1,
    commitsPerPage: 30, // Aumentar a 30 por página para aprovechar el espacio
    totalCommits: 0
};

/**
 * Cambia entre vistas (3D y Timeline)
 */
function switchToView(viewType) {
    console.log(`🔄 Cambiando a vista: ${viewType}`);
    
    const view3d = document.getElementById('view-3d');
    const viewTimeline = document.getElementById('view-timeline');
    const btn3d = document.getElementById('btn-view-3d');
    const btnTimeline = document.getElementById('btn-view-timeline');
    
    if (!view3d || !viewTimeline || !btn3d || !btnTimeline) {
        console.error('❌ Elementos de vista no encontrados');
        return;
    }
    
    // Actualizar vista actual
    window.ViewSystem.currentView = viewType;
    
    // Ocultar todas las vistas
    view3d.classList.remove('active');
    viewTimeline.classList.remove('active');
    
    // Remover clases activas de botones
    btn3d.classList.remove('active');
    btn3d.classList.add('btn-outline-primary');
    btn3d.classList.remove('btn-primary');
    
    btnTimeline.classList.remove('active');
    btnTimeline.classList.add('btn-outline-primary');
    btnTimeline.classList.remove('btn-primary');
    
    // Mostrar vista seleccionada y actualizar botón
    if (viewType === '3d') {
        view3d.classList.add('active');
        btn3d.classList.add('active', 'btn-primary');
        btn3d.classList.remove('btn-outline-primary');
        console.log('✅ Vista 3D activada');
    } else if (viewType === 'timeline') {
        viewTimeline.classList.add('active');
        btnTimeline.classList.add('active', 'btn-primary');
        btnTimeline.classList.remove('btn-outline-primary');
        
        // Si hay commits, actualizar el timeline
        if (window.ViewSystem.timelineCommits.length > 0) {
            updateTimelineFullView(window.ViewSystem.timelineCommits);
        }
        console.log('✅ Vista Timeline activada');
    }
    
    // Mostrar/ocultar controles contextuales
    const controls3d = document.querySelectorAll('.view-controls-3d');
    const controlsTimeline = document.querySelectorAll('.view-controls-timeline');
    
    controls3d.forEach(control => {
        control.style.display = viewType === '3d' ? 'flex' : 'none';
    });
    
    controlsTimeline.forEach(control => {
        control.style.display = viewType === 'timeline' ? 'flex' : 'none';
    });
}

/**
 * Optimiza automáticamente la cantidad de commits por página según el espacio disponible
 */
function optimizeCommitsPerPage() {
    const viewportHeight = window.innerHeight;
    const availableHeight = viewportHeight - 180; // Reducir overhead para mejor aprovechamiento
    
    // Calcular commits que caben según la altura disponible
    // Cada commit ocupa aproximadamente 90px (incluyendo padding y margin optimizado)
    const estimatedCommitHeight = 90;
    const optimalCommitsPerPage = Math.floor(availableHeight / estimatedCommitHeight);
    
    // Establecer un rango razonable más amplio
    const minCommits = 20;  // Aumentar mínimo
    const maxCommits = 100; // Reducir máximo para mejor rendimiento
    const calculatedCommits = Math.max(minCommits, Math.min(maxCommits, optimalCommitsPerPage));
    
    // Solo actualizar si es significativamente diferente (evitar re-renderizados constantes)
    const currentCommits = window.ViewSystem.commitsPerPage;
    const difference = Math.abs(calculatedCommits - currentCommits);
    
    if (difference > 10) { // Aumentar umbral para evitar re-renderizados constantes
        window.ViewSystem.commitsPerPage = calculatedCommits;
        console.log(`📏 Optimización automática: ${calculatedCommits} commits por página (altura: ${viewportHeight}px)`);
        
        // NO re-renderizar automáticamente para evitar saltos de scroll
        // La paginación se ajustará en la próxima acción del usuario
    }
}

/**
 * Actualiza el timeline completo con paginación
 */
function updateTimelineFullView(commits) {
    console.log('🔄 Actualizando timeline completo con', commits?.length, 'commits');
    
    if (!commits || commits.length === 0) {
        showEmptyTimelineState();
        return;
    }
    
    // Verificar si ya tenemos estos commits para evitar re-renderizados innecesarios
    const currentCommitHashes = window.ViewSystem.timelineCommits.map(c => c.hash).join(',');
    const newCommitHashes = commits.map(c => c.hash).join(',');
    
    if (currentCommitHashes === newCommitHashes && window.ViewSystem.timelineCommits.length > 0) {
        console.log('⏭️ Los commits son idénticos, omitiendo actualización');
        return;
    }
    
    // Preservar página actual si es posible
    const currentPage = window.ViewSystem.currentPage || 1;
    
    // Optimizar automáticamente la paginación según el espacio disponible (solo una vez)
    if (window.ViewSystem.commitsPerPage === 30) { // Solo optimizar si está en valor por defecto
        optimizeCommitsPerPage();
    }
    
    // Guardar commits y configurar paginación
    window.ViewSystem.timelineCommits = [...commits];
    window.ViewSystem.totalCommits = commits.length;
    
    // Ordenar commits por fecha (más reciente primero)
    window.ViewSystem.timelineCommits.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    
    // Mantener página actual si es válida, sino ir a la primera
    const totalPages = Math.ceil(window.ViewSystem.totalCommits / window.ViewSystem.commitsPerPage);
    window.ViewSystem.currentPage = currentPage <= totalPages ? currentPage : 1;
    
    renderTimelinePage();
    updateTimelineStats();
    
    // Ocultar estado vacío
    const emptyState = document.getElementById('timeline-empty-state-full');
    if (emptyState) {
        emptyState.style.display = 'none';
    }
    
    // Mostrar footer de estadísticas
    const statsFooter = document.querySelector('.timeline-stats-footer');
    if (statsFooter) {
        statsFooter.style.display = 'block';
    }
    
    console.log('✅ Timeline completo actualizado');
}

/**
 * Renderiza una página específica del timeline
 */
function renderTimelinePage() {
    const container = document.getElementById('timeline-container-full');
    if (!container) {
        console.error('❌ Contenedor del timeline no encontrado');
        return;
    }
    
    // Calcular índices de la página actual
    const startIndex = (window.ViewSystem.currentPage - 1) * window.ViewSystem.commitsPerPage;
    const endIndex = Math.min(startIndex + window.ViewSystem.commitsPerPage, window.ViewSystem.totalCommits);
    const pageCommits = window.ViewSystem.timelineCommits.slice(startIndex, endIndex);
    
    // Limpiar contenido anterior
    container.innerHTML = '';
    
    // Crear header del timeline
    const header = document.createElement('div');
    header.className = 'timeline-header d-flex justify-content-between align-items-center p-3 border-bottom';
    header.innerHTML = `
        <div>
            <h4 class="mb-1">
                <i class="fas fa-clock me-2 text-primary"></i>
                Timeline Completo (${window.ViewSystem.totalCommits} commits)
            </h4>
            <small class="text-muted">
                Mostrando ${startIndex + 1}-${endIndex} de ${window.ViewSystem.totalCommits} commits
            </small>
        </div>
        <div class="d-flex gap-2">
            <button class="btn btn-outline-primary btn-sm" onclick="refreshTimeline()">
                <i class="fas fa-sync-alt"></i>
            </button>
            <button class="btn btn-outline-info btn-sm" onclick="exportTimelineData()">
                <i class="fas fa-download"></i>
            </button>
        </div>
    `;
    container.appendChild(header);
    
    // Crear contenedor de commits optimizado para máximo espacio
    const commitsContainer = document.createElement('div');
    commitsContainer.className = 'timeline-commits-container';
    // Optimizar el uso del espacio disponible
    commitsContainer.style.minHeight = '60vh'; // Usar viewport height para mejor aprovechamiento
    commitsContainer.style.maxHeight = '80vh'; // Evitar que se extienda demasiado
    commitsContainer.style.overflowY = 'auto';
    commitsContainer.style.padding = '12px 16px'; // Reducir padding vertical
    commitsContainer.style.flex = '1 1 auto';
    commitsContainer.style.height = 'auto';
    // Mejorar el scroll
    commitsContainer.style.scrollBehavior = 'smooth';
    commitsContainer.style.overscrollBehavior = 'contain';
    
    // Renderizar commits de la página actual
    pageCommits.forEach((commit, index) => {
        const commitElement = createTimelineCommitElement(commit, startIndex + index);
        commitsContainer.appendChild(commitElement);
    });
    
    container.appendChild(commitsContainer);
    
    // Crear controles de paginación
    createPaginationControls(container);
    
    console.log(`📄 Página ${currentPage} renderizada con ${pageCommits.length} commits`);
}

/**
 * Crea elemento HTML para un commit en el timeline
 */
function createTimelineCommitElement(commit, globalIndex) {
    const commitDate = new Date(commit.timestamp);
    const now = new Date();
    const diffTime = Math.abs(now - commitDate);
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    
    let timeAgo = '';
    if (diffDays === 0) {
        const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
        timeAgo = diffHours === 0 ? 'Hace unos minutos' : `Hace ${diffHours}h`;
    } else if (diffDays === 1) {
        timeAgo = 'Ayer';
    } else if (diffDays < 7) {
        timeAgo = `Hace ${diffDays} días`;
    } else if (diffDays < 30) {
        timeAgo = `Hace ${Math.floor(diffDays / 7)} semanas`;
    } else {
        timeAgo = `Hace ${Math.floor(diffDays / 30)} meses`;
    }
    
    const commitElement = document.createElement('div');
    commitElement.className = 'timeline-commit-item d-flex p-2 mb-2 bg-dark bg-opacity-25 rounded'; // Reducir padding y margin
    commitElement.style.cursor = 'pointer';
    commitElement.style.transition = 'all 0.2s ease';
    commitElement.style.minHeight = '70px'; // Altura mínima fija para consistencia
    commitElement.style.alignItems = 'center'; // Centrar contenido verticalmente
    
    // Agregar efecto hover
    commitElement.addEventListener('mouseenter', function() {
        this.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
        this.style.transform = 'translateX(5px)';
    });
    
    commitElement.addEventListener('mouseleave', function() {
        this.style.backgroundColor = 'rgba(0, 0, 0, 0.25)';
        this.style.transform = 'translateX(0)';
    });
    
    // Click para resaltar en vista 3D
    commitElement.addEventListener('click', function() {
        if (typeof highlightCommitIn3D === 'function') {
            highlightCommitIn3D(commit.hash);
        }
        // Cambiar a vista 3D para mostrar el commit resaltado
        switchToView('3d');
    });
    
    commitElement.innerHTML = `
        <div class="timeline-commit-index d-flex align-items-center justify-content-center me-2" 
             style="min-width: 35px; height: 35px; background: linear-gradient(135deg, #007bff, #0056b3); 
                    border-radius: 8px; color: white; font-weight: bold; font-size: 12px; flex-shrink: 0;">
            ${globalIndex + 1}
        </div>
        <div class="timeline-commit-content flex-grow-1 d-flex justify-content-between">
            <div class="timeline-commit-main" style="flex: 1; min-width: 0;">
                <div class="timeline-commit-message mb-1">
                    <strong class="d-block text-truncate" style="color: #ffffff; font-size: 14px; line-height: 1.3;">
                        ${commit.message.length > 70 ? commit.message.substring(0, 70) + '...' : commit.message}
                    </strong>
                    <div class="d-flex align-items-center gap-2 text-muted" style="font-size: 11px;">
                        <span class="d-flex align-items-center">
                            <i class="fas fa-user me-1" style="width: 10px;"></i>
                            <span class="text-truncate" style="max-width: 120px;">${commit.author?.name || 'Desconocido'}</span>
                        </span>
                        <span class="d-flex align-items-center">
                            <i class="fas fa-hashtag me-1" style="width: 10px;"></i>
                            <code style="background: rgba(255,255,255,0.1); padding: 1px 4px; border-radius: 2px; font-size: 10px;">
                                ${commit.hash.substring(0, 7)}
                            </code>
                        </span>
                        ${commit.branch ? `
                            <span class="d-flex align-items-center">
                                <i class="fas fa-code-branch me-1" style="width: 10px;"></i>
                                <span class="text-truncate" style="max-width: 80px;">${commit.branch}</span>
                            </span>
                        ` : ''}
                        ${commit.stats ? `
                            <span class="text-success">+${commit.stats.additions || 0}</span>
                            <span class="text-danger">-${commit.stats.deletions || 0}</span>
                        ` : ''}
                    </div>
                </div>
            </div>
            <div class="timeline-commit-time text-end" style="flex-shrink: 0; min-width: 70px;">
                <small class="text-muted d-block" style="font-size: 11px;">${timeAgo}</small>
                <small class="text-muted" style="font-size: 10px;">${commitDate.toLocaleDateString('es', { month: 'short', day: 'numeric' })}</small>
            </div>
        </div>
    `;
    
    return commitElement;
}

/**
 * Crea controles de paginación
 */
function createPaginationControls(container) {
    const totalPages = Math.ceil(window.ViewSystem.totalCommits / window.ViewSystem.commitsPerPage);
    
    if (totalPages <= 1) return; // No mostrar paginación si solo hay una página
    
    const paginationContainer = document.createElement('div');
    paginationContainer.className = 'timeline-pagination d-flex justify-content-between align-items-center p-2 border-top bg-dark bg-opacity-10';
    
    // Información de página más compacta
    const pageInfo = document.createElement('div');
    pageInfo.className = 'timeline-page-info';
    pageInfo.innerHTML = `
        <small class="text-muted" style="font-size: 11px;">
            ${window.ViewSystem.currentPage}/${totalPages} 
            <span class="d-none d-sm-inline">(${window.ViewSystem.commitsPerPage}/página)</span>
        </small>
    `;
    
    // Controles de navegación más compactos
    const navControls = document.createElement('div');
    navControls.className = 'timeline-nav-controls d-flex gap-1 align-items-center';
    
    // Botón anterior
    const prevBtn = document.createElement('button');
    prevBtn.className = `btn btn-outline-primary btn-sm ${window.ViewSystem.currentPage === 1 ? 'disabled' : ''}`;
    prevBtn.innerHTML = '<i class="fas fa-chevron-left"></i><span class="d-none d-md-inline"> Ant</span>';
    prevBtn.disabled = window.ViewSystem.currentPage === 1;
    prevBtn.style.fontSize = '11px';
    prevBtn.addEventListener('click', () => goToPage(window.ViewSystem.currentPage - 1));
    
    // Input de página más pequeño
    const pageInput = document.createElement('input');
    pageInput.type = 'number';
    pageInput.min = '1';
    pageInput.max = totalPages.toString();
    pageInput.value = window.ViewSystem.currentPage.toString();
    pageInput.className = 'form-control form-control-sm';
    pageInput.style.width = '50px';
    pageInput.style.fontSize = '11px';
    pageInput.style.textAlign = 'center';
    pageInput.addEventListener('change', (e) => {
        const page = parseInt(e.target.value);
        if (page >= 1 && page <= totalPages) {
            goToPage(page);
        }
    });
    
    // Botón siguiente
    const nextBtn = document.createElement('button');
    nextBtn.className = `btn btn-outline-primary btn-sm ${window.ViewSystem.currentPage === totalPages ? 'disabled' : ''}`;
    nextBtn.innerHTML = '<span class="d-none d-md-inline">Sig </span><i class="fas fa-chevron-right"></i>';
    nextBtn.disabled = window.ViewSystem.currentPage === totalPages;
    nextBtn.style.fontSize = '11px';
    nextBtn.addEventListener('click', () => goToPage(window.ViewSystem.currentPage + 1));
    
    // Selector de commits por página más compacto
    const perPageSelect = document.createElement('select');
    perPageSelect.className = 'form-select form-select-sm ms-2';
    perPageSelect.style.width = '70px';
    perPageSelect.style.fontSize = '11px';
    perPageSelect.innerHTML = `
        <option value="20" ${window.ViewSystem.commitsPerPage === 20 ? 'selected' : ''}>20</option>
        <option value="30" ${window.ViewSystem.commitsPerPage === 30 ? 'selected' : ''}>30</option>
        <option value="50" ${window.ViewSystem.commitsPerPage === 50 ? 'selected' : ''}>50</option>
        <option value="100" ${window.ViewSystem.commitsPerPage === 100 ? 'selected' : ''}>100</option>
    `;
    perPageSelect.addEventListener('change', (e) => {
        window.ViewSystem.commitsPerPage = parseInt(e.target.value);
        window.ViewSystem.currentPage = 1; // Resetear a primera página
        renderTimelinePage();
    });
    
    navControls.appendChild(prevBtn);
    navControls.appendChild(pageInput);
    navControls.appendChild(nextBtn);
    navControls.appendChild(perPageSelect);
    
    paginationContainer.appendChild(pageInfo);
    paginationContainer.appendChild(navControls);
    
    container.appendChild(paginationContainer);
}

/**
 * Navega a una página específica
 */
function goToPage(page) {
    const totalPages = Math.ceil(window.ViewSystem.totalCommits / window.ViewSystem.commitsPerPage);
    
    if (page < 1 || page > totalPages) {
        console.warn(`⚠️ Página ${page} fuera de rango (1-${totalPages})`);
        return;
    }
    
    // Guardar posición actual del scroll para preservar experiencia UX
    const container = document.getElementById('timeline-container-full');
    const currentScrollTop = container ? container.scrollTop : 0;
    
    window.ViewSystem.currentPage = page;
    renderTimelinePage();
    
    // Solo hacer scroll al top si es una navegación manual de página (no en re-renderizados)
    // Y solo si realmente cambió la página
    if (container && page !== window.ViewSystem.previousPage) {
        // Scroll suave al header en lugar de scroll abrupto
        const header = container.querySelector('.timeline-header');
        if (header) {
            header.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }
    
    // Guardar página anterior para comparación
    window.ViewSystem.previousPage = page;
    
    console.log(`📄 Navegado a página ${page}`);
}

/**
 * Muestra estado vacío del timeline
 */
function showEmptyTimelineState() {
    const container = document.getElementById('timeline-container-full');
    if (!container) return;
    
    container.innerHTML = '';
    
    const emptyState = document.getElementById('timeline-empty-state-full');
    if (emptyState) {
        emptyState.style.display = 'block';
    }
    
    const statsFooter = document.querySelector('.timeline-stats-footer');
    if (statsFooter) {
        statsFooter.style.display = 'none';
    }
}

/**
 * Actualiza las estadísticas del timeline
 */
function updateTimelineStats() {
    const statsElement = document.getElementById('timeline-stats');
    if (!statsElement || !window.ViewSystem.timelineCommits.length) return;
    
    // Calcular estadísticas
    const authors = [...new Set(window.ViewSystem.timelineCommits.map(c => c.author?.name).filter(Boolean))];
    const dateRange = {
        oldest: new Date(Math.min(...window.ViewSystem.timelineCommits.map(c => new Date(c.timestamp)))),
        newest: new Date(Math.max(...window.ViewSystem.timelineCommits.map(c => new Date(c.timestamp))))
    };
    
    const totalAdditions = window.ViewSystem.timelineCommits.reduce((sum, c) => sum + (c.stats?.additions || 0), 0);
    const totalDeletions = window.ViewSystem.timelineCommits.reduce((sum, c) => sum + (c.stats?.deletions || 0), 0);
    const totalFiles = window.ViewSystem.timelineCommits.reduce((sum, c) => sum + (c.stats?.filesChanged || 0), 0);
    
    const statsHtml = `
        <div class="row g-2 text-center">
            <div class="col-2">
                <div class="d-flex flex-column">
                    <strong class="text-primary">${window.ViewSystem.totalCommits}</strong>
                    <small class="text-muted">Commits</small>
                </div>
            </div>
            <div class="col-2">
                <div class="d-flex flex-column">
                    <strong class="text-info">${authors.length}</strong>
                    <small class="text-muted">Autores</small>
                </div>
            </div>
            <div class="col-2">
                <div class="d-flex flex-column">
                    <strong class="text-success">+${totalAdditions}</strong>
                    <small class="text-muted">Líneas</small>
                </div>
            </div>
            <div class="col-2">
                <div class="d-flex flex-column">
                    <strong class="text-danger">-${totalDeletions}</strong>
                    <small class="text-muted">Líneas</small>
                </div>
            </div>
            <div class="col-2">
                <div class="d-flex flex-column">
                    <strong class="text-warning">${totalFiles}</strong>
                    <small class="text-muted">Archivos</small>
                </div>
            </div>
            <div class="col-2">
                <div class="d-flex flex-column">
                    <strong class="text-secondary">
                        ${Math.ceil((dateRange.newest - dateRange.oldest) / (1000 * 60 * 60 * 24))}
                    </strong>
                    <small class="text-muted">Días</small>
                </div>
            </div>
        </div>
        <hr class="my-2">
        <div class="text-center">
            <small class="text-muted">
                <i class="fas fa-calendar me-1"></i>
                Desde ${dateRange.oldest.toLocaleDateString()} hasta ${dateRange.newest.toLocaleDateString()}
            </small>
        </div>
    `;
    
    statsElement.innerHTML = statsHtml;
}

/**
 * Funciones auxiliares para el timeline
 */
function refreshTimeline() {
    if (window.ViewSystem.timelineCommits.length > 0) {
        console.log('🔄 Refrescando timeline...');
        // Preservar posición de scroll actual
        const container = document.getElementById('timeline-container-full');
        const scrollTop = container ? container.scrollTop : 0;
        
        renderTimelinePage();
        updateTimelineStats();
        
        // Restaurar posición de scroll si es válida
        if (container && scrollTop > 0) {
            setTimeout(() => {
                container.scrollTop = scrollTop;
            }, 50);
        }
        
        showNotification('Timeline actualizado', 'success');
    } else {
        showNotification('No hay commits para actualizar', 'warning');
    }
}

/**
 * Preserva la posición de scroll durante operaciones del timeline
 */
function preserveScrollPosition(callback) {
    const container = document.getElementById('timeline-container-full');
    const scrollTop = container ? container.scrollTop : 0;
    
    if (typeof callback === 'function') {
        callback();
    }
    
    // Restaurar scroll después de un breve delay para permitir renderizado
    if (container && scrollTop > 0) {
        setTimeout(() => {
            container.scrollTop = scrollTop;
        }, 100);
    }
}

function debugTimeline() {
    console.log('🐛 Debug del Timeline:');
    console.log('Current View:', window.ViewSystem.currentView);
    console.log('Timeline Commits:', window.ViewSystem.timelineCommits.length);
    console.log('Current Page:', window.ViewSystem.currentPage);
    console.log('Commits Per Page:', window.ViewSystem.commitsPerPage);
    console.log('Total Commits:', window.ViewSystem.totalCommits);
    
    // Mostrar en consola para debugging
    window.timelineDebugInfo = {
        currentView: window.ViewSystem.currentView,
        timelineCommits: window.ViewSystem.timelineCommits.length,
        currentPage: window.ViewSystem.currentPage,
        commitsPerPage: window.ViewSystem.commitsPerPage,
        totalCommits: window.ViewSystem.totalCommits,
        totalPages: Math.ceil(window.ViewSystem.totalCommits / window.ViewSystem.commitsPerPage)
    };
    
    showNotification('Info de debug enviada a consola', 'info');
}

function exportTimelineData() {
    if (!window.ViewSystem.timelineCommits.length) {
        showNotification('No hay datos para exportar', 'warning');
        return;
    }
    
    const data = {
        repository: window.currentRepo || 'unknown',
        exportDate: new Date().toISOString(),
        totalCommits: window.ViewSystem.totalCommits,
        commits: window.ViewSystem.timelineCommits.map(commit => ({
            hash: commit.hash,
            message: commit.message,
            author: commit.author?.name,
            date: commit.timestamp,
            branch: commit.branch,
            stats: commit.stats
        }))
    };
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `timeline-${window.currentRepo || 'repo'}-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
    
    showNotification('Timeline exportado exitosamente', 'success');
}

/**
 * Escuchador de eventos para commits cargados
 */
window.addEventListener('commitsLoaded', (event) => {
    console.log('📡 Evento commitsLoaded recibido:', event.detail);
    
    if (event.detail && event.detail.commits) {
        // Actualizar timeline si estamos en esa vista
        if (window.ViewSystem.currentView === 'timeline') {
            updateTimelineFullView(event.detail.commits);
        } else {
            // Guardar commits para cuando cambien a vista timeline
            window.ViewSystem.timelineCommits = [...event.detail.commits];
            window.ViewSystem.totalCommits = event.detail.commits.length;
        }
    }
});

/**
 * Funciones globales expuestas para debugging y compatibilidad
 */
window.switchToView = switchToView;
window.updateTimelineFullView = updateTimelineFullView;
window.currentView = () => window.ViewSystem.currentView;
window.refreshTimeline = refreshTimeline;
window.debugTimeline = debugTimeline;
window.exportTimelineData = exportTimelineData;
window.goToPage = goToPage;

// Diagnóstico del sistema
window.diagnoseSystem = function() {
    console.log('🔍 Diagnóstico del Sistema de Vistas Alternas:');
    console.log('==========================================');
    console.log('Vista actual:', window.ViewSystem.currentView);
    console.log('Commits en timeline:', window.ViewSystem.timelineCommits.length);
    console.log('Página actual:', window.ViewSystem.currentPage);
    console.log('Commits por página:', window.ViewSystem.commitsPerPage);
    console.log('Total de commits:', window.ViewSystem.totalCommits);
    console.log('Total de páginas:', Math.ceil(window.ViewSystem.totalCommits / window.ViewSystem.commitsPerPage));
    
    // Verificar elementos DOM
    const elements = {
        'view-3d': document.getElementById('view-3d'),
        'view-timeline': document.getElementById('view-timeline'),
        'btn-view-3d': document.getElementById('btn-view-3d'),
        'btn-view-timeline': document.getElementById('btn-view-timeline'),
        'timeline-container-full': document.getElementById('timeline-container-full')
    };
    
    console.log('Elementos DOM:');
    Object.entries(elements).forEach(([name, element]) => {
        console.log(`  ${name}:`, element ? '✅ Encontrado' : '❌ No encontrado');
    });
    
    return {
        currentView: window.ViewSystem.currentView,
        timelineCommits: window.ViewSystem.timelineCommits.length,
        currentPage: window.ViewSystem.currentPage,
        commitsPerPage: window.ViewSystem.commitsPerPage,
        totalCommits: window.ViewSystem.totalCommits,
        elements
    };
};

console.log('✅ Sistema de Vistas Alternas con Paginación inicializado');

/**
 * Inicializa la visualización 3D
 */
function initVisualization() {
    console.log('🎬 Iniciando inicialización de Three.js...');
    
    const container = document.getElementById('visualization-container');
    if (!container) {
        console.error('❌ No se encontró el contenedor #visualization-container');
        return;
    }
    
    let width = container.clientWidth;
    let height = container.clientHeight;
    
    // Fallback si las dimensiones son 0
    if (width === 0 || height === 0) {
        console.warn('⚠️ Contenedor con dimensiones 0, usando fallback');
        width = 800;  // Fallback width
        height = 600; // Fallback height
        
        // Forzar dimensiones en el contenedor
        container.style.width = width + 'px';
        container.style.height = height + 'px';
        container.style.minHeight = height + 'px';
    }
    
    console.log(`📐 Dimensiones del contenedor: ${width}x${height}`);

    // Verificar WebGL
    if (!window.WebGLRenderingContext) {
        console.error('❌ WebGL no está soportado en este navegador');
        container.innerHTML = '<div class="alert alert-danger">WebGL no soportado</div>';
        return;
    }

    // Crear escena
    scene = new THREE.Scene();
    scene.background = new THREE.Color(config.backgroundColor);
    console.log('✅ Escena creada');
    
    // Exponer en window para acceso global
    window.scene = scene;

    // Crear cámara
    camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 2000);
    camera.position.set(0, 0, 100);
    console.log(`✅ Cámara creada en posición (${camera.position.x}, ${camera.position.y}, ${camera.position.z})`);
    
    // Exponer en window para acceso global
    window.camera = camera;

    // Crear renderer
    try {
        renderer = new THREE.WebGLRenderer({ antialias: true });
        renderer.setSize(width, height);
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        
        // Verificar que el canvas se creó
        console.log(`✅ Renderer creado: ${renderer.domElement.width}x${renderer.domElement.height}`);
        console.log(`🎨 Canvas tag: ${renderer.domElement.tagName}`);
        
        container.appendChild(renderer.domElement);
        console.log('✅ Canvas agregado al DOM');
        
        // Verificar que el canvas es visible
        const canvasStyle = window.getComputedStyle(renderer.domElement);
        console.log(`👁️ Canvas visible: display=${canvasStyle.display}, visibility=${canvasStyle.visibility}`);
        
        // Exponer en window para acceso global
        window.renderer = renderer;
        
    } catch (error) {
        console.error('❌ Error creando WebGL renderer:', error);
        container.innerHTML = '<div class="alert alert-danger">Error inicializando WebGL</div>';
        return;
    }

    // Añadir luces mejoradas
    const ambientLight = new THREE.AmbientLight(0x404040, 0.6);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(50, 50, 100);
    directionalLight.castShadow = true;
    directionalLight.shadow.mapSize.width = 2048;
    directionalLight.shadow.mapSize.height = 2048;
    scene.add(directionalLight);

    // Añadir luces de apoyo
    const pointLight1 = new THREE.PointLight(0x4a90e2, 0.3, 200);
    pointLight1.position.set(-50, 30, 50);
    scene.add(pointLight1);

    const pointLight2 = new THREE.PointLight(0x50c878, 0.3, 200);
    pointLight2.position.set(50, -30, 50);
    scene.add(pointLight2);

    // Crear grilla de fondo
    createBackgroundGrid();

    // Controles de órbita mejorados
    try {
        if (typeof THREE.OrbitControls !== 'undefined') {
            controls = new THREE.OrbitControls(camera, renderer.domElement);
            controls.enableDamping = true;
            controls.dampingFactor = 0.05;
            controls.enableZoom = true;
            controls.enablePan = true;
            controls.maxPolarAngle = Math.PI;
            controls.minDistance = 20;
            controls.maxDistance = 500;
            console.log('✅ OrbitControls configurados correctamente');
        } else {
            console.warn('⚠️ THREE.OrbitControls no está disponible - controles de mouse deshabilitados');
            // Crear controles básicos manuales si OrbitControls no está disponible
            renderer.domElement.addEventListener('wheel', (event) => {
                event.preventDefault();
                const delta = event.deltaY;
                camera.position.z += delta * 0.1;
                camera.position.z = Math.max(10, Math.min(500, camera.position.z));
            });
        }
    } catch (error) {
        console.error('❌ Error configurando OrbitControls:', error);
        console.log('💡 Usando controles básicos de fallback');
    }

    // Raycaster para interacción
    raycaster = new THREE.Raycaster();
    mouse = new THREE.Vector2();

    // Tooltip para información
    infoTooltip = document.createElement('div');
    infoTooltip.className = 'info-tooltip';
    infoTooltip.style.display = 'none';
    document.body.appendChild(infoTooltip);

    // Event listeners
    window.addEventListener('resize', onWindowResize);
    renderer.domElement.addEventListener('mousemove', onMouseMove);
    renderer.domElement.addEventListener('click', onMouseClick);

    // Exponer arrays globales en window para debugging
    window.nodes = nodes;
    window.connections = connections;
    
    // Iniciar animación
    animate();
    
    console.log('Visualización Git Graph inicializada');
}

/**
 * Crea una grilla de fondo para el contexto visual
 */
function createBackgroundGrid() {
    const gridHelper = new THREE.GridHelper(200, 20, config.gridColor, config.gridColor);
    gridHelper.position.y = -50;
    gridHelper.material.transparent = true;
    gridHelper.material.opacity = 0.1;
    scene.add(gridHelper);
}

/**
 * Maneja el redimensionamiento de la ventana
 */
function onWindowResize() {
    const container = document.getElementById('visualization-container');
    const width = container.clientWidth;
    const height = container.clientHeight;

    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
}

/**
 * Maneja el movimiento del mouse para mostrar tooltips
 */
function onMouseMove(event) {
    // Calcular posición del mouse normalizada
    const rect = renderer.domElement.getBoundingClientRect();
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    // Verificar intersección con objetos
    raycaster.setFromCamera(mouse, camera);
    const intersects = raycaster.intersectObjects(scene.children);

    if (intersects.length > 0) {
        const object = intersects[0].object;
        if (object.userData && object.userData.type) {
            // Mostrar tooltip
            infoTooltip.innerHTML = getTooltipContent(object.userData);
            infoTooltip.style.display = 'block';
            infoTooltip.style.left = event.clientX + 10 + 'px';
            infoTooltip.style.top = event.clientY + 10 + 'px';
            
            // Efecto hover - hacer el nodo más brillante
            if (object.material && object.material.emissive) {
                // Guardar color original si no existe
                if (!object.userData.originalEmissive) {
                    object.userData.originalEmissive = object.material.emissive.clone();
                }
                // Hacer más brillante en hover
                object.material.emissive.setHex(0x444444);
                
                // Cambiar cursor
                document.body.style.cursor = 'pointer';
            }
        } else {
            infoTooltip.style.display = 'none';
            // Restaurar cursor
            document.body.style.cursor = 'default';
        }
    } else {
        infoTooltip.style.display = 'none';
        // Restaurar cursor
        document.body.style.cursor = 'default';
        
        // Restaurar emisividad original de todos los nodos
        nodes.forEach(node => {
            if (node.material && node.material.emissive && node.userData.originalEmissive) {
                node.material.emissive.copy(node.userData.originalEmissive);
            }
        });
    }
}

/**
 * Maneja el clic del mouse para seleccionar nodos
 */
function onMouseClick(event) {
    // Calcular posición del mouse normalizada
    const rect = renderer.domElement.getBoundingClientRect();
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    // Verificar intersección con objetos
    raycaster.setFromCamera(mouse, camera);
    const intersects = raycaster.intersectObjects(scene.children);

    if (intersects.length > 0) {
        const object = intersects[0].object;
        if (object.userData && object.userData.type) {
            showElementInfo(object.userData);
            
            // Sincronizar con timeline si es un commit
            if (object.userData.hash && typeof highlightCommitInTimeline === 'function') {
                highlightCommitInTimeline(object.userData.hash);
            }
        }
    }
}

/**
 * Genera el contenido HTML para el tooltip
 */
function getTooltipContent(data) {
    let content = '';
    
    switch (data.type) {
        case 'commit':
            content = `
                <strong>${data.message}</strong><br>
                Autor: ${data.author}<br>
                Fecha: ${new Date(data.timestamp).toLocaleString()}<br>
                Hash: ${data.hash.substring(0, 7)}
            `;
            break;
        case 'pr':
            content = `
                <strong>PR: ${data.title}</strong><br>
                Estado: ${data.state}<br>
                Autor: ${data.author}<br>
                Fecha: ${new Date(data.timestamp).toLocaleString()}
            `;
            break;
        case 'issue':
            content = `
                <strong>Issue: ${data.title}</strong><br>
                Estado: ${data.state}<br>
                Autor: ${data.author}<br>
                Fecha: ${new Date(data.timestamp).toLocaleString()}
            `;
            break;
    }
    
    return content;
}

/**
 * Muestra información detallada del elemento en el panel lateral
 */
function showElementInfo(data) {
    const noInfoMessage = document.getElementById('noInfoMessage');
    const elementInfo = document.getElementById('elementInfo');
    
    noInfoMessage.classList.add('d-none');
    elementInfo.classList.remove('d-none');
    
    let content = '';
    
    switch (data.type) {
        case 'commit':
            content = `
                <h4>Commit</h4>
                <p><strong>Mensaje:</strong> ${data.message}</p>
                <p><strong>Autor:</strong> ${data.author}</p>
                <p><strong>Fecha:</strong> ${new Date(data.timestamp).toLocaleString()}</p>
                <p><strong>Hash:</strong> ${data.hash}</p>
                <p><strong>Rama:</strong> ${data.branch}</p>
                <p>
                    <strong>Estadísticas:</strong><br>
                    Archivos modificados: ${data.stats.filesChanged}<br>
                    Líneas añadidas: ${data.stats.additions}<br>
                    Líneas eliminadas: ${data.stats.deletions}
                </p>
                <button class="btn btn-sm btn-primary" onclick="showDetails('${data.hash}')">Ver Detalles</button>
            `;
            break;
        case 'pr':
            content = `
                <h4>Pull Request</h4>
                <p><strong>Título:</strong> ${data.title}</p>
                <p><strong>Estado:</strong> ${data.state}</p>
                <p><strong>Autor:</strong> ${data.author}</p>
                <p><strong>Fecha:</strong> ${new Date(data.timestamp).toLocaleString()}</p>
                <p><strong>Descripción:</strong> ${data.description}</p>
                <button class="btn btn-sm btn-primary" onclick="showDetails('pr_${data.id}')">Ver Detalles</button>
            `;
            break;
        case 'issue':
            content = `
                <h4>Issue</h4>
                <p><strong>Título:</strong> ${data.title}</p>
                <p><strong>Estado:</strong> ${data.state}</p>
                <p><strong>Autor:</strong> ${data.author}</p>
                <p><strong>Fecha:</strong> ${new Date(data.timestamp).toLocaleString()}</p>
                <p><strong>Descripción:</strong> ${data.description}</p>
                <button class="btn btn-sm btn-primary" onclick="showDetails('issue_${data.id}')">Ver Detalles</button>
            `;
            break;
    }
    
    elementInfo.innerHTML = content;
}

/**
 * Muestra detalles completos en un modal
 */
function showDetails(id) {
    // Esta función se implementará en eventHandler.js
    // Aquí solo se declara para evitar errores
    console.log('Mostrar detalles de:', id);
}

/**
 * Crea un nodo visual para un commit
 */
function createCommitNode(commit) {
    const geometry = new THREE.SphereGeometry(config.nodeSize);
    const material = new THREE.MeshPhongMaterial({ 
        color: config.nodeColors.commit,
        emissive: config.nodeColors.commit,
        emissiveIntensity: 0.2
    });
    
    const sphere = new THREE.Mesh(geometry, material);
    
    // Posicionar según timestamp (más recientes más arriba)
    const date = new Date(commit.timestamp);
    const timeValue = date.getTime();
    
    // Posición basada en tiempo y hash
    const hashValue = parseInt(commit.hash.substring(0, 8), 16);
    sphere.position.x = (hashValue % 1000) / 10 - 50;
    sphere.position.y = (timeValue % 1000000) / 20000 - 25;
    sphere.position.z = (hashValue % 500) / 10 - 25;
    
    // Guardar datos para interacción
    sphere.userData = commit;
    
    return sphere;
}

/**
 * Crea conexiones entre nodos relacionados
 */
function createConnections() {
    // Crear conexiones entre commits y sus padres
    for (let i = 0; i < commits.length; i++) {
        const commit = commits[i];
        if (commit.parents && commit.parents.length > 0) {
            for (const parentHash of commit.parents) {
                const parentNode = nodes.find(node => 
                    node.userData.type === 'commit' && 
                    node.userData.hash === parentHash
                );
                
                if (parentNode) {
                    const material = new THREE.LineBasicMaterial({
                        color: 0xffffff,
                        transparent: true,
                        opacity: 0.2
                    });
                    
                    const points = [
                        commit.position,
                        parentNode.position
                    ];
                    
                    const geometry = new THREE.BufferGeometry().setFromPoints(points);
                    const line = new THREE.Line(geometry, material);
                    scene.add(line);
                }
            }
        }
    }
}

/**
 * Verifica si las funciones de tiempo real están habilitadas
 */
function checkRealtimeStatus() {
    fetch('/api/user/auth-status')
        .then(response => response.json())
        .then(data => {
            if (data.authenticated) {
                // Habilitar funciones de tiempo real
                window.REALTIME_ENABLED = true;
                console.log('Usuario autenticado, tiempo real habilitado');
                
                // Mostrar indicador de estado
                showRealtimeStatus(true);
            } else {
                window.REALTIME_ENABLED = false;
                console.log('Usuario no autenticado, tiempo real deshabilitado');
                showRealtimeStatus(false);
            }
        })
        .catch(error => {
            console.error('Error verificando estado de autenticación:', error);
            window.REALTIME_ENABLED = false;
            showRealtimeStatus(false);
        });
}

/**
 * Muestra el estado del tiempo real en la interfaz
 */
function showRealtimeStatus(enabled) {
    const statusElement = document.getElementById('realtime-status');
    if (statusElement) {
        if (enabled) {
            statusElement.className = 'badge bg-success';
            statusElement.textContent = 'Tiempo Real Activo';
        } else {
            statusElement.className = 'badge bg-secondary';
            statusElement.textContent = 'Solo Lectura';
        }
    }
}

/**
 * Carga commits con manejo mejorado de errores
 */
function loadCommits(repoUrl) {
    showLoading(true);
    
    // Validar formato de URL del repositorio
    if (!repoUrl || !repoUrl.includes('/')) {
        showError('Formato de repositorio inválido. Use: owner/repo');
        showLoading(false);
        return;
    }
    
    // Limpiar visualización anterior
    clearVisualization();
    
    const repoParam = repoUrl.replace('https://github.com/', '').replace('.git', '');
    
    fetch(`/api/repository/commits?repo=${encodeURIComponent(repoParam)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(commits => {
            console.log(`Cargados ${commits.length} commits para ${repoParam}`);
            
            if (commits.length === 0) {
                showError('No se encontraron commits para este repositorio');
                return;
            }
            
            // Guardar commits globalmente para acceso posterior
            window.currentCommits = commits;
            window.currentRepository = repoParam;
            
            // Disparar evento para notificar al sistema de vistas alternas
            const commitsLoadedEvent = new CustomEvent('commitsLoaded', {
                detail: { commits: commits, repository: repoParam, source: 'loadCommits' }
            });
            window.dispatchEvent(commitsLoadedEvent);
            
            // Renderizar commits
            renderCommitsVisualization(commits);
            
            // Conectar a eventos en tiempo real si está habilitado
            if (window.REALTIME_ENABLED) {
                connectToEventStream(repoParam);
            }
            
            // Actualizar el timeline con los commits inmediatamente
            updateTimelineWithCommits(commits);
            console.log('✅ Timeline actualizado automáticamente');
            
            showSuccess(`Visualización cargada: ${commits.length} commits`);
        })
        .catch(error => {
            console.error('Error cargando commits:', error);
            showError(`Error cargando commits: ${error.message}`);
        })
        .finally(() => {
            showLoading(false);
        });
}

/**
 * Renderiza la visualización de commits en formato git graph
 */
function renderCommitsVisualization(commits) {
    console.log(`🚀 Iniciando renderizado de ${commits.length} commits...`);
    
    if (!scene || !camera || !renderer) {
        console.error('❌ Componentes de Three.js no inicializados');
        return;
    }
    
    // Limpiar objetos anteriores
    clearScene();
    
    if (commits.length === 0) {
        console.warn('⚠️ No hay commits para renderizar');
        return;
    }

    // Calcular posiciones del git graph
    console.log('📊 Calculando posiciones del git graph...');
    calculateGitGraphPositions(commits);
    
    // Crear nodos de commits
    console.log('🔮 Creando nodos de commits...');
    let nodesCreated = 0;
    commits.forEach((commit, index) => {
        const commitNode = createCommitNodeAdvanced(commit, index);
        if (commitNode) {
            scene.add(commitNode);
            nodes.push(commitNode);
            nodesCreated++;
            
            // Log cada 10 nodos para no saturar la consola
            if (nodesCreated % 10 === 0 || nodesCreated === commits.length) {
                console.log(`✅ Creados ${nodesCreated}/${commits.length} nodos`);
            }
        } else {
            console.warn(`⚠️ No se pudo crear nodo para commit ${index}: ${commit.hash}`);
        }
    });
    
    // Verificar que hay nodos en la escena
    console.log(`📦 Objetos en escena: ${scene.children.length}`);
    console.log(`🔗 Nodos en array: ${nodes.length}`);
    
    // Crear conexiones entre commits
    console.log('🔗 Creando conexiones...');
    createGitGraphConnections(commits);
    
    // Crear etiquetas de ramas
    console.log('🏷️ Creando etiquetas de ramas...');
    createBranchLabels();
    
    // Actualizar cámara para encuadrar todos los commits
    console.log('🎥 Ajustando cámara...');
    adjustCameraToFitScene();
    
    // Forzar un renderizado inmediato
    if (renderer && scene && camera) {
        renderer.render(scene, camera);
        console.log('🎬 Renderizado forzado ejecutado');
    }
    
    console.log(`✅ Git Graph renderizado completado: ${nodes.length} nodos, ${connections.length} conexiones`);
    
    // Crear algunos nodos de prueba visibles si no hay nodos
    if (nodes.length === 0) {
        console.log('🧪 Creando nodos de prueba...');
        createTestNodes();
    }
}

/**
 * Limpia la escena 3D
 */
function clearScene() {
    if (scene) {
        // Limpiar arrays de tracking
        nodes.length = 0;
        connections.length = 0;
        
        // Remover todos los objetos excepto luces y grid
        const objectsToRemove = [];
        scene.traverse((child) => {
            if (child !== scene && 
                child.type !== 'AmbientLight' && 
                child.type !== 'DirectionalLight' &&
                child.type !== 'PointLight' &&
                child.type !== 'GridHelper') {
                objectsToRemove.push(child);
            }
        });
        
        objectsToRemove.forEach(obj => {
            scene.remove(obj);
            if (obj.geometry) obj.geometry.dispose();
            if (obj.material) {
                if (obj.material.map) obj.material.map.dispose();
                obj.material.dispose();
            }
        });
        
        // Resetear git graph
        if (gitGraph) {
            gitGraph.branches.clear();
            gitGraph.commitPositions.clear();
            gitGraph.maxBranchLevel = 0;
        }
    }
}

/**
 * Muestra mensajes de estado
 */
function showLoading(isLoading) {
    const loadingElement = document.getElementById('loading-indicator');
    if (loadingElement) {
        loadingElement.style.display = isLoading ? 'block' : 'none';
    }
}

function showError(message) {
    console.error('Error:', message);
    // Usar showNotification si está disponible, sino fallback
    if (typeof showNotification === 'function') {
        showNotification(message, 'error');
    } else {
        alert('Error: ' + message);
    }
}

function showSuccess(message) {
    console.log('Success:', message);
    // Usar showNotification si está disponible
    if (typeof showNotification === 'function') {
        showNotification(message, 'success');
    } else {
        console.log('Success: ' + message);
    }
}

function clearVisualization() {
    clearScene();
    
    const infoPanel = document.getElementById('commit-info');
    if (infoPanel) {
        infoPanel.innerHTML = '<p class="text-muted">Selecciona un commit para ver detalles</p>';
    }
}

/**
 * Actualiza los selectores de filtro con los datos disponibles
 */
function updateFilters(commits) {
    // Obtener autores únicos
    const authors = [...new Set(commits.map(commit => commit.author))];
    const authorFilter = document.getElementById('authorFilter');
    authorFilter.innerHTML = '<option value="">Todos</option>';
    
    authors.forEach(author => {
        const option = document.createElement('option');
        option.value = author;
        option.textContent = author;
        authorFilter.appendChild(option);
    });
    
    // Obtener ramas únicas
    const branches = [...new Set(commits.map(commit => commit.branch))];
    const branchFilter = document.getElementById('branchFilter');
    branchFilter.innerHTML = '<option value="">Todas</option>';
    
    branches.forEach(branch => {
        const option = document.createElement('option');
        option.value = branch;
        option.textContent = branch;
        branchFilter.appendChild(option);
    });
}

/**
 * Aplica filtros a la visualización
 */
function applyFilters() {
    const authorFilter = document.getElementById('authorFilter').value;
    const branchFilter = document.getElementById('branchFilter').value;
    const eventTypeFilter = document.getElementById('eventTypeFilter').value;
    const dateFromFilter = document.getElementById('dateFromFilter').value;
    const dateToFilter = document.getElementById('dateToFilter').value;
    
    // Convertir fechas si están definidas
    const fromDate = dateFromFilter ? new Date(dateFromFilter) : null;
    const toDate = dateToFilter ? new Date(dateToFilter + 'T23:59:59') : null;
    
    // Filtrar commits para el timeline
    let filteredCommits = commits.slice();
    
    if (authorFilter || branchFilter || eventTypeFilter || fromDate || toDate) {
        filteredCommits = commits.filter(commit => {
            let include = true;
            
            // Filtrar por tipo
            if (eventTypeFilter && commit.type !== eventTypeFilter) {
                include = false;
            }
            
            // Filtrar por autor
            if (authorFilter && commit.author !== authorFilter) {
                include = false;
            }
            
            // Filtrar por rama
            if (branchFilter && commit.branch !== branchFilter) {
                include = false;
            }
            
            // Filtrar por fecha
            if (fromDate || toDate) {
                const commitDate = new Date(commit.timestamp);
                if (fromDate && commitDate < fromDate) {
                    include = false;
                }
                if (toDate && commitDate > toDate) {
                    include = false;
                }
            }
            
            return include;
        });
    }
    
    // Aplicar filtros a la visualización 3D
    for (const node of nodes) {
        const data = node.userData;
        let visible = true;
        
        // Filtrar por tipo
        if (eventTypeFilter && data.type !== eventTypeFilter) {
            visible = false;
        }
        
        // Filtrar por autor
        if (authorFilter && data.author !== authorFilter) {
            visible = false;
        }
        
        // Filtrar por rama
        if (branchFilter && data.branch !== branchFilter) {
            visible = false;
        }
        
        // Filtrar por fecha
        if (fromDate || toDate) {
            const nodeDate = new Date(data.timestamp);
            if (fromDate && nodeDate < fromDate) {
                visible = false;
            }
            if (toDate && nodeDate > toDate) {
                visible = false;
            }
        }
        
        // Aplicar visibilidad
        node.visible = visible;
    }
    
    // Actualizar timeline con commits filtrados
    if (typeof renderTimeline === 'function') {
        renderTimeline(filteredCommits);
    }
}

/**
 * Limpia todos los filtros
 */
function clearFilters() {
    document.getElementById('authorFilter').value = '';
    document.getElementById('branchFilter').value = '';
    document.getElementById('eventTypeFilter').value = '';
    document.getElementById('dateFromFilter').value = '';
    document.getElementById('dateToFilter').value = '';
    
    // Mostrar todos los nodos
    for (const node of nodes) {
        node.visible = true;
    }
    
    // Restaurar timeline completo
    if (typeof renderTimeline === 'function') {
        renderTimeline(commits);
    }
}

/**
 * Función de animación
 */
function animate() {
    requestAnimationFrame(animate);
    
    // Verificar que los componentes básicos existen
    if (!scene || !camera || !renderer) {
        console.warn('⚠️ Componentes básicos de Three.js no disponibles en animate()');
        return;
    }
    
    // Actualizar controles si están disponibles
    if (controls && controls.update) {
        controls.update();
    }
    
    // Rotar ligeramente la escena para efecto de movimiento (opcional)
    // scene.rotation.y += config.animationSpeed;
    
    // Renderizar escena
    renderer.render(scene, camera);
    
    // Log periódico para debugging (cada 5 segundos aprox)
    if (Math.random() < 0.001) {
        console.log(`🎬 Animando: ${scene.children.length} objetos en escena, ${nodes.length} nodos`);
        if (controls) {
            console.log(`🎮 Controls activos, cámara en (${camera.position.x.toFixed(1)}, ${camera.position.y.toFixed(1)}, ${camera.position.z.toFixed(1)})`);
        } else {
            console.log('🎮 Controls no disponibles - usando controles básicos');
        }
    }
}

/**
 * Resalta un commit específico en la visualización 3D
 */
function highlightCommitIn3D(commitHash) {
    // Restaurar todos los nodos a su estado normal
    nodes.forEach(node => {
        node.material.emissive.setHex(0x000000);
        node.scale.set(1, 1, 1);
    });
    
    // Buscar y resaltar el commit específico
    const targetNode = nodes.find(node => 
        node.userData.hash === commitHash
    );
    
    if (targetNode) {
        targetNode.material.emissive.setHex(0xffffff);
        
        // Enfocar la cámara en el commit
        if (controls) {
            controls.target.copy(targetNode.position);
            controls.update();
        }
        
        // Mostrar información del commit
        showElementInfo(targetNode.userData);
    } else {
        console.warn('⚠️ No se encontró nodo para commit:', commitHash);
    }
}

/**
 * Verifica que todas las dependencias necesarias estén cargadas
 */
function checkDependencies() {
    const dependencies = {
        'THREE': typeof THREE !== 'undefined',
        'THREE.OrbitControls': typeof THREE !== 'undefined' && typeof THREE.OrbitControls !== 'undefined', 
        'D3': typeof d3 !== 'undefined'
    };
    
    const missing = [];
    const success = Object.entries(dependencies).every(([name, available]) => {
        if (!available) {
            missing.push(name);
            return false;
        }
        return true;
    });
    
    return {
        success: missing.length === 0,
        missing: missing,
        available: Object.keys(dependencies).filter(name => dependencies[name])
    };
}

/**
 * Inicialización cuando el DOM está cargado
 */
document.addEventListener('DOMContentLoaded', () => {
    // Verificar soporte de WebGL
    if (!window.WebGLRenderingContext) {
        alert('Tu navegador no soporta WebGL, necesario para la visualización 3D');
        return;
    }
    
    // Verificar dependencias críticas
    const dependenciesCheck = checkDependencies();
    if (!dependenciesCheck.success) {
        console.error('Dependencias faltantes:', dependenciesCheck.missing);
        
        // Mostrar notificación si la función está disponible
        if (typeof showNotification === 'function') {
            showNotification(`Error: Faltan librerías necesarias: ${dependenciesCheck.missing.join(', ')}`, 'error');
        } else {
            // Fallback: mostrar alert
            alert(`Error: Faltan librerías necesarias: ${dependenciesCheck.missing.join(', ')}`);
        }
    } else {
        console.log('✅ Todas las dependencias cargadas correctamente:', dependenciesCheck.available);
    }
    
    // Inicializar con un pequeño delay para asegurar carga completa
    setTimeout(() => {
        // Inicializar visualización
        initVisualization();
        
        // Timeline D3.js deshabilitado - usando sistema de vistas alternas
        console.log('📈 Timeline D3.js omitido - usando nuevo sistema de vistas alternas');
        // if (typeof initTimeline === 'function' && typeof d3 !== 'undefined') {
        //     initTimeline();
        // } else if (typeof initTimeline === 'function') {
        //     console.warn('D3.js no disponible, timeline no se inicializará');
        // }
    }, 100);
    
    // Manejar envío del formulario
    const repoForm = document.getElementById('repoForm');
    repoForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const repoUrl = document.getElementById('repoUrl').value;
        loadCommits(repoUrl);
    });
    
    // Manejar botones de zoom con verificación de existencia
    const btnZoomIn = document.getElementById('btnZoomIn');
    if (btnZoomIn) {
        btnZoomIn.addEventListener('click', () => {
            if (camera) {
                camera.position.z *= 0.8;
                console.log('🔍 Zoom In (botón regular)');
            }
        });
    }
    
    const btnZoomOut = document.getElementById('btnZoomOut');
    if (btnZoomOut) {
        btnZoomOut.addEventListener('click', () => {
            if (camera) {
                camera.position.z *= 1.2;
                console.log('🔍 Zoom Out (botón regular)');
            }
        });
    }
    
    const btnReset = document.getElementById('btnReset');
    if (btnReset) {
        btnReset.addEventListener('click', () => {
            if (camera) {
                camera.position.set(0, 0, 100);
                camera.lookAt(0, 0, 0);
                if (controls && controls.reset) {
                    controls.reset();
                }
                console.log('🏠 Reset View (botón regular)');
            }
        });
    }
    
    // Botón de nodos de prueba para debugging (opcional)
    const btnDebugNodes = document.getElementById('btnDebugNodes');
    if (btnDebugNodes) {
        btnDebugNodes.addEventListener('click', () => {
            console.log('🧪 Botón de debug presionado');
            
            // Diagnóstico completo del estado
            console.log('🔍 DIAGNÓSTICO COMPLETO:');
            console.log(`- Scene: ${scene ? 'OK' : 'NULL'}`);
            console.log(`- Camera: ${camera ? 'OK' : 'NULL'}`);
            console.log(`- Renderer: ${renderer ? 'OK' : 'NULL'}`);
            console.log(`- Controls: ${controls ? 'OK' : 'NULL'}`);
            
            const container = document.getElementById('visualization-container');
            if (container) {
                const rect = container.getBoundingClientRect();
                console.log(`- Container: ${rect.width}x${rect.height} px`);
                console.log(`- Container visible: ${rect.width > 0 && rect.height > 0}`);
            }
            
            if (renderer && renderer.domElement) {
                const canvas = renderer.domElement;
                console.log(`- Canvas: ${canvas.width}x${canvas.height} px`);
                console.log(`- Canvas en DOM: ${document.body.contains(canvas)}`);
                
                const canvasStyle = window.getComputedStyle(canvas);
                console.log(`- Canvas display: ${canvasStyle.display}`);
                console.log(`- Canvas visibility: ${canvasStyle.visibility}`);
                console.log(`- Canvas opacity: ${canvasStyle.opacity}`);
            }
            
            clearScene();
            createTestNodes();
        });
    }
    
    // Botón de test del timeline
    const btnTestTimeline = document.getElementById('btnTestTimeline');
    if (btnTestTimeline) {
        btnTestTimeline.addEventListener('click', () => {
            console.log('🕐 Botón Test Timeline presionado');
            
            // Verificar estado del timeline
            console.log('🔍 DIAGNÓSTICO TIMELINE:');
            console.log(`- D3: ${typeof d3 !== 'undefined' ? 'OK' : 'NULL'}`);
            console.log(`- initTimeline función: ${typeof initTimeline === 'function' ? 'OK' : 'NULL'}`);
            console.log(`- renderTimeline función: ${typeof renderTimeline === 'function' ? 'OK' : 'NULL'}`);
            
            const timelineContainer = document.getElementById('timeline-container-full');
            if (timelineContainer) {
                console.log(`- Timeline container: OK (${timelineContainer.children.length} elementos)`);
            } else {
                console.log('- Timeline container: NULL');
            }
            
            // Crear datos de prueba para el timeline
            if (typeof renderTimeline === 'function') {
                const testCommits = [
                    {
                        hash: 'abc123',
                        message: 'Commit de prueba 1',
                        author: 'Usuario Test',
                        timestamp: new Date(Date.now() - 86400000).toISOString(), // 1 día atrás
                        branch: 'main'
                    },
                    {
                        hash: 'def456',
                        message: 'Commit de prueba 2',
                        author: 'Usuario Test',
                        timestamp: new Date(Date.now() - 43200000).toISOString(), // 12 horas atrás
                        branch: 'main'
                    },
                    {
                        hash: 'ghi789',
                        message: 'Commit de prueba 3',
                        author: 'Usuario Test',
                        timestamp: new Date().toISOString(), // Ahora
                        branch: 'feature'
                    }
                ];
                
                console.log('📊 Creando timeline con datos de prueba...');
                renderTimeline(testCommits);
            } else {
                console.warn('⚠️ Función renderTimeline no disponible');
                showNotification('Timeline no disponible - D3.js requerido', 'warning');
            }
        });
    }
    
    // Manejar botones de filtros con verificación de existencia
    const btnApplyFilters = document.getElementById('applyFilters');
    if (btnApplyFilters) {
        btnApplyFilters.addEventListener('click', applyFilters);
    }
    
    const btnClearFilters = document.getElementById('clearFilters');
    if (btnClearFilters) {
        btnClearFilters.addEventListener('click', clearFilters);
    }
    
    // Manejar botones flotantes (que son los que están visibles)
    const btnZoomInFloat = document.getElementById('btnZoomInFloat');
    if (btnZoomInFloat) {
        btnZoomInFloat.addEventListener('click', () => {
            console.log('🔍 Zoom In');
            if (camera) {
                camera.position.z *= 0.8;
                console.log(`Cámara zoom in: z=${camera.position.z.toFixed(2)}`);
            }
        });
    }
    
    const btnZoomOutFloat = document.getElementById('btnZoomOutFloat');
    if (btnZoomOutFloat) {
        btnZoomOutFloat.addEventListener('click', () => {
            console.log('🔍 Zoom Out');
            if (camera) {
                camera.position.z *= 1.2;
                console.log(`Cámara zoom out: z=${camera.position.z.toFixed(2)}`);
            }
        });
    }
    
    const btnResetFloat = document.getElementById('btnResetFloat');
    if (btnResetFloat) {
        btnResetFloat.addEventListener('click', () => {
            console.log('🏠 Reset View');
            if (camera && controls) {
                camera.position.set(50, 50, 100);
                camera.lookAt(0, 0, 0);
                controls.target.set(0, 0, 0);
                controls.update();
                console.log('Vista restablecida');
            }
        });
    }
    
    // Manejar botones del timeline
    const timelineExport = document.getElementById('timeline-export');
    const timelineFullscreen = document.getElementById('timeline-fullscreen');
    
    if (timelineExport) {
        timelineExport.addEventListener('click', () => {
            console.log('📊 Exportar timeline');
            if (typeof exportTimelineData === 'function') {
                exportTimelineData();
            } else {
                console.warn('Función exportTimelineData no está disponible');
                showNotification('Función de exportar no disponible aún', 'warning');
            }
        });
    }
    
    if (timelineFullscreen) {
        timelineFullscreen.addEventListener('click', () => {
            console.log('🖥️ Pantalla completa timeline');
            const timelineContainer = document.getElementById('timeline-container-full');
            if (timelineContainer) {
                if (timelineContainer.requestFullscreen) {
                    timelineContainer.requestFullscreen();
                } else if (timelineContainer.webkitRequestFullscreen) {
                    timelineContainer.webkitRequestFullscreen();
                } else if (timelineContainer.msRequestFullscreen) {
                    timelineContainer.msRequestFullscreen();
                } else {
                    console.warn('Pantalla completa no soportada');
                    showNotification('Pantalla completa no soportada', 'warning');
                }
            }
        });
    }
    
    // Timeline D3.js deshabilitado - usando sistema de vistas alternas
    console.log('📈 Timeline D3.js omitido - usando sistema de vistas alternas en su lugar');
    // console.log('🕐 Inicializando timeline...');
    // if (typeof initTimeline === 'function') {
    //     initTimeline();
    //     console.log('✅ Timeline inicializado');
    // } else {
    //     console.warn('⚠️ Función initTimeline no disponible');
    // }
});

/**
 * Ajusta la cámara para encuadrar toda la escena (FUNCIÓN FALTANTE)
 */
function adjustCameraToFitScene() {
    console.log(`🎥 Ajustando cámara para ${nodes.length} nodos...`);
    
    if (nodes.length === 0) {
        console.warn('No hay nodos para ajustar la cámara');
        return;
    }

    // Calcular bounding box de todos los nodos
    const box = new THREE.Box3();
    nodes.forEach((node, index) => {
        box.expandByObject(node);
        console.log(`Nodo ${index}: posición(${node.position.x.toFixed(1)}, ${node.position.y.toFixed(1)}, ${node.position.z.toFixed(1)})`);
    });

    // Si no hay nodos visibles, usar valores por defecto
    if (box.isEmpty()) {
        console.warn('Bounding box vacío, usando posición por defecto');
        camera.position.set(0, 0, 100);
        camera.lookAt(0, 0, 0);
        return;
    }

    // Calcular el centro y tamaño de la caja
    const center = box.getCenter(new THREE.Vector3());
    const size = box.getSize(new THREE.Vector3());

    console.log(`📦 Bounding box - Centro: (${center.x.toFixed(1)}, ${center.y.toFixed(1)}, ${center.z.toFixed(1)})`);
    console.log(`📏 Tamaño: (${size.x.toFixed(1)}, ${size.y.toFixed(1)}, ${size.z.toFixed(1)})`);

    // Calcular la distancia de cámara necesaria
    const maxDim = Math.max(size.x, size.y, size.z);
    const fov = camera.fov * (Math.PI / 180);
    let cameraDistance = Math.max(50, Math.abs(maxDim / Math.sin(fov / 2)) * 2); // Aumentar multiplicador

    // Posicionar la cámara con una vista mejor
    const cameraOffset = new THREE.Vector3(
        center.x + cameraDistance * 0.5, 
        center.y + cameraDistance * 0.3, 
        center.z + cameraDistance
    );
    
    camera.position.copy(cameraOffset);
    camera.lookAt(center);

    // Actualizar controles
    controls.target.copy(center);
    controls.update();

    console.log(`✅ Cámara ajustada - Posición: (${camera.position.x.toFixed(1)}, ${camera.position.y.toFixed(1)}, ${camera.position.z.toFixed(1)})`);
    console.log(`🎯 Target: (${center.x.toFixed(1)}, ${center.y.toFixed(1)}, ${center.z.toFixed(1)}), distancia: ${cameraDistance.toFixed(1)}`);
}

/**
 * Calcula posiciones de commits en estilo git graph
 */
function calculateGitGraphPositions(commits) {
    gitGraph.branches.clear();
    gitGraph.commitPositions.clear();
    gitGraph.maxBranchLevel = 0;

    // Ordenar commits por fecha
    const sortedCommits = [...commits].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    
    // Asignar posiciones de rama
    sortedCommits.forEach((commit, index) => {
        const branchName = commit.branch || 'main';
        
        if (!gitGraph.branches.has(branchName)) {
            gitGraph.branches.set(branchName, {
                level: gitGraph.maxBranchLevel++,
                color: generateBranchColor(branchName),
                commits: []
            });
        }
        
        const branch = gitGraph.branches.get(branchName);
        branch.commits.push(commit);
        
        // Calcular posición en el grafo con más variación Z
        const position = {
            x: index * config.commitSpacing,
            y: branch.level * config.branchSpacing,
            z: (Math.sin(index * 0.1) * 10) + (branch.level * 5), // Añadir variación en Z
            branch: branchName,
            level: branch.level
        };
        
        gitGraph.commitPositions.set(commit.hash, position);
    });

    console.log(`Git Graph calculado: ${gitGraph.branches.size} ramas, ${sortedCommits.length} commits`);
}

/**
 * Genera un color único para cada rama
 */
function generateBranchColor(branchName) {
    const colors = [
        0x28a745, // verde
        0x007bff, // azul
        0xffc107, // amarillo
        0xdc3545, // rojo
        0x6f42c1, // morado
        0x20c997, // teal
        0xfd7e14, // naranja
        0xe83e8c  // rosa
    ];
    
    let hash = 0;
    for (let i = 0; i < branchName.length; i++) {
        hash = branchName.charCodeAt(i) + ((hash << 5) - hash);
    }
    
    return colors[Math.abs(hash) % colors.length];
}

/**
 * Crea un nodo de commit avanzado con información visual
 */
function createCommitNodeAdvanced(commit, index) {
    // Verificar datos del commit
    if (!commit || !commit.hash) {
        console.warn(`⚠️ Commit inválido en índice ${index}:`, commit);
        return null;
    }
    
    const position = gitGraph.commitPositions.get(commit.hash);
    if (!position) {
        console.warn(`⚠️ No se encontró posición para commit ${commit.hash}`);
        return null;
    }

    const branch = gitGraph.branches.get(position.branch);
    if (!branch) {
        console.warn(`⚠️ No se encontró rama ${position.branch} para commit ${commit.hash}`);
        return null;
    }
    
    // Geometría basada en el tipo de commit
    let geometry;
    if (commit.message && commit.message.toLowerCase().includes('merge')) {
        geometry = new THREE.OctahedronGeometry(config.nodeSize * 1.2);
    } else if (commit.message && commit.message.toLowerCase().includes('tag')) {
        geometry = new THREE.ConeGeometry(config.nodeSize, config.nodeSize * 2, 6);
    } else {
        geometry = new THREE.SphereGeometry(config.nodeSize, 16, 16);
    }

    // Determinar color basado en la edad del commit
    let nodeColor = branch.color;
    let nodeSize = config.nodeSize;
    
    // Commits más recientes son más brillantes y grandes
    const commitDate = new Date(commit.timestamp);
    const daysSinceCommit = (Date.now() - commitDate.getTime()) / (1000 * 60 * 60 * 24);
    
    if (daysSinceCommit < 7) {
        // Commits de la última semana - más brillantes y grandes
        nodeColor = config.nodeColors.recent;
        nodeSize *= 1.3;
    } else if (daysSinceCommit < 30) {
        // Commits del último mes - normales
        nodeColor = branch.color;
    } else {
        // Commits antiguos - más pequeños y grises
        nodeColor = config.nodeColors.old;
        nodeSize *= 0.8;
    }
    
    // Actualizar geometría con el nuevo tamaño
    if (commit.message && commit.message.toLowerCase().includes('merge')) {
        geometry = new THREE.OctahedronGeometry(nodeSize * 1.2);
    } else if (commit.message && commit.message.toLowerCase().includes('tag')) {
        geometry = new THREE.ConeGeometry(nodeSize, nodeSize * 2, 6);
    } else {
        geometry = new THREE.SphereGeometry(nodeSize, 20, 20); // Más detallado
    }

    // Material con efectos visuales mejorados
    const material = new THREE.MeshPhongMaterial({
        color: nodeColor,
        shininess: 150,
        transparent: false,
        opacity: 1.0,
        emissive: new THREE.Color(nodeColor).multiplyScalar(0.15), // Más emisividad
        specular: 0x222222 // Añadir especularidad
    });

    const commitNode = new THREE.Mesh(geometry, material);
    
    // Posicionar el nodo
    commitNode.position.set(position.x, position.y, position.z);
    
    // Log de la posición para debugging
    if (index < 5) { // Solo los primeros 5 para no saturar
        console.log(`🔹 Nodo ${index}: hash=${commit.hash.substring(0, 7)}, pos=(${position.x.toFixed(1)}, ${position.y.toFixed(1)}, ${position.z.toFixed(1)}), color=${branch.color.toString(16)}`);
    }
    
    // Añadir datos de usuario
    commitNode.userData = {
        ...commit,
        type: 'commit',
        branch: position.branch,
        level: position.level,
        index: index
    };

    // Efectos visuales adicionales
    if (commit.message && commit.message.toLowerCase().includes('merge')) {
        commitNode.userData.type = 'merge';
        // Añadir anillo para merges
        const ringGeometry = new THREE.RingGeometry(config.nodeSize * 1.5, config.nodeSize * 1.8, 16);
        const ringMaterial = new THREE.MeshBasicMaterial({
            color: branch.color,
            transparent: true,
            opacity: 0.3,
            side: THREE.DoubleSide
        });
        const ring = new THREE.Mesh(ringGeometry, ringMaterial);
        ring.position.copy(commitNode.position);
        scene.add(ring);
        connections.push(ring);
    }

    // Sombras
    commitNode.castShadow = true;
    commitNode.receiveShadow = true;

    return commitNode;
}

/**
 * Crea conexiones entre commits en el git graph
 */
function createGitGraphConnections(commits) {
    const sortedCommits = [...commits].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    
    for (let i = 1; i < sortedCommits.length; i++) {
        const currentCommit = sortedCommits[i];
        const previousCommit = sortedCommits[i - 1];
        
        const currentPos = gitGraph.commitPositions.get(currentCommit.hash);
        const previousPos = gitGraph.commitPositions.get(previousCommit.hash);
        
        if (currentPos && previousPos) {
            const connection = createConnection(previousPos, currentPos, currentCommit.branch);
            if (connection) {
                scene.add(connection);
                connections.push(connection);
            }
        }
    }
}

/**
 * Crea una conexión visual entre dos commits
 */
function createConnection(fromPos, toPos, branchName) {
    const branch = gitGraph.branches.get(branchName);
    if (!branch) return null;

    const points = [];
    points.push(new THREE.Vector3(fromPos.x, fromPos.y, fromPos.z));
    
    // Si es el mismo nivel, línea recta
    if (fromPos.level === toPos.level) {
        points.push(new THREE.Vector3(toPos.x, toPos.y, toPos.z));
    } else {
        // Crear curva para cambios de rama
        const midX = (fromPos.x + toPos.x) / 2;
        points.push(new THREE.Vector3(midX, fromPos.y, fromPos.z));
        points.push(new THREE.Vector3(midX, toPos.y, toPos.z));
        points.push(new THREE.Vector3(toPos.x, toPos.y, toPos.z));
    }
    
    const geometry = new THREE.BufferGeometry().setFromPoints(points);
    
    // Usar el color de conexión configurado para mejor visibilidad
    const material = new THREE.LineBasicMaterial({
        color: config.connectionColor, // Cyan brillante
        linewidth: 3, // Líneas más gruesas
        transparent: true,
        opacity: 0.8 // Menos transparencia
    });
    
    return new THREE.Line(geometry, material);
}

/**
 * Crea etiquetas de ramas
 */
function createBranchLabels() {
    gitGraph.branches.forEach((branch, branchName) => {
        if (branch.commits.length > 0) {
            const lastCommit = branch.commits[branch.commits.length - 1];
            const position = gitGraph.commitPositions.get(lastCommit.hash);
            
            if (position) {
                const label = createTextLabel(branchName, branch.color);
                label.position.set(position.x + 10, position.y, position.z);
                scene.add(label);
                connections.push(label);
            }
        }
    });
}

/**
 * Crea una etiqueta de texto 3D
 */
function createTextLabel(text, color) {
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    canvas.width = 256;
    canvas.height = 64;
    
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.fillStyle = `#${color.toString(16)}`;
    context.font = '20px Arial';
    context.textAlign = 'center';
    context.fillText(text, canvas.width / 2, canvas.height / 2 + 5);
    
    const texture = new THREE.CanvasTexture(canvas);
    const material = new THREE.MeshBasicMaterial({
        map: texture,
        transparent: true,
        opacity: 0.8
    });
    
    const geometry = new THREE.PlaneGeometry(20, 5);
    return new THREE.Mesh(geometry, material);
}

/**
 * Actualiza el timeline con información de commits
 */
function updateTimelineWithCommits(commits) {
    console.log('🔄 Actualizando timeline con', commits?.length, 'commits');
    
    // Buscar el contenedor correcto del sistema de vistas alternas
    const timelineContainer = document.getElementById('timeline-container-full');
    const emptyState = document.getElementById('timeline-empty-state-full');
    
    if (!timelineContainer) {
        console.warn('⚠️ Timeline container no encontrado - delegando al sistema de vistas alternas');
        // Disparar evento para que el sistema correcto lo maneje
        if (commits && commits.length > 0) {
            const commitsLoadedEvent = new CustomEvent('commitsLoaded', {
                detail: { commits: commits, source: 'delegated' }
            });
            window.dispatchEvent(commitsLoadedEvent);
        }
        return;
    }
    
    if (!commits || commits.length === 0) {
        console.warn('⚠️ No hay commits para mostrar en timeline');
        return;
    }
    
    // Asegurar que el timeline sea visible
    timelineContainer.style.display = 'block';
    timelineContainer.style.visibility = 'visible';
    
    // Ocultar estado vacío
    if (emptyState) {
        emptyState.style.display = 'none';
    }
    
    // Limpiar contenido anterior
    const existingTimeline = timelineContainer.querySelector('.timeline-content');
    if (existingTimeline) {
        existingTimeline.remove();
    }
    
    // Crear contenido del timeline
    const timelineContent = document.createElement('div');
    timelineContent.className = 'timeline-content';
    
    // Ordenar commits por fecha (más reciente primero)
    const sortedCommits = [...commits].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    
    console.log('📊 Commits ordenados para timeline:', sortedCommits.length);
    console.log('📄 Primer commit:', sortedCommits[0]);
    
    // Crear elementos del timeline - HTML con botón de toggle
    const timelineHTML = `
        <button class="timeline-toggle-btn" onclick="toggleTimeline()" title="Expandir/Colapsar Timeline">
            <i class="fas fa-expand-alt"></i>
            <span class="d-none d-sm-inline">Expandir</span>
        </button>
        <div class="timeline-header">
            <h6>📊 Actividad reciente (${commits.length} commits)</h6>
            <small>Últimos ${Math.min(commits.length, 10)} commits mostrados</small>
        </div>
        <div class="timeline-items">
            ${sortedCommits.slice(0, 10).map((commit, index) => {
                const commitDate = new Date(commit.timestamp);
                const daysSince = Math.floor((Date.now() - commitDate.getTime()) / (1000 * 60 * 60 * 24));
                
                let timeAgo = '';
                if (daysSince === 0) {
                    timeAgo = 'Hoy';
                } else if (daysSince === 1) {
                    timeAgo = 'Ayer';
                } else if (daysSince < 7) {
                    timeAgo = `${daysSince} días`;
                } else if (daysSince < 30) {
                    timeAgo = `${Math.floor(daysSince / 7)} semanas`;
                } else {
                    timeAgo = `${Math.floor(daysSince / 30)} meses`;
                }
                
                console.log(`📝 Generando item ${index + 1}: ${commit.message.substring(0, 30)}...`);
                
                return `
                    <div class="timeline-item d-flex align-items-center mb-2 p-3" 
                         onclick="highlightCommit('${commit.hash}')"
                         style="cursor: pointer;">
                        <div class="timeline-icon me-3">
                            <i class="fas fa-code-branch"></i>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex justify-content-between align-items-start">
                                <div style="flex: 1;">
                                    <strong style="display: block; margin-bottom: 4px;">
                                        ${commit.message.length > 45 ? commit.message.substring(0, 45) + '...' : commit.message}
                                    </strong>
                                    <div class="small">
                                        <i class="fas fa-user me-1"></i>
                                        ${commit.author?.name || 'Desconocido'}
                                        <span class="mx-2">•</span>
                                        <code>${commit.hash.substring(0, 7)}</code>
                                    </div>
                                </div>
                                <div style="text-align: right; min-width: 80px;">
                                    <small>${timeAgo}</small>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            }).join('')}
        </div>
        ${commits.length > 10 ? `
            <div class="timeline-footer text-center mt-3">
                <small>... y ${commits.length - 10} commits más en el repositorio</small>
            </div>
        ` : ''}
        <div class="timeline-compact-indicator">
            <i class="fas fa-chevron-down me-1"></i>
            <small>
                ${commits.length > 3 ? `+${commits.length - 3} commits más` : 'Haz clic en "Expandir" para ver más opciones'}
            </small>
        </div>
    `;
    
    timelineContent.innerHTML = timelineHTML;
    timelineContainer.appendChild(timelineContent);
    
    // Forzar visibilidad y verificar que se agregó correctamente
    timelineContainer.style.display = 'block';
    timelineContainer.style.visibility = 'visible';
    timelineContainer.style.opacity = '1';
    
    // Verificar que el contenido se agregó
    const addedItems = timelineContainer.querySelectorAll('.timeline-item');
    console.log('✅ Timeline actualizado con', commits.length, 'commits');
    console.log('👁️ Items visibles en DOM:', addedItems.length);
    console.log('📦 HTML del timeline:', timelineContent.innerHTML.substring(0, 200) + '...');
    
    // Marcar como listo
    timelineContainer.setAttribute('data-loaded', 'true');
    
    // Disparar evento para notificar al sistema de vistas alternas
    const commitsLoadedEvent = new CustomEvent('commitsLoaded', {
        detail: { commits: commits, source: 'updateTimelineWithCommits' }
    });
    window.dispatchEvent(commitsLoadedEvent);
}

/**
 * Resalta un commit específico en la visualización 3D
 */
function highlightCommit(commitHash) {
    console.log('🎯 Resaltando commit:', commitHash);
    
    // Buscar el nodo del commit
    const commitNode = nodes.find(node => 
        node.userData && node.userData.hash === commitHash
    );
    
    if (commitNode) {
        // Restaurar todos los nodos a su estado normal
        nodes.forEach(node => {
            if (node.material && node.material.emissive) {
                if (node.userData.originalEmissive) {
                    node.material.emissive.copy(node.userData.originalEmissive);
                }
                node.material.opacity = 1.0;
            }
        });
        
        // Resaltar el nodo seleccionado
        if (commitNode.material && commitNode.material.emissive) {
            commitNode.material.emissive.setHex(0xffffff);
            
            // Enfocar la cámara en el commit
            if (controls) {
                controls.target.copy(commitNode.position);
                controls.update();
            }
            
            // Mostrar información del commit
            showElementInfo(commitNode.userData);
        }
    } else {
        console.warn('⚠️ No se encontró nodo para commit:', commitHash);
    }
}

/**
 * Crea nodos de prueba para diagnosticar problemas de visualización
 */
function createTestNodes() {
    console.log('🧪 Creando nodos de prueba para debugging...');
    
    // Verificar que los componentes básicos están disponibles
    if (!scene) {
        console.error('❌ Scene no está inicializada');
        return;
    }
    if (!camera) {
        console.error('❌ Camera no está inicializada');
        return;
    }
    if (!renderer) {
        console.error('❌ Renderer no está inicializado');
        return;
    }
    
    console.log(`📊 Estado antes de crear nodos: scene.children=${scene.children.length}, nodes.length=${nodes.length}`);
    
    // Crear 5 nodos de prueba en posiciones conocidas (más grandes y visibles)
    const testPositions = [
        { x: 0, y: 0, z: 0, name: 'Centro' },
        { x: 30, y: 0, z: 0, name: 'Derecha' },
        { x: -30, y: 0, z: 0, name: 'Izquierda' },
        { x: 0, y: 30, z: 0, name: 'Arriba' },
        { x: 0, y: -30, z: 0, name: 'Abajo' }
    ];
    
    const testColors = [0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0xff00ff];
    const colorNames = ['Rojo', 'Verde', 'Azul', 'Amarillo', 'Magenta'];
    
    testPositions.forEach((pos, index) => {
        // Crear geometría MUY grande y visible
        const geometry = new THREE.SphereGeometry(8, 32, 32);
        
        // Usar MeshBasicMaterial para máxima visibilidad (no depende de luces)
        const material = new THREE.MeshBasicMaterial({ 
            color: testColors[index],
            wireframe: false // Sólido para mejor visibilidad
        });
        
        const testNode = new THREE.Mesh(geometry, material);
        testNode.position.set(pos.x, pos.y, pos.z);
        
        // Añadir datos de prueba
        testNode.userData = {
            type: 'test',
            message: `Nodo de prueba ${index + 1}`,
            hash: `test${index}`,
            author: 'Sistema de prueba',
            timestamp: new Date(),
            branch: 'test',
            testName: pos.name
        };
        
        // Agregar a la escena
        scene.add(testNode);
        nodes.push(testNode);
        
        console.log(`🔹 Nodo ${index + 1} (${colorNames[index]}, ${pos.name}): pos=(${pos.x}, ${pos.y}, ${pos.z}), color=#${testColors[index].toString(16)}`);
    });
    
    console.log(`📊 Estado después de crear nodos: scene.children=${scene.children.length}, nodes.length=${nodes.length}`);
    
    // Posicionar cámara manualmente para asegurar visibilidad
    camera.position.set(50, 50, 100);
    camera.lookAt(0, 0, 0);
    
    if (controls) {
        controls.target.set(0, 0, 0);
        controls.update();
    }
    
    console.log(`🎥 Cámara posicionada en (${camera.position.x}, ${camera.position.y}, ${camera.position.z}) mirando hacia (0, 0, 0)`);
    
    // Forzar renderizado inmediato
    if (renderer && scene && camera) {
        renderer.render(scene, camera);
        console.log('🎬 Renderizado forzado ejecutado');
        
        // Información del contexto WebGL
        const gl = renderer.getContext();
        console.log(`🔧 WebGL info: ${gl.getParameter(gl.VERSION)}`);
        console.log(`🔧 Renderer info: ${gl.getParameter(gl.RENDERER)}`);
    }
    
    console.log(`✅ ${testPositions.length} nodos de prueba creados y renderizados`);
    
    // Crear un test adicional: agregar un cubo wireframe grande
    const cubeGeometry = new THREE.BoxGeometry(20, 20, 20);
    const cubeMaterial = new THREE.MeshBasicMaterial({ 
        color: 0xffffff, 
        wireframe: true,
        linewidth: 3
    });
    const testCube = new THREE.Mesh(cubeGeometry, cubeMaterial);
    testCube.position.set(0, 0, 0);
    scene.add(testCube);
    
    console.log('📦 Cubo wireframe de prueba agregado en el centro');
}

/**
 * Funciones auxiliares adicionales para integración completa
 */
function loadTimelineData() {
    console.log('🔄 Cargando datos del timeline...');
    
    // Si ya tenemos commits almacenados, los usamos
    if (window.ViewSystem.timelineCommits.length > 0) {
        updateTimelineFullView(window.ViewSystem.timelineCommits);
        showNotification('Timeline cargado desde caché', 'success');
        return;
    }
    
    // Intentar cargar desde la visualización 3D actual
    if (typeof commits !== 'undefined' && commits.length > 0) {
        updateTimelineFullView(commits);
        showNotification('Timeline cargado desde visualización 3D', 'success');
        return;
    }
    
    // Si no hay datos, mostrar mensaje
    showNotification('No hay commits disponibles. Primero carga un repositorio.', 'warning');
}

/**
 * Integración con el sistema de carga de commits existente (con prevención de bucles)
 */
function integrateWithExistingSystem() {
    let lastIntegrationTime = 0;
    let isIntegrating = false;
    
    // Sobrescribir la función updateTimelineWithCommits existente
    const originalUpdateTimeline = window.updateTimelineWithCommits;
    
    window.updateTimelineWithCommits = function(commits) {
        const now = Date.now();
        
        // Evitar bucles infinitos - solo procesar si han pasado al menos 2 segundos
        if (isIntegrating || (now - lastIntegrationTime) < 2000) {
            console.log('🔗 Integración omitida - evitando bucle infinito');
            return;
        }
        
        isIntegrating = true;
        lastIntegrationTime = now;
        
        console.log('🔗 Integrando con sistema existente:', commits?.length, 'commits');
        
        // Mantener funcionalidad original si es necesaria
        if (originalUpdateTimeline && typeof originalUpdateTimeline === 'function') {
            try {
                originalUpdateTimeline.call(this, commits);
            } catch (error) {
                console.warn('⚠️ Error en función original:', error);
            }
        }
        
        // Integrar con nuevo sistema de vistas alternas
        if (commits && commits.length > 0) {
            // Guardar commits para el sistema de vistas alternas
            window.ViewSystem.timelineCommits = [...commits];
            window.ViewSystem.totalCommits = commits.length;
            
            // Si estamos en vista timeline, actualizar inmediatamente
            if (window.ViewSystem.currentView === 'timeline') {
                updateTimelineFullView(commits);
            }
            
            // Disparar evento para notificar al sistema (solo si no se ha disparado recientemente)
            const event = new CustomEvent('commitsLoaded', {
                detail: { commits: commits, source: 'integrated' }
            });
            window.dispatchEvent(event);
        }
        
        // Resetear flag después de procesar
        setTimeout(() => {
            isIntegrating = false;
        }, 1000);
    };
}

/**
 * Función para sincronizar con commits globales
 */
function syncWithGlobalCommits() {
    // Verificar si hay commits en variables globales
    if (typeof window.commits !== 'undefined' && window.commits.length > 0) {
        console.log('🔗 Sincronizando con commits globales:', window.commits.length);
        window.ViewSystem.timelineCommits = [...window.commits];
        window.ViewSystem.totalCommits = window.commits.length;
        
        if (window.ViewSystem.currentView === 'timeline') {
            updateTimelineFullView(window.commits);
        }
    }
}

/**
 * Sistema de notificaciones mejorado
 */
function showNotification(message, type = 'info') {
    // Usar sistema existente si está disponible
    if (typeof window.uiController !== 'undefined' && window.uiController.showNotification) {
        window.uiController.showNotification(message, type);
        return;
    }
    
    // Sistema de respaldo simple
    console.log(`${type.toUpperCase()}: ${message}`);
    
    // Crear toast simple si no hay sistema
    const toast = document.createElement('div');
    toast.className = `alert alert-${type === 'error' ? 'danger' : type} position-fixed`;
    toast.style.top = '20px';
    toast.style.right = '20px';
    toast.style.zIndex = '9999';
    toast.style.minWidth = '250px';
    toast.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check' : type === 'error' ? 'exclamation-triangle' : 'info-circle'}"></i>
        ${message}
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
    }, 3000);
}

/**
 * Inicialización del sistema cuando el DOM está listo
 */
function initializeTimelineSystem() {
    console.log('🚀 Inicializando sistema completo de timeline...');
    
    // Integrar con sistema existente
    integrateWithExistingSystem();
    
    // Sincronizar con commits globales si existen
    syncWithGlobalCommits();
    
    // Configurar controles contextuales iniciales
    const controls3d = document.querySelectorAll('.view-controls-3d');
    const controlsTimeline = document.querySelectorAll('.view-controls-timeline');
    
    // Mostrar controles de vista 3D por defecto
    controls3d.forEach(control => {
        control.style.display = 'flex';
    });
    
    controlsTimeline.forEach(control => {
        control.style.display = 'none';
    });
    
    // Verificar periódicamente por nuevos commits
    setInterval(() => {
        if (typeof window.commits !== 'undefined' && 
            window.commits.length > 0 && 
            window.commits.length !== window.ViewSystem.totalCommits) {
            console.log('🔄 Detectados nuevos commits, sincronizando...');
            syncWithGlobalCommits();
        }
    }, 5000);
    
    console.log('✅ Sistema de timeline inicializado completamente');
}

/**
 * Optimiza el layout del contenedor del timeline para máximo aprovechamiento del espacio
 */
function optimizeTimelineLayout() {
    const container = document.getElementById('timeline-container-full');
    if (!container) return;
    
    // Aplicar estilos optimizados para mejor aprovechamiento del espacio
    const style = container.style;
    style.display = 'flex';
    style.flexDirection = 'column';
    style.height = '100%';
    style.maxHeight = '90vh';
    style.overflow = 'hidden';
    
    // Optimizar el contenedor de commits si existe
    const commitsContainer = container.querySelector('.timeline-commits-container');
    if (commitsContainer) {
        commitsContainer.style.flex = '1 1 auto';
        commitsContainer.style.overflow = 'auto';
        commitsContainer.style.scrollBehavior = 'smooth';
    }
    
    console.log('📐 Layout del timeline optimizado');
}

// Inicializar cuando el DOM esté listo
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        initializeTimelineSystem();
        // Optimizar layout después de la inicialización
        setTimeout(optimizeTimelineLayout, 200);
    });
} else {
    initializeTimelineSystem();
    setTimeout(optimizeTimelineLayout, 200);
}

// Optimizar automáticamente cuando se redimensiona la ventana (con throttling)
window.addEventListener('resize', function() {
    // Debounce más largo para evitar re-renderizados constantes
    clearTimeout(window.ViewSystem.resizeTimeout);
    window.ViewSystem.resizeTimeout = setTimeout(() => {
        // Solo optimizar si el cambio de tamaño es significativo
        const currentHeight = window.innerHeight;
        const lastHeight = window.ViewSystem.lastWindowHeight || currentHeight;
        const heightDifference = Math.abs(currentHeight - lastHeight);
        
        // Solo re-optimizar si el cambio es mayor a 100px para evitar micro-ajustes
        if (heightDifference > 100 && window.ViewSystem.currentView === 'timeline' && window.ViewSystem.timelineCommits.length > 0) {
            console.log(`📏 Re-optimizando por cambio significativo de altura: ${lastHeight}px -> ${currentHeight}px`);
            optimizeCommitsPerPage();
            window.ViewSystem.lastWindowHeight = currentHeight;
        }
    }, 750); // Aumentar tiempo de debounce
});

// Funciones globales adicionales para compatibilidad
window.loadTimelineData = loadTimelineData;
window.syncWithGlobalCommits = syncWithGlobalCommits;
window.optimizeCommitsPerPage = optimizeCommitsPerPage;
window.optimizeTimelineLayout = optimizeTimelineLayout;
window.preserveScrollPosition = preserveScrollPosition;

// Función de utilidad para debugging del timeline
window.debugTimelineScroll = function() {
    const container = document.getElementById('timeline-container-full');
    if (!container) {
        console.log('❌ Timeline container no encontrado');
        return;
    }
    
    console.log('🔍 Debug del Timeline Scroll:');
    console.log('Container height:', container.offsetHeight);
    console.log('Container scrollHeight:', container.scrollHeight);
    console.log('Container scrollTop:', container.scrollTop);
    console.log('Container style.height:', container.style.height);
    console.log('Container style.maxHeight:', container.style.maxHeight);
    
    const commitsContainer = container.querySelector('.timeline-commits-container');
    if (commitsContainer) {
        console.log('Commits container height:', commitsContainer.offsetHeight);
        console.log('Commits container scrollHeight:', commitsContainer.scrollHeight);
        console.log('Commits container scrollTop:', commitsContainer.scrollTop);
    }
    
    return {
        container: {
            height: container.offsetHeight,
            scrollHeight: container.scrollHeight,
            scrollTop: container.scrollTop
        },
        commitsContainer: commitsContainer ? {
            height: commitsContainer.offsetHeight,
            scrollHeight: commitsContainer.scrollHeight,
            scrollTop: commitsContainer.scrollTop
        } : null
    };
};

console.log('✅ Sistema de Timeline Completo con Paginación inicializado');
    