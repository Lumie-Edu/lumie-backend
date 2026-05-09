#!/bin/bash
# PreToolUse Edit|Write guard for Java files in lumie-backend.
# Delegates regex detection to .claude/lint/sweep.sh (workspace SSOT).
# Path-context-only logic (NEW *-svc directory creation) stays here.

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
NEW_CONTENT=$(echo "$INPUT" | jq -r '.tool_input.content // .tool_input.new_string // empty')

[[ "$FILE_PATH" != *"/lumie-backend/"*".java" ]] && exit 0
case "$FILE_PATH" in */build/*|*/.gradle/*|*/target/*) exit 0 ;; esac

# === Walk up to WORKSPACE_ROOT to locate workspace lint sweep ===
DIR=$(dirname "$FILE_PATH")
while [ "$DIR" != "/" ] && [ ! -f "$DIR/.claude/WORKSPACE_ROOT" ]; do
  DIR=$(dirname "$DIR")
done
SWEEP="$DIR/.claude/lint/sweep.sh"

# === Forbidden-pattern sweep (SSOT in .claude/lint/patterns/backend.yaml) ===
if [ -x "$SWEEP" ]; then
  HITS=$(printf '%s\n' "$NEW_CONTENT" | bash "$SWEEP" backend --stdin --path-context "$FILE_PATH")
  if [ -n "$HITS" ]; then
    echo "차단: 백엔드 금지 패턴 검출." >&2
    echo "$HITS" >&2
    echo "참고: .claude/lint/patterns/backend.yaml — 패턴은 그곳에서만 추가/수정." >&2
    exit 2
  fi
fi

# === Path-aware: block creation of NEW *-svc directories ===
if echo "$FILE_PATH" | grep -qE "/services/[^/]+/[^/]+-svc/"; then
  PARENT=$(echo "$FILE_PATH" | grep -oE "/services/[^/]+/[^/]+-svc/" | head -1)
  PARENT_DIR="${FILE_PATH%%${PARENT}*}${PARENT}"
  if [ ! -d "$PARENT_DIR" ]; then
    SVC_NAME=$(echo "$PARENT" | grep -oE "[^/]+-svc")
    if ! grep -q "$SVC_NAME" "${FILE_PATH%/lumie-backend/*}/lumie-backend/settings.gradle.kts" 2>/dev/null; then
      echo "차단: 새 *-svc 디렉토리 생성 금지." >&2
      echo "  → 새 모듈은 -svc 접미사 없이 생성하세요. 참고: ARCHITECTURE_INTENT.md" >&2
      exit 2
    fi
  fi
fi

exit 0
