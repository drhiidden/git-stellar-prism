/**
 * ================================
 * GITSTELLARPRISM - UI CONTROLLER
 * ================================
 * 
 * Controlador moderno de la interfaz de usuario con:
 * - Gestión de estados mejorada
 * - Animaciones suaves
 * - Feedback visual profesional
 * - Responsividad completa
 */

class UIController {
    constructor() {
        this.currentRepo = null;
        this.isLoading = false;
        this.isConnected = false;
        
        // Referencias a elementos DOM
        this.elements = {
            repoForm: document.getElementById('repoForm'),
            repoUrl: document.getElementById('repoUrl'),
            loadingIndicator: document.getElementById('loading-indicator'),
            emptyState: document.getElementById('empty-state'),
            filtersSection: document.getElementById('filters-section'),
            commitInfo: document.getElementById('commit-info'),
            realtimeStatus: document.getElementById('realtime-status'),
            notificationToast: document.getElementById('notification-toast')
        };
        
        this.init();
    }
    
    /**
     * Inicialización del controlador
     */
    init() {
        // Verificar y reinicializar elementos DOM después de que todo esté cargado
        setTimeout(() => {
            this.reinitializeElements();
        }, 100);
        
        this.setupEventListeners();
        this.setupKeyboardShortcuts();
        this.checkInitialStatus();
        
        // Animación de entrada
        this.animateInitialLoad();
        
        console.log('🎮 UIController inicializado correctamente');
    }
    
    /**
     * Reinicializar referencias a elementos DOM para asegurar que existan
     */
    reinitializeElements() {
        const elementChecks = {
            loadingIndicator: 'loading-indicator',
            emptyState: 'empty-state',
            filtersSection: 'filters-section',
            commitInfo: 'commit-info',
            realtimeStatus: 'realtime-status'
        };
        
        let elementsFound = 0;
        let totalElements = Object.keys(elementChecks).length;
        
        for (const [key, id] of Object.entries(elementChecks)) {
            const element = document.getElementById(id);
            if (element) {
                this.elements[key] = element;
                elementsFound++;
            } else {
                console.warn(`⚠️ Elemento ${id} no encontrado en el DOM`);
            }
        }
        
        console.log(`🔍 Elementos DOM verificados: ${elementsFound}/${totalElements} encontrados`);
        
        // Elementos específicos para vistas alternas
        const timelineContainerFull = document.getElementById('timeline-container-full');
        if (timelineContainerFull) {
            console.log('✅ Sistema de vistas alternas detectado');
        }
        
        const view3d = document.getElementById('view-3d');
        const viewTimeline = document.getElementById('view-timeline');
        if (view3d && viewTimeline) {
            console.log('✅ Vistas 3D y Timeline detectadas correctamente');
        }
    }
    
    /**
     * Configurar event listeners
     */
    setupEventListeners() {
        // Formulario de repositorio
        if (this.elements.repoForm) {
            this.elements.repoForm.addEventListener('submit', (e) => this.handleRepoSubmit(e));
        }
        
        // Controles de zoom - sidebar
        this.setupButton('btnZoomIn', () => this.handleZoom('in'));
        this.setupButton('btnZoomOut', () => this.handleZoom('out'));
        this.setupButton('btnReset', () => this.handleReset());
        
        // Controles de zoom flotantes
        this.setupButton('btnZoomInFloat', () => this.handleZoom('in'));
        this.setupButton('btnZoomOutFloat', () => this.handleZoom('out'));
        this.setupButton('btnResetFloat', () => this.handleReset());
        
        // Filtros
        this.setupButton('applyFilters', () => this.handleFilters());
        this.setupButton('clearFilters', () => this.handleClearFilters());
        
        // Navegación
        this.setupButton('btnAnalisisTecnologias', () => this.navigateToAnalysis());
        this.setupButton('btnGenerarResumen', () => this.navigateToSummary());
        
        // Timeline
        this.setupButton('timeline-export', () => this.handleTimelineExport());
        this.setupButton('timeline-fullscreen', () => this.handleTimelineFullscreen());
        
        // Auto-sugerencias en el input
        if (this.elements.repoUrl) {
            this.elements.repoUrl.addEventListener('input', (e) => this.handleRepoInput(e));
            this.elements.repoUrl.addEventListener('focus', () => this.handleRepoFocus());
        }
    }
    
