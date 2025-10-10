/**
 * Integración de CV en el Dashboard
 * Maneja la generación y exportación de CVs técnicos
 */

(function() {
    'use strict';
    
    let cvGenerator = null;
    let generatedCV = null;
    
    /**
     * Inicializa la funcionalidad de CV cuando el dashboard está listo
     */
    function initCV() {
        console.log('📄 Inicializando funcionalidad de CV...');
        
        const cvButton = document.getElementById('generate-cv-btn');
        const cvContainer = document.getElementById('cv-action-container');
        
        if (!cvButton) {
            console.warn('⚠️ Botón de CV no encontrado');
            return;
        }
        
        // Mostrar botón solo si hay repos cargados
        const stateManager = window.AppStateManager?.getInstance();
        if (stateManager) {
            stateManager.subscribe('repositories', (repos) => {
                if (repos && repos.length > 0 && cvContainer) {
                    cvContainer.style.display = 'block';
                    console.log('✅ Botón de CV visible -', repos.length, 'repos');
                }
            });
        }
        
        // Event listener para generar CV
        cvButton.addEventListener('click', generateCV);
    }
    
    /**
     * Genera el CV técnico llamando al backend
     */
    async function generateCV() {
        console.log('🎯 Generando CV en backend...');
        
        try {
            // Mostrar loading
            showNotification('🔄 Generando tu CV técnico...', 'info');
            
            // Llamar al backend para generar CV
            const response = await fetch('/api/cv/generate', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                },
                credentials: 'include'
            });
            
            if (!response.ok) {
                throw new Error(`Error del servidor: ${response.status} ${response.statusText}`);
            }
            
            generatedCV = await response.json();
            
            console.log('✅ CV generado por backend:', generatedCV);
            
            // Mostrar opciones de exportación
            showExportOptions(generatedCV);
            showNotification('✅ CV generado exitosamente', 'success');
            
        } catch (error) {
            console.error('❌ Error generando CV:', error);
            showNotification('❌ Error al generar CV: ' + error.message, 'error');
        }
    }
    
    /**
     * Muestra opciones de exportación
     */
    function showExportOptions(cv) {
        const modal = createExportModal(cv);
        document.body.appendChild(modal);
        
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
        
        // Limpiar al cerrar
        modal.addEventListener('hidden.bs.modal', () => {
            modal.remove();
        });
    }
    
    /**
     * Crea el modal de exportación
     */
    function createExportModal(cv) {
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.id = 'cvExportModal';
        modal.tabIndex = -1;
        
        modal.innerHTML = `
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header bg-success text-white">
                        <h5 class="modal-title">
                            <i class="fas fa-check-circle me-2"></i>
                            ¡CV Técnico Generado!
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="cv-summary mb-4">
                            <h6>${cv.header?.name || cv.header?.username || 'Developer'}</h6>
                            <p class="text-muted mb-2">${cv.header?.bio || 'Desarrollador de Software'}</p>
                            <div class="d-flex gap-3 flex-wrap">
                                <span class="badge bg-primary">
                                    <i class="fas fa-project-diagram me-1"></i>
                                    ${cv.metadata?.totalRepositories || cv.summary?.totalProjects || 0} proyectos
                                </span>
                                <span class="badge bg-success">
                                    <i class="fas fa-check-circle me-1"></i>
                                    CV Generado
                                </span>
                            </div>
                        </div>
                        
                        <h6 class="mb-3">Exportar CV como:</h6>
                        <div class="row g-3">
                            <div class="col-md-4">
                                <button class="btn btn-outline-primary w-100" onclick="window.exportCV('markdown')">
                                    <i class="fab fa-markdown fa-2x mb-2"></i>
                                    <div>Markdown</div>
                                    <small class="text-muted">Para GitHub</small>
                                </button>
                            </div>
                            <div class="col-md-4">
                                <button class="btn btn-outline-success w-100" onclick="window.exportCV('json')">
                                    <i class="fas fa-code fa-2x mb-2"></i>
                                    <div>JSON</div>
                                    <small class="text-muted">Para APIs</small>
                                </button>
                            </div>
                            <div class="col-md-4">
                                <button class="btn btn-outline-info w-100" onclick="window.exportCV('html')">
                                    <i class="fas fa-globe fa-2x mb-2"></i>
                                    <div>HTML</div>
                                    <small class="text-muted">Para web</small>
                                </button>
                            </div>
                        </div>
                        
                        <div class="alert alert-info mt-4 mb-0">
                            <i class="fas fa-info-circle me-2"></i>
                            <strong>Nota:</strong> Tu CV se genera instantáneamente sin hacer llamadas adicionales a GitHub.
                            Eficiente y privado.
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                    </div>
                </div>
            </div>
        `;
        
        return modal;
    }
    
    /**
     * Exporta el CV en el formato especificado (llamando al backend)
     */
    window.exportCV = async function(format) {
        if (!generatedCV) {
            showNotification('❌ No hay CV generado', 'error');
            return;
        }
        
        try {
            showNotification(`🔄 Exportando a ${format.toUpperCase()}...`, 'info');
            
            let content = '';
            let filename = '';
            let mimeType = '';
            const username = generatedCV.header?.username || 'developer';
            
            switch (format) {
                case 'markdown':
                    // Llamar al backend para generar Markdown
                    const mdResponse = await fetch('/api/cv/export/markdown', {
                        credentials: 'include'
                    });
                    content = await mdResponse.text();
                    filename = `cv-${username}.md`;
                    mimeType = 'text/markdown';
                    break;
                    
                case 'json':
                    // JSON directo del CV generado
                    content = JSON.stringify(generatedCV, null, 2);
                    filename = `cv-${username}.json`;
                    mimeType = 'application/json';
                    break;
                    
                case 'html':
                    // Llamar al backend para generar HTML
                    const htmlResponse = await fetch('/api/cv/export/html', {
                        credentials: 'include'
                    });
                    content = await htmlResponse.text();
                    filename = `cv-${username}.html`;
                    mimeType = 'text/html';
                    break;
                    
                default:
                    throw new Error('Formato no soportado: ' + format);
            }
            
            // Crear blob y descargar
            const blob = new Blob([content], { type: mimeType });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            
            showNotification(`✅ CV exportado como ${format.toUpperCase()}`, 'success');
            console.log('📄 CV exportado:', filename);
            
        } catch (error) {
            console.error('❌ Error exportando CV:', error);
            showNotification('❌ Error al exportar: ' + error.message, 'error');
        }
    };
    
    /**
     * Muestra una notificación
     */
    function showNotification(message, type = 'info') {
        // Usar el sistema de notificaciones vía event bus
        if (window.EventBus) {
            const eventBus = window.EventBus.getInstance();
            if (eventBus) {
                eventBus.emit('notification:show', { message, type });
                return;
            }
        }
        
        // Fallback: console
        console.log(`[${type.toUpperCase()}] ${message}`);
    }
    
    // Inicializar cuando el DOM esté listo
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCV);
    } else {
        initCV();
    }
    
    console.log('📄 CV Integration cargado');
})();

