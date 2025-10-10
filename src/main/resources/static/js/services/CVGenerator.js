/**
 * CVGenerator - Generador Inteligente de CV Técnico desde GitHub
 * 
 * Analiza repositorios para crear un CV profesional SIN llamadas adicionales a la API.
 * Usa datos ya disponibles: lenguajes, topics, stars, descripción, etc.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */

class CVGenerator {
    constructor(repositories, userInfo) {
        this.repos = repositories || [];
        this.user = userInfo || {};
        this.detector = window.TechnologyDetector;
        
        // Configuración de scoring
        this.config = {
            minProjectScore: 10,        // Score mínimo para incluir proyecto
            topProjectsCount: 10,       // Top N proyectos para CV
            minLanguagePercentage: 5.0, // Porcentaje mínimo para considerar lenguaje
            experienceThreshold: 3      // Mínimo de proyectos para considerar "experiencia"
        };
    }

    /**
     * Genera el CV completo estructurado
     * @returns {Object} CV estructurado con todas las secciones
     */
    generate() {
        console.log('🎯 Generando CV técnico para:', this.user.login || 'Usuario');
        
        return {
            metadata: this.generateMetadata(),
            header: this.generateHeader(),
            summary: this.generateSummary(),
            technologies: this.generateTechnologiesSection(),
            experience: this.generateExperienceSection(),
            projects: this.generateProjectsSection(),
            activity: this.generateActivitySection(),
            skills: this.generateSkillsSection(),
            statistics: this.generateStatistics()
        };
    }

    /**
     * Metadata del CV
     */
    generateMetadata() {
        return {
            generatedAt: new Date().toISOString(),
            version: '1.0.0',
            source: 'GitStellarPrism',
            totalRepositories: this.repos.length,
            githubProfile: this.user.html_url || this.user.htmlUrl
        };
    }

    /**
     * Cabecera del CV con información personal
     */
    generateHeader() {
        return {
            name: this.user.name || this.user.login || 'Developer',
            username: this.user.login || '',
            role: this.deriveRole(),
            bio: this.user.bio || '',
            location: this.user.location || '',
            email: this.user.email || '',
            website: this.user.blog || this.user.website || '',
            avatarUrl: this.user.avatar_url || this.user.avatarUrl || '',
            github: this.user.html_url || this.user.htmlUrl || '',
            followers: this.user.followers || 0,
            following: this.user.following || 0
        };
    }

    /**
     * Resume ejecutivo
     */
    generateSummary() {
        const techStats = this.detector.getStatistics(this.repos);
        const primaryTech = techStats.technologies.slice(0, 3).map(t => t.name);
        const totalProjects = this.repos.length;
        const publicProjects = this.repos.filter(r => !r.private && !r.isPrivate).length;
        const role = this.deriveRole();
        
        // Calcular años de experiencia (desde el repo más antiguo)
        const oldestRepo = this.repos.reduce((oldest, repo) => {
            const created = new Date(repo.created_at || repo.createdAt);
            return !oldest || created < oldest ? created : oldest;
        }, null);
        
        const yearsActive = oldestRepo 
            ? Math.max(1, new Date().getFullYear() - oldestRepo.getFullYear())
            : 1;

        return {
            role,
            yearsActive,
            totalProjects,
            publicProjects,
            primaryTechnologies: primaryTech,
            headline: this.generateHeadline(role, yearsActive, primaryTech),
            description: this.generateDescription(role, primaryTech, totalProjects)
        };
    }

    /**
     * Genera un headline profesional
     */
    generateHeadline(role, years, technologies) {
        const techList = technologies.slice(0, 2).join(' & ');
        return `${role} | ${years}+ años | ${techList}`;
    }

    /**
     * Genera descripción profesional
     */
    generateDescription(role, technologies, projectCount) {
        const techList = technologies.join(', ');
        return `${role} con experiencia en ${techList}. ${projectCount} proyectos desarrollados, ` +
               `demostrando habilidades en desarrollo full-stack, arquitectura de software y buenas prácticas.`;
    }

