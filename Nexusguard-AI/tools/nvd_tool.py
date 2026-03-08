import os
import requests
from langchain_core.tools import tool

@tool
def get_cve_details(cve_id: str) -> str:
    """
    Fetches detailed CVE information from 
    National Vulnerability Database.
    Use this to get full vulnerability details
    including affected versions and severity.
    """
    api_key = os.getenv("NVD_API_KEY")

    url = f"https://services.nvd.nist.gov/rest/json/cves/2.0?cveId={cve_id}"

    headers = {}
    if api_key:
        headers["apiKey"] = api_key

    try:
        response = requests.get(
            url,
            headers=headers,
            timeout=10)

        data = response.json()

        if not data.get("vulnerabilities"):
            return f"No details found for {cve_id}"

        vuln = data["vulnerabilities"][0]["cve"]

        # Get description
        description = "No description"
        for desc in vuln.get("descriptions", []):
            if desc.get("lang") == "en":
                description = desc.get("value", "")
                break

        # Get CVSS score
        score = "N/A"
        severity = "UNKNOWN"
        metrics = vuln.get("metrics", {})

        if "cvssMetricV31" in metrics:
            cvss = metrics["cvssMetricV31"][0]["cvssData"]
            score = cvss.get("baseScore", "N/A")
            severity = cvss.get("baseSeverity", "UNKNOWN")
        elif "cvssMetricV30" in metrics:
            cvss = metrics["cvssMetricV30"][0]["cvssData"]
            score = cvss.get("baseScore", "N/A")
            severity = cvss.get("baseSeverity", "UNKNOWN")

        # Get affected versions from configurations
        affected_versions = []
        for config in vuln.get("configurations", []):
            for node in config.get("nodes", []):
                for cpe in node.get("cpeMatch", []):
                    if cpe.get("vulnerable"):
                        version_end = cpe.get(
                            "versionEndExcluding",
                            cpe.get("versionEndIncluding", ""))
                        if version_end:
                            affected_versions.append(
                                version_end)

        return f"""
CVE ID: {cve_id}
Severity: {severity}
CVSS Score: {score}
Description: {description}
Fixed in versions before: {', '.join(affected_versions) if affected_versions else 'Check NVD'}
        """

    except Exception as e:
        return f"Error fetching CVE details: {str(e)}"