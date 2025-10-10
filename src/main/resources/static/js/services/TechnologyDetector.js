/**
 * TechnologyDetector - Sistema inteligente de detección de tecnologías
 * Analiza lenguajes, archivos, dependencias y estructura del proyecto
 * para clasificar y jerarquizar tecnologías automáticamente
 */

class TechnologyDetector {
    constructor() {
        this.technologyHierarchy = this.buildTechnologyHierarchy();
        this.frameworkDetectors = this.buildFrameworkDetectors();
        this.databaseDetectors = this.buildDatabaseDetectors();
        this.toolDetectors = this.buildToolDetectors();
    }

    /**
     * Construye la jerarquía de tecnologías con categorías, subcategorías y tags
     */
    buildTechnologyHierarchy() {
        return {
            // ========== LENGUAJES DE PROGRAMACIÓN ==========
            'Programming Languages': {
                icon: '💻',
                priority: 1,
                technologies: {
                    'JavaScript': { icon: '🟨', category: 'Web', tags: ['frontend', 'backend', 'fullstack'] },
                    'TypeScript': { icon: '🔷', category: 'Web', tags: ['frontend', 'backend', 'type-safe'] },
                    'Python': { icon: '🐍', category: 'General Purpose', tags: ['backend', 'data-science', 'ai', 'scripting'] },
                    'Java': { icon: '☕', category: 'Enterprise', tags: ['backend', 'enterprise', 'oop'] },
                    'C++': { icon: '⚡', category: 'Systems', tags: ['performance', 'systems', 'gaming'] },
                    'C#': { icon: '🔷', category: 'Enterprise', tags: ['backend', '.net', 'enterprise'] },
                    'Go': { icon: '🐹', category: 'Systems', tags: ['backend', 'concurrent', 'performance'] },
                    'Rust': { icon: '🦀', category: 'Systems', tags: ['systems', 'memory-safe', 'performance'] },
                    'PHP': { icon: '🐘', category: 'Web', tags: ['backend', 'web'] },
                    'Ruby': { icon: '💎', category: 'Web', tags: ['backend', 'web'] },
                    'Swift': { icon: '🍎', category: 'Mobile', tags: ['mobile', 'ios'] },
                    'Kotlin': { icon: '🟣', category: 'Mobile', tags: ['mobile', 'android'] },
                    'Scala': { icon: '🔴', category: 'JVM', tags: ['backend', 'functional'] },
                    'R': { icon: '📊', category: 'Data Science', tags: ['data-science', 'statistics', 'analytics'] },
                    'Dart': { icon: '🎯', category: 'Mobile', tags: ['mobile', 'flutter'] },
                    'C': { icon: '⚙️', category: 'Systems', tags: ['systems', 'low-level', 'performance'] },
                    'D': { icon: '🔷', category: 'Systems', tags: ['systems', 'performance'] },
                    'Elixir': { icon: '💜', category: 'Functional', tags: ['backend', 'concurrent'] },
                    'Haskell': { icon: '🔷', category: 'Functional', tags: ['functional', 'academic'] },
                    'Perl': { icon: '🐪', category: 'Scripting', tags: ['scripting', 'text-processing'] },
                    'Lua': { icon: '🌙', category: 'Scripting', tags: ['scripting', 'embedded'] },
                    'Objective-C': { icon: '🍎', category: 'Mobile', tags: ['mobile', 'ios', 'legacy'] },
                    'F#': { icon: '🔷', category: 'Functional', tags: ['functional', '.net'] },
                    'Clojure': { icon: '🔷', category: 'Functional', tags: ['functional', 'jvm', 'lisp'] },
                    'Erlang': { icon: '📞', category: 'Functional', tags: ['concurrent', 'telecom'] },
                    'Assembly': { icon: '⚙️', category: 'Systems', tags: ['systems', 'low-level'] },
                    'Visual Basic': { icon: '🔷', category: 'Legacy', tags: ['legacy', 'windows'] },
                    'Groovy': { icon: '☕', category: 'JVM', tags: ['jvm', 'scripting'] },
                    'Julia': { icon: '🔢', category: 'Data Science', tags: ['data-science', 'numerical'] },
                    'MATLAB': { icon: '📊', category: 'Data Science', tags: ['data-science', 'numerical'] },
                    'Fortran': { icon: '🔢', category: 'Scientific', tags: ['scientific', 'legacy'] },
                    'COBOL': { icon: '🏢', category: 'Legacy', tags: ['legacy', 'enterprise'] },
                    'Solidity': { icon: '⛓️', category: 'Blockchain', tags: ['blockchain', 'smart-contracts'] },
                    'Zig': { icon: '⚡', category: 'Systems', tags: ['systems', 'performance'] },
                    'Nim': { icon: '👑', category: 'Systems', tags: ['systems', 'performance'] }
                }
            },

            // ========== FRAMEWORKS WEB ==========
            'Web Frameworks': {
                icon: '🌐',
                priority: 2,
                technologies: {
                    'React': { icon: '⚛️', language: 'JavaScript', tags: ['frontend', 'spa', 'ui'] },
                    'Vue.js': { icon: '💚', language: 'JavaScript', tags: ['frontend', 'spa', 'ui'] },
                    'Angular': { icon: '🅰️', language: 'TypeScript', tags: ['frontend', 'spa', 'enterprise'] },
                    'Svelte': { icon: '🧡', language: 'JavaScript', tags: ['frontend', 'spa', 'compiler'] },
                    'Next.js': { icon: '▲', language: 'JavaScript', tags: ['frontend', 'ssr', 'fullstack'] },
                    'Nuxt.js': { icon: '💚', language: 'JavaScript', tags: ['frontend', 'ssr', 'vue'] },
                    'Express': { icon: '🚂', language: 'JavaScript', tags: ['backend', 'api', 'nodejs'] },
                    'Fastify': { icon: '⚡', language: 'JavaScript', tags: ['backend', 'api', 'performance'] },
                    'NestJS': { icon: '🐱', language: 'TypeScript', tags: ['backend', 'api', 'enterprise'] },
                    'Django': { icon: '🎸', language: 'Python', tags: ['backend', 'fullstack', 'orm'] },
                    'Flask': { icon: '🧪', language: 'Python', tags: ['backend', 'api', 'microservice'] },
                    'FastAPI': { icon: '⚡', language: 'Python', tags: ['backend', 'api', 'async'] },
                    'Spring Boot': { icon: '🍃', language: 'Java', tags: ['backend', 'enterprise', 'microservice'] },
                    'Laravel': { icon: '🔺', language: 'PHP', tags: ['backend', 'fullstack', 'orm'] },
                    'Ruby on Rails': { icon: '💎', language: 'Ruby', tags: ['backend', 'fullstack', 'orm'] },
                    'ASP.NET Core': { icon: '🔷', language: 'C#', tags: ['backend', 'enterprise'] },
                    'Phoenix': { icon: '🔥', language: 'Elixir', tags: ['backend', 'realtime'] }
                }
            },

            // ========== FRAMEWORKS MOBILE ==========
            'Mobile Frameworks': {
                icon: '📱',
                priority: 3,
                technologies: {
                    'React Native': { icon: '⚛️', language: 'JavaScript', tags: ['mobile', 'cross-platform'] },
                    'Flutter': { icon: '🎯', language: 'Dart', tags: ['mobile', 'cross-platform'] },
                    'Ionic': { icon: '⚡', language: 'JavaScript', tags: ['mobile', 'hybrid'] },
                    'SwiftUI': { icon: '🍎', language: 'Swift', tags: ['mobile', 'ios', 'native'] },
                    'Jetpack Compose': { icon: '🤖', language: 'Kotlin', tags: ['mobile', 'android', 'native'] }
                }
            },

            // ========== BASES DE DATOS ==========
            'Databases': {
                icon: '🗄️',
                priority: 4,
                technologies: {
                    'PostgreSQL': { icon: '🐘', category: 'Relational', tags: ['sql', 'relational', 'acid'] },
                    'MySQL': { icon: '🐬', category: 'Relational', tags: ['sql', 'relational'] },
                    'MongoDB': { icon: '🍃', category: 'NoSQL', tags: ['nosql', 'document', 'json'] },
                    'Redis': { icon: '🔴', category: 'Cache', tags: ['cache', 'nosql', 'in-memory'] },
                    'SQLite': { icon: '📦', category: 'Embedded', tags: ['sql', 'embedded', 'lightweight'] },
                    'Elasticsearch': { icon: '🔍', category: 'Search', tags: ['search', 'nosql', 'analytics'] },
                    'Cassandra': { icon: '🔷', category: 'NoSQL', tags: ['nosql', 'distributed', 'wide-column'] },
                    'Neo4j': { icon: '🕸️', category: 'Graph', tags: ['graph', 'nosql', 'relationships'] },
                    'DynamoDB': { icon: '☁️', category: 'NoSQL', tags: ['nosql', 'aws', 'serverless'] },
                    'Firebase': { icon: '🔥', category: 'BaaS', tags: ['nosql', 'realtime', 'cloud'] },
                    'Supabase': { icon: '⚡', category: 'BaaS', tags: ['sql', 'realtime', 'open-source'] }
                }
            },

            // ========== HERRAMIENTAS & DEVOPS ==========
            'DevOps & Tools': {
                icon: '🛠️',
                priority: 5,
                technologies: {
                    'Docker': { icon: '🐳', category: 'Container', tags: ['container', 'deployment', 'devops'] },
                    'Kubernetes': { icon: '☸️', category: 'Orchestration', tags: ['orchestration', 'container', 'cloud'] },
                    'GitHub Actions': { icon: '⚙️', category: 'CI/CD', tags: ['ci-cd', 'automation', 'github'] },
                    'Jenkins': { icon: '👷', category: 'CI/CD', tags: ['ci-cd', 'automation'] },
                    'Terraform': { icon: '🌍', category: 'IaC', tags: ['infrastructure', 'cloud', 'automation'] },
                    'Ansible': { icon: '🔴', category: 'Configuration', tags: ['configuration', 'automation'] },
                    'Nginx': { icon: '🟢', category: 'Web Server', tags: ['web-server', 'reverse-proxy'] },
                    'Apache': { icon: '🪶', category: 'Web Server', tags: ['web-server'] },
                    'Git': { icon: '📊', category: 'Version Control', tags: ['vcs', 'collaboration'] },
                    'Webpack': { icon: '📦', category: 'Build Tool', tags: ['bundler', 'build', 'javascript'] },
                    'Vite': { icon: '⚡', category: 'Build Tool', tags: ['bundler', 'build', 'fast'] },
                    'Gradle': { icon: '🐘', category: 'Build Tool', tags: ['build', 'java'] },
                    'Maven': { icon: '📦', category: 'Build Tool', tags: ['build', 'java', 'dependency'] }
                }
            },

            // ========== CLOUD & INFRASTRUCTURE ==========
            'Cloud Platforms': {
                icon: '☁️',
                priority: 6,
                technologies: {
                    'AWS': { icon: '☁️', tags: ['cloud', 'infrastructure', 'paas'] },
                    'Azure': { icon: '☁️', tags: ['cloud', 'infrastructure', 'microsoft'] },
                    'Google Cloud': { icon: '☁️', tags: ['cloud', 'infrastructure', 'google'] },
                    'Heroku': { icon: '💜', tags: ['cloud', 'paas', 'deployment'] },
                    'Vercel': { icon: '▲', tags: ['cloud', 'frontend', 'serverless'] },
                    'Netlify': { icon: '🌐', tags: ['cloud', 'frontend', 'jamstack'] },
                    'DigitalOcean': { icon: '🌊', tags: ['cloud', 'infrastructure', 'vps'] }
                }
            },

            // ========== TESTING & QUALITY ==========
            'Testing & QA': {
                icon: '🧪',
                priority: 7,
                technologies: {
                    'Jest': { icon: '🃏', language: 'JavaScript', tags: ['testing', 'unit-test'] },
                    'Cypress': { icon: '🌲', language: 'JavaScript', tags: ['testing', 'e2e'] },
                    'Selenium': { icon: '🔍', tags: ['testing', 'e2e', 'browser'] },
                    'JUnit': { icon: '☕', language: 'Java', tags: ['testing', 'unit-test'] },
                    'PyTest': { icon: '🐍', language: 'Python', tags: ['testing', 'unit-test'] },
                    'Mocha': { icon: '☕', language: 'JavaScript', tags: ['testing', 'unit-test'] },
                    'Vitest': { icon: '⚡', language: 'JavaScript', tags: ['testing', 'unit-test', 'fast'] }
                }
            },

            // ========== AI & DATA SCIENCE ==========
            'AI & Data Science': {
                icon: '🤖',
                priority: 8,
                technologies: {
                    'TensorFlow': { icon: '🧠', language: 'Python', tags: ['ai', 'ml', 'deep-learning'] },
                    'PyTorch': { icon: '🔥', language: 'Python', tags: ['ai', 'ml', 'deep-learning'] },
                    'scikit-learn': { icon: '📊', language: 'Python', tags: ['ml', 'data-science'] },
                    'Pandas': { icon: '🐼', language: 'Python', tags: ['data-science', 'data-analysis'] },
                    'NumPy': { icon: '🔢', language: 'Python', tags: ['data-science', 'numerical'] },
                    'Jupyter': { icon: '📓', language: 'Python', tags: ['notebook', 'data-science'] },
                    'OpenAI': { icon: '🤖', tags: ['ai', 'api', 'nlp'] },
                    'Hugging Face': { icon: '🤗', tags: ['ai', 'nlp', 'transformers'] }
                }
            }
        };
    }