    /**
     * Sección de tecnologías organizadas por categoría
     */
    generateTechnologiesSection() {
        const techStats = this.detector.getStatistics(this.repos);
        const grouped = this.detector.groupByCategory(techStats.technologies);
        
        return {
            summary: {
                total: techStats.totalUniqueTechnologies,
                categories: Object.keys(grouped).length,
                primary: techStats.technologies.slice(0, 5).map(t => ({
                    name: t.name,
                    count: t.count,
                    icon: t.icon,
                    proficiency: this.calculateProficiency(t.count, this.repos.length)
                }))
            },
            byCategory: this.organizeTechnologiesByCategory(grouped),
            byProficiency: this.organizeTechnologiesByProficiency(techStats.technologies),
            timeline: this.generateTechTimeline()
        };
    }

    /**
     * Organiza tecnologías por categoría
     */
    organizeTechnologiesByCategory(grouped) {
        const result = {};
        
        for (const [category, data] of Object.entries(grouped)) {
            result[category] = {
                icon: data.icon,
                priority: data.priority,
                technologies: data.technologies.map(tech => ({
                    name: tech.name,
                    icon: tech.icon,
                    count: tech.count,
                    proficiency: this.calculateProficiency(tech.count, this.repos.length),
                    repos: tech.repos
                }))
            };
        }
        
        return result;
    }

    /**
     * Organiza tecnologías por nivel de competencia
     */
    organizeTechnologiesByProficiency(technologies) {
        const expert = [];      // >= 50% de repos
        const advanced = [];    // >= 30% de repos
        const intermediate = []; // >= 15% de repos
        const basic = [];       // < 15% de repos
        
        technologies.forEach(tech => {
            const percentage = (tech.count / this.repos.length) * 100;
            const item = {
                name: tech.name,
                icon: tech.icon,
                count: tech.count,
                percentage: Math.round(percentage),
                category: tech.category
            };
            
            if (percentage >= 50) expert.push(item);
            else if (percentage >= 30) advanced.push(item);
            else if (percentage >= 15) intermediate.push(item);
            else basic.push(item);
        });
        
        return { expert, advanced, intermediate, basic };
    }

    /**
     * Calcula nivel de competencia
     */
    calculateProficiency(projectCount, totalRepos) {
        const percentage = (projectCount / totalRepos) * 100;
        
        if (percentage >= 50) return 'Expert';
        if (percentage >= 30) return 'Advanced';
        if (percentage >= 15) return 'Intermediate';
        return 'Basic';
    }

    /**
     * Timeline de adopción de tecnologías
     */
    generateTechTimeline() {
        const timeline = {};
        
        this.repos.forEach(repo => {
            const year = new Date(repo.created_at || repo.createdAt).getFullYear();
            const languages = repo.languages || repo.languageDistribution || {};
            const topics = repo.topics || [];
            
            if (!timeline[year]) {
                timeline[year] = { languages: new Set(), frameworks: new Set(), count: 0 };
            }
            
            timeline[year].count++;
            Object.keys(languages).forEach(lang => timeline[year].languages.add(lang));
            topics.forEach(topic => timeline[year].frameworks.add(topic));
        });
        
        // Convertir Sets a Arrays
        Object.keys(timeline).forEach(year => {
            timeline[year].languages = Array.from(timeline[year].languages);
            timeline[year].frameworks = Array.from(timeline[year].frameworks);
        });
        
        return timeline;
    }

    /**
     * Sección de experiencia
     */
    generateExperienceSection() {
        const techStats = this.detector.getStatistics(this.repos);
        const experiences = [];
        
        techStats.technologies.forEach(tech => {
            if (tech.count >= this.config.experienceThreshold) {
                const yearsUsing = this.calculateYearsUsing(tech.name);
                
                experiences.push({
                    technology: tech.name,
                    icon: tech.icon,
                    category: tech.category,
                    yearsOfExperience: yearsUsing,
                    projectCount: tech.count,
                    proficiency: this.calculateProficiency(tech.count, this.repos.length),
                    repos: tech.repos,
                    firstUsed: this.findFirstUsage(tech.name),
                    lastUsed: this.findLastUsage(tech.name)
                });
            }
        });
        
        // Ordenar por años de experiencia y cantidad de proyectos
        experiences.sort((a, b) => {
            if (b.yearsOfExperience !== a.yearsOfExperience) {
                return b.yearsOfExperience - a.yearsOfExperience;
            }
            return b.projectCount - a.projectCount;
        });
        
        return experiences;
    }

