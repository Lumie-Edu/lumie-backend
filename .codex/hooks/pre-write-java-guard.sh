#!/bin/bash
# PreToolUse Edit|Write guard for Java files in lumie-backend.
# Delegates regex detection to .codex/lint/sweep.sh (workspace SSOT).
# Path-context-only logic (NEW *-svc directory creation) stays here.

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
NEW_CONTENT=$(echo "$INPUT" | jq -r '.tool_input.content // .tool_input.new_string // empty')

[[ "$FILE_PATH" != *"/lumie-backend/"*".java" ]] && exit 0
case "$FILE_PATH" in */build/*|*/.gradle/*|*/target/*) exit 0 ;; esac

# === Walk up to WORKSPACE_ROOT to locate workspace lint sweep ===
DIR=$(dirname "$FILE_PATH")
while [ "$DIR" != "/" ] && [ ! -f "$DIR/.codex/WORKSPACE_ROOT" ]; do
  DIR=$(dirname "$DIR")
done
SWEEP="$DIR/.codex/lint/sweep.sh"

# === Forbidden-pattern sweep (SSOT in .codex/lint/patterns/backend.yaml) ===
if [ -x "$SWEEP" ]; then
  HITS=$(printf '%s\n' "$NEW_CONTENT" | bash "$SWEEP" backend --stdin --path-context "$FILE_PATH")
  if [ -n "$HITS" ]; then
    echo "차단: 백엔드 금지 패턴 검출." >&2
    echo "$HITS" >&2
    echo "참고: .codex/lint/patterns/backend.yaml — 패턴은 그곳에서만 추가/수정." >&2
    exit 2
  fi
fi

# === Path-aware: block creation of files under legacy services/{tier}/<name>-svc/ paths ===
# After Tier 1 flattening, all backend modules live under modules/<name>/.
# Any write into services/* or under a *-svc directory is a relapse to the old layout.
if echo "$FILE_PATH" | grep -qE "/lumie-backend/services/|/lumie-backend/modules/[^/]+-svc/"; then
  echo "차단: 레거시 백엔드 모듈 경로 (services/ 또는 *-svc) 사용 금지." >&2
  echo "  → 모든 모듈은 modules/<name>/ 평탄 구조." >&2
  echo "  참고: lumie-backend/AGENTS.md, BACKEND_RULES.md" >&2
  exit 2
fi

exit 0
