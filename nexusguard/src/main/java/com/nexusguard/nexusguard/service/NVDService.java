// package com.nexusguard.nexusguard.service;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.nexusguard.nexusguard.dto.*;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;

// @Service
// public class NVDService {

//     @Value("${nvd.api.key}")
//     private String apiKey;

//     @Autowired
//     private AIService aiService;

//     private final WebClient webClient = WebClient
//             .builder()
//             .baseUrl("https://services.nvd.nist.gov")
//             .build();

//     private final ObjectMapper objectMapper =
//             new ObjectMapper();

//     // ── Main method — check vulnerabilities ──────
//     public List<VulnerabilityDTO> checkVulnerabilities(
//             DependencyDTO dependency) {

//         List<VulnerabilityDTO> vulnerabilities =
//                 new ArrayList<>();

//         try {
//             String keyword =
//                     dependency.getArtifactId();

//             System.out.println(
//                 "Calling NVD API for: " + keyword);

//             String response = webClient.get()
//                 .uri(uriBuilder -> uriBuilder
//                     .path("/rest/json/cves/2.0")
//                     .queryParam("keywordSearch",
//                                 keyword)
//                     .queryParam("resultsPerPage", 5)
//                     .build())
//                 .header("apiKey", apiKey)
//                 .retrieve()
//                 .bodyToMono(String.class)
//                 .block();

//             vulnerabilities = parseNVDResponse(
//                     response, dependency);

//             System.out.println(
//                 "Found " +
//                 vulnerabilities.size() +
//                 " vulnerabilities for: " + keyword);

//             // NVD rate limit delay
//             Thread.sleep(1000);

//         } catch (Exception e) {
//             System.out.println(
//                 "Error checking " +
//                 dependency.getArtifactId() +
//                 ": " + e.getMessage());
//         }

//         return vulnerabilities;
//     }

//     // ── Parse NVD API response ───────────────────
//     private List<VulnerabilityDTO> parseNVDResponse(
//             String response,
//             DependencyDTO dependency) throws Exception {

//         List<VulnerabilityDTO> results =
//                 new ArrayList<>();

//         JsonNode root = objectMapper
//                 .readTree(response);
//         JsonNode vulnerabilities =
//                 root.path("vulnerabilities");

//         for (JsonNode vuln : vulnerabilities) {
//             JsonNode cve = vuln.path("cve");

//             // ── Get CVE ID ───────────────────────
//             String cveId = cve
//                     .path("id")
//                     .asText();

//             // ── Get description ──────────────────
//             String description =
//                     "No description available";
//             JsonNode descriptions =
//                     cve.path("descriptions");

//             for (JsonNode desc : descriptions) {
//                 if (desc.path("lang")
//                         .asText()
//                         .equals("en")) {
//                     description = desc
//                             .path("value")
//                             .asText();
//                     break;
//                 }
//             }

//             // ── Get severity ─────────────────────
//             String severity = "UNKNOWN";
//             JsonNode metrics = cve.path("metrics");

//             if (metrics.has("cvssMetricV31")) {
//                 JsonNode cvss = metrics
//                         .path("cvssMetricV31")
//                         .get(0)
//                         .path("cvssData");
//                 severity = cvss
//                         .path("baseSeverity")
//                         .asText("UNKNOWN");

//             } else if (metrics.has("cvssMetricV30")) {
//                 JsonNode cvss = metrics
//                         .path("cvssMetricV30")
//                         .get(0)
//                         .path("cvssData");
//                 severity = cvss
//                         .path("baseSeverity")
//                         .asText("UNKNOWN");

//             } else if (metrics.has("cvssMetricV2")) {
//                 JsonNode cvss = metrics
//                         .path("cvssMetricV2")
//                         .get(0)
//                         .path("cvssData");
//                 String score = cvss
//                         .path("baseScore")
//                         .asText("0");
//                 severity = getSeverityFromV2Score(
//                         Double.parseDouble(score));
//             }

//             // ── Build DTO ────────────────────────
//             VulnerabilityDTO vulnDTO =
//                     new VulnerabilityDTO();
//             vulnDTO.setLibraryName(
//                     dependency.getArtifactId());
//             vulnDTO.setCurrentVersion(
//                     dependency.getVersion());
//             vulnDTO.setCveId(cveId);
//             vulnDTO.setSeverity(severity);
//             vulnDTO.setDescription(description);