    /**
     * Calcula años usando una tecnología
     */
    calculateYearsUsing(techName) {
        const years = new Set();
        
        this.repos.forEach(repo => {
            const techs = this.detector.analyzeRepository(repo);
            if (techs.some(t => t.name === techName)) {
                const year = new Date(repo.created_at || repo.createdAt).getFullYear();
                years.add(year);
            }
        });
        
        if (years.size === 0) return 0;
        
        const sortedYears = Array.from(years).sort();
        const firstYear = sortedYears[0];
        const lastYear = sortedYears[sortedYears.length - 1];
        
        return Math.max(1, new Date().getFullYear() - firstYear);
    }

    /**
     * Encuentra primer uso de una tecnología
     */
    findFirstUsage(techName) {
        let earliest = null;
        
        this.repos.forEach(repo => {
            const techs = this.detector.analyzeRepository(repo);
            if (techs.some(t => t.name === techName)) {
                const date = new Date(repo.created_at || repo.createdAt);
                if (!earliest || date < earliest) {
                    earliest = date;
                }
            }
        });
        
        return earliest ? earliest.toISOString() : null;
    }

    /**
     * Encuentra último uso de una tecnología
     */
    findLastUsage(techName) {
        let latest = null;
        
        this.repos.forEach(repo => {
            const techs = this.detector.analyzeRepository(repo);
            if (techs.some(t => t.name === techName)) {
                const date = new Date(repo.updated_at || repo.updatedAt);
                if (!latest || date > latest) {
                    latest = date;
                }
            }
        });
        
        return latest ? latest.toISOString() : null;
    }

    /**
     * Sección de proyectos destacados
     */
    generateProjectsSection() {
        const scoredProjects = this.repos
            .map(repo => ({
                ...repo,
                score: this.calculateProjectScore(repo)
            }))
            .filter(repo => repo.score >= this.config.minProjectScore)
            .sort((a, b) => b.score - a.score)
            .slice(0, this.config.topProjectsCount);
        
        return scoredProjects.map(repo => ({
            name: repo.name,
            fullName: repo.full_name || repo.fullName || `${repo.owner}/${repo.name}`,
            description: repo.description || 'Sin descripción',
            url: repo.html_url || repo.htmlUrl || '',
            homepage: repo.homepage || '',
            stars: repo.stargazers_count || repo.stargazersCount || 0,
            forks: repo.forks_count || repo.forksCount || 0,
            watchers: repo.watchers_count || repo.watchersCount || 0,
            language: repo.language || 'Unknown',
            technologies: this.extractTechnologies(repo),
            topics: repo.topics || [],
            createdAt: repo.created_at || repo.createdAt,
            updatedAt: repo.updated_at || repo.updatedAt,
            isActive: this.isRecentlyActive(repo.updated_at || repo.updatedAt),
            isFork: repo.fork || false,
            visibility: repo.private || repo.isPrivate ? 'Private' : 'Public',
            score: Math.round(repo.score),
            highlights: this.generateProjectHighlights(repo)
        }));
    }

    /**
     * Calcula score de un proyecto
     */
    calculateProjectScore(repo) {
        let score = 0;
        
        // Popularidad (stars y forks)
        score += (repo.stargazers_count || repo.stargazersCount || 0) * 10;
        score += (repo.forks_count || repo.forksCount || 0) * 5;
        score += (repo.watchers_count || repo.watchersCount || 0) * 3;
        
        // Documentación
        score += repo.description ? 20 : 0;
        score += repo.homepage ? 15 : 0;
        
        // Tecnologías (más topics = más complejo/completo)
        score += (repo.topics || []).length * 3;
        
        // Actividad reciente
        score += this.isRecentlyActive(repo.updated_at || repo.updatedAt) ? 50 : 0;
        
        // Originalidad (no fork)
        score += !(repo.fork || false) ? 30 : -10;
        
        // Tamaño del proyecto (indicador de complejidad)
        const size = repo.size || 0;
        if (size > 10000) score += 25; // Proyecto grande
        else if (size > 1000) score += 15; // Proyecto mediano
        else if (size > 100) score += 5;   // Proyecto pequeño
        
        return score;
    }

