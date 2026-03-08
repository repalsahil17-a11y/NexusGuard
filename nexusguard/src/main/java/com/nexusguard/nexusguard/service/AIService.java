
package com.nexusguard.nexusguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    @Value("${python.ai.url:http://localhost:8000}")
    private String pythonAiUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> analyzeVulnerability(
            String libraryName,
            String currentVersion,
            String cveId,
            String severity,
            String description) {

        try {
            System.out.println(
                "Calling Python AI service for: "
                + libraryName + " " + cveId);

            // Build request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("library_name", libraryName);
            requestBody.put("current_version", currentVersion);
            requestBody.put("cve_id", cveId);
            requestBody.put("severity", severity);
            requestBody.put("cve_description", description);

            String jsonBody = objectMapper
                .writeValueAsString(requestBody);

            System.out.println(
                "Request body: " + jsonBody);

            // Call Python FastAPI with 120 second timeout
            // Agent takes time to run tools
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pythonAiUrl + "/analyze"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers
                    .ofString(jsonBody))
                .build();

            HttpResponse<String> response =
                httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println(
                "Python AI status: "
                + response.statusCode());
            System.out.println(
                "Python AI response: "
                + response.body());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper
                    .readTree(response.body());

                Map<String, Object> result =
                    new HashMap<>();

                result.put("simple_explanation",
                    root.path("simple_explanation")
                        .asText("Analysis unavailable"));

                result.put("what_could_happen",
                    root.path("what_could_happen")
                        .asText("Check NVD for details"));

                result.put("fix_suggestion",
                    root.path("fix_suggestion")
                        .asText("Upgrade to latest version"));

                result.put("fixed_version",
                    root.path("fixed_version")
                        .asText("latest stable version"));

                result.put("is_false_positive",
                    root.path("is_false_positive")
                        .asBoolean(false));

                result.put("false_positive_reason",
                    root.path("false_positive_reason")
                        .asText(null));

                result.put("exploit_available",
                    root.path("exploit_available")
                        .asBoolean(false));

                result.put("version_affected",
                    root.path("version_affected")
                        .asBoolean(true));

                result.put("risk_score",
                    root.path("risk_score")
                        .asInt(5));

                result.put("priority",
                    root.path("priority")
                        .asText("MEDIUM"));

                System.out.println(
                    "AI analysis complete for " + cveId);
                return result;

            } else {
                System.err.println(
                    "Python AI error: "
                    + response.statusCode());
                return getFallbackResponse(
                    libraryName, severity);
            }

        } catch (Exception e) {
            System.err.println(
                "Python AI call failed: "
                + e.getMessage());
            return getFallbackResponse(
                libraryName, severity);
        }
    }

    private Map<String, Object> getFallbackResponse(
            String libraryName,
            String severity) {

        System.out.println(
            "Using fallback response for "
            + libraryName);

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("simple_explanation",
            libraryName + " has a " + severity
            + " vulnerability requiring attention.");
        fallback.put("what_could_happen",
            "Security risk — upgrade immediately");
        fallback.put("fix_suggestion",
            "Upgrade " + libraryName
            + " to latest stable version");
        fallback.put("fixed_version",
            "latest stable version");
        fallback.put("is_false_positive", false);
        fallback.put("false_positive_reason", null);
        fallback.put("exploit_available", false);
        fallback.put("version_affected", true);
        fallback.put("risk_score",
            severity.equalsIgnoreCase("CRITICAL") ? 9
            : severity.equalsIgnoreCase("HIGH") ? 7
            : severity.equalsIgnoreCase("MEDIUM") ? 5
            : 2);
        fallback.put("priority",
            severity.equalsIgnoreCase("CRITICAL")
                ? "IMMEDIATE"
            : severity.equalsIgnoreCase("HIGH")
                ? "HIGH"
            : "MEDIUM");
        return fallback;
    }
}


// package com.nexusguard.nexusguard.service;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;

// import jakarta.annotation.PostConstruct;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// @Service
// public class AIService {

//     @Value("${groq.api.key}")
//     private String apiKey;

//     @Value("${groq.api.url}")
//     private String apiUrl;

//     @Value("${groq.model}")
//     private String model;

//     private final ObjectMapper objectMapper =
//             new ObjectMapper();

//     // ── Verify key is loading correctly ─────────
//     @PostConstruct
//     public void init() {
//         System.out.println(
//             "Groq API Key loaded: " +
//             apiKey.substring(0, 8) + "...");
//     }

//     public Map<String, String> analyzeVulnerability(
//             String libraryName,
//             String currentVersion,
//             String cveId,
//             String severity,
//             String description) {

//         Map<String, String> result = new HashMap<>();

//         try {
//             // Build WebClient
//             WebClient webClient = WebClient
//                     .builder()
//                     .baseUrl(apiUrl)
//                     .build();

