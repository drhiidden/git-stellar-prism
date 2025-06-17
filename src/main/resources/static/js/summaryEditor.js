/**
 * Lógica para la página de análisis de tecnologías y generación de resúmenes
 */

// Variables globales
let languagesChart = null;

document.addEventListener('DOMContentLoaded', () => {
    // Manejar envío del formulario de análisis
    const repoAnalysisForm = document.getElementById('repoAnalysisForm');
    if (repoAnalysisForm) {
        repoAnalysisForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const repoUrl = document.getElementById('repoUrlAnalysis').value;
            if (repoUrl) {
                analyzeRepository(repoUrl);
            }
        });
    }

    // Manejar botones de edición y exportación
    const btnEditResumen = document.getElementById('btnEditResumen');
    if (btnEditResumen) {
        btnEditResumen.addEventListener('click', toggleSummaryEditMode);
    }
    
    const btnCancelEdit = document.getElementById('btnCancelEdit');
    if (btnCancelEdit) {
        btnCancelEdit.addEventListener('click', toggleSummaryEditMode);
    }
    
    const editSummaryForm = document.getElementById('editSummaryForm');
    if (editSummaryForm) {
        editSummaryForm.addEventListener('submit', saveSummaryChanges);
    }
});

/**
 * Realiza una petición al backend para analizar un repositorio
 */
function analyzeRepository(repoUrl) {
    const loadingIndicator = document.getElementById('analysisLoadingIndicator');
    const analysisResults = document.getElementById('analysisResults');

    // Mostrar indicador de carga y ocultar resultados anteriores
    loadingIndicator.classList.remove('d-none');
    analysisResults.classList.add('d-none');

    // Realizar la petición
    fetch(`/api/analysis/analyze?repo=${encodeURIComponent(repoUrl)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al analizar el repositorio');
            }
            return response.json();
        })
        .then(data => {
            loadingIndicator.classList.add('d-none');
            analysisResults.classList.remove('d-none');
            displayAnalysisResults(data);
        })
        .catch(error => {
            console.error('Error:', error);
            loadingIndicator.classList.add('d-none');
            alert('Error al analizar el repositorio. Verifique la URL e inténtelo de nuevo.');
        });
}

/**
 * Muestra los resultados del análisis en la página
 */
function displayAnalysisResults(data) {
    // 1. Mostrar distribución de lenguajes
    if (data.languageDistribution) {
        renderLanguagesChart(data.languageDistribution);
    }

    // 2. Mostrar tecnologías detectadas
    if (data.technologies) {
        renderTechnologies(data.technologies);
    }

    // 3. Mostrar estructura del proyecto
    if (data.projectStructure) {
        renderProjectStructure(data.projectStructure);
    }

    // 4. Poblar el resumen técnico
    if (data.technicalSummary) {
        populateTechnicalSummary(data.technicalSummary);
    }
}

/**
 * Renderiza el gráfico de distribución de lenguajes
 */
function renderLanguagesChart(languageData) {
    const ctx = document.getElementById('languagesChart').getContext('2d');
    
    if (languagesChart) {
        languagesChart.destroy();
    }
    
    languagesChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: Object.keys(languageData),
            datasets: [{
                label: 'Distribución de Lenguajes',
                data: Object.values(languageData),
                backgroundColor: [
                    'rgba(255, 99, 132, 0.7)',
                    'rgba(54, 162, 235, 0.7)',
                    'rgba(255, 206, 86, 0.7)',
                    'rgba(75, 192, 192, 0.7)',
                    'rgba(153, 102, 255, 0.7)',
                    'rgba(255, 159, 64, 0.7)'
                ],
                borderColor: [
                    'rgba(255, 99, 132, 1)',
                    'rgba(54, 162, 235, 1)',
                    'rgba(255, 206, 86, 1)',
                    'rgba(75, 192, 192, 1)',
                    'rgba(153, 102, 255, 1)',
                    'rgba(255, 159, 64, 1)'
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                },
                title: {
                    display: true,
                    text: 'Distribución de Lenguajes'
                }
            }
        }
    });
}

/**
 * Renderiza la lista de tecnologías detectadas
 */
function renderTechnologies(technologies) {
    const list = document.getElementById('technologiesList');
    list.innerHTML = ''; // Limpiar lista anterior

    if (technologies.length === 0) {
        list.innerHTML = '<p class="text-muted">No se detectaron tecnologías específicas.</p>';
        return;
    }

    technologies.forEach(tech => {
        const badge = document.createElement('span');
        badge.className = `tech-badge tech-${tech.type.toLowerCase()}`;
        badge.textContent = tech.name;
        list.appendChild(badge);
    });
}

/**
 * Renderiza la estructura del proyecto
 */
function renderProjectStructure(structure) {
    const container = document.getElementById('projectStructure');
    container.innerHTML = ''; // Limpiar estructura anterior
    
    const tree = buildTree(structure.tree);
    container.appendChild(tree);
}

// Función auxiliar para construir el árbol de estructura de carpetas
function buildTree(nodes) {
    const ul = document.createElement('ul');
    ul.className = 'list-unstyled';

    nodes.forEach(node => {
        const li = document.createElement('li');
        const isFolder = node.type === 'tree' || node.type === 'folder';
        
        li.innerHTML = `<i class="bi ${isFolder ? 'bi-folder' : 'bi-file-earmark'}"></i> ${node.path}`;
        li.className = isFolder ? 'folder' : 'file';

        if (isFolder && node.children && node.children.length > 0) {
            const childrenUl = buildTree(node.children);
            childrenUl.style.display = 'none';
            li.appendChild(childrenUl);
            li.addEventListener('click', (e) => {
                e.stopPropagation();
                childrenUl.style.display = childrenUl.style.display === 'none' ? 'block' : 'none';
            });
        }
        ul.appendChild(li);
    });
    
    return ul;
}


/**
 * Rellena los campos del resumen técnico
 */
function populateTechnicalSummary(summary) {
    // Rellenar la vista de lectura
    document.getElementById('projectPurpose').textContent = summary.projectPurpose || 'No disponible';
    document.getElementById('rolesResponsibilities').textContent = summary.rolesAndResponsibilities || 'No disponible';
    document.getElementById('documentationQuality').textContent = summary.documentationQuality || 'No disponible';

    const mainTechList = document.getElementById('mainTechnologies');
    mainTechList.innerHTML = '';
    summary.mainTechnologies.forEach(tech => {
        const li = document.createElement('li');
        li.textContent = tech;
        mainTechList.appendChild(li);
    });

    const achievementsList = document.getElementById('achievements');
    achievementsList.innerHTML = '';
    summary.achievements.forEach(ach => {
        const li = document.createElement('li');
        li.textContent = ach;
        achievementsList.appendChild(li);
    });

    const codeSnippetsContainer = document.getElementById('codeSnippets');
    codeSnippetsContainer.innerHTML = '';
    summary.codeSnippets.forEach(snippet => {
        const pre = document.createElement('pre');
        const code = document.createElement('code');
        code.className = `language-${snippet.language}`;
        code.textContent = snippet.code;
        pre.appendChild(code);
        codeSnippetsContainer.appendChild(pre);
    });
    
    // Rellenar el formulario de edición
    document.getElementById('editProjectPurpose').value = summary.projectPurpose || '';
    document.getElementById('editRolesResponsibilities').value = summary.rolesAndResponsibilities || '';
    document.getElementById('editDocumentationQuality').value = summary.documentationQuality || '';
    document.getElementById('editMainTechnologies').value = summary.mainTechnologies.join('\n');
    document.getElementById('editAchievements').value = summary.achievements.join('\n');
    document.getElementById('editCodeSnippets').value = summary.codeSnippets.map(s => `\`\`\`${s.language}\n${s.code}\n\`\`\``).join('\n\n');
}