    /**
     * Verifica si un proyecto está activamente mantenido
     */
    isRecentlyActive(updatedAt) {
        if (!updatedAt) return false;
        
        const updated = new Date(updatedAt);
        const sixMonthsAgo = new Date();
        sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
        
        return updated > sixMonthsAgo;
    }

    /**
     * Extrae tecnologías de un repositorio
     */
    extractTechnologies(repo) {
        const techs = this.detector.analyzeRepository(repo);
        return techs.map(t => ({
            name: t.name,
            icon: t.icon,
            category: t.category,
            confidence: t.confidence
        }));
    }

    /**
     * Genera highlights de un proyecto
     */
    generateProjectHighlights(repo) {
        const highlights = [];
        
        const stars = repo.stargazers_count || repo.stargazersCount || 0;
        if (stars >= 100) highlights.push(`⭐ ${stars}+ stars`);
        else if (stars >= 50) highlights.push(`⭐ ${stars} stars`);
        
        const forks = repo.forks_count || repo.forksCount || 0;
        if (forks >= 20) highlights.push(`🔱 ${forks}+ forks`);
        
        if (repo.homepage) highlights.push('🌐 Live demo');
        
        if (this.isRecentlyActive(repo.updated_at || repo.updatedAt)) {
            highlights.push('🔥 Activamente mantenido');
        }
        
        if (repo.topics && repo.topics.length >= 5) {
            highlights.push('🏷️ Bien documentado');
        }
        
        return highlights;
    }

    /**
     * Sección de actividad
     */
    generateActivitySection() {
        const activity = {
            totalCommits: this.estimateTotalCommits(),
            activeYears: this.calculateActiveYears(),
            mostActiveYear: this.findMostActiveYear(),
            recentActivity: this.analyzeRecentActivity(),
            consistency: this.calculateConsistency()
        };
        
        return activity;
    }

    /**
     * Estima total de commits (sin hacer requests adicionales)
     */
    estimateTotalCommits() {
        // Estimación conservadora: 10 commits por proyecto activo
        const activeProjects = this.repos.filter(r => 
            this.isRecentlyActive(r.updated_at || r.updatedAt)
        ).length;
        
        return activeProjects * 10 + this.repos.length * 5;
    }

    /**
     * Calcula años activos
     */
    calculateActiveYears() {
        const years = new Set();
        
        this.repos.forEach(repo => {
            const created = new Date(repo.created_at || repo.createdAt);
            const updated = new Date(repo.updated_at || repo.updatedAt);
            
            for (let y = created.getFullYear(); y <= updated.getFullYear(); y++) {
                years.add(y);
            }
        });
        
        return years.size;
    }

    /**
     * Encuentra año más activo
     */
    findMostActiveYear() {
        const yearCounts = {};
        
        this.repos.forEach(repo => {
            const year = new Date(repo.created_at || repo.createdAt).getFullYear();
            yearCounts[year] = (yearCounts[year] || 0) + 1;
        });
        
        let maxYear = null;
        let maxCount = 0;
        
        Object.entries(yearCounts).forEach(([year, count]) => {
            if (count > maxCount) {
                maxCount = count;
                maxYear = year;
            }
        });
        
        return { year: maxYear, projects: maxCount };
    }

    /**
     * Analiza actividad reciente
     */
    analyzeRecentActivity() {
        const now = new Date();
        const last30Days = this.repos.filter(r => {
            const updated = new Date(r.updated_at || r.updatedAt);
            const diffDays = (now - updated) / (1000 * 60 * 60 * 24);
            return diffDays <= 30;
        }).length;
        
        const last90Days = this.repos.filter(r => {
            const updated = new Date(r.updated_at || r.updatedAt);
            const diffDays = (now - updated) / (1000 * 60 * 60 * 24);
            return diffDays <= 90;
        }).length;
        
        return {
            last30Days,
            last90Days,
            status: last30Days > 0 ? 'Active' : (last90Days > 0 ? 'Moderate' : 'Low')
        };
    }

