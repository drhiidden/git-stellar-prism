/**
 * Timeline de commits para visualización histórica del desarrollo
 * Integrado con el sistema Git Graph
 */

// Variables globales del timeline
let timelineData = [];
let timelineSvg = null;
let timelineScale = null;
let timelineHeight = 250;
let timelineMargin = { top: 30, right: 40, bottom: 50, left: 40 };
let selectedCommit = null;
let timelineTooltip = null;
let timelineBranches = new Map();
let timelineZoom = null;

// Configuración del timeline
const timelineConfig = {
    commitRadius: 5,
    branchHeight: 20,
    maxBranches: 8,
    animationDuration: 300,
    zoomExtent: [0.1, 10],
    colors: [
        '#28a745', '#007bff', '#ffc107', '#dc3545', 
        '#6f42c1', '#20c997', '#fd7e14', '#e83e8c'
    ]
};

/**
 * Inicializa el timeline
 */
function initTimeline() {
    // Verificar que D3.js esté disponible
    if (typeof d3 === 'undefined') {
        console.error('D3.js no está cargado. Timeline no puede inicializarse.');
        const container = document.getElementById('timeline-container');
        if (container) {
            container.innerHTML = `
                <div class="alert alert-danger text-center m-3">
                    <i class="fas fa-exclamation-triangle"></i>
                    <strong>Error:</strong> D3.js no está disponible. 
                    El timeline no puede funcionar sin esta librería.
                </div>
            `;
        }
        return;
    }
    
    const container = d3.select('#timeline-container');
    
    // Limpiar contenido previo
    container.selectAll('*').remove();
    
    // Crear contenedor principal
    const mainContainer = container.append('div')
        .attr('class', 'timeline-main-container');
    
    // Crear controles del timeline
    const controls = mainContainer.append('div')
        .attr('class', 'timeline-controls d-flex justify-content-between align-items-center mb-3');
    
    const leftControls = controls.append('div')
        .attr('class', 'timeline-left-controls');
    
    leftControls.append('button')
        .attr('class', 'btn btn-outline-primary btn-sm me-2')
        .attr('id', 'timeline-zoom-reset')
        .html('<i class="fas fa-search-minus"></i> Reset Zoom')
        .on('click', resetTimelineZoom);
    
    leftControls.append('button')
        .attr('class', 'btn btn-outline-secondary btn-sm me-2')
        .attr('id', 'timeline-group-toggle')
        .html('<i class="fas fa-layer-group"></i> Agrupar')
        .on('click', toggleDayGrouping);
    
    leftControls.append('button')
        .attr('class', 'btn btn-outline-info btn-sm')
        .attr('id', 'timeline-filter-toggle')
        .html('<i class="fas fa-filter"></i> Solo Commits')
        .on('click', toggleCommitFilter);

    const rightControls = controls.append('div')
        .attr('class', 'timeline-right-controls');
    
    rightControls.append('button')
        .attr('class', 'btn btn-outline-success btn-sm me-2')
        .attr('id', 'timeline-export')
        .html('<i class="fas fa-download"></i> Exportar')
        .on('click', exportTimelineData);
    
    rightControls.append('button')
        .attr('class', 'btn btn-outline-dark btn-sm')
        .attr('id', 'timeline-fullscreen')
        .html('<i class="fas fa-expand"></i>')
        .on('click', toggleFullscreen);
    
    // Crear área de estadísticas
    const statsContainer = mainContainer.append('div')
        .attr('class', 'timeline-stats-container mb-3');
    
    const stats = statsContainer.append('div')
        .attr('class', 'timeline-stats card')
        .attr('id', 'timeline-stats');
    
    const statsBody = stats.append('div')
        .attr('class', 'card-body p-2');
    
    statsBody.append('div')
        .attr('class', 'timeline-stats-content')
        .html('<small class="text-muted">Cargando estadísticas...</small>');
    
    // Crear leyenda de ramas
    const legendContainer = mainContainer.append('div')
        .attr('class', 'timeline-legend-container mb-2');
    
    const legend = legendContainer.append('div')
        .attr('class', 'timeline-legend')
        .attr('id', 'timeline-legend');
    
    // Crear SVG principal
    const svgContainer = mainContainer.append('div')
        .attr('class', 'timeline-svg-container')
        .style('overflow', 'hidden')
        .style('border', '1px solid #dee2e6')
        .style('border-radius', '0.375rem')
        .style('background', 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)');
    
    const containerRect = svgContainer.node().getBoundingClientRect();
    const width = Math.max(800, containerRect.width || 800);
    
    timelineSvg = svgContainer.append('svg')
        .attr('class', 'timeline-svg')
        .attr('width', '100%')
        .attr('height', timelineHeight)
        .style('display', 'block');
    
    // Crear definiciones para efectos
    const defs = timelineSvg.append('defs');
    
    // Gradiente para el fondo
    const gradient = defs.append('linearGradient')
        .attr('id', 'timelineGradient')
        .attr('x1', '0%')
        .attr('y1', '0%')
        .attr('x2', '100%')
        .attr('y2', '0%');
    
    gradient.append('stop')
        .attr('offset', '0%')
        .attr('stop-color', '#e3f2fd')
        .attr('stop-opacity', 0.3);
    
    gradient.append('stop')
        .attr('offset', '100%')
        .attr('stop-color', '#f3e5f5')
        .attr('stop-opacity', 0.3);
    
    // Sombras para elementos
    const filter = defs.append('filter')
        .attr('id', 'shadow')
        .attr('x', '-50%')
        .attr('y', '-50%')
        .attr('width', '200%')
        .attr('height', '200%');
    
    filter.append('feDropShadow')
        .attr('dx', 2)
        .attr('dy', 2)
        .attr('stdDeviation', 2)
        .attr('flood-color', '#000000')
        .attr('flood-opacity', 0.3);
    
    // Configurar zoom y pan con verificaciones adicionales
    timelineZoom = d3.zoom()
        .scaleExtent(timelineConfig.zoomExtent)
        .on('zoom', (event) => {
            // Verificación adicional antes de manejar zoom
            if (timelineScale && event && event.transform) {
                handleTimelineZoom(event);
            }
        });
    
    timelineSvg.call(timelineZoom);
    
    // Crear grupo principal
    const mainGroup = timelineSvg.append('g')
        .attr('class', 'timeline-main-group')
        .attr('transform', `translate(${timelineMargin.left}, ${timelineMargin.top})`);
    
    // Fondo con gradiente
    mainGroup.append('rect')
        .attr('class', 'timeline-background')
        .attr('width', '100%')
        .attr('height', timelineHeight - timelineMargin.top - timelineMargin.bottom)
        .attr('fill', 'url(#timelineGradient)')
        .attr('opacity', 0.1);
    
    // Crear tooltip mejorado
    timelineTooltip = d3.select('body').append('div')
        .attr('class', 'timeline-tooltip')
        .style('position', 'absolute')
        .style('background', 'rgba(0, 0, 0, 0.9)')
        .style('color', 'white')
        .style('padding', '10px')
        .style('border-radius', '5px')
        .style('font-size', '12px')
        .style('pointer-events', 'none')
        .style('z-index', 1000)
        .style('display', 'none');
    
    console.log('Timeline avanzado inicializado');
}

