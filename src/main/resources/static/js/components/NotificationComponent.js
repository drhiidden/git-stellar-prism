/**
 * Componente de Notificaciones - Manejo centralizado de toasts y alertas
 * Implementa principios SOLID y patrón Observer
 */

class NotificationComponent extends BaseComponent {
    constructor(selector, options = {}) {
        super(selector, options);
        this.notifications = [];
        this.maxNotifications = 5;
    }

    getDefaultOptions() {
        return {
            position: 'bottom-end',
            autoHide: true,
            defaultDelay: 5000,
            enableSound: false,
            enableProgress: true
        };
    }

    async beforeInit() {
        // Suscribirse a eventos de notificación
        this.subscribe('notification:show', (data) => {
            this.show(data.message, data.type, data.options);
        });

        this.subscribe('api:error', (data) => {
            this.showError(`Error en ${data.service}: ${data.error.message}`);
        });

        this.subscribe('github:rateLimit', (data) => {
            if (data.remaining < 10) {
                this.showWarning(`Límite de API bajo: ${data.remaining} requests restantes`);
            }
        });

        this.subscribe('realtime:connected', () => {
            this.showSuccess('Conexión en tiempo real establecida');
        });

        this.subscribe('realtime:disconnected', () => {
            this.showWarning('Conexión en tiempo real perdida');
        });

        this.subscribe('cache:invalidated', () => {
            this.showInfo('Caché actualizada');
        });
    }

    async render() {
        if (!this.element) return;

        const { position } = this.options;

        this.element.innerHTML = `
            <div class="toast-container position-fixed ${this.getPositionClasses(position)} p-3" 
                 style="z-index: 1100;">
                <!-- Los toasts se añadirán dinámicamente aquí -->
            </div>
        `;
    }

    bindEvents() {
        // Event delegation para botones de cierre y acciones
        this.element.addEventListener('click', (e) => {
            if (e.target.closest('.toast-close-btn')) {
                const toastId = e.target.closest('.toast').dataset.toastId;
                this.hide(toastId);
            }

            if (e.target.closest('.toast-action-btn')) {
                const toastId = e.target.closest('.toast').dataset.toastId;
                const action = e.target.dataset.action;
                this.handleAction(toastId, action);
            }
        });
    }

    // Métodos públicos para mostrar diferentes tipos de notificaciones
    show(message, type = 'info', options = {}) {
        const id = this.generateId();
        const notification = {
            id,
            message,
            type,
            timestamp: new Date(),
            options: { ...this.options, ...options }
        };

        this.notifications.push(notification);
        this.renderNotification(notification);
        this.manageNotificationLimit();

        return id;
    }

    showSuccess(message, options = {}) {
        return this.show(message, 'success', options);
    }

    showError(message, options = {}) {
        return this.show(message, 'error', { autoHide: false, ...options });
    }

    showWarning(message, options = {}) {
        return this.show(message, 'warning', options);
    }

    showInfo(message, options = {}) {
        return this.show(message, 'info', options);
    }

    showProgress(message, options = {}) {
        return this.show(message, 'progress', { autoHide: false, ...options });
    }

    // Método para mostrar notificaciones con acciones
    showWithActions(message, type, actions, options = {}) {
        return this.show(message, type, { ...options, actions });
    }

    hide(id) {
        const toastElement = this.element.querySelector(`[data-toast-id="${id}"]`);
        if (toastElement) {
            const bsToast = bootstrap.Toast.getInstance(toastElement);
            if (bsToast) {
                bsToast.hide();
            }
        }

        this.notifications = this.notifications.filter(n => n.id !== id);
    }

    hideAll() {
        this.notifications.forEach(notification => {
            this.hide(notification.id);
        });
    }

    updateProgress(id, progress, message) {
        const toastElement = this.element.querySelector(`[data-toast-id="${id}"]`);
        if (toastElement) {
            const progressBar = toastElement.querySelector('.progress-bar');
            const messageEl = toastElement.querySelector('.toast-body .message');
            
            if (progressBar) {
                progressBar.style.width = `${progress}%`;
                progressBar.setAttribute('aria-valuenow', progress);
            }
            
            if (messageEl && message) {
                messageEl.textContent = message;
            }
        }
    }

    renderNotification(notification) {
        const container = this.element.querySelector('.toast-container');
        const toastElement = this.createToastElement(notification);
        
        container.appendChild(toastElement);
        
        // Inicializar Bootstrap Toast
        const bsToast = new bootstrap.Toast(toastElement, {
            autohide: notification.options.autoHide,
            delay: notification.options.defaultDelay
        });

        // Manejar eventos del toast
        toastElement.addEventListener('hidden.bs.toast', () => {
            this.onToastHidden(notification.id);
        });

        toastElement.addEventListener('shown.bs.toast', () => {
            this.onToastShown(notification);
        });

        bsToast.show();
    }