    /**
     * Detectores de frameworks basados en archivos específicos
     */
    buildFrameworkDetectors() {
        return {
            'Spring Boot': {
                files: ['pom.xml', 'build.gradle'],
                content: ['spring-boot', 'org.springframework.boot'],
                confidence: 0.95
            },
            'React': {
                files: ['package.json'],
                content: ['react', '"react":', '"@types/react"'],
                confidence: 0.9
            },
            'Vue.js': {
                files: ['package.json', 'vue.config.js'],
                content: ['vue', '"vue":', 'Vue.'],
                confidence: 0.9
            },
            'Angular': {
                files: ['package.json', 'angular.json'],
                content: ['@angular/', 'angular.json'],
                confidence: 0.95
            },
            'Next.js': {
                files: ['package.json', 'next.config.js'],
                content: ['next', '"next":'],
                confidence: 0.9
            },
            'Django': {
                files: ['requirements.txt', 'setup.py', 'manage.py'],
                content: ['django', 'Django'],
                confidence: 0.9
            },
            'Flask': {
                files: ['requirements.txt', 'setup.py'],
                content: ['flask', 'Flask'],
                confidence: 0.85
            },
            'FastAPI': {
                files: ['requirements.txt', 'setup.py'],
                content: ['fastapi', 'FastAPI'],
                confidence: 0.9
            },
            'Express': {
                files: ['package.json'],
                content: ['express', '"express":'],
                confidence: 0.85
            },
            'NestJS': {
                files: ['package.json', 'nest-cli.json'],
                content: ['@nestjs/', 'nestjs'],
                confidence: 0.95
            },
            'Laravel': {
                files: ['composer.json', 'artisan'],
                content: ['laravel/framework', 'Laravel'],
                confidence: 0.95
            }
        };
    }

