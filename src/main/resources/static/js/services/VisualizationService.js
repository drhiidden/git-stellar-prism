/**
 * VisualizationService - Servicio especializado para visualización 3D
 * SOLID: Single Responsibility - Solo maneja visualización 3D
 * DRY: Centraliza lógica de Three.js y git graph
 */

class VisualizationService {
    constructor() {
        this.scene = null;
        this.camera = null;
        this.renderer = null;
        this.controls = null;
        this.nodes = [];
        this.connections = [];
        this.isInitialized = false;
        this.eventBus = EventBus.getInstance();
        
        this.config = {
            nodeSize: 2,
            commitSpacing: 20,
            branchSpacing: 30,
            cameraDistance: 100,
            nodeColors: {
                recent: 0x00ff00,
                normal: 0x0077ff,
                old: 0x888888,
                selected: 0xffff00
            }
        };
    }

    /**
     * Inicializa el sistema 3D
     */
    async initialize(containerId) {
        if (this.isInitialized) return;

        const container = document.getElementById(containerId);
        if (!container) {
            throw new Error(`Container ${containerId} no encontrado`);
        }

        await this.initThreeJS(container);
        this.setupEventListeners();
        this.isInitialized = true;
        
        this.emit('initialized', { containerId });
    }

    /**
     * Inicializa Three.js y componentes básicos
     */
    async initThreeJS(container) {
        const { width, height } = container.getBoundingClientRect();

        // Scene
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x0a0a0a);