/**
 * Renderiza el timeline con los datos de commits
 */
function renderTimeline(commits) {
    // Verificar que D3.js esté disponible
    if (typeof d3 === 'undefined') {
        console.error('D3.js no está disponible. No se puede renderizar el timeline.');
        showEmptyTimeline();
        return;
    }
    
    if (!commits || commits.length === 0) {
        console.warn('No hay commits para renderizar en el timeline');
        showEmptyTimeline();
        return;
    }
    
    // Procesar y limpiar datos
    timelineData = preprocessCommits(commits);
    
    // Actualizar estadísticas
    updateTimelineStats();
    
    // Preparar escalas y dimensiones
    const containerRect = d3.select('#timeline-container .timeline-svg-container').node().getBoundingClientRect();
    const width = Math.max(800, containerRect.width - timelineMargin.left - timelineMargin.right);
    const height = timelineHeight - timelineMargin.top - timelineMargin.bottom;
    
    // Calcular ramas y posiciones
    calculateBranchPositions();
    
    // Configurar escalas
    const dateExtent = d3.extent(timelineData, d => new Date(d.timestamp));
    timelineScale = d3.scaleTime()
        .domain(dateExtent)
        .range([0, width]);
    
    // Limpiar contenido previo del grupo principal
    const mainGroup = timelineSvg.select('.timeline-main-group');
    mainGroup.selectAll('.timeline-content').remove();
    
    // Crear contenedor para el contenido
    const contentGroup = mainGroup.append('g')
        .attr('class', 'timeline-content');
    
    // Dibujar elementos del timeline
    drawTimelineGrid(contentGroup, width, height);
    drawBranchLines(contentGroup, width, height);
    drawTimelineAxis(contentGroup, width, height);
    drawTimelineCommits(contentGroup, height);
    drawTimelineConnections(contentGroup);
    
    // Actualizar leyenda
    updateBranchLegend();
    
    console.log(`Timeline renderizado con ${timelineData.length} commits en ${timelineBranches.size} ramas`);
}

