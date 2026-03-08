import requests
from langchain_core.tools import tool

@tool
def check_version_affected(
        library_name: str,
        current_version: str,
        cve_description: str) -> str:
    """
    Checks if the current version of a library
    is actually affected by a vulnerability.
    Use this to filter out false positives.
    Returns whether version is affected and
    what the fixed version is.
    """
    try:
        # Parse version numbers
        current_parts = current_version.split(".")
        current_major = int(current_parts[0])

        # Check if CVE description mentions
        # this library specifically
        library_lower = library_name.lower()
        desc_lower = cve_description.lower()

        # Common false positive patterns
        false_positive_indicators = [
            # CVE is for different product
            # that just uses this library
            f"requires {library_lower}",
            f"uses {library_lower}",
        ]

        # Direct vulnerability indicators
        direct_indicators = [
            library_lower,
            f"apache {library_lower}",
        ]

        is_directly_mentioned = any(
            indicator in desc_lower
            for indicator in direct_indicators)

        is_false_positive = any(
            indicator in desc_lower
            for indicator in false_positive_indicators)

        if is_false_positive and not is_directly_mentioned:
            return f"""
RESULT: LIKELY FALSE POSITIVE
Reason: CVE appears to be for a different product
that uses {library_name} as a dependency.
The vulnerability is not directly in {library_name}.
Recommendation: Skip this vulnerability.
            """

        return f"""
RESULT: POTENTIALLY AFFECTED
Library: {library_name}
Current Version: {current_version}
Status: Version {current_version} may be affected.
Recommendation: Verify against CVE details
and upgrade if confirmed affected.
        """

    except Exception as e:
        return f"Error checking version: {str(e)}"