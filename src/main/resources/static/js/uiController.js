/**
 * Controlador de la interfaz de usuario (UI)
 */

document.addEventListener('DOMContentLoaded', () => {
    // Manejar la navegación
    const btnAnalisisTecnologias = document.getElementById('btnAnalisisTecnologias');
    if (btnAnalisisTecnologias) {
        btnAnalisisTecnologias.addEventListener('click', (e) => {
            e.preventDefault();
            window.location.href = '/summary';
        });
    }

    const btnGenerarResumen = document.getElementById('btnGenerarResumen');
    if (btnGenerarResumen) {
        btnGenerarResumen.addEventListener('click', (e) => {
            e.preventDefault();
            // Podríamos redirigir a la misma página de análisis o a una específica
            window.location.href = '/summary';
        });
    }

    // Manejar el envío del formulario de repositorio en la página principal
    const repoForm = document.getElementById('repoForm');
    if (repoForm) {
        repoForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const repoUrl = document.getElementById('repoUrl').value;
            if (repoUrl) {
                // Iniciar la carga de la visualización
                loadCommits(repoUrl);
                // Conectar al stream de eventos
                connectToEventStream(repoUrl);
            }
        });
    }

    // Manejar controles de la visualización
    const btnZoomIn = document.getElementById('btnZoomIn');
    if (btnZoomIn) {
        btnZoomIn.addEventListener('click', () => {
            if (camera && typeof camera.position.z !== 'undefined') {
                camera.position.z *= 0.8;
            }
        });
    }

    const btnZoomOut = document.getElementById('btnZoomOut');
    if (btnZoomOut) {
        btnZoomOut.addEventListener('click', () => {
            if (camera && typeof camera.position.z !== 'undefined') {
                camera.position.z *= 1.2;
            }
        });
    }

    const btnReset = document.getElementById('btnReset');
    if (btnReset) {
        btnReset.addEventListener('click', () => {
            if (camera && controls) {
                camera.position.set(0, 0, 100);
                camera.lookAt(0, 0, 0);
                controls.reset();
            }
        });
    }

    // Manejar el botón de aplicar filtros
    const applyFiltersBtn = document.getElementById('applyFilters');
    if (applyFiltersBtn) {
        applyFiltersBtn.addEventListener('click', () => {
            if (typeof applyFilters === 'function') {
                applyFilters();
            }
        });
    }
}); 