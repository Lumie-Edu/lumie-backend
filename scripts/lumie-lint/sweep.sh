#!/usr/bin/env bash
# Thin wrapper around sweep.py — exists so callers can invoke
# `bash scripts/lumie-lint/sweep.sh ...` without depending on python3 being
# on PATH-as-default-interpreter. The Python script owns all logic.
exec python3 "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/sweep.py" "$@"