        // Camera
        this.camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 10000);
        this.camera.position.set(0, 0, this.config.cameraDistance);

        // Renderer
        this.renderer = new THREE.WebGLRenderer({ antialias: true });
        this.renderer.setSize(width, height);
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        
        container.appendChild(this.renderer.domElement);

        // Controls
        this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = true;
        this.controls.dampingFactor = 0.05;

        // Lighting
        this.setupLighting();
        
        // Start animation loop
        this.animate();
    }

    /**
     * Configura iluminación de la escena
     */
    setupLighting() {
        const ambientLight = new THREE.AmbientLight(0x404040, 0.6);
        this.scene.add(ambientLight);

        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(100, 100, 50);
        directionalLight.castShadow = true;
        this.scene.add(directionalLight);
    }

    /**
     * Renderiza commits como git graph 3D
     */
    renderCommits(commits) {
        this.clearScene();
        
        if (!commits || commits.length === 0) {
            this.emit('render:empty');
            return;
        }

        const gitGraph = this.calculateGitGraphPositions(commits);
        this.createCommitNodes(commits, gitGraph);
        this.createConnections(commits, gitGraph);
        this.adjustCameraToFitScene();
        
        this.emit('render:complete', { 
            commitsCount: commits.length, 
            nodesCount: this.nodes.length 
        });
    }

    /**
     * Calcula posiciones del git graph
     */
    calculateGitGraphPositions(commits) {
        const branches = new Map();
        const positions = new Map();
        let maxBranchLevel = 0;

        commits.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
               .forEach((commit, index) => {
                   const branchName = commit.branch || 'main';
                   
                   if (!branches.has(branchName)) {
                       branches.set(branchName, {
                           level: maxBranchLevel++,
                           color: this.generateBranchColor(branchName)
                       });
                   }
                   
                   const branch = branches.get(branchName);
                   const position = {
                       x: index * this.config.commitSpacing,
                       y: branch.level * this.config.branchSpacing,
                       z: (Math.sin(index * 0.1) * 10) + (branch.level * 5),
                       branchName,
                       branchLevel: branch.level,
                       branchColor: branch.color
                   };
                   
                   positions.set(commit.hash, position);
               });

        return { branches, positions };
    }

    /**
     * Crea nodos de commits 3D
     */
    createCommitNodes(commits, gitGraph) {
        commits.forEach((commit, index) => {
            const position = gitGraph.positions.get(commit.hash);
            if (!position) return;

            const node = this.createCommitNode(commit, position, index);
            this.scene.add(node);
            this.nodes.push(node);
        });
    }

    /**
     * Crea un nodo individual de commit
     */
    createCommitNode(commit, position, index) {
        const age = this.getCommitAge(commit);
        const nodeColor = this.getNodeColorByAge(age);
        const nodeSize = this.getNodeSizeByAge(age);

        const geometry = new THREE.SphereGeometry(nodeSize, 16, 16);
        const material = new THREE.MeshPhongMaterial({ 
            color: nodeColor,
            transparent: true,
            opacity: 0.8
        });

        const node = new THREE.Mesh(geometry, material);
        node.position.set(position.x, position.y, position.z);
        
        // Metadata del commit
        node.userData = {
            ...commit,
            index,
            originalColor: nodeColor,
            branchName: position.branchName,
            branchLevel: position.branchLevel
        };

        return node;
    }

    /**
     * Ajusta la cámara para mostrar toda la escena
     */
    adjustCameraToFitScene() {
        if (this.nodes.length === 0) return;

        const box = new THREE.Box3();
        this.nodes.forEach(node => box.expandByObject(node));

        if (box.isEmpty()) return;

        const center = box.getCenter(new THREE.Vector3());
        const size = box.getSize(new THREE.Vector3());
        const maxDim = Math.max(size.x, size.y, size.z);
        
        const cameraDistance = maxDim * 2;
        this.camera.position.set(
            center.x + cameraDistance * 0.5,
            center.y + cameraDistance * 0.3,
            center.z + cameraDistance
        );
        
        this.controls.target.copy(center);
        this.controls.update();
    }

    /**
     * Resalta un commit específico
     */
    highlightCommit(commitHash) {
        // Restaurar todos los nodos
        this.nodes.forEach(node => {
            node.material.color.setHex(node.userData.originalColor);
            node.material.emissive.setHex(0x000000);
        });

        // Resaltar el nodo seleccionado
        const targetNode = this.nodes.find(node => node.userData.hash === commitHash);
        if (targetNode) {
            targetNode.material.emissive.setHex(0xffffff);
            this.controls.target.copy(targetNode.position);
            this.controls.update();
            
            this.emit('commit:highlighted', { commit: targetNode.userData });
        }
    }

    /**
     * Métodos de utilidad
     */
    getCommitAge(commit) {
        const now = new Date();
        const commitDate = new Date(commit.timestamp);
        return Math.floor((now - commitDate) / (1000 * 60 * 60 * 24)); // días
    }

    getNodeColorByAge(days) {
        if (days < 7) return this.config.nodeColors.recent;
        if (days < 30) return this.config.nodeColors.normal;
        return this.config.nodeColors.old;
    }

    getNodeSizeByAge(days) {
        if (days < 7) return this.config.nodeSize * 1.3;
        if (days < 30) return this.config.nodeSize;
        return this.config.nodeSize * 0.7;
    }

    generateBranchColor(branchName) {
        const colors = [0x28a745, 0x007bff, 0xffc107, 0xdc3545, 0x6f42c1, 0x20c997];
        let hash = 0;
        for (let i = 0; i < branchName.length; i++) {
            hash = branchName.charCodeAt(i) + ((hash << 5) - hash);
        }
        return colors[Math.abs(hash) % colors.length];
    }

    clearScene() {
        this.nodes.forEach(node => this.scene.remove(node));
        this.connections.forEach(connection => this.scene.remove(connection));
        this.nodes = [];
        this.connections = [];
    }

    setupEventListeners() {
        window.addEventListener('resize', () => this.onWindowResize());
        this.renderer.domElement.addEventListener('click', (event) => this.onMouseClick(event));
    }

    onWindowResize() {
        const container = this.renderer.domElement.parentElement;
        const { width, height } = container.getBoundingClientRect();
        
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(width, height);
    }

    onMouseClick(event) {
        const rect = this.renderer.domElement.getBoundingClientRect();
        const mouse = new THREE.Vector2(
            ((event.clientX - rect.left) / rect.width) * 2 - 1,
            -((event.clientY - rect.top) / rect.height) * 2 + 1
        );

        const raycaster = new THREE.Raycaster();
        raycaster.setFromCamera(mouse, this.camera);
        
        const intersects = raycaster.intersectObjects(this.nodes);
        if (intersects.length > 0) {
            const clickedNode = intersects[0].object;
            this.emit('commit:clicked', { commit: clickedNode.userData });
        }
    }

    animate() {
        requestAnimationFrame(() => this.animate());
        
        if (this.controls) {
            this.controls.update();
        }
        
        if (this.renderer && this.scene && this.camera) {
            this.renderer.render(this.scene, this.camera);
        }
    }

    emit(eventName, data) {
        this.eventBus.emit(`visualization:${eventName}`, data);
    }

    /**
     * Factory method para crear instancia singleton
     */
    static getInstance() {
        if (!VisualizationService.instance) {
            VisualizationService.instance = new VisualizationService();
        }
        return VisualizationService.instance;
    }
}

// Registrar en el sistema global
window.VisualizationService = VisualizationService; 