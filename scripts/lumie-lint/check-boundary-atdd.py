#!/usr/bin/env python3
"""Require one passing JUnit owner for every backend-boundary acceptance example."""

from collections import defaultdict
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


EXPECTED = {
    *(f"ATDD-US1-001:example-{number:03d}" for number in range(1, 15)),
    "ATDD-US1-002:example-001",
    "ATDD-US1-003:example-001",
    "ATDD-US1-004:example-001",
}
ID_PATTERN = re.compile(r"ATDD-US1-00[1-4]:example-\d{3}")


def main() -> int:
    owners: dict[str, set[tuple[str, str]]] = defaultdict(set)
    failed: set[str] = set()
    results = [
        *Path("app/build/test-results").glob("*/TEST-*.xml"),
        *Path("modules").glob("*/build/test-results/*/TEST-*.xml"),
    ]
    for result in results:
        try:
            suite = ET.parse(result).getroot()
        except ET.ParseError as error:
            print(f"Cannot parse {result}: {error}", file=sys.stderr)
            return 2
        for testcase in suite.findall("testcase"):
            match = ID_PATTERN.search(testcase.attrib.get("name", ""))
            if match is None:
                continue
            scenario_id = match.group()
            owners[scenario_id].add((
                testcase.attrib.get("classname", ""),
                testcase.attrib.get("name", ""),
            ))
            if testcase.find("failure") is not None or testcase.find("error") is not None:
                failed.add(scenario_id)
            if testcase.find("skipped") is not None:
                failed.add(scenario_id)

    missing = EXPECTED - owners.keys()
    unexpected = owners.keys() - EXPECTED
    duplicate = {scenario_id: methods for scenario_id, methods in owners.items()
                 if len(methods) != 1}
    for scenario_id in sorted(EXPECTED):
        status = "PASS" if scenario_id in owners and scenario_id not in failed else "FAIL"
        print(f"{scenario_id}: {status}")
    if missing:
        print(f"Missing scenario IDs: {', '.join(sorted(missing))}", file=sys.stderr)
    if unexpected:
        print(f"Unexpected scenario IDs: {', '.join(sorted(unexpected))}", file=sys.stderr)
    if duplicate:
        print(f"IDs with multiple owning methods: {duplicate}", file=sys.stderr)
    if failed:
        print(f"Failed or skipped scenarios: {', '.join(sorted(failed))}", file=sys.stderr)
    if not results:
        print("No JUnit XML results found.", file=sys.stderr)
    return 1 if missing or unexpected or duplicate or failed or not results else 0


if __name__ == "__main__":
    raise SystemExit(main())
