/**
 * Timeline de commits para visualización histórica del desarrollo
 */

// Variables globales del timeline
let timelineData = [];
let timelineSvg = null;
let timelineScale = null;
let timelineHeight = 180;
let timelineMargin = { top: 20, right: 20, bottom: 40, left: 20 };
let selectedCommit = null;
let timelineTooltip = null;

// Configuración del timeline
const timelineConfig = {
    commitRadius: 4,
    commitSpacing: 2,
    maxCommitsPerDay: 10,
    animationDuration: 300,
    zoomExtent: [0.1, 10]
};

/**
 * Inicializa el timeline
 */
function initTimeline() {
    const container = d3.select('#timeline-container');
    
    // Limpiar contenido previo
    container.selectAll('*').remove();
    
    // Crear controles del timeline
    const controls = container.append('div')
        .attr('class', 'timeline-controls');
    
    controls.append('button')
        .attr('class', 'timeline-control-btn')
        .attr('id', 'timeline-zoom-reset')
        .text('Reset Zoom')
        .on('click', resetTimelineZoom);
    
    controls.append('button')
        .attr('class', 'timeline-control-btn')
        .attr('id', 'timeline-group-toggle')
        .text('Agrupar por Día')
        .on('click', toggleDayGrouping);
    
    controls.append('button')
        .attr('class', 'timeline-control-btn')
        .attr('id', 'timeline-filter-toggle')
        .text('Solo Commits')
        .on('click', toggleCommitFilter);
    
    // Crear área de estadísticas
    const stats = container.append('div')
        .attr('class', 'timeline-stats')
        .attr('id', 'timeline-stats');
    
    // Crear SVG principal
    const containerRect = container.node().getBoundingClientRect();
    const width = containerRect.width - timelineMargin.left - timelineMargin.right;
    const height = timelineHeight - timelineMargin.top - timelineMargin.bottom;
    
    timelineSvg = container.append('svg')
        .attr('class', 'timeline-svg')
        .attr('width', '100%')
        .attr('height', timelineHeight);
    
    // Crear área de zoom
    const zoomArea = timelineSvg.append('rect')
        .attr('class', 'timeline-zoom-area')
        .attr('width', '100%')
        .attr('height', '100%');
    
    // Configurar zoom
    const zoom = d3.zoom()
        .scaleExtent(timelineConfig.zoomExtent)
        .on('zoom', handleTimelineZoom);
    
    timelineSvg.call(zoom);
    
    // Crear grupo principal
    const mainGroup = timelineSvg.append('g')
        .attr('class', 'timeline-main-group')
        .attr('transform', `translate(${timelineMargin.left}, ${timelineMargin.top})`);
    
    // Crear tooltip
    timelineTooltip = d3.select('body').append('div')
        .attr('class', 'timeline-tooltip')
        .style('display', 'none');
    
    console.log('Timeline inicializado');
}

/**
 * Renderiza el timeline con los datos de commits
 */
function renderTimeline(commits) {
    if (!commits || commits.length === 0) {
        console.warn('No hay commits para renderizar en el timeline');
        return;
    }
    
    timelineData = commits.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    
    // Actualizar estadísticas
    updateTimelineStats();
    
    // Calcular escalas
    const containerRect = d3.select('#timeline-container').node().getBoundingClientRect();
    const width = containerRect.width - timelineMargin.left - timelineMargin.right;
    const height = timelineHeight - timelineMargin.top - timelineMargin.bottom;
    
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
    
    // Dibujar eje principal
    drawTimelineAxis(contentGroup, width, height);
    
    // Dibujar commits
    drawTimelineCommits(contentGroup, height);
    
    // Dibujar conexiones
    drawTimelineConnections(contentGroup);
    
    console.log(`Timeline renderizado con ${timelineData.length} commits`);
}

/**
 * Dibuja el eje principal del timeline
 */
function drawTimelineAxis(container, width, height) {
    const axisY = height / 2;
    
    // Línea principal
    container.append('line')
        .attr('class', 'timeline-axis')
        .attr('x1', 0)
        .attr('y1', axisY)
        .attr('x2', width)
        .attr('y2', axisY);
    
    // Crear eje de tiempo
    const timeAxis = d3.axisBottom(timelineScale)
        .tickFormat(d3.timeFormat('%d/%m'))
        .ticks(Math.min(10, Math.floor(width / 80)));
    
    container.append('g')
        .attr('class', 'timeline-axis-labels')
        .attr('transform', `translate(0, ${axisY + 20})`)
        .call(timeAxis);
}