/**
 * Preprocesa los commits para el timeline
 */
function preprocessCommits(commits) {
    return commits
        .map(commit => ({
            ...commit,
            branch: commit.branch || 'main',
            timestamp: new Date(commit.timestamp),
            message: commit.message || 'Sin mensaje',
            author: commit.author || 'Desconocido'
        }))
        .sort((a, b) => a.timestamp - b.timestamp);
}

/**
 * Calcula las posiciones de las ramas en el timeline
 */
function calculateBranchPositions() {
    timelineBranches.clear();
    
    // Agrupar commits por rama
    const branchGroups = d3.group(timelineData, d => d.branch);
    
    let branchIndex = 0;
    branchGroups.forEach((commits, branchName) => {
        if (branchIndex < timelineConfig.maxBranches) {
            timelineBranches.set(branchName, {
                index: branchIndex,
                color: timelineConfig.colors[branchIndex % timelineConfig.colors.length],
                commits: commits,
                yPosition: branchIndex * timelineConfig.branchHeight
            });
            branchIndex++;
        }
    });
}

/**
 * Dibuja la grilla de fondo del timeline
 */
function drawTimelineGrid(container, width, height) {
    const gridGroup = container.append('g')
        .attr('class', 'timeline-grid');
    
    // Líneas verticales (fechas)
    const timeAxisTicks = timelineScale.ticks(10);
    
    gridGroup.selectAll('.timeline-grid-line-vertical')
        .data(timeAxisTicks)
        .enter()
        .append('line')
        .attr('class', 'timeline-grid-line-vertical')
        .attr('x1', d => timelineScale(d))
        .attr('y1', 0)
        .attr('x2', d => timelineScale(d))
        .attr('y2', height)
        .attr('stroke', '#dee2e6')
        .attr('stroke-width', 1)
        .attr('opacity', 0.3);
    
    // Líneas horizontales (ramas)
    gridGroup.selectAll('.timeline-grid-line-horizontal')
        .data(Array.from(timelineBranches.values()))
        .enter()
        .append('line')
        .attr('class', 'timeline-grid-line-horizontal')
        .attr('x1', 0)
        .attr('y1', d => d.yPosition + timelineConfig.branchHeight / 2)
        .attr('x2', width)
        .attr('y2', d => d.yPosition + timelineConfig.branchHeight / 2)
        .attr('stroke', '#dee2e6')
        .attr('stroke-width', 1)
        .attr('opacity', 0.2);
}

/**
 * Dibuja las líneas de las ramas
 */
function drawBranchLines(container, width, height) {
    const branchGroup = container.append('g')
        .attr('class', 'timeline-branches');
    
    timelineBranches.forEach((branch, branchName) => {
        if (branch.commits.length > 0) {
            const branchCommits = branch.commits.sort((a, b) => a.timestamp - b.timestamp);
            const startX = timelineScale(branchCommits[0].timestamp);
            const endX = timelineScale(branchCommits[branchCommits.length - 1].timestamp);
            const y = branch.yPosition + timelineConfig.branchHeight / 2;
            
            branchGroup.append('line')
                .attr('class', `timeline-branch-line branch-${branchName}`)
                .attr('x1', startX)
                .attr('y1', y)
                .attr('x2', endX)
                .attr('y2', y)
                .attr('stroke', branch.color)
                .attr('stroke-width', 3)
                .attr('opacity', 0.6)
                .attr('filter', 'url(#shadow)');
        }
    });
}

/**
 * Dibuja el eje principal del timeline
 */
