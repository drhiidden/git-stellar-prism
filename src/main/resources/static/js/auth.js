/**
 * Manejo de autenticación OAuth2 y información del usuario
 */

// Variables globales de autenticación
let currentUser = null;
let isAuthenticated = false;
let authToken = null;

/**
 * Inicializa el sistema de autenticación
 */
function initAuth() {
    checkAuthStatus();
    setupAuthEventListeners();
}

/**
 * Verifica el estado de autenticación del usuario
 */
function checkAuthStatus() {
    fetch('/api/user/auth-status')
        .then(response => response.json())
        .then(data => {
            isAuthenticated = data.authenticated;
            
            if (isAuthenticated) {
                loadUserInfo();
                showUserInterface();
            } else {
                showLoginInterface();
            }
        })
        .catch(error => {
            console.error('Error al verificar estado de autenticación:', error);
            showLoginInterface();
        });
}

/**
 * Carga la información del usuario autenticado
 */
function loadUserInfo() {
    fetch('/api/user/info')
        .then(response => {
            if (!response.ok) {
                throw new Error('No se pudo obtener información del usuario');
            }
            return response.json();
        })
        .then(userInfo => {
            currentUser = userInfo;
            updateUserDisplay(userInfo);
            console.log('Usuario autenticado:', userInfo);
        })
        .catch(error => {
            console.error('Error al cargar información del usuario:', error);
            showLoginInterface();
        });
}

/**
 * Actualiza la visualización de información del usuario
 */
function updateUserDisplay(userInfo) {
    const userNameElement = document.getElementById('userName');
    const userAvatarElement = document.getElementById('userAvatar');
    
    if (userNameElement) {
        userNameElement.textContent = userInfo.name || userInfo.login || 'Usuario';
    }
    
    if (userAvatarElement && userInfo.avatarUrl) {
        userAvatarElement.src = userInfo.avatarUrl;
        userAvatarElement.alt = `Avatar de ${userInfo.login}`;
    }
}

/**
 * Muestra la interfaz para usuarios autenticados
 */
function showUserInterface() {
    const userInfo = document.getElementById('userInfo');
    const loginSection = document.getElementById('loginSection');
    const logoutSection = document.getElementById('logoutSection');
    
    if (userInfo) userInfo.style.display = 'block';
    if (loginSection) loginSection.style.display = 'none';
    if (logoutSection) logoutSection.style.display = 'block';
    
    // Habilitar funcionalidades que requieren autenticación
    enableAuthenticatedFeatures();
}

/**
 * Muestra la interfaz para usuarios no autenticados
 */
function showLoginInterface() {
    const userInfo = document.getElementById('userInfo');
    const loginSection = document.getElementById('loginSection');
    const logoutSection = document.getElementById('logoutSection');
    
    if (userInfo) userInfo.style.display = 'none';
    if (loginSection) loginSection.style.display = 'block';
    if (logoutSection) logoutSection.style.display = 'none';
    
    // Deshabilitar funcionalidades que requieren autenticación
    disableAuthenticatedFeatures();
}

/**
 * Habilita funcionalidades que requieren autenticación
 */
function enableAuthenticatedFeatures() {
    // Habilitar acceso a repositorios privados
    const repoForm = document.getElementById('repoForm');
    if (repoForm) {
        const submitButton = repoForm.querySelector('button[type="submit"]');
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Visualizar';
        }
    }
    
    // Mostrar mensaje de bienvenida si es la primera vez
    if (currentUser && !sessionStorage.getItem('welcomeShown')) {
        showWelcomeMessage();
        sessionStorage.setItem('welcomeShown', 'true');
    }
}

/**
 * Deshabilita funcionalidades que requieren autenticación
 */
function disableAuthenticatedFeatures() {
    // Mostrar advertencia sobre repositorios públicos únicamente
    const repoForm = document.getElementById('repoForm');
    if (repoForm) {
        const submitButton = repoForm.querySelector('button[type="submit"]');
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Visualizar (Solo Públicos)';
        }
    }
}

/**
 * Muestra mensaje de bienvenida para usuarios autenticados
 */
function showWelcomeMessage() {
    if (!currentUser) return;
    
    const welcomeHtml = `
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <strong>¡Bienvenido, ${currentUser.name || currentUser.login}!</strong> 
            Ahora puedes acceder a tus repositorios privados y obtener límites de API más altos.
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
    
    const container = document.querySelector('.container.mt-4');
    if (container) {
        container.insertAdjacentHTML('afterbegin', welcomeHtml);
    }
}

/**
 * Configura event listeners para autenticación
 */
function setupAuthEventListeners() {
    // Manejar clic en logout
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }
    
    // Manejar clic en login
    const loginBtn = document.getElementById('loginBtn');
    if (loginBtn) {
        loginBtn.addEventListener('click', handleLogin);
    }
}

/**
 * Maneja el proceso de logout
 */
function handleLogout(event) {
    event.preventDefault();
    
    // Limpiar estado local
    currentUser = null;
    isAuthenticated = false;
    authToken = null;
    sessionStorage.clear();
    
    // Redirigir al logout del servidor
    window.location.href = '/logout';
}

/**
 * Maneja el proceso de login
 */
function handleLogin(event) {
    // El enlace ya redirige a OAuth2, solo añadimos logging
    console.log('Iniciando proceso de autenticación OAuth2...');
}

/**
 * Obtiene información del token para debugging
 */
function getTokenInfo() {
    if (!isAuthenticated) {
        console.log('Usuario no autenticado');
        return;
    }
    
    fetch('/api/user/token-info')
        .then(response => response.json())
        .then(data => {
            console.log('Información del token:', data);
        })
        .catch(error => {
            console.error('Error al obtener información del token:', error);
        });
}

/**
 * Verifica si el usuario puede acceder a un repositorio
 */
function canAccessRepository(repoUrl) {
    // Si está autenticado, puede acceder a repositorios privados
    if (isAuthenticated) {
        return true;
    }
    
    // Si no está autenticado, solo repositorios públicos
    // Esto se validará en el servidor
    return true; // El servidor manejará la validación real
}

/**
 * Obtiene el estado de autenticación actual
 */
function getAuthState() {
    return {
        authenticated: isAuthenticated,
        user: currentUser,
        hasToken: authToken !== null
    };
}

// Exportar funciones para uso global
window.authFunctions = {
    initAuth,
    checkAuthStatus,
    getAuthState,
    canAccessRepository,
    getTokenInfo
};

// Inicializar cuando el DOM esté cargado
document.addEventListener('DOMContentLoaded', initAuth); 