    createToastElement(notification) {
        const { id, message, type, options } = notification;
        const icon = this.getTypeIcon(type);
        const colorClass = this.getTypeColorClass(type);

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.setAttribute('role', 'alert');
        toast.setAttribute('aria-live', 'assertive');
        toast.setAttribute('aria-atomic', 'true');
        toast.setAttribute('data-toast-id', id);

        toast.innerHTML = `
            <div class="toast-header ${colorClass}">
                <i class="${icon} me-2"></i>
                <strong class="me-auto">${this.getTypeTitle(type)}</strong>
                <small class="text-muted">${this.formatTime(notification.timestamp)}</small>
                <button type="button" class="btn-close toast-close-btn" aria-label="Cerrar"></button>
            </div>
            <div class="toast-body">
                <div class="message">${message}</div>
                ${type === 'progress' ? this.renderProgressBar() : ''}
                ${options.actions ? this.renderActions(options.actions, id) : ''}
            </div>
        `;

        return toast;
    }

    renderProgressBar() {
        return `
            <div class="progress mt-2" style="height: 4px;">
                <div class="progress-bar" role="progressbar" 
                     style="width: 0%" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">
                </div>
            </div>
        `;
    }

    renderActions(actions, toastId) {
        if (!actions || actions.length === 0) return '';

        return `
            <div class="toast-actions mt-2">
                ${actions.map(action => `
                    <button type="button" class="btn btn-sm btn-outline-primary toast-action-btn me-2" 
                            data-action="${action.id}" data-toast-id="${toastId}">
                        ${action.icon ? `<i class="${action.icon} me-1"></i>` : ''}
                        ${action.label}
                    </button>
                `).join('')}
            </div>
        `;
    }

    getTypeIcon(type) {
        const icons = {
            success: 'fas fa-check-circle',
            error: 'fas fa-exclamation-circle',
            warning: 'fas fa-exclamation-triangle',
            info: 'fas fa-info-circle',
            progress: 'fas fa-spinner fa-spin'
        };
        return icons[type] || icons.info;
    }

    getTypeColorClass(type) {
        const classes = {
            success: 'text-success',
            error: 'text-danger', 
            warning: 'text-warning',
            info: 'text-primary',
            progress: 'text-info'
        };
        return classes[type] || classes.info;
    }

    getTypeTitle(type) {
        const titles = {
            success: 'Éxito',
            error: 'Error',
            warning: 'Advertencia',
            info: 'Información',
            progress: 'Progreso'
        };
        return titles[type] || 'Notificación';
    }

    getPositionClasses(position) {
        const positions = {
            'top-start': 'top-0 start-0',
            'top-center': 'top-0 start-50 translate-middle-x',
            'top-end': 'top-0 end-0',
            'middle-start': 'top-50 start-0 translate-middle-y',
            'middle-center': 'top-50 start-50 translate-middle',
            'middle-end': 'top-50 end-0 translate-middle-y',
            'bottom-start': 'bottom-0 start-0',
            'bottom-center': 'bottom-0 start-50 translate-middle-x',
            'bottom-end': 'bottom-0 end-0'
        };
        return positions[position] || positions['bottom-end'];
    }

    formatTime(timestamp) {
        const now = new Date();
        const diff = now - timestamp;
        
        if (diff < 60000) { // Menos de 1 minuto
            return 'ahora';
        } else if (diff < 3600000) { // Menos de 1 hora
            const minutes = Math.floor(diff / 60000);
            return `${minutes}m`;
        } else {
            return timestamp.toLocaleTimeString('es-ES', { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
        }
    }

    generateId() {
        return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    manageNotificationLimit() {
        if (this.notifications.length > this.maxNotifications) {
            const oldest = this.notifications[0];
            this.hide(oldest.id);
        }
    }

    onToastShown(notification) {
        if (notification.options.enableSound) {
            this.playNotificationSound(notification.type);
        }

        // Emitir evento de notificación mostrada
        this.emit('notification:shown', notification);
    }

    onToastHidden(id) {
        const toastElement = this.element.querySelector(`[data-toast-id="${id}"]`);
        if (toastElement) {
            toastElement.remove();
        }

        this.notifications = this.notifications.filter(n => n.id !== id);
        this.emit('notification:hidden', { id });
    }

    handleAction(toastId, action) {
        const notification = this.notifications.find(n => n.id === toastId);
        if (notification && notification.options.actions) {
            const actionConfig = notification.options.actions.find(a => a.id === action);
            if (actionConfig && actionConfig.handler) {
                actionConfig.handler(notification);
            }
        }

        // Emitir evento de acción
        this.emit('notification:action', { toastId, action });
        
        // Ocultar la notificación después de la acción
        this.hide(toastId);
    }

    playNotificationSound(type) {
        // Implementar reproducción de sonidos si está habilitada
        if (this.options.enableSound && 'AudioContext' in window) {
            // Aquí se puede implementar la reproducción de sonidos
            console.log(`🔊 Sonido de notificación: ${type}`);
        }
    }

    // Método para mostrar notificaciones de API loading
    showApiLoading(operation) {
        return this.showProgress(`Cargando ${operation}...`, {
            defaultDelay: 0
        });
    }

    // Método para completar una operación de loading
    completeApiLoading(id, success, message) {
        this.hide(id);
        
        if (success) {
            this.showSuccess(message || 'Operación completada');
        } else {
            this.showError(message || 'Error en la operación');
        }
    }
}

// Registrar el componente
ComponentFactory.register('notification', NotificationComponent);

console.log('🔔 NotificationComponent registrado'); 