function drawTimelineAxis(container, width, height) {
    // Crear eje de tiempo
    const timeAxis = d3.axisBottom(timelineScale)
        .tickFormat(d3.timeFormat('%d/%m/%y'))
        .ticks(Math.min(10, Math.floor(width / 100)));
    
    container.append('g')
        .attr('class', 'timeline-axis-labels')
        .attr('transform', `translate(0, ${height + 10})`)
        .call(timeAxis)
        .selectAll('text')
        .style('font-size', '11px')
        .style('fill', '#6c757d');
    
    // Etiquetas de ramas
    const branchLabels = container.append('g')
        .attr('class', 'timeline-branch-labels');
    
    timelineBranches.forEach((branch, branchName) => {
        branchLabels.append('text')
            .attr('class', 'timeline-branch-label')
            .attr('x', -5)
            .attr('y', branch.yPosition + timelineConfig.branchHeight / 2 + 4)
            .attr('text-anchor', 'end')
            .style('font-size', '10px')
            .style('font-weight', 'bold')
            .style('fill', branch.color)
            .text(branchName);
    });
}

/**
 * Dibuja los commits en el timeline
 */
function drawTimelineCommits(container, height) {
    const commitsGroup = container.append('g')
        .attr('class', 'timeline-commits');
    
    const commits = commitsGroup.selectAll('.timeline-commit')
        .data(timelineData)
        .enter()
        .append('g')
        .attr('class', 'timeline-commit')
        .attr('transform', d => {
            const branch = timelineBranches.get(d.branch);
            const x = timelineScale(d.timestamp);
            const y = branch ? branch.yPosition + timelineConfig.branchHeight / 2 : height / 2;
            return `translate(${x}, ${y})`;
        });
    
    // Círculos de commits con efectos
    commits.append('circle')
        .attr('class', d => `timeline-commit-circle ${getCommitType(d)}`)
        .attr('r', d => getCommitRadius(d))
        .attr('fill', d => {
            const branch = timelineBranches.get(d.branch);
            return branch ? branch.color : '#6c757d';
        })
        .attr('stroke', '#ffffff')
        .attr('stroke-width', 2)
        .attr('filter', 'url(#shadow)')
        .style('cursor', 'pointer')
        .on('mouseover', handleCommitMouseOver)
        .on('mouseout', handleCommitMouseOut)
        .on('click', handleCommitClick);
    
    // Etiquetas para commits importantes
    commits.filter(d => isImportantCommit(d))
        .append('text')
        .attr('class', 'timeline-commit-label')
        .attr('x', 0)
        .attr('y', -15)
        .attr('text-anchor', 'middle')
        .style('font-size', '9px')
        .style('font-weight', 'bold')
        .style('fill', '#495057')
        .style('pointer-events', 'none')
        .text(d => getCommitShortMessage(d));
}

/**
 * Determina el tipo de commit para styling
 */
function getCommitType(commit) {
    if (commit.message.toLowerCase().includes('merge')) return 'merge';
    if (commit.message.toLowerCase().includes('initial')) return 'initial';
    if (commit.message.toLowerCase().includes('release')) return 'release';
    if (commit.message.toLowerCase().includes('hotfix')) return 'hotfix';
    return 'normal';
}

/**
 * Calcula el radio del commit basado en su importancia
 */
function getCommitRadius(commit) {
    const type = getCommitType(commit);
    switch (type) {
        case 'merge': return timelineConfig.commitRadius * 1.5;
        case 'release': return timelineConfig.commitRadius * 1.3;
        case 'initial': return timelineConfig.commitRadius * 1.2;
        default: return timelineConfig.commitRadius;
    }
}

/**
 * Determina si un commit es importante para mostrar etiqueta
 */
function isImportantCommit(commit) {
    const type = getCommitType(commit);
    return ['merge', 'release', 'initial'].includes(type);
}

/**
 * Obtiene un mensaje corto para el commit
 */
function getCommitShortMessage(commit) {
    const message = commit.message || '';
    if (message.includes('merge')) return 'Merge';
    if (message.includes('release')) return 'Release';
    if (message.includes('initial')) return 'Initial';
    return message.substring(0, 10) + (message.length > 10 ? '...' : '');
}

/**
 * Dibuja las conexiones entre commits
 */