/**
 * Alterna entre el modo de vista y el modo de edición del resumen
 */
function toggleSummaryEditMode() {
    const viewMode = document.getElementById('summaryView');
    const editMode = document.getElementById('summaryEditForm');

    viewMode.classList.toggle('d-none');
    editMode.classList.toggle('d-none');
}

/**
 * Guarda los cambios realizados en el formulario de edición del resumen
 */
function saveSummaryChanges(event) {
    event.preventDefault();
    
    // Recoger los datos del formulario
    const updatedSummary = {
        projectPurpose: document.getElementById('editProjectPurpose').value,
        rolesAndResponsibilities: document.getElementById('editRolesResponsibilities').value,
        documentationQuality: document.getElementById('editDocumentationQuality').value,
        mainTechnologies: document.getElementById('editMainTechnologies').value.split('\n').filter(t => t),
        achievements: document.getElementById('editAchievements').value.split('\n').filter(a => a),
        codeSnippets: [], // Parsear snippets es más complejo, se omite por simplicidad
    };

    // Actualizar la vista de lectura
    populateTechnicalSummary(updatedSummary);
    
    // Volver al modo de vista
    toggleSummaryEditMode();

    // Aquí se podría enviar una petición al backend para guardar los cambios permanentemente
    console.log('Resumen actualizado:', updatedSummary);
    showToast('Resumen guardado localmente', 'success');
} 