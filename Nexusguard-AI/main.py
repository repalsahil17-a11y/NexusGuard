from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from models.schemas import (
    VulnerabilityInput,
    AIAnalysisResult,
    BatchAnalysisRequest,
    BatchAnalysisResponse
)
from agents.vulnerability_agent import (
    analyze_vulnerability
)

# Load environment variables
load_dotenv()

app = FastAPI(
    title="NexusGuard AI Service",
    description="LangGraph powered vulnerability analysis"
)

# Allow Spring Boot to call this
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],
    allow_methods=["*"],
    allow_headers=["*"]
)

@app.get("/health")
def health_check():
    return {
        "status": "NexusGuard AI Service running",
        "version": "2.0 - LangGraph Agent"
    }

@app.post("/analyze",
          response_model=AIAnalysisResult)
def analyze_single(vuln: VulnerabilityInput):

    result = analyze_vulnerability(
        library_name=vuln.library_name,
        current_version=vuln.current_version,
        cve_id=vuln.cve_id,
        severity=vuln.severity,
        description=vuln.cve_description
    )

    return AIAnalysisResult(
        library_name=vuln.library_name,
        cve_id=vuln.cve_id,
        is_false_positive=result.get(
            "is_false_positive", False),
        false_positive_reason=result.get(
            "false_positive_reason"),
        simple_explanation=result.get(
            "simple_explanation", ""),
        what_could_happen=result.get(
            "what_could_happen", ""),
        fix_suggestion=result.get(
            "fix_suggestion", ""),
        fixed_version=result.get(
            "fixed_version", "latest"),
        risk_score=result.get("risk_score", 5),
        exploit_available=result.get("exploit_available", False), 
        version_affected=result.get("version_affected", True),
        priority=result.get("priority", "MEDIUM")
    )

@app.post("/analyze/batch",
          response_model=BatchAnalysisResponse)
def analyze_batch(request: BatchAnalysisRequest):

    results = []
    false_positive_count = 0

    for vuln in request.vulnerabilities:
        result = analyze_vulnerability(
            library_name=vuln.library_name,
            current_version=vuln.current_version,
            cve_id=vuln.cve_id,
            severity=vuln.severity,
            description=vuln.cve_description
        )

        analysis = AIAnalysisResult(
            library_name=vuln.library_name,
            cve_id=vuln.cve_id,
            is_false_positive=result.get(
                "is_false_positive", False),
            false_positive_reason=result.get(
                "false_positive_reason"),
            simple_explanation=result.get(
                "simple_explanation", ""),
            what_could_happen=result.get(
                "what_could_happen", ""),
            fix_suggestion=result.get(
                "fix_suggestion", ""),
            fixed_version=result.get(
                "fixed_version", "latest"),
            risk_score=result.get("risk_score", 5),
            exploit_available=result.get("exploit_available", False),
            version_affected=result.get("version_affected", True),
            priority=result.get("priority", "MEDIUM")
        )

        if analysis.is_false_positive:
            false_positive_count += 1

        results.append(analysis)

    return BatchAnalysisResponse(
        results=results,
        total_analyzed=len(results),
        false_positives_filtered=false_positive_count
    )



# import time
# from fastapi import FastAPI
# from fastapi.middleware.cors import CORSMiddleware
# from dotenv import load_dotenv
# from models.schemas import (
#     VulnerabilityInput,
#     AIAnalysisResult,
#     BatchAnalysisRequest,
#     BatchAnalysisResponse
# )
# from agents.vulnerability_agent import (
#     analyze_vulnerability
# )

# load_dotenv()

# app = FastAPI(
#     title="NexusGuard AI Service",
#     description="LangGraph powered vulnerability analysis"
# )

# app.add_middleware(
#     CORSMiddleware,
#     allow_origins=["http://localhost:8080"],
#     allow_methods=["*"],
#     allow_headers=["*"]
# )

# @app.get("/health")
# def health_check():
#     return {
#         "status": "NexusGuard AI Service running",
#         "version": "2.0 - LangGraph Agent"
#     }

# @app.post("/analyze",
#           response_model=AIAnalysisResult)
# def analyze_single(vuln: VulnerabilityInput):
#     result = analyze_vulnerability(
#         library_name=vuln.library_name,
#         current_version=vuln.current_version,
#         cve_id=vuln.cve_id,
#         severity=vuln.severity,
#         description=vuln.cve_description
#     )
#     return AIAnalysisResult(
#         library_name=vuln.library_name,
#         cve_id=vuln.cve_id,
#         is_false_positive=result.get(
#             "is_false_positive", False),
#         false_positive_reason=result.get(
#             "false_positive_reason"),
#         simple_explanation=result.get(
#             "simple_explanation", ""),
#         what_could_happen=result.get(
#             "what_could_happen", ""),
#         fix_suggestion=result.get(
#             "fix_suggestion", ""),
#         fixed_version=result.get(
#             "fixed_version", "latest"),
#         risk_score=result.get("risk_score", 5),
#         priority=result.get("priority", "MEDIUM")
#     )

# @app.post("/analyze/batch",
#           response_model=BatchAnalysisResponse)
# def analyze_batch(request: BatchAnalysisRequest):
#     results = []
#     false_positive_count = 0

#     for vuln in request.vulnerabilities:
#         result = analyze_vulnerability(
#             library_name=vuln.library_name,
#             current_version=vuln.current_version,
#             cve_id=vuln.cve_id,
#             severity=vuln.severity,
#             description=vuln.cve_description
#         )
#         analysis = AIAnalysisResult(
#             library_name=vuln.library_name,
#             cve_id=vuln.cve_id,
#             is_false_positive=result.get(
#                 "is_false_positive", False),
#             false_positive_reason=result.get(
#                 "false_positive_reason"),
#             simple_explanation=result.get(
#                 "simple_explanation", ""),
#             what_could_happen=result.get(
#                 "what_could_happen", ""),
#             fix_suggestion=result.get(
#                 "fix_suggestion", ""),
#             fixed_version=result.get(
#                 "fixed_version", "latest"),
#             risk_score=result.get("risk_score", 5),
#             priority=result.get("priority", "MEDIUM")
#         )
#         if analysis.is_false_positive:
#             false_positive_count += 1
#         results.append(analysis)
#         time.sleep(2)

#     return BatchAnalysisResponse(
#         results=results,
#         total_analyzed=len(results),
#         false_positives_filtered=false_positive_count
#     )