//             // ── Call Python AI Service ───────────
//             System.out.println(
//                 "Calling Python AI for: " + cveId);

//             // Now returns Map<String, Object>
//             // not Map<String, String>
//             Map<String, Object> aiAnalysis =
//                     aiService.analyzeVulnerability(
//                             dependency.getArtifactId(),
//                             dependency.getVersion(),
//                             cveId,
//                             severity,
//                             description);

//             // ── Check false positive ─────────────
//             boolean isFalsePositive =
//                 (boolean) aiAnalysis.getOrDefault(
//                     "is_false_positive", false);

//             if (isFalsePositive) {
//                 System.out.println(
//                     "False positive detected — "
//                     + "skipping: " + cveId);
//                 // Skip this vulnerability
//                 continue;
//             }

//             // ── Set fixed version from AI ────────
//             vulnDTO.setFixedVersion(
//                 String.valueOf(
//                     aiAnalysis.getOrDefault(
//                         "fixed_version",
//                         "latest stable version")));

//             // ── Set AI explanation ───────────────
//             String explanation = String.valueOf(
//                 aiAnalysis.getOrDefault(
//                     "explanation", ""));
//             String whatCouldHappen = String.valueOf(
//                 aiAnalysis.getOrDefault(
//                     "what_could_happen", ""));

//             vulnDTO.setAiExplanation(
//                 explanation +
//                 "\n\nRisk: " + whatCouldHappen);

//             // ── Set AI fix suggestion ────────────
//             vulnDTO.setAiFixSuggestion(
//                 String.valueOf(
//                     aiAnalysis.getOrDefault(
//                         "fix_suggestion", "")));

//             // ── Set exploit info ─────────────────
//             boolean exploitAvailable =
//                 (boolean) aiAnalysis.getOrDefault(
//                     "exploit_available", false);

//             if (exploitAvailable) {
//                 System.out.println(
//                     "Public exploit found for: "
//                     + cveId);
//                 // Boost severity if exploit exists
//                 if (severity.equals("HIGH")) {
//                     vulnDTO.setSeverity("CRITICAL");
//                     System.out.println(
//                         "Severity boosted to CRITICAL"
//                         + " due to public exploit");
//                 }
//             }

//             results.add(vulnDTO);
//         }

//         return results;
//     }

//     // ── Calculate severity from CVSS v2 score ────
//     private String getSeverityFromV2Score(
//             double score) {
//         if (score >= 9.0) return "CRITICAL";
//         if (score >= 7.0) return "HIGH";
//         if (score >= 4.0) return "MEDIUM";
//         return "LOW";
//     }
// }



// // package com.nexusguard.nexusguard.service;

// // import com.fasterxml.jackson.databind.JsonNode;
// // import com.fasterxml.jackson.databind.ObjectMapper;
// // import com.nexusguard.nexusguard.dto.*;

// // import org.springframework.beans.factory.annotation.Autowired;
// // import org.springframework.beans.factory.annotation.Value;
// // import org.springframework.stereotype.Service;
// // import org.springframework.web.reactive.function.client.WebClient;

// // import java.util.ArrayList;
// // import java.util.List;
// // import java.util.Map;

// // @Service
// // public class NVDService {

// //     @Value("${nvd.api.key}")
// //     private String apiKey;

// //     // ── Inject AIService ─────────────────────────
// //     @Autowired
// //     private AIService aiService;

// //     private final WebClient webClient = WebClient
// //             .builder()
// //             .baseUrl("https://services.nvd.nist.gov")
// //             .build();

// //     private final ObjectMapper objectMapper =
// //             new ObjectMapper();

// //     // ── Main method — check vulnerabilities ──────
// //     public List<VulnerabilityDTO> checkVulnerabilities(
// //             DependencyDTO dependency) {

// //         List<VulnerabilityDTO> vulnerabilities =
// //                 new ArrayList<>();

// //         try {
// //             String keyword = 
// //                     dependency.getArtifactId();

