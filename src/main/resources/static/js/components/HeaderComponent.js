/**
 * Componente Header - Navegación y autenticación
 * Sigue principios SOLID y es completamente reutilizable
 */

class HeaderComponent extends BaseComponent {
    constructor(selector, options = {}) {
        super(selector, options);
        this.githubService = ServiceFactory.getGithub();
        this.stateManager = AppStateManager.getInstance();
    }

    getDefaultOptions() {
        return {
            showBreadcrumb: false,
            showRealtimeStatus: false,
            breadcrumbTitle: '',
            breadcrumbIcon: 'fas fa-file'
        };
    }

    async beforeInit() {
        // Suscribirse a cambios de estado global
        this.stateManager.subscribe('user', (user) => {
            this.setState({ user });
        });

        this.stateManager.subscribe('isLoading', (isLoading) => {
            this.setState({ isLoading });
        });

        // Suscribirse a cambios de repositorio actual
        this.stateManager.subscribe('currentRepo', (currentRepo) => {
            if (currentRepo && this.shouldConnectRealtime()) {
                this.startRealtimeConnections();
            } else if (!currentRepo) {
                this.stopRealtimeConnections();
            }
        });

        // Suscribirse a eventos de tiempo real
        this.subscribe('realtime:connected', () => {
            this.updateRealtimeStatus(true);
        });

        this.subscribe('realtime:disconnected', () => {
            this.updateRealtimeStatus(false);
        });

        this.subscribe('github:rateLimit', (data) => {
            this.updateRateLimit(data.remaining);
        });

        // Conectar tiempo real si estamos en una página que lo necesita
        if (this.shouldConnectRealtime()) {
            const state = this.stateManager.getState();
            if (state.currentRepo) {
                this.startRealtimeConnections();
            }
        }
    }

    async render() {
        if (!this.element) return;

        const { user } = this.state;
        const { showBreadcrumb, showRealtimeStatus, breadcrumbTitle, breadcrumbIcon } = this.options;

        this.element.innerHTML = `
            <div class="container-fluid px-4">
                <a class="navbar-brand d-flex align-items-center" href="/">
                    <i class="fas fa-cube me-2"></i>
                    <span>GitStellarPrism</span>
                </a>
                
                <div class="d-flex align-items-center gap-3">
                    ${showBreadcrumb ? this.renderBreadcrumb(breadcrumbTitle, breadcrumbIcon) : ''}
                    ${showRealtimeStatus ? this.renderRealtimeStatus() : ''}
                    ${this.renderUserSection(user)}
                </div>
            </div>
        `;
    }

    renderBreadcrumb(title, icon) {
        return `
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb mb-0">
                    <li class="breadcrumb-item">
                        <a href="/dashboard" class="text-light">
                            <i class="fas fa-tachometer-alt me-1"></i>Dashboard
                        </a>
                    </li>
                    <li class="breadcrumb-item active text-light" aria-current="page">
                        <i class="${icon} me-1"></i>
                        <span>${title}</span>
                    </li>
                </ol>
            </nav>
        `;
    }

    renderRealtimeStatus() {
        return `
            <div id="realtime-status" class="status-indicator">
                <i class="fas fa-circle pulse"></i>
                <span>Verificando conexión...</span>
            </div>
        `;
    }

