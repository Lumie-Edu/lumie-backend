#!/usr/bin/env python3
"""Lumie forbidden-pattern sweep. SSOT for regex-based anti-pattern detection.

Pattern files live at `scripts/lumie-lint/patterns/<lang>.yaml`. Schema:

    patterns:
      - id: kebab-case-id
        severity: error | warn
        paths: ["<glob>", ...]   # OPTIONAL — restricts rule to matching paths
        msg: "human message"
        regex: "<ERE regex>"
        mode: line | multiline   # OPTIONAL — default line

Modes (mirror the prior bash sweep so callers don't change):
  --diff                          scan working-tree diff (per file)
  --staged                        scan staged diff (per file)
  --stdin [--path-context PATH]   scan stdin (PreToolUse hooks)
  --file PATH                     scan a single file
  --paths GLOB                    scan files matching glob (git ls-files)

Lang: backend | frontend | worker | all

Exit codes: 0 clean, 2 violations (PreToolUse-compatible block), 64 usage error.

Inline suppression — annotate the offending line OR the line immediately
above it with a comment containing ``lint: allow <rule-id>[, <rule-id>]``.
The marker is matched anywhere on the line so any comment syntax works
(Python ``#``, Java/TS ``//``). Suppression is localized to one offending
line; it does NOT silence other matches elsewhere in the file. Always
explain *why* in a sibling comment — the marker is a deliberate exemption
recorded in the diff, not a backdoor for bypassing the rule.
"""

from __future__ import annotations

import argparse
import fnmatch
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover - exercised in fresh local shells.
    yaml = None

LINT_DIR = Path(__file__).resolve().parent
PATTERNS_DIR = LINT_DIR / "patterns"

# Inline-suppression marker (see module docstring). Comma-separated rule ids.
ALLOW_RE = re.compile(r"lint:\s*allow\s+([A-Za-z0-9_-]+(?:\s*,\s*[A-Za-z0-9_-]+)*)")


@dataclass(frozen=True)
class Pattern:
    rule_id: str
    severity: str
    paths: tuple[str, ...]
    exclude_paths: tuple[str, ...]
    msg: str
    regex: re.Pattern[str]
    raw_regex: str
    mode: str


def _parse_extended_class(rx: str) -> str:
    """Convert grep ERE POSIX classes to Python equivalents.

    Currently supports `[[:space:]]` → `[ \\t\\r\\n\\f\\v]`. We use POSIX
    syntax in pattern files for grep compatibility (callers may still wire
    sweep into `grep -E`); Python re uses `\\s` semantics, so translate.
    """
    return rx.replace("[[:space:]]", r"[ \t\r\n\f\v]")


def _strip_yaml_comment(line: str) -> str:
    """Remove comments outside simple quoted strings.

    This fallback parser intentionally supports only the tiny YAML subset used
    by `scripts/lumie-lint/patterns/*.yaml`: top-level `patterns:`, list entries,
    scalar `key: value`, inline string arrays, and block string arrays.
    PyYAML remains the preferred parser when installed.
    """
    quote: str | None = None
    escaped = False
    out: list[str] = []
    for ch in line:
        if escaped:
            out.append(ch)
            escaped = False
            continue
        if ch == "\\" and quote == '"':
            out.append(ch)
            escaped = True
            continue
        if ch in {"'", '"'}:
            if quote == ch:
                quote = None
            elif quote is None:
                quote = ch
            out.append(ch)
            continue
        if ch == "#" and quote is None:
            break
        out.append(ch)
    return "".join(out).rstrip()


