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
                        
                        <h6 class="mb-3">Opciones de Exportación & IA:</h6>
                        <div class="row g-3">
                            <div class="col-6 col-md-3">
                                <button class="btn btn-outline-primary w-100 h-100 py-3" onclick="window.exportCV('markdown')">
                                    <i class="fab fa-markdown fa-2x mb-2"></i>
                                    <div class="fw-bold">Markdown</div>
                                    <small class="text-muted d-block">GitHub Profile</small>
                                </button>
                            </div>
                            <div class="col-6 col-md-3">
                                <button class="btn btn-outline-success w-100 h-100 py-3" onclick="window.exportCV('json')">
                                    <i class="fas fa-code fa-2x mb-2"></i>
                                    <div class="fw-bold">JSON</div>
                                    <small class="text-muted d-block">API Data</small>
                                </button>
                            </div>
                            <div class="col-6 col-md-3">
                                <button class="btn btn-outline-info w-100 h-100 py-3" onclick="window.exportCV('html')">
                                    <i class="fas fa-globe fa-2x mb-2"></i>
                                    <div class="fw-bold">HTML</div>
                                    <small class="text-muted d-block">Web Portfolio</small>
                                </button>
                            </div>
                            <div class="col-6 col-md-3">
                                <button class="btn btn-outline-warning w-100 h-100 py-3" onclick="window.copyAIPrompt()">
                                    <i class="fas fa-magic fa-2x mb-2"></i>
                                    <div class="fw-bold">IA Prompt</div>
                                    <small class="text-muted d-block">Para ChatGPT</small>
                                </button>
                            </div>
                        </div>
                        
                        <div class="mt-3 px-2">
                            <div class="d-flex gap-3 justify-content-center">
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="checkIncludeUrl" checked>
                                    <label class="form-check-label small text-muted" for="checkIncludeUrl">Incluir URLs</label>
                                </div>
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="checkShowDate">
                                    <label class="form-check-label small text-muted" for="checkShowDate">Fecha Creación</label>
                                </div>
                            </div>
                        </div>
                        
                        <div class="alert alert-light border mt-4 mb-0">
                            <div class="d-flex">
                                <i class="fas fa-robot text-warning mt-1 me-3"></i>
                                <div>
                                    <strong>Tip:</strong> Usa el botón "IA Prompt" para copiar un resumen estructurado de tu perfil. 
                                    Pégalo en ChatGPT o Claude para obtener un resumen ejecutivo profesional redactado por IA.
                                </div>
                            </div>
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
                    // Leer opciones
                    const includeUrl = document.getElementById('checkIncludeUrl')?.checked ?? true;
                    const showDate = document.getElementById('checkShowDate')?.checked ?? false;
                    
                    // Llamar al backend para generar Markdown
                    const mdResponse = await fetch(`/api/cv/export/markdown?includeUrl=${includeUrl}&showFirstCommitDate=${showDate}`, {
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
     * Copia el prompt optimizado para IA al portapapeles
     */
    window.copyAIPrompt = function() {
        if (!generatedCV || !generatedCV.aiPrompt) {
            showNotification('⚠️ El prompt de IA no está disponible en este CV.', 'warning');
            return;
        }
        
        navigator.clipboard.writeText(generatedCV.aiPrompt).then(() => {
            showNotification('✨ Prompt copiado! Pégalo en ChatGPT/Claude para obtener tu resumen.', 'success');
        }).catch(err => {
            console.error('Error copiando al portapapeles:', err);
            showNotification('❌ Error al copiar. Revisa permisos del navegador.', 'error');
        });
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