    renderUserSection(user) {
        if (user) {
            return `
                <div class="dropdown">
                    <button class="btn btn-outline-light dropdown-toggle d-flex align-items-center" 
                            type="button" data-bs-toggle="dropdown">
                        <img id="userAvatar" src="${user.avatar_url || ''}" alt="Avatar" 
                             class="rounded-circle me-2" width="32" height="32" 
                             style="${user.avatar_url ? '' : 'display: none;'}">
                        <i class="fas fa-user-circle me-2" id="defaultAvatar" 
                           style="${user.avatar_url ? 'display: none;' : ''}"></i>
                        <span id="userName">${user.login || 'Usuario'}</span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li><a class="dropdown-item" href="/dashboard">
                            <i class="fas fa-tachometer-alt me-2"></i> Dashboard
                        </a></li>
                        <li><a class="dropdown-item" href="/api/user/info" target="_blank">
                            <i class="fas fa-user me-2"></i> Mi Perfil
                        </a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item text-danger" href="/logout">
                            <i class="fas fa-sign-out-alt me-2"></i> Cerrar Sesión
                        </a></li>
                    </ul>
                </div>
            `;
        } else {
            return `
                <div>
                    <a class="btn btn-primary" href="/oauth2/authorization/github">
                        <i class="fab fa-github me-2"></i>
                        <span>Iniciar Sesión</span>
                    </a>
                </div>
            `;
        }
    }

    bindEvents() {
        // Event delegation para botones dinámicos
        this.element.addEventListener('click', (e) => {
            if (e.target.closest('.dropdown-toggle')) {
                // Bootstrap maneja el dropdown automáticamente
                return;
            }
        });

        // Cargar información del usuario al inicializar
        this.loadUserInfo();
    }

    async loadUserInfo() {
        try {
            const user = await this.githubService.getUserInfo();
            console.log('👤 Información de usuario recibida:', user);
            console.log('👤 Login:', user.login);
            console.log('👤 Name:', user.name);
            console.log('👤 ID:', user.id);
            this.stateManager.setState({ user });
        } catch (error) {
            // Usuario no autenticado, no es un error
            console.log('Usuario no autenticado');
            this.stateManager.setState({ user: null });
        }
    }

    updateRealtimeStatus(connected) {
        const statusEl = this.element.querySelector('#realtime-status');
        if (statusEl) {
            if (connected) {
                statusEl.className = 'status-indicator status-success';
                statusEl.innerHTML = '<i class="fas fa-circle"></i><span>Conectado</span>';
            } else {
                statusEl.className = 'status-indicator status-error';
                statusEl.innerHTML = '<i class="fas fa-circle"></i><span>Desconectado</span>';
            }
        }
    }

    updateRateLimit(remaining) {
        if (remaining < 10) {
            this.emit('notification:show', {
                message: `⚠️ Límite de API bajo: ${remaining} requests restantes`,
                type: 'warning'
            });
        }
    }

    /**
     * Determina si debe conectarse al servicio de tiempo real
     * basado en la página actual
     */
    shouldConnectRealtime() {
        const path = window.location.pathname;
        return path.includes('/dashboard') || path.includes('/analysis');
    }

    /**
     * Inicia conexiones de tiempo real
     */
    startRealtimeConnections() {
        if (this.realtimeService && this.realtimeService.isConnected()) {
            console.log('🔗 Servicio de tiempo real ya conectado');
            return;
        }

        try {
            this.realtimeService = ServiceFactory.getRealtime();
            const state = this.stateManager.getState();
            
            if (state.currentRepo) {
                console.log('🔗 Conectando servicio de tiempo real para:', state.currentRepo);
                this.realtimeService.connect(state.currentRepo);
                this.updateRealtimeStatus(true);
            } else {
                console.log('⚠️ No hay repositorio actual para conectar tiempo real');
                this.updateRealtimeStatus(false);
            }
        } catch (error) {
            console.error('❌ Error conectando servicio de tiempo real:', error);
            this.updateRealtimeStatus(false);
        }
    }

    /**
     * Detiene conexiones de tiempo real
     */
    stopRealtimeConnections() {
        if (this.realtimeService) {
            this.realtimeService.disconnect();
            this.updateRealtimeStatus(false);
            console.log('🔌 Servicio de tiempo real desconectado');
        }
    }

    // Método público para actualizar opciones dinámicamente
    updateOptions(newOptions) {
        Object.assign(this.options, newOptions);
        this.update();
    }
}

// Registrar el componente
ComponentFactory.register('header', HeaderComponent);

console.log('🔗 HeaderComponent registrado'); 