def _parse_yaml_scalar(value: str) -> object:
    value = value.strip()
    if value == "":
        return ""
    if value.startswith("[") and value.endswith("]"):
        inner = value[1:-1].strip()
        if not inner:
            return []
        items: list[str] = []
        current: list[str] = []
        quote: str | None = None
        escaped = False
        for ch in inner:
            if escaped:
                current.append(ch)
                escaped = False
                continue
            if ch == "\\" and quote == '"':
                current.append(ch)
                escaped = True
                continue
            if ch in {"'", '"'}:
                if quote == ch:
                    quote = None
                elif quote is None:
                    quote = ch
                current.append(ch)
                continue
            if ch == "," and quote is None:
                items.append(str(_parse_yaml_scalar("".join(current).strip())))
                current = []
                continue
            current.append(ch)
        items.append(str(_parse_yaml_scalar("".join(current).strip())))
        return items
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        return value[1:-1]
    return value


def _load_pattern_doc(path: Path) -> dict[str, object]:
    if yaml is not None:
        return yaml.safe_load(path.read_text()) or {}

    patterns: list[dict[str, object]] = []
    current: dict[str, object] | None = None
    pending_list_key: str | None = None

    for raw_line in path.read_text().splitlines():
        line = _strip_yaml_comment(raw_line)
        if not line.strip():
            continue
        stripped = line.strip()
        if stripped == "patterns:":
            continue
        if stripped.startswith("- "):
            rest = stripped[2:].strip()
            if ":" in rest:
                current = {}
                patterns.append(current)
                pending_list_key = None
                key, value = rest.split(":", 1)
                current[key.strip()] = _parse_yaml_scalar(value)
            elif pending_list_key and current is not None:
                current.setdefault(pending_list_key, [])
                current[pending_list_key].append(str(_parse_yaml_scalar(rest)))  # type: ignore[index, union-attr]
            continue
        if current is None or ":" not in stripped:
            continue
        key, value = stripped.split(":", 1)
        key = key.strip()
        value = value.strip()
        if value == "":
            current[key] = []
            pending_list_key = key
        else:
            current[key] = _parse_yaml_scalar(value)
            pending_list_key = None

    return {"patterns": patterns}


def load_patterns(lang: str) -> list[Pattern]:
    if lang == "all":
        files = ["backend.yaml", "frontend.yaml", "worker.yaml"]
    elif lang in {"backend", "frontend", "worker"}:
        files = [f"{lang}.yaml"]
    else:
        sys.stderr.write(f"sweep.py: unknown lang '{lang}'\n")
        sys.exit(64)

    out: list[Pattern] = []
    for f in files:
        path = PATTERNS_DIR / f
        if not path.is_file():
            sys.stderr.write(f"sweep.py: pattern file missing: {path}\n")
            sys.exit(64)
        doc = _load_pattern_doc(path)
        for entry in doc.get("patterns", []) or []:
            rid = entry.get("id", "?")
            sev = entry.get("severity", "error")
            msg = entry.get("msg", "?")
            paths = tuple(entry.get("paths") or ())
            exclude_paths = tuple(entry.get("exclude_paths") or ())
            raw = entry["regex"]
            mode = entry.get("mode", "line")
            flags = re.MULTILINE if mode == "multiline" else 0
            try:
                compiled = re.compile(_parse_extended_class(raw), flags)
            except re.error as e:
                sys.stderr.write(f"sweep.py: invalid regex in [{rid}]: {e}\n")
                sys.exit(64)
            if mode not in {"line", "multiline"}:
                sys.stderr.write(f"sweep.py: invalid mode in [{rid}]: {mode}\n")
                sys.exit(64)
            out.append(Pattern(rid, sev, paths, exclude_paths, msg, compiled, raw, mode))
    return out


def rule_applies(pat: Pattern, ctx_path: str | None) -> bool:
    """A pattern with no `paths` applies to all files. With `paths`, the
    context path must match at least one glob; if context is unknown
    (e.g. --stdin without --path-context), PATHS-restricted rules are
    SKIPPED to avoid spurious blocks on partial content.

    `exclude_paths` exempts matching paths even when `paths` would match
    (e.g. vendored/forked dirs); evaluated only when ctx_path is known so
    security rules without exclude_paths are never affected."""
    if ctx_path and pat.exclude_paths and any(
        fnmatch.fnmatch(ctx_path, g) for g in pat.exclude_paths
    ):
        return False
    if not pat.paths:
        return True
    if not ctx_path:
        return False
    return any(fnmatch.fnmatch(ctx_path, g) for g in pat.paths)


