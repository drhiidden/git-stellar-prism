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
    nodeSize: 6,
    nodeColors: {
        commit: 0x28a745,
        merge: 0x007bff,
        branch: 0xffc107,
        tag: 0xdc3545,
        head: 0x6f42c1
    },
    backgroundColor: 0x1a1a1a,
    gridColor: 0x333333,
    connectionColor: 0x666666,
    branchSpacing: 20,
    commitSpacing: 15,
    animationSpeed: 0.002
};

// Variables para git graph
let gitGraph = {
    branches: new Map(),
    commitPositions: new Map(),
    maxBranchLevel: 0
};

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
            
            // Renderizar commits
            renderCommitsVisualization(commits);
            
            // Conectar a eventos en tiempo real si está habilitado
            if (window.REALTIME_ENABLED) {
                connectToEventStream(repoParam);
            }
            
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
        
        // Inicializar timeline si D3 está disponible
        if (typeof initTimeline === 'function' && typeof d3 !== 'undefined') {
            initTimeline();
        } else if (typeof initTimeline === 'function') {
            console.warn('D3.js no disponible, timeline no se inicializará');
        }
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
            
            const timelineContainer = document.getElementById('timeline-container');
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
            const timelineContainer = document.getElementById('timeline-container');
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
    
    // Inicializar timeline
    console.log('🕐 Inicializando timeline...');
    if (typeof initTimeline === 'function') {
        initTimeline();
        console.log('✅ Timeline inicializado');
    } else {
        console.warn('⚠️ Función initTimeline no disponible');
    }
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

    // Material con efectos visuales mejorados
    const material = new THREE.MeshPhongMaterial({
        color: branch.color,
        shininess: 100,
        transparent: false, // Cambiar a false para mejor visibilidad
        opacity: 1.0,      // Opacidad completa
        emissive: new THREE.Color(branch.color).multiplyScalar(0.1) // Añadir emisividad
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
    const material = new THREE.LineBasicMaterial({
        color: branch.color,
        linewidth: 2,
        transparent: true,
        opacity: 0.7
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