    /**
     * Configurar botón con manejo de errores
     */
    setupButton(id, handler) {
        const btn = document.getElementById(id);
        if (btn) {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                try {
                    handler();
                } catch (error) {
                    console.error(`Error en ${id}:`, error);
                    this.showNotification('Error en la operación', 'error');
                }
            });
        }
    }
    
    /**
     * Configurar atajos de teclado
     */
    setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey || e.metaKey) {
                switch (e.key) {
                    case 'k':
                        e.preventDefault();
                        this.focusSearchInput();
                        break;
                    case 'r':
                        e.preventDefault();
                        this.handleReset();
                        break;
                    case 'Enter':
                        if (e.target === this.elements.repoUrl) {
                            e.preventDefault();
                            this.elements.repoForm.dispatchEvent(new Event('submit'));
                        }
                        break;
                }
            }
            
            // Escape para cerrar modales
            if (e.key === 'Escape') {
                const modals = document.querySelectorAll('.modal.show');
                modals.forEach(modal => {
                    const bsModal = bootstrap.Modal.getInstance(modal);
                    if (bsModal) bsModal.hide();
                });
            }
        });
    }
    
    /**
     * Verificar estado inicial
     */
    checkInitialStatus() {
        // Verificar autenticación
        const userElement = document.querySelector('[sec\\:authentication="principal.attributes[\'login\']"]');
        const isAuthenticated = userElement && userElement.textContent.trim() !== 'Usuario';
        
        if (isAuthenticated) {
            this.showNotification('¡Sesión activa! Puedes explorar repositorios privados.', 'success');
        }
        
        // Verificar conexión tiempo real
        setTimeout(() => this.updateRealtimeStatus(true), 2000);
    }
    
    /**
     * Animación de carga inicial
     */
    animateInitialLoad() {
        const elementsToAnimate = document.querySelectorAll('.fade-in');
        elementsToAnimate.forEach((el, index) => {
            el.style.opacity = '0';
            el.style.transform = 'translateY(20px)';
            
            setTimeout(() => {
                el.style.transition = 'all 0.5s ease';
                el.style.opacity = '1';
                el.style.transform = 'translateY(0)';
            }, index * 100);
        });
    }
    
    /**
     * Manejar envío del formulario de repositorio
     */
    async handleRepoSubmit(e) {
        e.preventDefault();
        
        const repoUrl = this.elements.repoUrl.value.trim();
        if (!repoUrl) {
            this.showNotification('Por favor ingresa un repositorio', 'warning');
            return;
        }
        
        // Validar formato
        const repoPattern = /^[\w.-]+\/[\w.-]+$/;
        if (!repoPattern.test(repoUrl)) {
            this.showNotification('Formato inválido. Use: owner/repository', 'error');
            return;
        }
        
        this.currentRepo = repoUrl;
        this.showLoading(true);
        
        try {
            await this.loadRepositoryData(repoUrl);
            this.showNotification(`Repositorio ${repoUrl} cargado exitosamente`, 'success');
        } catch (error) {
            console.error('Error cargando repositorio:', error);
            this.showNotification('Error al cargar el repositorio', 'error');
            this.showLoading(false);
        }
    }
    
    /**
     * Cargar datos del repositorio
     */
    async loadRepositoryData(repoUrl) {
        try {
            // Llamar a la función de visualización si existe
            if (typeof loadCommits === 'function') {
                await loadCommits(repoUrl);
            }
            
            // Conectar al stream de eventos si está habilitado
            if (window.REALTIME_ENABLED && typeof connectToEventStream === 'function') {
                connectToEventStream(repoUrl);
            }
            
            this.showLoading(false);
            this.showVisualizationUI(true);
            
        } catch (error) {
            throw error;
        }
    }
    
    /**
     * Mostrar/ocultar estado de carga
     */
    showLoading(show = true) {
        this.isLoading = show;
        
        const elements = [
            this.elements.loadingIndicator,
            this.elements.emptyState,
            this.elements.timelineEmptyState
        ];
        
        if (show) {
            // Verificación de seguridad para cada elemento
            if (this.elements.loadingIndicator) {
                this.elements.loadingIndicator.style.display = 'block';
            } else {
                console.warn('⚠️ Elemento loading-indicator no encontrado');
            }
            
            if (this.elements.emptyState) {
                this.elements.emptyState.style.display = 'none';
            }
            
            if (this.elements.timelineEmptyState) {
                this.elements.timelineEmptyState.style.display = 'none';
            }
            
            this.showVisualizationUI(false);
        } else {
            if (this.elements.loadingIndicator) {
                this.elements.loadingIndicator.style.display = 'none';
            }
        }
        
        console.log(`🔄 Loading state: ${show ? 'ON' : 'OFF'}`);
    }
    
    /**
     * Mostrar/ocultar controles de visualización
     */
    showVisualizationUI(show = true) {
        const elementsToShow = [
            this.elements.visualizationControls,
            this.elements.filtersSection,
            this.elements.infoPanel
        ];
        
        elementsToShow.forEach(el => {
            if (el) {
                el.style.display = show ? 'block' : 'none';
                if (show) {
                    el.classList.add('fade-in');
                }
            }
        });
        
        // Ocultar estados vacíos si se muestra la UI
        if (show) {
            if (this.elements.emptyState) {
                this.elements.emptyState.style.display = 'none';
            }
            if (this.elements.timelineEmptyState) {
                this.elements.timelineEmptyState.style.display = 'none';
            }
        }
    }
    
    /**
     * Manejar zoom
     */
    handleZoom(direction) {
        if (!window.camera) {
            this.showNotification('Primero carga un repositorio', 'warning');
            return;
        }
        
        const factor = direction === 'in' ? 0.8 : 1.2;
        window.camera.position.z *= factor;
        
        this.showNotification(`Zoom ${direction === 'in' ? 'aumentado' : 'reducido'}`, 'info');
    }
    
    /**
     * Resetear vista
     */
    handleReset() {
        if (!window.camera || !window.controls) {
            this.showNotification('Primero carga un repositorio', 'warning');
            return;
        }
        
        window.camera.position.set(0, 0, 100);
        window.camera.lookAt(0, 0, 0);
        window.controls.reset();
        
        this.showNotification('Vista restablecida', 'info');
    }
    
    /**
     * Aplicar filtros
     */
    handleFilters() {
        const dateRange = document.getElementById('dateRange')?.value;
        const authorFilter = document.getElementById('authorFilter')?.value;
        
        if (typeof applyFilters === 'function') {
            applyFilters(dateRange, authorFilter);
            this.showNotification('Filtros aplicados', 'success');
        } else {
            this.showNotification('Función de filtros no disponible', 'warning');
        }
    }
    
    /**
     * Limpiar filtros
     */
    handleClearFilters() {
        const dateRange = document.getElementById('dateRange');
        const authorFilter = document.getElementById('authorFilter');
        
        if (dateRange) dateRange.value = 'all';
        if (authorFilter) authorFilter.value = '';
        
        this.handleFilters();
        this.showNotification('Filtros limpiados', 'info');
    }
    
    /**
     * Navegar a análisis
     */
    navigateToAnalysis() {
        if (!this.currentRepo) {
            this.showNotification('Primero carga un repositorio', 'warning');
            return;
        }
        
        window.location.href = '/summary';
    }
    
    /**
     * Navegar a resumen
     */
    navigateToSummary() {
        if (!this.currentRepo) {
            this.showNotification('Primero carga un repositorio', 'warning');
            return;
        }
        
        window.location.href = '/summary';
    }
    
    /**
     * Exportar timeline
     */
    handleTimelineExport() {
        if (!this.currentRepo) {
            this.showNotification('Primero carga un repositorio', 'warning');
            return;
        }
        
        // Implementar exportación
        this.showNotification('Funcionalidad de exportación próximamente', 'info');
    }
    
    /**
     * Pantalla completa del timeline
     */
    handleTimelineFullscreen() {
        const timelineContainer = document.getElementById('timeline-container');
        if (timelineContainer) {
            if (timelineContainer.requestFullscreen) {
                timelineContainer.requestFullscreen();
            } else if (timelineContainer.webkitRequestFullscreen) {
                timelineContainer.webkitRequestFullscreen();
            } else if (timelineContainer.msRequestFullscreen) {
                timelineContainer.msRequestFullscreen();
            }
        }
    }
    
    /**
     * Manejar input del repositorio (auto-sugerencias)
     */
    handleRepoInput(e) {
        const value = e.target.value;
        
        // Validación en tiempo real
        const isValid = /^[\w.-]*\/[\w.-]*$/.test(value) || value === '';
        
        if (isValid) {
            e.target.classList.remove('is-invalid');
            e.target.classList.add('is-valid');
        } else {
            e.target.classList.remove('is-valid');
            e.target.classList.add('is-invalid');
        }
    }
    
    /**
     * Manejar foco en input del repositorio
     */
    handleRepoFocus() {
        // Mostrar sugerencias populares
        this.showNotification('Ejemplos: microsoft/vscode, facebook/react, google/tensorflow', 'info');
    }
    
    /**
     * Enfocar input de búsqueda
     */
    focusSearchInput() {
        if (this.elements.repoUrl) {
            this.elements.repoUrl.focus();
            this.elements.repoUrl.select();
        }
    }
    
    /**
     * Actualizar estado de conexión tiempo real
     */
    updateRealtimeStatus(connected) {
        this.isConnected = connected;
        
        if (this.elements.realtimeStatus) {
            if (connected) {
                this.elements.realtimeStatus.className = 'status-indicator status-success';
                this.elements.realtimeStatus.innerHTML = '<i class="fas fa-circle"></i><span>Conectado</span>';
            } else {
                this.elements.realtimeStatus.className = 'status-indicator status-error';
                this.elements.realtimeStatus.innerHTML = '<i class="fas fa-circle"></i><span>Desconectado</span>';
            }
        }
    }
    
    /**
     * Mostrar notificación toast
     */
    showNotification(message, type = 'info') {
        if (!this.elements.notificationToast) return;
        
        const toast = this.elements.notificationToast;
        const toastBody = toast.querySelector('.toast-body');
        const toastIcon = toast.querySelector('.toast-header i');
        
        // Configurar ícono según tipo
        const icons = {
            success: 'fas fa-check-circle text-success',
            error: 'fas fa-exclamation-circle text-danger',
            warning: 'fas fa-exclamation-triangle text-warning',
            info: 'fas fa-info-circle text-primary'
        };
        
        if (toastIcon) {
            toastIcon.className = icons[type] || icons.info;
        }
        
        if (toastBody) {
            toastBody.textContent = message;
        }
        
        const bsToast = new bootstrap.Toast(toast, {
            autohide: true,
            delay: type === 'error' ? 5000 : 3000
        });
        
        bsToast.show();
    }
    
    /**
     * Actualizar información del commit seleccionado
     */
    updateCommitInfo(commitData) {
        if (!this.elements.commitInfo) return;
        
        const html = `
            <div class="commit-details">
                <div class="d-flex align-items-center mb-3">
                    <div class="avatar me-3">
                        <img src="${commitData.authorAvatar || '/api/placeholder/40/40'}" 
                             alt="${commitData.author}" 
                             class="rounded-circle" 
                             width="40" height="40">
                    </div>
                    <div>
                        <h6 class="mb-0">${commitData.author}</h6>
                        <small class="text-muted">${new Date(commitData.date).toLocaleString()}</small>
                    </div>
                </div>
                
                <div class="commit-message mb-3">
                    <strong>${commitData.message}</strong>
                </div>
                
                <div class="commit-stats">
                    <div class="row g-2">
                        <div class="col-4">
                            <div class="stat-item text-center">
                                <div class="text-success fw-bold">+${commitData.additions || 0}</div>
                                <small class="text-muted">Agregado</small>
                            </div>
                        </div>
                        <div class="col-4">
                            <div class="stat-item text-center">
                                <div class="text-danger fw-bold">-${commitData.deletions || 0}</div>
                                <small class="text-muted">Eliminado</small>
                            </div>
                        </div>
                        <div class="col-4">
                            <div class="stat-item text-center">
                                <div class="text-primary fw-bold">${commitData.changedFiles || 0}</div>
                                <small class="text-muted">Archivos</small>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="mt-3">
                    <small class="text-muted">SHA: <code>${commitData.sha?.substring(0, 8) || 'N/A'}</code></small>
                </div>
            </div>
        `;
        
        this.elements.commitInfo.innerHTML = html;
    }
    
    /**
     * Limpiar información del commit
     */
    clearCommitInfo() {
        if (this.elements.commitInfo) {
            this.elements.commitInfo.innerHTML = `
                <div class="text-center text-muted">
                    <i class="fas fa-mouse-pointer fa-2x mb-3 opacity-50"></i>
                    <p>Haz clic en un elemento de la visualización para ver sus detalles</p>
                </div>
            `;
        }
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    window.uiController = new UIController();
    
    // Exponer funciones globales para compatibilidad
    window.showNotification = (message, type) => window.uiController.showNotification(message, type);
    window.updateRealtimeStatus = (connected) => window.uiController.updateRealtimeStatus(connected);
    window.showLoading = (show) => window.uiController.showLoading(show);
    window.updateCommitInfo = (data) => window.uiController.updateCommitInfo(data);
    window.clearCommitInfo = () => window.uiController.clearCommitInfo();
    
    console.log('🚀 GitStellarPrism UI Controller iniciado correctamente');
}); 