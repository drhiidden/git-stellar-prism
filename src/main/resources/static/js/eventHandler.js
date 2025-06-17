/**
 * Manejo de eventos en tiempo real y comunicación con el servidor
 */

// Variables globales
let eventSource = null;

/**
 * Conecta al stream de eventos del servidor para un repositorio específico
 */
function connectToEventStream(repoUrl) {
    // Si ya hay una conexión, la cerramos
    if (eventSource) {
        eventSource.close();
    }

    // Creamos una nueva conexión Server-Sent Events (SSE)
    const url = `/api/stream/events?repo=${encodeURIComponent(repoUrl)}`;
    eventSource = new EventSource(url);

    // Manejador para cuando se abre la conexión
    eventSource.onopen = () => {
        console.log('Conexión SSE establecida con el servidor.');
        showToast('Conectado al repositorio', 'success');
    };

    // Manejador para errores de conexión
    eventSource.onerror = (error) => {
        console.error('Error en la conexión SSE:', error);
        showToast('Error de conexión', 'danger');
        eventSource.close();
    };

    // Escuchar eventos de tipo 'new-commit'
    eventSource.addEventListener('new-commit', (event) => {
        const commitData = JSON.parse(event.data);
        console.log('Nuevo commit recibido:', commitData);
        
        // Añadir el nuevo commit a la visualización
        addNewCommitNode(commitData);
        
        // Mostrar notificación
        showToast(`Nuevo commit de ${commitData.author}`, 'info');
    });

    // Escuchar eventos de tipo 'new-pr'
    eventSource.addEventListener('new-pr', (event) => {
        const prData = JSON.parse(event.data);
        console.log('Nuevo Pull Request recibido:', prData);
        
        // Añadir el nuevo PR a la visualización
        addNewPrNode(prData);
        
        // Mostrar notificación
        showToast(`Nuevo PR: ${prData.title}`, 'info');
    });

    // Escuchar eventos de tipo 'new-issue'
    eventSource.addEventListener('new-issue', (event) => {
        const issueData = JSON.parse(event.data);
        console.log('Nuevo Issue recibido:', issueData);
        
        // Añadir el nuevo Issue a la visualización
        addNewIssueNode(issueData);
        
        // Mostrar notificación
        showToast(`Nuevo Issue: ${issueData.title}`, 'info');
    });
    
    // Escuchar eventos de actualización general
    eventSource.addEventListener('update', (event) => {
        const updateData = JSON.parse(event.data);
        console.log('Actualización recibida:', updateData);
        
        // Aquí se podría manejar una recarga completa o parcial de datos
        loadCommits(repoUrl);
        showToast('Datos del repositorio actualizados', 'success');
    });
}

/**
 * Muestra un toast (notificación) en la pantalla
 */
function showToast(message, type = 'info') {
    const toastContainer = document.createElement('div');
    toastContainer.className = `toast align-items-center text-white bg-${type} border-0`;
    toastContainer.setAttribute('role', 'alert');
    toastContainer.setAttribute('aria-live', 'assertive');
    toastContainer.setAttribute('aria-atomic', 'true');
    
    const dFlex = document.createElement('div');
    dFlex.className = 'd-flex';
    
    const toastBody = document.createElement('div');
    toastBody.className = 'toast-body';
    toastBody.textContent = message;
    
    const closeButton = document.createElement('button');
    closeButton.className = 'btn-close btn-close-white me-2 m-auto';
    closeButton.setAttribute('type', 'button');
    closeButton.setAttribute('data-bs-dismiss', 'toast');
    closeButton.setAttribute('aria-label', 'Close');

    dFlex.appendChild(toastBody);
    dFlex.appendChild(closeButton);
    toastContainer.appendChild(dFlex);

    const toastPlacement = document.createElement('div');
    toastPlacement.className = 'position-fixed bottom-0 end-0 p-3';
    toastPlacement.style.zIndex = '1100';
    toastPlacement.appendChild(toastContainer);
    
    document.body.appendChild(toastPlacement);

    const toast = new bootstrap.Toast(toastContainer);
    toast.show();
    
    toastContainer.addEventListener('hidden.bs.toast', () => {
        document.body.removeChild(toastPlacement);
    });
}

/**
 * Función para mostrar detalles en el modal (implementación real)
 */
function showDetails(id) {
    const modalTitle = document.getElementById('detailsModalTitle');
    const modalBody = document.getElementById('detailsModalBody');
    
    // Mostrar indicador de carga
    modalTitle.textContent = 'Cargando detalles...';
    modalBody.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div>';
    
    // Determinar el tipo de entidad y construir la URL
    let url;
    if (id.startsWith('pr_')) {
        url = `/api/details/pr/${id.substring(3)}`;
    } else if (id.startsWith('issue_')) {
        url = `/api/details/issue/${id.substring(6)}`;
    } else {
        url = `/api/details/commit/${id}`;
    }

    // Realizar la petición al servidor
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar los detalles');
            }
            return response.json();
        })
        .then(data => {
            modalTitle.textContent = `Detalles de ${data.type}`;
            modalBody.innerHTML = generateDetailsHtml(data);
        })
        .catch(error => {
            console.error('Error:', error);
            modalTitle.textContent = 'Error';
            modalBody.innerHTML = '<p>No se pudieron cargar los detalles. Por favor, inténtelo de nuevo.</p>';
        });

    const modal = new bootstrap.Modal(document.getElementById('detailsModal'));
    modal.show();
}

/**
 * Genera el HTML para el contenido del modal de detalles
 */
function generateDetailsHtml(data) {
    let content = `<dl class="row">`;
    
    for (const [key, value] of Object.entries(data)) {
        if (typeof value === 'object' && value !== null) {
            content += `<dt class="col-sm-3">${key}</dt><dd class="col-sm-9"><pre>${JSON.stringify(value, null, 2)}</pre></dd>`;
        } else {
            content += `<dt class="col-sm-3">${key}</dt><dd class="col-sm-9">${value}</dd>`;
        }
    }
    
    content += `</dl>`;
    return content;
} 