    /**
     * Calcula consistencia de actividad
     */
    calculateConsistency() {
        const yearCounts = {};
        
        this.repos.forEach(repo => {
            const year = new Date(repo.created_at || repo.createdAt).getFullYear();
            yearCounts[year] = (yearCounts[year] || 0) + 1;
        });
        
        const years = Object.keys(yearCounts).length;
        if (years === 0) return 0;
        
        const avgPerYear = this.repos.length / years;
        const variance = Object.values(yearCounts).reduce((sum, count) => {
            return sum + Math.pow(count - avgPerYear, 2);
        }, 0) / years;
        
        // Score de consistencia (0-100)
        const consistencyScore = Math.max(0, 100 - (variance / avgPerYear) * 10);
        
        return Math.round(consistencyScore);
    }

    /**
     * Sección de habilidades derivadas
     */
    generateSkillsSection() {
        return {
            technical: this.deriveTechnicalSkills(),
            soft: this.deriveSoftSkills(),
            domain: this.deriveDomainExpertise()
        };
    }

    /**
     * Deriva habilidades técnicas
     */
    deriveTechnicalSkills() {
        const skills = [];
        const techStats = this.detector.getStatistics(this.repos);
        
        // Habilidades por categoría de tecnología
        const categories = this.detector.groupByCategory(techStats.technologies);
        
        if (categories['Web Frameworks']) {
            skills.push('Desarrollo Web Full-Stack');
        }
        if (categories['Mobile Frameworks']) {
            skills.push('Desarrollo Mobile Cross-Platform');
        }
        if (categories['Databases']) {
            skills.push('Diseño y Gestión de Bases de Datos');
        }
        if (categories['DevOps & Tools']) {
            skills.push('DevOps & CI/CD');
        }
        if (categories['AI & Data Science']) {
            skills.push('Machine Learning & Data Science');
        }
        if (categories['Testing & QA']) {
            skills.push('Testing & Quality Assurance');
        }
        
        // Habilidades por patrones de proyectos
        const hasAPIs = this.repos.some(r => 
            (r.topics || []).some(t => t.includes('api') || t.includes('rest'))
        );
        if (hasAPIs) skills.push('Diseño e Implementación de APIs RESTful');
        
        const hasDocker = this.repos.some(r => 
            (r.topics || []).includes('docker')
        );
        if (hasDocker) skills.push('Containerización con Docker');
        
        return skills;
    }

    /**
     * Deriva habilidades blandas
     */
    deriveSoftSkills() {
        const skills = [];
        
        // Colaboración
        const totalForks = this.repos.reduce((sum, r) => 
            sum + (r.forks_count || r.forksCount || 0), 0
        );
        if (totalForks > 10) {
            skills.push('Colaboración en Proyectos Open Source');
        }
        
        // Documentación
        const wellDocumented = this.repos.filter(r => 
            r.description && (r.topics || []).length >= 3
        ).length;
        if (wellDocumented > this.repos.length * 0.3) {
            skills.push('Documentación Técnica');
        }
        
        // Consistencia
        const consistency = this.calculateConsistency();
        if (consistency > 70) {
            skills.push('Gestión del Tiempo y Consistencia');
        }
        
        // Mantenimiento
        const maintained = this.repos.filter(r => 
            this.isRecentlyActive(r.updated_at || r.updatedAt)
        ).length;
        if (maintained > 5) {
            skills.push('Mantenimiento de Proyectos a Largo Plazo');
        }
        
        return skills;
    }

