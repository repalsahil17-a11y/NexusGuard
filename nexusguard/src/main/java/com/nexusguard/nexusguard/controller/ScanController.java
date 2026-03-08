package com.nexusguard.nexusguard.controller;

import com.nexusguard.nexusguard.dto.DependencyDTO;
import com.nexusguard.nexusguard.dto.ScanResultDTO;
import com.nexusguard.nexusguard.dto.VulnerabilityDTO;
import com.nexusguard.nexusguard.model.Scan;
import com.nexusguard.nexusguard.service.NVDService;
import com.nexusguard.nexusguard.service.PomParserService;
import com.nexusguard.nexusguard.service.ScanService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController
@RequestMapping("/api/scan")
public class ScanController {

    @Autowired
    private ScanService scanService;

    // Main endpoint — full scan
    @PostMapping("/upload")
    public ResponseEntity<?> scanPom(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectName")
            String projectName) {
        try {
            ScanResultDTO result =
                    scanService.scan(file, projectName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    // Get all past scans
    @GetMapping("/history")
    public ResponseEntity<List<Scan>> getHistory() {
        return ResponseEntity.ok(
                scanService.getAllScans());
    }

    // Get one scan details
    @GetMapping("/{scanId}")
    public ResponseEntity<ScanResultDTO> getScan(
            @PathVariable Long scanId) {
        return ResponseEntity.ok(
                scanService.getScanById(scanId));
    }
}
