package com.nexusguard.nexusguard.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DependencyDTO {
    private String groupId;
    private String artifactId;
    private String version;
    
    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}