    /**
     * Deriva expertise de dominio
     */
    deriveDomainExpertise() {
        const domains = {};
        
        this.repos.forEach(repo => {
            const topics = repo.topics || [];
            
            // Mapeo de topics a dominios
            const domainMappings = {
                'web': 'Web Development',
                'frontend': 'Frontend Development',
                'backend': 'Backend Development',
                'mobile': 'Mobile Development',
                'api': 'API Development',
                'machine-learning': 'Machine Learning',
                'data-science': 'Data Science',
                'devops': 'DevOps',
                'cloud': 'Cloud Computing',
                'game': 'Game Development',
                'iot': 'Internet of Things',
                'blockchain': 'Blockchain',
                'security': 'Cybersecurity'
            };
            
            topics.forEach(topic => {
                const domain = domainMappings[topic.toLowerCase()];
                if (domain) {
                    domains[domain] = (domains[domain] || 0) + 1;
                }
            });
        });
        
        // Convertir a array y ordenar
        return Object.entries(domains)
            .map(([domain, count]) => ({
                domain,
                projectCount: count,
                proficiency: this.calculateProficiency(count, this.repos.length)
            }))
            .sort((a, b) => b.projectCount - a.projectCount)
            .slice(0, 5);
    }

    /**
     * Estadísticas generales
     */
    generateStatistics() {
        return {
            totalRepositories: this.repos.length,
            publicRepositories: this.repos.filter(r => !r.private && !r.isPrivate).length,
            privateRepositories: this.repos.filter(r => r.private || r.isPrivate).length,
            forkedRepositories: this.repos.filter(r => r.fork).length,
            originalRepositories: this.repos.filter(r => !r.fork).length,
            totalStars: this.repos.reduce((sum, r) => sum + (r.stargazers_count || r.stargazersCount || 0), 0),
            totalForks: this.repos.reduce((sum, r) => sum + (r.forks_count || r.forksCount || 0), 0),
            averageStarsPerRepo: Math.round(
                this.repos.reduce((sum, r) => sum + (r.stargazers_count || r.stargazersCount || 0), 0) / this.repos.length
            ),
            mostStarredRepo: this.findMostStarredRepo(),
            languageDistribution: this.calculateLanguageDistribution()
        };
    }

    /**
     * Encuentra repositorio más popular
     */
    findMostStarredRepo() {
        if (this.repos.length === 0) return null;
        
        return this.repos.reduce((max, repo) => {
            const stars = repo.stargazers_count || repo.stargazersCount || 0;
            const maxStars = max.stargazers_count || max.stargazersCount || 0;
            return stars > maxStars ? repo : max;
        });
    }

    /**
     * Calcula distribución de lenguajes
     */
    calculateLanguageDistribution() {
        const langCount = {};
        
        this.repos.forEach(repo => {
            const lang = repo.language || 'Unknown';
            langCount[lang] = (langCount[lang] || 0) + 1;
        });
        
        return Object.entries(langCount)
            .map(([language, count]) => ({
                language,
                count,
                percentage: Math.round((count / this.repos.length) * 100)
            }))
            .sort((a, b) => b.count - a.count);
    }

    /**
     * Deriva rol profesional del desarrollador
     */
    deriveRole() {
        const techStats = this.detector.getStatistics(this.repos);
        const categories = this.detector.groupByCategory(techStats.technologies);
        
        let frontendScore = 0;
        let backendScore = 0;
        let mobileScore = 0;
        let dataScore = 0;
        let devopsScore = 0;
        
        // Scoring por categorías
        if (categories['Web Frameworks']) {
            const webTechs = categories['Web Frameworks'].technologies;
            const frontendFrameworks = ['React', 'Vue.js', 'Angular', 'Svelte'];
            const backendFrameworks = ['Express', 'Django', 'Flask', 'FastAPI', 'Spring Boot'];
            
            webTechs.forEach(tech => {
                if (frontendFrameworks.includes(tech.name)) frontendScore += tech.count;
                if (backendFrameworks.includes(tech.name)) backendScore += tech.count;
            });
        }
        
        if (categories['Mobile Frameworks']) {
            mobileScore += categories['Mobile Frameworks'].technologies.reduce((sum, t) => sum + t.count, 0);
        }
        
        if (categories['AI & Data Science']) {
            dataScore += categories['AI & Data Science'].technologies.reduce((sum, t) => sum + t.count, 0);
        }
        
        if (categories['DevOps & Tools']) {
            devopsScore += categories['DevOps & Tools'].technologies.reduce((sum, t) => sum + t.count, 0);
        }
        
        // Determinar rol principal
        const scores = [
            { role: 'Frontend Developer', score: frontendScore },
            { role: 'Backend Developer', score: backendScore },
            { role: 'Mobile Developer', score: mobileScore },
            { role: 'Data Scientist', score: dataScore },
            { role: 'DevOps Engineer', score: devopsScore }
        ].sort((a, b) => b.score - a.score);
        
        // Si frontend y backend están balanceados
        if (frontendScore > 0 && backendScore > 0 && 
            Math.abs(frontendScore - backendScore) < Math.max(frontendScore, backendScore) * 0.3) {
            return 'Full Stack Developer';
        }
        
        return scores[0].score > 0 ? scores[0].role : 'Software Developer';
    }