    /**
     * Detectores de bases de datos
     */
    buildDatabaseDetectors() {
        return {
            'PostgreSQL': {
                files: ['package.json', 'requirements.txt', 'pom.xml'],
                content: ['pg', 'postgresql', 'psycopg2'],
                confidence: 0.85
            },
            'MySQL': {
                files: ['package.json', 'requirements.txt', 'pom.xml'],
                content: ['mysql', 'mysql2', 'pymysql'],
                confidence: 0.85
            },
            'MongoDB': {
                files: ['package.json', 'requirements.txt', 'pom.xml'],
                content: ['mongodb', 'mongoose', 'pymongo'],
                confidence: 0.85
            },
            'Redis': {
                files: ['package.json', 'requirements.txt', 'pom.xml'],
                content: ['redis', 'ioredis'],
                confidence: 0.85
            },
            'SQLite': {
                files: ['*.db', '*.sqlite', '*.sqlite3'],
                content: ['sqlite', 'sqlite3'],
                confidence: 0.8
            }
        };
    }

    /**
     * Detectores de herramientas
     */
    buildToolDetectors() {
        return {
            'Docker': {
                files: ['Dockerfile', 'docker-compose.yml', '.dockerignore'],
                confidence: 0.95
            },
            'Kubernetes': {
                files: ['*.yaml', '*.yml'],
                content: ['kind: Deployment', 'kind: Service', 'apiVersion: apps/v1'],
                confidence: 0.9
            },
            'GitHub Actions': {
                files: ['.github/workflows/*.yml'],
                confidence: 0.95
            },
            'Terraform': {
                files: ['*.tf', 'terraform.tfstate'],
                confidence: 0.95
            },
            'Webpack': {
                files: ['webpack.config.js', 'webpack.config.ts'],
                content: ['webpack'],
                confidence: 0.9
            },
            'Vite': {
                files: ['vite.config.js', 'vite.config.ts'],
                content: ['vite'],
                confidence: 0.95
            },
            'Maven': {
                files: ['pom.xml'],
                confidence: 0.95
            },
            'Gradle': {
                files: ['build.gradle', 'build.gradle.kts'],
                confidence: 0.95
            }
        };
    }