/**
 * Dibuja los commits en el timeline
 */
function drawTimelineCommits(container, height) {
    const axisY = height / 2;
    
    // Agrupar commits por día para evitar superposición
    const commitsByDay = d3.group(timelineData, d => d3.timeDay(new Date(d.timestamp)));
    
    const commits = container.selectAll('.timeline-commit')
        .data(timelineData)
        .enter()
        .append('g')
        .attr('class', 'timeline-commit')
        .attr('transform', d => {
            const x = timelineScale(new Date(d.timestamp));
            const dayCommits = commitsByDay.get(d3.timeDay(new Date(d.timestamp)));
            const dayIndex = dayCommits.findIndex(commit => commit.hash === d.hash);
            const y = axisY + (dayIndex % 2 === 0 ? -15 : 15) - (Math.floor(dayIndex / 2) * 5);
            return `translate(${x}, ${y})`;
        });
    
    // Círculos de commits
    commits.append('circle')
        .attr('class', d => `timeline-commit-circle ${d.type || 'commit'}`)
        .attr('r', timelineConfig.commitRadius)
        .on('mouseover', handleCommitMouseOver)
        .on('mouseout', handleCommitMouseOut)
        .on('click', handleCommitClick);
    
    // Etiquetas de commits importantes (merges, releases, etc.)
    commits.filter(d => d.message && (
        d.message.toLowerCase().includes('merge') ||
        d.message.toLowerCase().includes('release') ||
        d.message.toLowerCase().includes('version')
    ))
    .append('text')
        .attr('class', 'timeline-commit-label')
        .attr('y', -8)
        .attr('text-anchor', 'middle')
        .style('font-size', '10px')
        .style('fill', '#495057')
        .text(d => d.message.substring(0, 20) + (d.message.length > 20 ? '...' : ''));
}

/**
 * Dibuja las conexiones entre commits
 */
function drawTimelineConnections(container) {
    const axisY = timelineHeight / 2 - timelineMargin.top - timelineMargin.bottom;
    
    // Crear líneas de conexión basadas en el orden temporal
    for (let i = 1; i < timelineData.length; i++) {
        const prevCommit = timelineData[i - 1];
        const currentCommit = timelineData[i];
        
        const x1 = timelineScale(new Date(prevCommit.timestamp));
        const x2 = timelineScale(new Date(currentCommit.timestamp));
        
        container.append('line')
            .attr('class', 'timeline-connection')
            .attr('x1', x1)
            .attr('y1', axisY)
            .attr('x2', x2)
            .attr('y2', axisY)
            .style('opacity', 0)
            .transition()
            .duration(timelineConfig.animationDuration)
            .delay(i * 20)
            .style('opacity', 0.6);
    }
}

/**
 * Maneja el hover sobre commits
 */
function handleCommitMouseOver(event, d) {
    // Resaltar commit
    d3.select(this)
        .transition()
        .duration(200)
        .attr('r', timelineConfig.commitRadius * 1.5);
    
    // Mostrar tooltip
    const tooltipContent = `
        <strong>${d.message || 'Sin mensaje'}</strong><br>
        <strong>Autor:</strong> ${d.author || 'Desconocido'}<br>
        <strong>Fecha:</strong> ${new Date(d.timestamp).toLocaleString()}<br>
        <strong>Hash:</strong> ${d.hash ? d.hash.substring(0, 7) : 'N/A'}
        ${d.stats ? `<br><strong>Archivos:</strong> ${d.stats.filesChanged}` : ''}
    `;
    
    timelineTooltip
        .style('display', 'block')
        .html(tooltipContent)
        .style('left', (event.pageX + 10) + 'px')
        .style('top', (event.pageY + 10) + 'px');
}

/**
 * Maneja cuando el mouse sale del commit
 */
function handleCommitMouseOut(event, d) {
    // Restaurar tamaño original
    d3.select(this)
        .transition()
        .duration(200)
        .attr('r', timelineConfig.commitRadius);
    
    // Ocultar tooltip
    timelineTooltip.style('display', 'none');
}