//             // Build prompt
//             String prompt = buildPrompt(
//                     libraryName,
//                     currentVersion,
//                     cveId,
//                     severity,
//                     description);

//             // Build request body
//             Map<String, Object> requestBody =
//                     new HashMap<>();
//             requestBody.put("model", model);
//             requestBody.put("temperature", 0.3);
//             requestBody.put("max_tokens", 500);
//             requestBody.put("messages", List.of(
//                 Map.of(
//                     "role", "system",
//                     "content", """
//                         You are a senior security engineer.
//                         You ALWAYS respond in valid JSON only.
//                         Never add any text outside the JSON.
//                         Never add markdown code blocks.
//                         Never add explanations outside JSON.
//                         Your entire response must be 
//                         parseable as JSON directly.
//                         Always include these exact keys:
//                         explanation, what_could_happen,
//                         fix_suggestion, fixed_version.
//                         For fixed_version always give only
//                         the version number like 2.17.1
//                         """
//                 ),
//                 Map.of(
//                     "role", "user",
//                     "content", prompt
//                 )
//             ));

//             // Call Groq API
//             String response = webClient.post()
//                     .uri("/chat/completions")
//                     .header("Authorization",
//                             "Bearer " + apiKey.trim())
//                     .header("Content-Type",
//                             "application/json")
//                     .bodyValue(requestBody)
//                     .retrieve()
//                     .bodyToMono(String.class)
//                     .block();

//             // Parse response
//             result = parseAIResponse(response);

//         } catch (Exception e) {
//             System.out.println(
//                 "AI analysis error: " + e.getMessage());
//             result.put("explanation",
//                 "AI analysis unavailable");
//             result.put("what_could_happen",
//                 "Please check NVD for details");
//             result.put("fix_suggestion",
//                 "Upgrade to latest stable version");
//             result.put("fixed_version",
//                 "latest stable version");
//         }

//         return result;
//     }

//     // ── Build prompt ─────────────────────────────
//     private String buildPrompt(
//             String libraryName,
//             String currentVersion,
//             String cveId,
//             String severity,
//             String description) {

//         return """
//                 Analyze this vulnerability and respond
//                 ONLY in valid JSON format:

//                 Library: %s
//                 Current Version: %s
//                 CVE ID: %s
//                 Severity: %s
//                 Description: %s

//                 Respond ONLY with this exact JSON:
//                 {
//                     "explanation": "2-3 sentence simple explanation of why this is dangerous",
//                     "what_could_happen": "what could happen if not fixed",
//                     "fix_suggestion": "exact steps to fix this",
//                     "fixed_version": "only the version number like 2.17.1"
//                 }

//                 For fixed_version give ONLY version number.
//                 No extra text. No markdown. Only JSON.
//                 """.formatted(
//                         libraryName,
//                         currentVersion,
//                         cveId,
//                         severity,
//                         description);
//     }

//     // ── Parse AI response ────────────────────────
//     private Map<String, String> parseAIResponse(
//             String response) throws Exception {

//         Map<String, String> result = new HashMap<>();

//         JsonNode root = objectMapper
//                 .readTree(response);

//         // Extract content from Groq response
//         String content = root
//                 .path("choices")
//                 .get(0)
//                 .path("message")
//                 .path("content")
//                 .asText();

//         System.out.println(
//             "AI Raw Response: " + content);

//         // Clean up markdown formatting if present
//         content = content
//                 .replace("```json", "")
//                 .replace("```", "")
//                 .trim();

//         // Extract JSON object from response
//         // handles cases where AI adds extra text
//         int startIndex = content.indexOf("{");
//         int endIndex = content.lastIndexOf("}");

//         if (startIndex != -1 && endIndex != -1) {
//             content = content.substring(
//                     startIndex, endIndex + 1);
//         }

//         System.out.println(
//             "Cleaned AI Response: " + content);

//         try {
//             JsonNode aiJson = objectMapper
//                     .readTree(content);

//             result.put("explanation",
//                 aiJson.path("explanation")
//                       .asText("Analysis unavailable"));

//             result.put("what_could_happen",
//                 aiJson.path("what_could_happen")
//                       .asText("Check NVD for details"));

//             result.put("fix_suggestion",
//                 aiJson.path("fix_suggestion")
//                       .asText("Upgrade to latest version"));

//             result.put("fixed_version",
//                 aiJson.path("fixed_version")
//                       .asText("latest stable version"));

//         } catch (Exception e) {
//             System.out.println(
//                 "Failed to parse AI JSON: " +
//                 e.getMessage());

//             // If JSON parsing fails
//             // return raw content as explanation
//             result.put("explanation", content);
//             result.put("what_could_happen",
//                 "Check NVD for details");
//             result.put("fix_suggestion",
//                 "Upgrade to latest stable version");
//             result.put("fixed_version",
//                 "latest stable version");
//         }

//         return result;
//     }
// }
