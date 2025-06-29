/**
 * TimelineService - Servicio especializado para gestión de timeline
 * SOLID: Single Responsibility - Solo maneja lógica de timeline
 * DRY: Centraliza toda la lógica de timeline en un lugar
 */

class TimelineService {
    constructor() {
        this.commits = [];
        this.filteredCommits = [];
        this.currentPage = 1;
        this.commitsPerPage = 15;
        this.eventBus = EventBus.getInstance();
    }

    /**
     * Configura commits y aplica filtros automáticamente
     */
    setCommits(commits) {
        this.commits = [...commits].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        this.applyFilters();
        this.emit('commits:updated', { 
            total: this.commits.length,
            filtered: this.filteredCommits.length 
        });
    }

    /**
     * Obtiene commits para la página actual
     */
    getCurrentPageCommits() {
        const startIndex = (this.currentPage - 1) * this.commitsPerPage;
        const endIndex = startIndex + this.commitsPerPage;
        return this.filteredCommits.slice(startIndex, endIndex);
    }

    /**
     * Navega a una página específica
     */
    goToPage(page) {
        const totalPages = this.getTotalPages();
        if (page < 1 || page > totalPages) return false;
        
        this.currentPage = page;
        this.emit('page:changed', { 
            page: this.currentPage, 
            commits: this.getCurrentPageCommits() 
        });
        return true;
    }

    /**
     * Aplica filtros a los commits
     */
    applyFilters(filters = {}) {
        this.filteredCommits = this.commits.filter(commit => {
            if (filters.author && !commit.author?.toLowerCase().includes(filters.author.toLowerCase())) {
                return false;
            }
            if (filters.dateRange && !this.isInDateRange(commit, filters.dateRange)) {
                return false;
            }
            if (filters.message && !commit.message.toLowerCase().includes(filters.message.toLowerCase())) {
                return false;
            }
            return true;
        });
        
        this.currentPage = 1; // Reset a primera página
        this.emit('filters:applied', { 
            total: this.filteredCommits.length,
            page: this.currentPage 
        });
    }

    /**
     * Optimiza commits por página según el espacio disponible
     */
    optimizeCommitsPerPage() {
        const viewportHeight = window.innerHeight;
        const availableHeight = viewportHeight - 300; // Header, pagination, etc.
        const estimatedCommitHeight = 80;
        const optimal = Math.floor(availableHeight / estimatedCommitHeight);
        
        const newCommitsPerPage = Math.max(10, Math.min(50, optimal));
        
        if (Math.abs(newCommitsPerPage - this.commitsPerPage) > 3) {
            this.commitsPerPage = newCommitsPerPage;
            this.emit('pagination:optimized', { commitsPerPage: this.commitsPerPage });
        }
    }

    /**
     * Métodos de utilidad
     */
    getTotalPages() {
        return Math.ceil(this.filteredCommits.length / this.commitsPerPage);
    }

    getPaginationInfo() {
        return {
            currentPage: this.currentPage,
            totalPages: this.getTotalPages(),
            commitsPerPage: this.commitsPerPage,
            totalCommits: this.filteredCommits.length,
            totalOriginalCommits: this.commits.length
        };
    }

    isInDateRange(commit, dateRange) {
        const commitDate = new Date(commit.timestamp);
        const now = new Date();
        
        switch (dateRange) {
            case 'week':
                return (now - commitDate) <= (7 * 24 * 60 * 60 * 1000);
            case 'month':
                return (now - commitDate) <= (30 * 24 * 60 * 60 * 1000);
            case 'quarter':
                return (now - commitDate) <= (90 * 24 * 60 * 60 * 1000);
            case 'year':
                return (now - commitDate) <= (365 * 24 * 60 * 60 * 1000);
            default:
                return true;
        }
    }

    emit(eventName, data) {
        this.eventBus.emit(`timeline:${eventName}`, data);
    }

    /**
     * Factory method para crear instancia singleton
     */
    static getInstance() {
        if (!TimelineService.instance) {
            TimelineService.instance = new TimelineService();
        }
        return TimelineService.instance;
    }
}

// Registrar en el sistema global
window.TimelineService = TimelineService; 