    /**
     * Analiza un repositorio y detecta todas las tecnologías
     * @param {Object} repo - Repositorio con lenguajes, topics, descripción, etc.
     * @returns {Array} - Array de tecnologías detectadas con metadata
     */
    analyzeRepository(repo) {
        const detectedTechnologies = new Map();

        // 1. Detectar lenguajes de programación (GitHub API con filtrado por significancia)
        const languages = repo.languages || repo.languageDistribution || {};
        const significantLanguages = this.filterSignificantLanguages(languages);
        
        significantLanguages.forEach(({ name, percentage }) => {
            // Solo añadir lenguajes válidos
            if (this.isValidLanguage(name)) {
                this.addDetectedTechnology(
                    detectedTechnologies, 
                    name, 
                    'Programming Languages', 
                    0.95, 
                    'language',
                    { percentage }
                );
            }
        });

        // 2. Detectar desde topics de GitHub (baja prioridad, se generalizarán)
        const topics = repo.topics || [];
        topics.forEach(topic => {
            this.mapTopicToTechnology(detectedTechnologies, topic);
        });

        // 3. Detectar desde descripción
        if (repo.description) {
            this.detectFromDescription(detectedTechnologies, repo.description);
        }

        // 4. TODO: Detectar desde estructura de archivos (requiere API de árbol)
        // Esta parte se implementará cuando tengamos acceso al árbol de archivos

        // 5. Enriquecer con información jerárquica
        return this.enrichDetectedTechnologies(detectedTechnologies);
    }
    
