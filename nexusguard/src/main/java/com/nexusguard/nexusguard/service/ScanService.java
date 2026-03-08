package com.nexusguard.nexusguard.service;

import com.nexusguard.nexusguard.dto.DependencyDTO;
import com.nexusguard.nexusguard.dto.ScanResultDTO;
import com.nexusguard.nexusguard.dto.VulnerabilityDTO;
import com.nexusguard.nexusguard.model.Scan;
import com.nexusguard.nexusguard.model.Vulnerability;
import com.nexusguard.nexusguard.repository.ScanRepository;
import com.nexusguard.nexusguard.repository.VulnerabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScanService {

    @Autowired
    private PomParserService pomParserService;

    @Autowired
    private NVDService nvdService;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    public ScanResultDTO scan(
            MultipartFile file,
            String projectName) throws Exception {

        System.out.println(
            "Starting scan for: " + projectName);

        // ── Step 1 — Parse pom.xml ──────────────
        List<DependencyDTO> dependencies =
                pomParserService.parsePom(file);

        System.out.println("Found " +
                dependencies.size() +
                " dependencies to scan");

        // ── Step 2 — Create scan record ─────────
        Scan scan = new Scan();
        scan.setProjectName(projectName);
        scan.setScannedAt(LocalDateTime.now());
        scan.setStatus("IN_PROGRESS");
        scan.setTotalVulnerabilities(0);
        scan.setCriticalCount(0);
        scan.setHighCount(0);
        scan.setMediumCount(0);
        scan.setLowCount(0);
        scan = scanRepository.save(scan);

        // ── Step 3 — Scan each dependency ───────
        List<VulnerabilityDTO> allVulnerabilities =
                new ArrayList<>();

        for (DependencyDTO dependency : dependencies) {
            System.out.println(
                "Scanning: " +
                dependency.getArtifactId() +
                " " +
                dependency.getVersion());

            List<VulnerabilityDTO> vulns =
                    nvdService.checkVulnerabilities(
                            dependency);

            allVulnerabilities.addAll(vulns);
        }

        // ── Step 4 — Count by severity ──────────
        int critical = 0, high = 0,
            medium = 0, low = 0;

        for (VulnerabilityDTO vuln :
                allVulnerabilities) {
            switch (vuln.getSeverity()
                        .toUpperCase()) {
                case "CRITICAL" -> critical++;
                case "HIGH"     -> high++;
                case "MEDIUM"   -> medium++;
                case "LOW"      -> low++;
            }
        }

        // ── Step 5 — Update scan record ─────────
        scan.setTotalVulnerabilities(
                allVulnerabilities.size());
        scan.setCriticalCount(critical);
        scan.setHighCount(high);
        scan.setMediumCount(medium);
        scan.setLowCount(low);
        scan.setStatus("COMPLETED");
        scan = scanRepository.save(scan);

        // ── Step 6 — Save vulnerabilities ───────
        for (VulnerabilityDTO vulnDTO :
                allVulnerabilities) {
           Vulnerability vuln = new Vulnerability();
    vuln.setScan(scan);
    vuln.setLibraryName(
            vulnDTO.getLibraryName());
    vuln.setCurrentVersion(
            vulnDTO.getCurrentVersion());
    vuln.setFixedVersion(
            vulnDTO.getFixedVersion());
    vuln.setCveId(vulnDTO.getCveId());
    vuln.setSeverity(vulnDTO.getSeverity());
    vuln.setDescription(
            vulnDTO.getDescription());
    vuln.setAiExplanation(
            vulnDTO.getAiExplanation());
    vuln.setAiFixSuggestion(
            vulnDTO.getAiFixSuggestion());

    // ── New fields ───────────────────────
    vuln.setExploitAvailable(
            vulnDTO.isExploitAvailable());
    vuln.setVersionAffected(
            vulnDTO.isVersionAffected());
    vuln.setRiskScore(
            vulnDTO.getRiskScore());
    vuln.setPriority(
            vulnDTO.getPriority());
    vuln.setFalsePositiveReason(
            vulnDTO.getFalsePositiveReason());
            vuln.setFalsePositive(
    vulnDTO.isFalsePositive());

    vulnerabilityRepository.save(vuln);
        }

        System.out.println("Scan completed. Found " +
                allVulnerabilities.size() +
                " vulnerabilities");

        // ── Step 7 — Build and return result ────
        ScanResultDTO result = new ScanResultDTO();
        result.setScanId(scan.getId());
        result.setProjectName(projectName);
        result.setScannedAt(scan.getScannedAt());
        result.setTotalVulnerabilities(
                allVulnerabilities.size());
        result.setCriticalCount(critical);
        result.setHighCount(high);
        result.setMediumCount(medium);
        result.setLowCount(low);
        result.setVulnerabilities(allVulnerabilities);

        return result;
    }

    // Get all past scans
    public List<Scan> getAllScans() {
        return scanRepository
                .findAllByOrderByScannedAtDesc();
    }

    // Get one scan with vulnerabilities
    public ScanResultDTO getScanById(Long scanId) {
        Scan scan = scanRepository
                .findById(scanId)
                .orElseThrow(() ->
                    new RuntimeException(
                        "Scan not found"));

        List<Vulnerability> vulns =
                vulnerabilityRepository
                .findByScanId(scanId);

        List<VulnerabilityDTO> vulnDTOs =
                new ArrayList<>();

        for (Vulnerability vuln : vulns) {
            VulnerabilityDTO dto =
                    new VulnerabilityDTO();
            dto.setLibraryName(
                    vuln.getLibraryName());
            dto.setCurrentVersion(
                    vuln.getCurrentVersion());
            dto.setFixedVersion(
                    vuln.getFixedVersion());
            dto.setCveId(vuln.getCveId());
            dto.setSeverity(vuln.getSeverity());
            dto.setDescription(
                    vuln.getDescription());
            dto.setAiExplanation(
                    vuln.getAiExplanation());
            dto.setAiFixSuggestion(
                    vuln.getAiFixSuggestion());
                    dto.setExploitAvailable(
        vuln.isExploitAvailable());
dto.setVersionAffected(
        vuln.isVersionAffected());
dto.setRiskScore(vuln.getRiskScore());
dto.setPriority(vuln.getPriority());
dto.setFalsePositiveReason(
        vuln.getFalsePositiveReason());
            vulnDTOs.add(dto);
        }

        ScanResultDTO result = new ScanResultDTO();
        result.setScanId(scan.getId());
        result.setProjectName(
                scan.getProjectName());
        result.setScannedAt(scan.getScannedAt());
        result.setTotalVulnerabilities(
                scan.getTotalVulnerabilities());
        result.setCriticalCount(
                scan.getCriticalCount());
        result.setHighCount(scan.getHighCount());
        result.setMediumCount(
                scan.getMediumCount());
        result.setLowCount(scan.getLowCount());
        result.setVulnerabilities(vulnDTOs);

        return result;
    }
}