// //             System.out.println(
// //                 "Calling NVD API for: " + keyword);

// //             String response = webClient.get()
// //                 .uri(uriBuilder -> uriBuilder
// //                     .path("/rest/json/cves/2.0")
// //                     .queryParam("keywordSearch", 
// //                                 keyword)
// //                     .queryParam("resultsPerPage", 5)
// //                     .build())
// //                 .header("apiKey", apiKey)
// //                 .retrieve()
// //                 .bodyToMono(String.class)
// //                 .block();

// //             // Parse NVD response
// //             vulnerabilities = parseNVDResponse(
// //                     response, dependency);

// //             System.out.println(
// //                 "Found " + 
// //                 vulnerabilities.size() +
// //                 " vulnerabilities for: " + keyword);

// //             // Rate limit delay
// //             // NVD allows 5 requests per 30 seconds
// //             Thread.sleep(1000);

// //         } catch (Exception e) {
// //             System.out.println(
// //                 "Error checking " +
// //                 dependency.getArtifactId() +
// //                 ": " + e.getMessage());
// //         }

// //         return vulnerabilities;
// //     }

// //     // ── Parse NVD API response ───────────────────
// //     private List<VulnerabilityDTO> parseNVDResponse(
// //             String response,
// //             DependencyDTO dependency) throws Exception {

// //         List<VulnerabilityDTO> results =
// //                 new ArrayList<>();

// //         JsonNode root = objectMapper
// //                 .readTree(response);
// //         JsonNode vulnerabilities =
// //                 root.path("vulnerabilities");

// //         // Loop through each CVE found
// //         for (JsonNode vuln : vulnerabilities) {
// //             JsonNode cve = vuln.path("cve");

// //             // ── Get CVE ID ───────────────────────
// //             String cveId = cve
// //                     .path("id")
// //                     .asText();

// //             // ── Get description ──────────────────
// //             String description = 
// //                     "No description available";
// //             JsonNode descriptions =
// //                     cve.path("descriptions");

// //             for (JsonNode desc : descriptions) {
// //                 if (desc.path("lang")
// //                         .asText()
// //                         .equals("en")) {
// //                     description = desc
// //                             .path("value")
// //                             .asText();
// //                     break;
// //                 }
// //             }

// //             // ── Get severity ─────────────────────
// //             String severity = "UNKNOWN";
// //             JsonNode metrics = cve.path("metrics");

// //             // Try CVSS v3.1 first
// //             if (metrics.has("cvssMetricV31")) {
// //                 JsonNode cvss = metrics
// //                         .path("cvssMetricV31")
// //                         .get(0)
// //                         .path("cvssData");
// //                 severity = cvss
// //                         .path("baseSeverity")
// //                         .asText("UNKNOWN");

// //             // Fall back to CVSS v3.0
// //             } else if (metrics.has("cvssMetricV30")) {
// //                 JsonNode cvss = metrics
// //                         .path("cvssMetricV30")
// //                         .get(0)
// //                         .path("cvssData");
// //                 severity = cvss
// //                         .path("baseSeverity")
// //                         .asText("UNKNOWN");

// //             // Fall back to CVSS v2
// //             } else if (metrics.has("cvssMetricV2")) {
// //                 JsonNode cvss = metrics
// //                         .path("cvssMetricV2")
// //                         .get(0)
// //                         .path("cvssData");
// //                 String score = cvss
// //                         .path("baseScore")
// //                         .asText("0");
// //                 severity = getSeverityFromV2Score(
// //                         Double.parseDouble(score));
// //             }

// //             // ── Build DTO ────────────────────────
// //             VulnerabilityDTO vulnDTO =
// //                     new VulnerabilityDTO();
// //             vulnDTO.setLibraryName(
// //                     dependency.getArtifactId());
// //             vulnDTO.setCurrentVersion(
// //                     dependency.getVersion());
// //             vulnDTO.setCveId(cveId);
// //             vulnDTO.setSeverity(severity);
// //             vulnDTO.setDescription(description);

// //             // ── Call AI for analysis ─────────────
// //             System.out.println(
// //                 "Getting AI analysis for: " + cveId);