    /**
     * Filtra lenguajes por significancia (porcentaje de código)
     * @param {Object} languages - Mapa de lenguajes y bytes
     * @returns {Array} - Array de lenguajes significativos con porcentaje
     */
    filterSignificantLanguages(languages) {
        if (!languages || Object.keys(languages).length === 0) {
            return [];
        }
        
        // Calcular total de bytes
        const totalBytes = Object.values(languages).reduce((sum, bytes) => sum + bytes, 0);
        if (totalBytes === 0) return [];
        
        // Convertir a array con porcentajes
        const languagesWithPercentage = Object.entries(languages).map(([name, bytes]) => ({
            name,
            bytes,
            percentage: (bytes / totalBytes) * 100
        }));
        
        // Ordenar por bytes (descendente)
        languagesWithPercentage.sort((a, b) => b.bytes - a.bytes);
        
        // Estrategia de filtrado:
        // 1. Siempre incluir el lenguaje principal (top 1)
        // 2. Incluir lenguajes con >= 5% de código
        // 3. Máximo 5 lenguajes (evita repositorios con muchos lenguajes menores)
        
        const significant = [];
        for (let i = 0; i < languagesWithPercentage.length && i < 5; i++) {
            const lang = languagesWithPercentage[i];
            // Primera lenguaje siempre, luego solo si >= 5%
            if (i === 0 || lang.percentage >= 5.0) {
                significant.push(lang);
            }
        }
        
        return significant;
    }
    
