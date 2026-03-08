package com.nexusguard.nexusguard.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class ScanResultDTO {
    private Long scanId;
    private String projectName;
    private LocalDateTime scannedAt;
    private Integer totalVulnerabilities;
    private Integer criticalCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
    private List<VulnerabilityDTO> vulnerabilities;
}