/**
 * Maneja el clic en commits
 */
function handleCommitClick(event, d) {
    // Deseleccionar commit anterior
    timelineSvg.selectAll('.timeline-commit-circle')
        .classed('selected', false);
    
    // Seleccionar nuevo commit
    d3.select(this).classed('selected', true);
    selectedCommit = d;
    
    // Sincronizar con visualización 3D
    if (typeof highlightCommitIn3D === 'function') {
        highlightCommitIn3D(d.hash);
    }
    
    // Mostrar información detallada
    if (typeof showElementInfo === 'function') {
        showElementInfo(d);
    }
    
    console.log('Commit seleccionado:', d);
}

/**
 * Maneja el zoom del timeline
 */
function handleTimelineZoom(event) {
    const { transform } = event;
    
    // Aplicar transformación al contenido
    timelineSvg.select('.timeline-content')
        .attr('transform', transform);
}

/**
 * Resetea el zoom del timeline
 */
function resetTimelineZoom() {
    timelineSvg.transition()
        .duration(500)
        .call(d3.zoom().transform, d3.zoomIdentity);
}

/**
 * Alterna la agrupación por días
 */
function toggleDayGrouping() {
    const button = d3.select('#timeline-group-toggle');
    const isActive = button.classed('active');
    
    button.classed('active', !isActive);
    button.text(isActive ? 'Agrupar por Día' : 'Vista Linear');
    
    // Re-renderizar con nueva configuración
    if (timelineData.length > 0) {
        renderTimeline(timelineData);
    }
}

/**
 * Alterna el filtro de solo commits
 */
function toggleCommitFilter() {
    const button = d3.select('#timeline-filter-toggle');
    const isActive = button.classed('active');
    
    button.classed('active', !isActive);
    button.text(isActive ? 'Solo Commits' : 'Todos los Eventos');
    
    // Aplicar filtro
    const filteredData = isActive ? 
        timelineData : 
        timelineData.filter(d => !d.type || d.type === 'commit');
    
    renderTimeline(filteredData);
}

/**
 * Actualiza las estadísticas del timeline
 */
function updateTimelineStats() {
    if (!timelineData || timelineData.length === 0) return;
    
    const totalCommits = timelineData.length;
    const uniqueAuthors = new Set(timelineData.map(d => d.author)).size;
    const dateRange = d3.extent(timelineData, d => new Date(d.timestamp));
    const daySpan = Math.ceil((dateRange[1] - dateRange[0]) / (1000 * 60 * 60 * 24));
    
    const statsHtml = `
        <strong>Commits:</strong> ${totalCommits}<br>
        <strong>Autores:</strong> ${uniqueAuthors}<br>
        <strong>Período:</strong> ${daySpan} días
    `;
    
    d3.select('#timeline-stats').html(statsHtml);
}

/**
 * Resalta un commit específico en el timeline
 */
function highlightCommitInTimeline(commitHash) {
    if (!commitHash) return;
    
    // Deseleccionar todos
    timelineSvg.selectAll('.timeline-commit-circle')
        .classed('selected', false);
    
    // Buscar y seleccionar el commit específico
    const targetCommit = timelineData.find(d => d.hash === commitHash);
    if (targetCommit) {
        timelineSvg.selectAll('.timeline-commit-circle')
            .filter(d => d.hash === commitHash)
            .classed('selected', true);
        
        selectedCommit = targetCommit;
    }
}

/**
 * Filtra el timeline por rango de fechas
 */
function filterTimelineByDateRange(startDate, endDate) {
    if (!timelineData || timelineData.length === 0) return;
    
    const filteredData = timelineData.filter(d => {
        const commitDate = new Date(d.timestamp);
        return commitDate >= startDate && commitDate <= endDate;
    });
    
    renderTimeline(filteredData);
}

/**
 * Exporta los datos del timeline
 */
function exportTimelineData() {
    if (!timelineData || timelineData.length === 0) {
        alert('No hay datos para exportar');
        return;
    }
    
    const dataStr = JSON.stringify(timelineData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    
    const link = document.createElement('a');
    link.href = URL.createObjectURL(dataBlob);
    link.download = 'timeline-commits.json';
    link.click();
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