function drawTimelineConnections(container) {
    const connectionsGroup = container.append('g')
        .attr('class', 'timeline-connections');
    
    // Crear líneas de conexión entre commits consecutivos en la misma rama
    timelineBranches.forEach((branch, branchName) => {
        const branchCommits = branch.commits.sort((a, b) => a.timestamp - b.timestamp);
        
        for (let i = 1; i < branchCommits.length; i++) {
            const prevCommit = branchCommits[i - 1];
            const currentCommit = branchCommits[i];
            
            const x1 = timelineScale(prevCommit.timestamp);
            const x2 = timelineScale(currentCommit.timestamp);
            const y = branch.yPosition + timelineConfig.branchHeight / 2;
            
            connectionsGroup.append('line')
                .attr('class', 'timeline-connection')
                .attr('x1', x1)
                .attr('y1', y)
                .attr('x2', x2)
                .attr('y2', y)
                .attr('stroke', branch.color)
                .attr('stroke-width', 2)
                .attr('opacity', 0.4)
                .style('stroke-dasharray', '3,3');
        }
    });
}

/**
 * Muestra timeline vacío
 */
function showEmptyTimeline() {
    const container = document.getElementById('timeline-container');
    if (container) {
        container.innerHTML = `
            <div class="alert alert-info text-center m-3">
                <i class="fas fa-info-circle"></i> 
                No hay commits para mostrar en el timeline
            </div>
        `;
    }
}

/**
 * Actualiza las estadísticas del timeline
 */
function updateTimelineStats() {
    if (!timelineData || timelineData.length === 0) return;
    
    const statsContainer = d3.select('#timeline-stats .timeline-stats-content');
    
    // Calcular estadísticas
    const totalCommits = timelineData.length;
    const uniqueAuthors = new Set(timelineData.map(d => d.author)).size;
    const dateRange = d3.extent(timelineData, d => d.timestamp);
    const daysDiff = Math.ceil((dateRange[1] - dateRange[0]) / (1000 * 60 * 60 * 24));
    const avgCommitsPerDay = (totalCommits / Math.max(1, daysDiff)).toFixed(1);
    
    // Tipos de commits
    const commitTypes = {
        normal: timelineData.filter(d => getCommitType(d) === 'normal').length,
        merge: timelineData.filter(d => getCommitType(d) === 'merge').length,
        release: timelineData.filter(d => getCommitType(d) === 'release').length,
        hotfix: timelineData.filter(d => getCommitType(d) === 'hotfix').length
    };
    
    const statsHtml = `
        <div class="row g-2 text-center">
            <div class="col-6 col-md-3">
                <div class="border rounded p-2">
                    <strong class="text-primary">${totalCommits}</strong>
                    <div class="small text-muted">Commits</div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="border rounded p-2">
                    <strong class="text-success">${timelineBranches.size}</strong>
                    <div class="small text-muted">Ramas</div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="border rounded p-2">
                    <strong class="text-info">${uniqueAuthors}</strong>
                    <div class="small text-muted">Autores</div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="border rounded p-2">
                    <strong class="text-warning">${avgCommitsPerDay}</strong>
                    <div class="small text-muted">Commits/día</div>
                </div>
            </div>
        </div>
        <div class="row g-1 text-center mt-2">
            <div class="col-3">
                <small class="text-muted">${commitTypes.normal} Normal</small>
            </div>
            <div class="col-3">
                <small class="text-muted">${commitTypes.merge} Merge</small>
            </div>
            <div class="col-3">
                <small class="text-muted">${commitTypes.release} Release</small>
            </div>
            <div class="col-3">
                <small class="text-muted">${commitTypes.hotfix} Hotfix</small>
            </div>
        </div>
    `;
    
    statsContainer.html(statsHtml);
}

/**
 * Actualiza la leyenda de ramas
 */
function updateBranchLegend() {
    const legend = d3.select('#timeline-legend');
    legend.selectAll('*').remove();
    
    if (timelineBranches.size === 0) return;
    
    const legendContainer = legend.append('div')
        .attr('class', 'd-flex flex-wrap align-items-center');
    
    legendContainer.append('small')
        .attr('class', 'text-muted me-3 fw-bold')
        .text('Ramas:');
    
    timelineBranches.forEach((branch, branchName) => {
        const branchItem = legendContainer.append('div')
            .attr('class', 'me-3 d-flex align-items-center');
        
        branchItem.append('div')
            .style('width', '12px')
            .style('height', '12px')
            .style('background-color', branch.color)
            .style('border-radius', '2px')
            .style('margin-right', '5px');
        
        branchItem.append('small')
            .attr('class', 'text-muted')
            .text(`${branchName} (${branch.commits.length})`);
    });
}

/**
 * Maneja el hover sobre commits
 */
