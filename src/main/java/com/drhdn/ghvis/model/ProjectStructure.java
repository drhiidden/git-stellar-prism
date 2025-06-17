package com.drhdn.ghvis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representa la estructura de archivos y carpetas de un proyecto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStructure {
    
    /**
     * Nodos raíz de la estructura
     */
    private List<TreeNode> tree;
    
    /**
     * Número total de archivos
     */
    private int totalFiles;
    
    /**
     * Número total de carpetas
     */
    private int totalFolders;
    
    /**
     * Profundidad máxima de la estructura
     */
    private int maxDepth;
    
    /**
     * Representa un nodo en el árbol de estructura del proyecto
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreeNode {
        /**
         * Tipo de nodo (archivo o carpeta)
         */
        private String type;
        
        /**
         * Nombre del archivo o carpeta
         */
        private String name;
        
        /**
         * Ruta relativa desde la raíz del proyecto
         */
        private String path;
        
        /**
         * Tamaño en bytes (solo para archivos)
         */
        private Long size;
        
        /**
         * Extensión del archivo (solo para archivos)
         */
        private String extension;
        
        /**
         * Nodos hijos (solo para carpetas)
         */
        private List<TreeNode> children;
        
        /**
         * Tipos de nodo
         */
        public static class NodeType {
            public static final String FILE = "blob";
            public static final String FOLDER = "tree";
        }
    }
} 