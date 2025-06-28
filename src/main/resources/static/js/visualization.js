/**
 * Visualización de repositorios GitHub con Three.js
 */

// Variables globales
let scene, camera, renderer;
let commits = [];
let nodes = [];
let controls;
let raycaster, mouse;
let infoTooltip;

// Configuración
const config = {
    nodeSize: 5,
    nodeColors: {
        commit: 0x28a745,
        pr: 0x007bff,
        issue: 0xdc3545
    },
    backgroundColor: 0x000000,
    orbitSpeed: 0.001
};

/**
 * Inicializa la visualización 3D
 */
function initVisualization() {
    const container = document.getElementById('visualization-container');
    const width = container.clientWidth;
    const height = container.clientHeight;

    // Crear escena
    scene = new THREE.Scene();
    scene.background = new THREE.Color(config.backgroundColor);

    // Crear cámara
    camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000);
    camera.position.z = 100;

    // Crear renderer
    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(width, height);
    container.appendChild(renderer.domElement);

    // Añadir luces
    const ambientLight = new THREE.AmbientLight(0x404040);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 1);
    directionalLight.position.set(1, 1, 1).normalize();
    scene.add(directionalLight);

    // Controles de órbita
    controls = new THREE.OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;

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

    // Iniciar animación
    animate();
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
        } else {
            infoTooltip.style.display = 'none';
        }
    } else {
        infoTooltip.style.display = 'none';
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
 * Carga los datos de commits desde el servidor
 */
function loadCommits(repoUrl) {
    // Mostrar indicador de carga
    document.getElementById('loadingIndicator').classList.remove('d-none');
    
    // Limpiar escena
    while(scene.children.length > 0) { 
        scene.remove(scene.children[0]); 
    }
    
    // Añadir luces nuevamente
    const ambientLight = new THREE.AmbientLight(0x404040);
    scene.add(ambientLight);
    
    const directionalLight = new THREE.DirectionalLight(0xffffff, 1);
    directionalLight.position.set(1, 1, 1).normalize();
    scene.add(directionalLight);
    
    // Realizar petición al servidor
    fetch(`/api/repository/commits?repo=${encodeURIComponent(repoUrl)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar los commits');
            }
            return response.json();
        })
        .then(data => {
            commits = data;
            renderCommits(commits);
            document.getElementById('loadingIndicator').classList.add('d-none');
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('loadingIndicator').classList.add('d-none');
            alert('Error al cargar los datos del repositorio');
        });
}

/**
 * Renderiza los commits en la visualización
 */
function renderCommits(commits) {
    nodes = [];
    
    // Crear nodos para cada commit
    for (const commit of commits) {
        const node = createCommitNode(commit);
        scene.add(node);
        nodes.push(node);
    }
    
    // Crear conexiones entre nodos
    createConnections();
    
    // Actualizar filtros
    updateFilters(commits);
    
    // Renderizar timeline
    if (typeof renderTimeline === 'function') {
        renderTimeline(commits);
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
    
    // Actualizar controles
    controls.update();
    
    // Rotar ligeramente la escena para efecto de movimiento
    scene.rotation.y += config.orbitSpeed;
    
    // Renderizar escena
    renderer.render(scene, camera);
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
    const targetNode = nodes.find(node => node.userData.hash === commitHash);
    if (targetNode) {
        targetNode.material.emissive.setHex(0x444444);
        targetNode.scale.set(1.5, 1.5, 1.5);
        
        // Mover la cámara hacia el commit
        const targetPosition = targetNode.position.clone();
        camera.position.copy(targetPosition);
        camera.position.z += 50;
        camera.lookAt(targetPosition);
    }
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
    
    // Inicializar visualización
    initVisualization();
    
    // Inicializar timeline
    if (typeof initTimeline === 'function') {
        initTimeline();
    }
    
    // Manejar envío del formulario
    const repoForm = document.getElementById('repoForm');
    repoForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const repoUrl = document.getElementById('repoUrl').value;
        loadCommits(repoUrl);
    });
    
    // Manejar botones de zoom
    document.getElementById('btnZoomIn').addEventListener('click', () => {
        camera.position.z *= 0.8;
    });
    
    document.getElementById('btnZoomOut').addEventListener('click', () => {
        camera.position.z *= 1.2;
    });
    
    document.getElementById('btnReset').addEventListener('click', () => {
        camera.position.set(0, 0, 100);
        camera.lookAt(0, 0, 0);
        controls.reset();
    });
    
    // Manejar botones de filtros
    document.getElementById('applyFilters').addEventListener('click', applyFilters);
    document.getElementById('clearFilters').addEventListener('click', clearFilters);
    
    // Manejar botones del timeline
    document.getElementById('timeline-export').addEventListener('click', () => {
        if (typeof exportTimelineData === 'function') {
            exportTimelineData();
        }
    });
    
    document.getElementById('timeline-fullscreen').addEventListener('click', () => {
        const timelineContainer = document.getElementById('timeline-container');
        if (timelineContainer.requestFullscreen) {
            timelineContainer.requestFullscreen();
        } else if (timelineContainer.webkitRequestFullscreen) {
            timelineContainer.webkitRequestFullscreen();
        } else if (timelineContainer.msRequestFullscreen) {
            timelineContainer.msRequestFullscreen();
        }
    });
}); 