function handleCommitMouseOver(event, d) {
    // Resaltar commit
    d3.select(this)
        .transition()
        .duration(200)
        .attr('r', getCommitRadius(d) * 1.3)
        .attr('stroke-width', 3);
    
    // Mostrar tooltip
    const tooltipContent = `
        <div class="fw-bold">${d.message}</div>
        <div class="small mt-1">
            <div><i class="fas fa-user"></i> ${d.author}</div>
            <div><i class="fas fa-code-branch"></i> ${d.branch}</div>
            <div><i class="fas fa-clock"></i> ${d.timestamp.toLocaleString()}</div>
            <div><i class="fas fa-hashtag"></i> ${d.hash ? d.hash.substring(0, 7) : 'N/A'}</div>
        </div>
    `;
    
    timelineTooltip
        .html(tooltipContent)
        .style('display', 'block')
        .style('left', (event.pageX + 10) + 'px')
        .style('top', (event.pageY - 10) + 'px');
    
    // Sincronizar con visualización 3D
    if (typeof highlightCommitIn3D === 'function' && d.hash) {
        highlightCommitIn3D(d.hash);
    }
}

/**
 * Maneja cuando el mouse sale del commit
 */
function handleCommitMouseOut(event, d) {
    // Restaurar commit
    d3.select(this)
        .transition()
        .duration(200)
        .attr('r', getCommitRadius(d))
        .attr('stroke-width', 2);
    
    // Ocultar tooltip
    timelineTooltip.style('display', 'none');
}

/**
 * Maneja el clic en commits
 */
function handleCommitClick(event, d) {
    // Marcar como seleccionado
    d3.selectAll('.timeline-commit-circle').classed('selected', false);
    d3.select(this).classed('selected', true);
    
    selectedCommit = d;
    
    // Mostrar información en panel lateral
    if (typeof showElementInfo === 'function') {
        showElementInfo({
            ...d,
            type: 'commit'
        });
    }
    
    // Sincronizar con visualización 3D
    if (typeof highlightCommitIn3D === 'function' && d.hash) {
        highlightCommitIn3D(d.hash);
    }
    
    console.log('Commit seleccionado:', d);
}

/**
 * Maneja el zoom del timeline
 */
function handleTimelineZoom(event) {
    const { transform } = event;
    
    // Verificar que timelineScale esté inicializado
    if (!timelineScale) {
        console.warn('⚠️ TimelineScale no inicializado, ignorando zoom');
        return;
    }
    
    // Aplicar transformación a los elementos principales
    const timelineContent = d3.select('.timeline-content');
    if (!timelineContent.empty()) {
        timelineContent.attr('transform', transform);
    }
    
    // Actualizar escala de tiempo
    const newScale = transform.rescaleX(timelineScale);
    
    // Actualizar eje
    const timeAxis = d3.axisBottom(newScale)
        .tickFormat(d3.timeFormat('%d/%m/%y'))
        .ticks(Math.min(15, Math.floor(newScale.range()[1] / 80)));
    
    const axisLabels = d3.select('.timeline-axis-labels');
    if (!axisLabels.empty()) {
        axisLabels
            .call(timeAxis)
            .selectAll('text')
            .style('font-size', '11px')
            .style('fill', '#6c757d');
    }
}

/**
 * Resetea el zoom del timeline
 */
function resetTimelineZoom() {
    if (timelineSvg && timelineZoom) {
        timelineSvg.transition()
            .duration(750)
            .call(timelineZoom.transform, d3.zoomIdentity);
    }
}

/**
 * Alterna la agrupación por días
 */
function toggleDayGrouping() {
    const button = d3.select('#timeline-group-toggle');
    const isGrouped = button.classed('active');
    
    button.classed('active', !isGrouped);
    
    if (!isGrouped) {
        button.html('<i class="fas fa-layer-group"></i> Desagrupar');
        // Implementar agrupamiento
        console.log('Agrupamiento activado');
    } else {
        button.html('<i class="fas fa-layer-group"></i> Agrupar');
        // Quitar agrupamiento
        console.log('Agrupamiento desactivado');
    }
}

/**
 * Alterna el filtro de solo commits
 */
function toggleCommitFilter() {
    const button = d3.select('#timeline-filter-toggle');
    const isFiltered = button.classed('active');
    
    button.classed('active', !isFiltered);
    
    if (!isFiltered) {
        button.html('<i class="fas fa-filter"></i> Mostrar Todo');
        // Filtrar solo commits normales
        const filteredData = timelineData.filter(d => getCommitType(d) === 'normal');
        renderFilteredTimeline(filteredData);
    } else {
        button.html('<i class="fas fa-filter"></i> Solo Commits');
        // Mostrar todos los commits
        renderFilteredTimeline(timelineData);
    }
}