// //             Map<String, String> aiAnalysis =
// //                     aiService.analyzeVulnerability(
// //                             dependency.getArtifactId(),
// //                             dependency.getVersion(),
// //                             cveId,
// //                             severity,
// //                             description);

// //             // ── Set fixed version from AI ────────
// //             vulnDTO.setFixedVersion(
// //                 aiAnalysis.getOrDefault(
// //                     "fixed_version",
// //                     "latest stable version"));

// //             // ── Set AI explanation ───────────────
// //             vulnDTO.setAiExplanation(
// //                 aiAnalysis.getOrDefault(
// //                     "explanation", "") +
// //                 "\n\nRisk: " +
// //                 aiAnalysis.getOrDefault(
// //                     "what_could_happen", ""));

// //             // ── Set AI fix suggestion ────────────
// //             vulnDTO.setAiFixSuggestion(
// //                 aiAnalysis.getOrDefault(
// //                     "fix_suggestion", ""));

// //             results.add(vulnDTO);
// //         }

// //         return results;
// //     }

// //     // ── Calculate severity from CVSS v2 score ────
// //     private String getSeverityFromV2Score(
// //             double score) {
// //         if (score >= 9.0) return "CRITICAL";
// //         if (score >= 7.0) return "HIGH";
// //         if (score >= 4.0) return "MEDIUM";
// //         return "LOW";
// //     }
// // }
package com.nexusguard.nexusguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusguard.nexusguard.dto.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NVDService {

    @Value("${nvd.api.key}")
    private String apiKey;

    @Autowired
    private AIService aiService;

    private final WebClient webClient = WebClient
            .builder()
            .baseUrl("https://services.nvd.nist.gov")
            .build();

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public List<VulnerabilityDTO> checkVulnerabilities(
            DependencyDTO dependency) {

        List<VulnerabilityDTO> vulnerabilities =
                new ArrayList<>();

        try {
            String keyword =
                    dependency.getArtifactId();

            System.out.println(
                "Calling NVD API for: " + keyword);

            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/rest/json/cves/2.0")
                    .queryParam("keywordSearch",
                                keyword)
                    .queryParam("resultsPerPage", 5)
                    .build())
                .header("apiKey", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            vulnerabilities = parseNVDResponse(
                    response, dependency);

            System.out.println(
                "Found " +
                vulnerabilities.size() +
                " vulnerabilities for: " + keyword);

            Thread.sleep(1000);

        } catch (Exception e) {
            System.out.println(
                "Error checking " +
                dependency.getArtifactId() +
                ": " + e.getMessage());
        }

        return vulnerabilities;
    }

    private List<VulnerabilityDTO> parseNVDResponse(
            String response,
            DependencyDTO dependency) throws Exception {

        List<VulnerabilityDTO> results =
                new ArrayList<>();

        JsonNode root = objectMapper
                .readTree(response);
        JsonNode vulnerabilities =
                root.path("vulnerabilities");

        for (JsonNode vuln : vulnerabilities) {
            JsonNode cve = vuln.path("cve");

            // ── Get CVE ID ───────────────────────
            String cveId = cve
                    .path("id")
                    .asText();

            // ── Get description ──────────────────
            String description =
                    "No description available";
            JsonNode descriptions =
                    cve.path("descriptions");

            for (JsonNode desc : descriptions) {
                if (desc.path("lang")
                        .asText()
                        .equals("en")) {
                    description = desc
                            .path("value")
                            .asText();
                    break;
                }
            }

            // ── Get severity ─────────────────────
            String severity = "UNKNOWN";
            JsonNode metrics = cve.path("metrics");

            if (metrics.has("cvssMetricV31")) {
                JsonNode cvss = metrics
                        .path("cvssMetricV31")
                        .get(0)
                        .path("cvssData");
                severity = cvss
                        .path("baseSeverity")
                        .asText("UNKNOWN");

            } else if (metrics.has("cvssMetricV30")) {
                JsonNode cvss = metrics
                        .path("cvssMetricV30")
                        .get(0)
                        .path("cvssData");
                severity = cvss
                        .path("baseSeverity")
                        .asText("UNKNOWN");

            } else if (metrics.has("cvssMetricV2")) {
                JsonNode cvss = metrics
                        .path("cvssMetricV2")
                        .get(0)
                        .path("cvssData");
                String score = cvss
                        .path("baseScore")
                        .asText("0");
                severity = getSeverityFromV2Score(
                        Double.parseDouble(score));
            }

            // ── Build DTO ────────────────────────
            VulnerabilityDTO vulnDTO =
                    new VulnerabilityDTO();
            vulnDTO.setLibraryName(
                    dependency.getArtifactId());
            vulnDTO.setCurrentVersion(
                    dependency.getVersion());
            vulnDTO.setCveId(cveId);
            vulnDTO.setSeverity(severity);
            vulnDTO.setDescription(description);

            // ── Call Python AI Service ───────────
            System.out.println(
                "Calling Python AI for: " + cveId);

            Map<String, Object> aiAnalysis =
                    aiService.analyzeVulnerability(
                            dependency.getArtifactId(),
                            dependency.getVersion(),
                            cveId,
                            severity,
                            description);

            // ── Filter 1: False positive ─────────
      boolean falsePositive =
    (boolean) aiAnalysis.getOrDefault(
        "is_false_positive", false);


            if (falsePositive) {
                System.out.println(
                    "FALSE POSITIVE — skipping: "
                    + cveId + " ("
                    + dependency.getArtifactId()
                    + ")");
                continue;
            }

            // ── Filter 2: Version not affected ───
            boolean versionAffected =
                (boolean) aiAnalysis.getOrDefault(
                    "version_affected", true);

            if (!versionAffected) {
                System.out.println(
                    "VERSION NOT AFFECTED — skipping: "
                    + cveId + " (v"
                    + dependency.getVersion()
                    + ")");
                continue;
            }

            // ── Set fixed version ────────────────
            vulnDTO.setFixedVersion(
                String.valueOf(
                    aiAnalysis.getOrDefault(
                        "fixed_version",
                        "latest stable version")));

            // ── Set AI explanation ───────────────
            // Python returns "simple_explanation"
            // not "explanation" — this was the bug!
            String explanation = String.valueOf(
                aiAnalysis.getOrDefault(
                    "simple_explanation", ""));
            String whatCouldHappen = String.valueOf(
                aiAnalysis.getOrDefault(
                    "what_could_happen", ""));

            vulnDTO.setAiExplanation(
                explanation +
                "\n\nRisk: " + whatCouldHappen);

            // ── Set AI fix suggestion ────────────
            vulnDTO.setAiFixSuggestion(
                String.valueOf(
                    aiAnalysis.getOrDefault(
                        "fix_suggestion", "")));

            // ── Boost severity if exploit exists ─
            boolean exploitAvailable =
                (boolean) aiAnalysis.getOrDefault(
                    "exploit_available", false);

            if (exploitAvailable) {
                System.out.println(
                    "Public exploit found for: "
                    + cveId);
                if (severity.equals("HIGH")) {
                    vulnDTO.setSeverity("CRITICAL");
                    System.out.println(
                        "Severity boosted to CRITICAL"
                        + " due to public exploit");
                }
            }
// ── Set new AI fields on DTO ─────────────
vulnDTO.setExploitAvailable(exploitAvailable);

vulnDTO.setVersionAffected(
    (boolean) aiAnalysis.getOrDefault(
        "version_affected", true));

vulnDTO.setRiskScore(
    ((Number) aiAnalysis.getOrDefault(
        "risk_score", 5)).intValue());

vulnDTO.setPriority(
    String.valueOf(
        aiAnalysis.getOrDefault(
            "priority", "MEDIUM")));
System.out.println("=== FINAL DTO CHECK ===");
System.out.println("aiExplanation: [" +
    vulnDTO.getAiExplanation() + "]");
System.out.println("=======================");
vulnDTO.setFalsePositiveReason(
    aiAnalysis.get("false_positive_reason") != null
        ? String.valueOf(
            aiAnalysis.get("false_positive_reason"))
        : null);
            results.add(vulnDTO);
        }

        return results;
    }

    private String getSeverityFromV2Score(
            double score) {
        if (score >= 9.0) return "CRITICAL";
        if (score >= 7.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        return "LOW";
    }
}