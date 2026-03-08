from pydantic import BaseModel
from typing import List, Optional

class VulnerabilityInput(BaseModel):
    library_name: str
    current_version: str
    cve_id: str
    cve_description: str
    severity: str

class AIAnalysisResult(BaseModel):
    library_name: str
    cve_id: str
    is_false_positive: bool
    false_positive_reason: Optional[str] = None
    simple_explanation: str
    what_could_happen: str
    fix_suggestion: str
    fixed_version: str
    exploit_available: bool = False
    version_affected: bool = True
    risk_score: int
    priority: str

class BatchAnalysisRequest(BaseModel):
    vulnerabilities: List[VulnerabilityInput]

class BatchAnalysisResponse(BaseModel):
    results: List[AIAnalysisResult]
    total_analyzed: int
    false_positives_filtered: int