    /**
     * Valida si un nombre es un lenguaje de programación válido
     * @param {string} name - Nombre del lenguaje
     * @returns {boolean} - True si es válido
     */
    isValidLanguage(name) {
        if (!name || name.trim().length === 0) return false;
        
        // Lenguajes de 1 carácter solo si son conocidos
        if (name.length === 1) {
            return this.isKnownSingleCharLanguage(name);
        }
        
        // Lista de "lenguajes" que NO son lenguajes de programación reales
        const nonProgrammingLanguages = [
            'Procfile',      // Heroku config
            'Dockerfile',    // Docker config
            'Makefile',      // Build tool
            'Batchfile',     // Windows batch
            'PowerShell',    // Scripting (puede ser válido, pero a menudo config)
            'YAML',          // Config
            'JSON',          // Data
            'XML',           // Markup/Config
            'Markdown',      // Documentation
            'Text',          // Plain text
            'INI',           // Config
            'TOML',          // Config
            'Ignore List',   // .gitignore
            'EditorConfig',  // Editor config
        ];
        
        // Si está en la lista negra, rechazar
        if (nonProgrammingLanguages.includes(name)) {
            return false;
        }
        
        // Si existe en nuestra jerarquía, es válido
        if (this.existsInHierarchy(name)) {
            return true;
        }
        
        // Para lenguajes no conocidos pero de GitHub API, usar heurística:
        // Aceptar si tiene más de 2 caracteres y no contiene caracteres especiales raros
        return name.length > 2 && /^[a-zA-Z0-9#+\-\s]+$/.test(name);
    }
    
    /**
     * Verifica si una tecnología existe en la jerarquía
     * @param {string} techName - Nombre de la tecnología
     * @returns {boolean} - True si existe
     */
    existsInHierarchy(techName) {
        for (const categoryData of Object.values(this.technologyHierarchy)) {
            if (categoryData.technologies && categoryData.technologies[techName]) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verifica si un lenguaje de un solo carácter es conocido (ej: C, R, Go)
     */
    isKnownSingleCharLanguage(lang) {
        const knownSingleChar = ['C', 'R', 'Go', 'D'];
        return knownSingleChar.includes(lang);
    }

    /**
     * Mapea un topic de GitHub a tecnologías conocidas
     */
    mapTopicToTechnology(detectedTechnologies, topic) {
        if (!topic || topic.trim().length === 0) return;
        
        const topicLower = topic.toLowerCase().trim();
        
        // Ignorar topics muy cortos que no sean tecnologías conocidas (evita false positives como "r", "js", etc.)
        if (topicLower.length === 1 && !['c', 'r', 'd'].includes(topicLower)) {
            return;
        }
        
        // Mapeos directos comunes
        const topicMappings = {
            'react': 'React',
            'reactjs': 'React',
            'vue': 'Vue.js',
            'vuejs': 'Vue.js',
            'angular': 'Angular',
            'nextjs': 'Next.js',
            'django': 'Django',
            'flask': 'Flask',
            'fastapi': 'FastAPI',
            'express': 'Express',
            'expressjs': 'Express',
            'nestjs': 'NestJS',
            'spring-boot': 'Spring Boot',
            'springboot': 'Spring Boot',
            'spring': 'Spring Boot',
            'laravel': 'Laravel',
            'postgresql': 'PostgreSQL',
            'postgres': 'PostgreSQL',
            'mysql': 'MySQL',
            'mongodb': 'MongoDB',
            'mongo': 'MongoDB',
            'redis': 'Redis',
            'docker': 'Docker',
            'kubernetes': 'Kubernetes',
            'k8s': 'Kubernetes',
            'aws': 'AWS',
            'amazon-web-services': 'AWS',
            'azure': 'Azure',
            'gcp': 'Google Cloud',
            'google-cloud': 'Google Cloud',
            'tensorflow': 'TensorFlow',
            'pytorch': 'PyTorch',
            'machine-learning': 'AI & Data Science',
            'ml': 'AI & Data Science',
            'ai': 'AI & Data Science',
            'artificial-intelligence': 'AI & Data Science',
            'nodejs': 'Express',
            'node': 'Express',
            'typescript': 'TypeScript',
            'javascript': 'JavaScript',
            'python': 'Python',
            'java': 'Java',
            'cpp': 'C++',
            'csharp': 'C#',
            'rust': 'Rust',
            'golang': 'Go',
            'go': 'Go',
            'php': 'PHP',
            'ruby': 'Ruby',
            'swift': 'Swift',
            'kotlin': 'Kotlin',
            'r-lang': 'R',
            'rstats': 'R',
            'r-language': 'R',
            'c-language': 'C',
            'clang': 'C',
            // Frameworks mobile
            'react-native': 'React Native',
            'flutter': 'Flutter',
            'ionic': 'Ionic',
            // Testing
            'jest': 'Jest',
            'cypress': 'Cypress',
            'selenium': 'Selenium',
            'testing': 'Testing & QA',
            // Cloud
            'heroku': 'Heroku',
            'vercel': 'Vercel',
            'netlify': 'Netlify',
            'digitalocean': 'DigitalOcean'
        };

        const mappedTech = topicMappings[topicLower];
        if (mappedTech) {
            this.addDetectedTechnology(detectedTechnologies, mappedTech, null, 0.7, 'topic');
        }
    }

    /**
     * Detecta tecnologías desde la descripción del repositorio
     */
    detectFromDescription(detectedTechnologies, description) {
        const descLower = description.toLowerCase();
        
        // Buscar menciones de tecnologías conocidas
        for (const [categoryName, categoryData] of Object.entries(this.technologyHierarchy)) {
            for (const [techName, techData] of Object.entries(categoryData.technologies)) {
                if (descLower.includes(techName.toLowerCase())) {
                    this.addDetectedTechnology(detectedTechnologies, techName, categoryName, 0.6, 'description');
                }
            }
        }
    }

    /**
     * Añade una tecnología detectada al mapa
     */
    addDetectedTechnology(detectedTechnologies, techName, categoryHint, confidence, source, metadata = {}) {
        // Validar nombre de tecnología
        if (!techName || techName.trim().length === 0) return;
        
        // Normalizar nombre (trim)
        techName = techName.trim();
        
        // Si ya existe, actualizar confianza (tomar la máxima)
        if (detectedTechnologies.has(techName)) {
            const existing = detectedTechnologies.get(techName);
            existing.confidence = Math.max(existing.confidence, confidence);
            existing.sources.push(source);
            // Actualizar metadata si hay porcentaje
            if (metadata.percentage && !existing.percentage) {
                existing.percentage = metadata.percentage;
            }
            return;
        }

        // Buscar información de la tecnología en la jerarquía
        const techInfo = this.findTechnologyInfo(techName, categoryHint);
        
        // Solo añadir si existe en la jerarquía O es un lenguaje de GitHub API
        if (techInfo.category !== 'Other' || source === 'language') {
            detectedTechnologies.set(techName, {
                name: techName,
                category: techInfo.category,
                categoryIcon: techInfo.categoryIcon,
                icon: techInfo.icon,
                tags: techInfo.tags,
                priority: techInfo.priority,
                confidence,
                sources: [source],
                percentage: metadata.percentage || null
            });
        }
    }

    /**
     * Busca información de una tecnología en la jerarquía
     */
    findTechnologyInfo(techName, categoryHint) {
        // Buscar primero en la categoría sugerida
        if (categoryHint && this.technologyHierarchy[categoryHint]) {
            const tech = this.technologyHierarchy[categoryHint].technologies[techName];
            if (tech) {
                return {
                    category: categoryHint,
                    categoryIcon: this.technologyHierarchy[categoryHint].icon,
                    icon: tech.icon,
                    tags: tech.tags || [],
                    priority: this.technologyHierarchy[categoryHint].priority
                };
            }
        }

        // Buscar en todas las categorías
        for (const [categoryName, categoryData] of Object.entries(this.technologyHierarchy)) {
            if (categoryData.technologies[techName]) {
                const tech = categoryData.technologies[techName];
                return {
                    category: categoryName,
                    categoryIcon: categoryData.icon,
                    icon: tech.icon,
                    tags: tech.tags || [],
                    priority: categoryData.priority
                };
            }
        }

        // Si no se encuentra, retornar valores por defecto
        return {
            category: 'Other',
            categoryIcon: '🏷️',
            icon: '🔧',
            tags: [],
            priority: 99
        };
    }

    /**
     * Enriquece las tecnologías detectadas con información adicional
     */
    enrichDetectedTechnologies(detectedTechnologies) {
        const technologies = Array.from(detectedTechnologies.values());
        
        // Ordenar por prioridad y confianza
        technologies.sort((a, b) => {
            if (a.priority !== b.priority) return a.priority - b.priority;
            return b.confidence - a.confidence;
        });

        return technologies;
    }

    /**
     * Agrupa tecnologías por categoría para visualización jerárquica
     */
    groupByCategory(technologies) {
        const grouped = {};
        
        technologies.forEach(tech => {
            if (!grouped[tech.category]) {
                grouped[tech.category] = {
                    icon: tech.categoryIcon,
                    priority: tech.priority,
                    technologies: []
                };
            }
            grouped[tech.category].technologies.push(tech);
        });

        return grouped;
    }

    /**
     * Filtra tecnologías por tags
     */
    filterByTags(technologies, tags) {
        return technologies.filter(tech => 
            tech.tags.some(tag => tags.includes(tag))
        );
    }

    /**
     * Obtiene estadísticas de tecnologías en múltiples repositorios
     */
    getStatistics(repositories) {
        const allTechs = new Map();
        const categoryStats = {};
        
        repositories.forEach(repo => {
            const techs = this.analyzeRepository(repo);
            techs.forEach(tech => {
                // Contar ocurrencias
                if (!allTechs.has(tech.name)) {
                    allTechs.set(tech.name, { ...tech, count: 0, repos: [] });
                }
                const techData = allTechs.get(tech.name);
                techData.count++;
                techData.repos.push(repo.full_name || `${repo.owner}/${repo.name}`);
                
                // Estadísticas por categoría
                if (!categoryStats[tech.category]) {
                    categoryStats[tech.category] = { count: 0, technologies: new Set() };
                }
                categoryStats[tech.category].count++;
                categoryStats[tech.category].technologies.add(tech.name);
            });
        });

        return {
            technologies: Array.from(allTechs.values()).sort((a, b) => b.count - a.count),
            categories: categoryStats,
            totalUniqueTechnologies: allTechs.size,
            totalRepositories: repositories.length
        };
    }
}

// Exportar como singleton
window.TechnologyDetector = new TechnologyDetector();
console.log('🔍 TechnologyDetector cargado');