/**
 * Renderiza el timeline con datos filtrados
 */
function renderFilteredTimeline(filteredData) {
    // Actualizar commits visibles
    d3.selectAll('.timeline-commit')
        .style('display', d => filteredData.includes(d) ? 'block' : 'none');
    
    console.log(`Timeline filtrado: ${filteredData.length} commits visibles`);
}

/**
 * Exporta los datos del timeline
 */
function exportTimelineData() {
    if (!timelineData || timelineData.length === 0) {
        alert('No hay datos para exportar');
        return;
    }
    
    const exportData = timelineData.map(commit => ({
        hash: commit.hash,
        message: commit.message,
        author: commit.author,
        branch: commit.branch,
        timestamp: commit.timestamp.toISOString(),
        type: getCommitType(commit)
    }));
    
    const dataStr = JSON.stringify(exportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `timeline-commits-${new Date().toISOString().slice(0, 10)}.json`;
    link.click();
    
    URL.revokeObjectURL(url);
    console.log('Datos del timeline exportados');
}

/**
 * Alterna el modo pantalla completa
 */
function toggleFullscreen() {
    const container = document.getElementById('timeline-container');
    
    if (!document.fullscreenElement) {
        if (container.requestFullscreen) {
            container.requestFullscreen();
        } else if (container.webkitRequestFullscreen) {
            container.webkitRequestFullscreen();
        } else if (container.msRequestFullscreen) {
            container.msRequestFullscreen();
        }
    } else {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.webkitExitFullscreen) {
            document.webkitExitFullscreen();
        } else if (document.msExitFullscreen) {
            document.msExitFullscreen();
        }
    }
}

/**
 * Resalta un commit específico en el timeline
 */
function highlightCommitInTimeline(commitHash) {
    if (!commitHash) return;
    
    // Remover highlights previos
    d3.selectAll('.timeline-commit-circle').classed('highlighted', false);
    
    // Buscar y resaltar el commit
    const targetCommit = timelineData.find(d => d.hash === commitHash);
    if (targetCommit) {
        d3.selectAll('.timeline-commit-circle')
            .filter(d => d.hash === commitHash)
            .classed('highlighted', true)
            .transition()
            .duration(500)
            .attr('r', getCommitRadius(targetCommit) * 1.5)
            .transition()
            .duration(500)
            .attr('r', getCommitRadius(targetCommit));
        
        console.log('Commit resaltado en timeline:', commitHash);
    }
}

/**
 * Filtra el timeline por rango de fechas
 */
function filterTimelineByDateRange(startDate, endDate) {
    if (!startDate || !endDate) return;
    
    const filteredData = timelineData.filter(d => {
        return d.timestamp >= startDate && d.timestamp <= endDate;
    });
    
    renderFilteredTimeline(filteredData);
    console.log(`Timeline filtrado por fechas: ${filteredData.length} commits`);
}

/**
 * Inicializa la integración del timeline con otros componentes
 */
function initTimelineIntegration() {
    // Escuchar eventos de cambio de repositorio
    document.addEventListener('repositoryChanged', (event) => {
        const { commits } = event.detail;
        renderTimeline(commits);
    });
    
    // Escuchar eventos de filtros
    document.addEventListener('filtersApplied', (event) => {
        const { filteredCommits } = event.detail;
        renderFilteredTimeline(filteredCommits);
    });
    
    // Escuchar eventos de selección de commits
    document.addEventListener('commitSelected', (event) => {
        const { commitHash } = event.detail;
        highlightCommitInTimeline(commitHash);
    });
    
    console.log('Integración del timeline inicializada');
}

// Inicializar cuando se carga el script
if (typeof d3 !== 'undefined') {
    console.log('Timeline.js cargado y listo');
} else {
    console.warn('D3.js no está disponible - Timeline no funcionará');
}

// Función para integración con el sistema existente
function initTimelineIntegration() {
    console.log('Timeline integration initialized');
}

// Exportar funciones para uso global
window.timelineFunctions = {
    initTimeline,
    renderTimeline,
    highlightCommitInTimeline,
    filterTimelineByDateRange,
    exportTimelineData
}; 