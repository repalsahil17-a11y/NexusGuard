from langchain_core.tools import tool

@tool
def assess_risk_level(
        severity: str,
        library_name: str,
        is_false_positive: bool) -> str:
    """
    Assesses the actual risk level and priority
    of a vulnerability based on severity and context.
    Use this to determine how urgently 
    this needs to be fixed.
    """

    if is_false_positive:
        return """
Risk Score: 0/10
Priority: IGNORE
Reason: False positive — not actually vulnerable
        """

    severity_scores = {
        "CRITICAL": 9,
        "HIGH": 7,
        "MEDIUM": 5,
        "LOW": 2,
        "UNKNOWN": 3
    }

    base_score = severity_scores.get(
        severity.upper(), 3)

    if base_score >= 8:
        priority = "IMMEDIATE"
        action = "Fix before next deployment"
    elif base_score >= 6:
        priority = "HIGH"
        action = "Fix within this sprint"
    elif base_score >= 4:
        priority = "MEDIUM"
        action = "Fix in next planned update"
    else:
        priority = "LOW"
        action = "Fix when convenient"

    return f"""
Risk Score: {base_score}/10
Priority: {priority}
Required Action: {action}
Severity Level: {severity}
    """