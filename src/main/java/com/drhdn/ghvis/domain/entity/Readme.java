package com.drhdn.ghvis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa el contenido de un archivo README de GitHub.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Readme {
    private String type;
    private String encoding;
    private Integer size;
    private String name;
    private String path;
    private String content;
    private String sha;
    private String url;
    private String gitUrl;
    private String htmlUrl;
    private String downloadUrl;
}