    /**
     * Exporta CV a Markdown
     */
    exportToMarkdown() {
        const cv = this.generate();
        const header = cv.header;
        const summary = cv.summary;
        const techs = cv.technologies;
        const exp = cv.experience;
        const projects = cv.projects;
        
        let md = `# ${header.name}\n\n`;
        md += `**${summary.headline}**\n\n`;
        
        if (header.location) md += `📍 ${header.location} | `;
        if (header.email) md += `📧 ${header.email} | `;
        if (header.github) md += `💻 [GitHub](${header.github})`;
        md += `\n\n`;
        
        // Bio
        if (header.bio) {
            md += `${header.bio}\n\n`;
        }
        
        // Resumen
        md += `## 💼 Professional Summary\n\n`;
        md += `${summary.description}\n\n`;
        
        // Tech Stack
        md += `## 🚀 Technical Skills\n\n`;
        techs.byProficiency.expert.forEach(tech => {
            md += `- **${tech.icon} ${tech.name}** (Expert) - ${tech.count} projects, ${tech.percentage}%\n`;
        });
        techs.byProficiency.advanced.forEach(tech => {
            md += `- **${tech.icon} ${tech.name}** (Advanced) - ${tech.count} projects, ${tech.percentage}%\n`;
        });
        md += `\n`;
        
        // Experience
        if (exp.length > 0) {
            md += `## 📊 Experience\n\n`;
            exp.slice(0, 10).forEach(e => {
                md += `### ${e.icon} ${e.technology}\n`;
                md += `- **${e.yearsOfExperience} years** of experience | ${e.projectCount} projects | ${e.proficiency} proficiency\n`;
                md += `- Category: ${e.category}\n\n`;
            });
        }
        
        // Top Projects
        md += `## 🌟 Featured Projects\n\n`;
        projects.slice(0, 5).forEach(proj => {
            md += `### ${proj.name}\n`;
            md += `${proj.description}\n\n`;
            md += `**Technologies**: ${proj.technologies.map(t => t.name).join(', ')}\n\n`;
            if (proj.highlights.length > 0) {
                md += `${proj.highlights.join(' • ')}\n\n`;
            }
            md += `[View Project](${proj.url})`;
            if (proj.homepage) md += ` | [Live Demo](${proj.homepage})`;
            md += `\n\n`;
        });
        
        // Stats
        md += `## 📈 GitHub Statistics\n\n`;
        md += `- **${cv.statistics.totalRepositories}** total repositories\n`;
        md += `- **${cv.statistics.totalStars}** total stars\n`;
        md += `- **${cv.statistics.totalForks}** total forks\n`;
        md += `- **${summary.yearsActive}+** years active on GitHub\n\n`;
        
        md += `---\n\n`;
        md += `*CV generated by GitStellarPrism on ${new Date().toLocaleDateString()}*\n`;
        
        return md;
    }

    /**
     * Exporta CV a HTML
     */
    exportToHTML() {
        const cv = this.generate();
        // TODO: Implementar template HTML profesional
        return `<html><body><h1>CV de ${cv.header.name}</h1><p>HTML export en desarrollo...</p></body></html>`;
    }

    /**
     * Exporta CV a JSON
     */
    exportToJSON() {
        return JSON.stringify(this.generate(), null, 2);
    }
}

// Exportar como singleton global
window.CVGenerator = CVGenerator;
console.log('📄 CVGenerator cargado');