def _suppressions_for(lines: list[str], lineno: int) -> set[str]:
    """Rule ids exempted for ``lines[lineno-1]``.

    Honors a ``lint: allow <id>[, <id>]`` marker on the offending line itself
    (handy for short lines) or on the line immediately above (preferred for
    block contexts like ``except`` where the keyword should not be cluttered).
    """
    ids: set[str] = set()
    # 1-based; check prev line (lineno-1 → index lineno-2) and current line.
    for idx in (lineno - 2, lineno - 1):
        if 0 <= idx < len(lines):
            m = ALLOW_RE.search(lines[idx])
            if m:
                for s in m.group(1).split(","):
                    ids.add(s.strip())
    return ids


def scan_text(
    label: str, ctx_path: str | None, text: str, patterns: list[Pattern]
) -> int:
    violations = 0
    lines = text.splitlines()
    for pat in patterns:
        if not rule_applies(pat, ctx_path):
            continue
        if pat.mode == "multiline":
            for m in pat.regex.finditer(text):
                lineno = text.count("\n", 0, m.start()) + 1
                if pat.rule_id in _suppressions_for(lines, lineno):
                    continue
                snippet = m.group(0).splitlines()[0] if m.group(0) else ""
                print(f"{label}:{lineno}: [{pat.rule_id}] {pat.msg}")
                print(f"    > {snippet}")
                violations += 1
            continue
        for lineno, line in enumerate(lines, start=1):
            if pat.regex.search(line):
                if pat.rule_id in _suppressions_for(lines, lineno):
                    continue
                print(f"{label}:{lineno}: [{pat.rule_id}] {pat.msg}")
                print(f"    > {line}")
                violations += 1
    return violations


def scan_file(path: str, patterns: list[Pattern]) -> int:
    p = Path(path)
    if not p.is_file():
        return 0
    try:
        text = p.read_text(errors="replace")
    except OSError:
        return 0
    return scan_text(path, path, text, patterns)


def diff_files(staged: bool) -> list[str]:
    cmd = ["git", "diff"]
    if staged:
        cmd.append("--cached")
    else:
        cmd.append("HEAD")
    cmd.append("--name-only")
    try:
        out = subprocess.run(
            cmd, check=False, capture_output=True, text=True
        ).stdout
    except FileNotFoundError:
        return []
    return [f for f in out.splitlines() if f]


def ls_files(glob: str) -> list[str]:
    try:
        out = subprocess.run(
            ["git", "ls-files", "--", glob],
            check=False,
            capture_output=True,
            text=True,
        ).stdout
    except FileNotFoundError:
        return []
    return [f for f in out.splitlines() if f]


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        prog="sweep.py", description=__doc__, add_help=True
    )
    parser.add_argument("lang", choices=["backend", "frontend", "worker", "all"])
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--diff", action="store_true")
    mode.add_argument("--staged", action="store_true")
    mode.add_argument("--stdin", action="store_true")
    mode.add_argument("--file", metavar="PATH")
    mode.add_argument("--paths", metavar="GLOB")
    parser.add_argument("--path-context", default=None,
                        help="for --stdin: virtual path used to evaluate `paths` rules")
    args = parser.parse_args(argv)

    patterns = load_patterns(args.lang)
    violations = 0

    if args.stdin:
        text = sys.stdin.read()
        label = args.path_context or "<stdin>"
        violations += scan_text(label, args.path_context, text, patterns)
    elif args.file:
        violations += scan_file(args.file, patterns)
    elif args.diff:
        for f in diff_files(staged=False):
            violations += scan_file(f, patterns)
    elif args.staged:
        for f in diff_files(staged=True):
            violations += scan_file(f, patterns)
    elif args.paths:
        for f in ls_files(args.paths):
            violations += scan_file(f, patterns)

    return 2 if violations else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
