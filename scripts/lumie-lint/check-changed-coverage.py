#!/usr/bin/env python3
"""Verify JaCoCo coverage on executable Java lines added since a Git base."""

import argparse
from collections import defaultdict
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def added_lines(base: str) -> dict[str, set[int]]:
    diff = subprocess.check_output(
        ["git", "diff", "--find-renames", "--no-ext-diff", "--no-color",
         "--unified=0", f"{base}...HEAD", "--", "modules"],
        text=True,
    )
    added: dict[str, set[int]] = defaultdict(set)
    path = None
    line_number = None
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            path = line[6:]
        elif line.startswith("+++ /dev/null"):
            path = None
        elif line.startswith("@@"):
            match = re.search(r"\+(\d+)(?:,\d+)?", line)
            line_number = int(match.group(1)) if match else None
        elif path and line_number is not None and line.startswith("+"):
            added[path].add(line_number)
            line_number += 1
        elif line_number is not None and line.startswith(" "):
            line_number += 1
    return added


def jacoco_source_lines(report: Path) -> dict[tuple[str, str], dict[int, dict[str, str]]]:
    root = ET.parse(report).getroot()
    return {
        (package.attrib["name"], source.attrib["name"]): {
            int(line.attrib["nr"]): line.attrib
            for line in source.findall("line")
        }
        for package in root.findall("package")
        for source in package.findall("sourcefile")
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True, help="Git base revision before the change")
    parser.add_argument("--line-min", type=int, default=90)
    parser.add_argument("--branch-min", type=int, default=85)
    parser.add_argument("--allow-empty", action="store_true",
                        help="Report N/A when the compared revision changes no module Java")
    args = parser.parse_args()
    if not 0 <= args.line_min <= 100 or not 0 <= args.branch_min <= 100:
        parser.error("coverage thresholds must be between 0 and 100")

    try:
        added = added_lines(args.base)
    except subprocess.CalledProcessError as error:
        print(f"Cannot compare HEAD with {args.base}: {error}", file=sys.stderr)
        return 2
    modules = sorted({
        PurePosixPath(path).parts[1]
        for path in added
        if path.startswith("modules/") and path.endswith(".java")
    })
    if not modules:
        if args.allow_empty:
            print("N/A: no changed module Java lines in the compared revision.")
            return 0
        print("No changed module Java lines found; refusing an empty coverage gate.", file=sys.stderr)
        return 2

    failed = False
    for module in modules:
        report = Path(f"modules/{module}/build/reports/jacoco/test/jacocoTestReport.xml")
        if not report.exists():
            print(f"{module}: missing JaCoCo XML at {report}", file=sys.stderr)
            failed = True
            continue
        source_lines = jacoco_source_lines(report)
        covered_lines = total_lines = covered_branches = total_branches = 0
        for path, numbers in added.items():
            parts = PurePosixPath(path).parts
            if (len(parts) < 8 or parts[:5] != ("modules", module, "src", "main", "java")
                    or not path.endswith(".java")):
                continue
            lines = source_lines.get(("/".join(parts[5:-1]), parts[-1]), {})
            for number in numbers:
                result = lines.get(number)
                if result is None:
                    continue
                missed_instructions = int(result["mi"])
                covered_instructions = int(result["ci"])
                if missed_instructions + covered_instructions:
                    total_lines += 1
                    covered_lines += covered_instructions > 0
                missed_branches = int(result["mb"])
                covered = int(result["cb"])
                total_branches += missed_branches + covered
                covered_branches += covered

        line_result = f"{covered_lines}/{total_lines}" if total_lines else "N/A"
        branch_result = f"{covered_branches}/{total_branches}" if total_branches else "N/A"
        line_pass = not total_lines or covered_lines * 100 >= total_lines * args.line_min
        branch_pass = not total_branches or covered_branches * 100 >= total_branches * args.branch_min
        status = "PASS" if line_pass and branch_pass else "FAIL"
        print(f"{module}: {status} changed line={line_result} branch={branch_result}")
        failed |= not line_pass or not branch_pass
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
