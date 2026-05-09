#!/bin/bash
# PreToolUse Edit|Write guard for Java files in lumie-backend.
# Blocks Category B violations from HARD_RULES_CROSS.md + BACKEND_RULES.md.

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
NEW_CONTENT=$(echo "$INPUT" | jq -r '.tool_input.content // .tool_input.new_string // empty')

# Only apply to Java files in lumie-backend
[[ "$FILE_PATH" != *"/lumie-backend/"*".java" ]] && exit 0

# Skip generated/build artifacts
case "$FILE_PATH" in
  */build/*|*/.gradle/*|*/target/*) exit 0 ;;
esac

# === LocalDateTime in DTOs ===
if echo "$NEW_CONTENT" | grep -qE "import\s+java\.time\.LocalDateTime"; then
  if echo "$FILE_PATH" | grep -qE "(Request|Response|Dto|application/dto)"; then
    echo "차단: LocalDateTime은 DTO에서 금지됩니다 (timezone 누락)." >&2
    echo "  → java.time.Instant 또는 OffsetDateTime을 사용하세요." >&2
    echo "  참고: BACKEND_RULES.md, HARD_RULES_CROSS.md" >&2
    exit 2
  fi
fi

# === Raw RuntimeException throw ===
if echo "$NEW_CONTENT" | grep -qE "throw\s+new\s+RuntimeException\("; then
  echo "차단: 원시 RuntimeException 사용 금지." >&2
  echo "  → 모듈의 BusinessException(ErrorCode) 서브클래스를 사용하세요." >&2
  echo "  참고: BACKEND_RULES.md" >&2
  exit 2
fi

# === Map<String, Object> in controllers ===
if echo "$FILE_PATH" | grep -qE "Controller\.java$"; then
  if echo "$NEW_CONTENT" | grep -qE "Map<String,\s*Object>"; then
    echo "차단: Controller에서 Map<String, Object> 사용 금지." >&2
    echo "  → 타입이 명시된 record DTO를 정의하세요." >&2
    exit 2
  fi
fi

# === @Setter on entities ===
if echo "$FILE_PATH" | grep -qE "/domain/entity/.*\.java$"; then
  if echo "$NEW_CONTENT" | grep -qE "@Setter"; then
    echo "차단: JPA entity에 @Setter 사용 금지." >&2
    echo "  → Builder 패턴 또는 명시적 도메인 메서드를 사용하세요." >&2
    exit 2
  fi
fi

# === Field injection ===
if echo "$NEW_CONTENT" | grep -qE "@Autowired\s*$"; then
  echo "차단: @Autowired 필드 주입 금지." >&2
  echo "  → @RequiredArgsConstructor (Lombok)으로 생성자 주입을 사용하세요." >&2
  exit 2
fi

# === New *-svc directory in NEW services ===
if echo "$FILE_PATH" | grep -qE "/services/[^/]+/[^/]+-svc/"; then
  PARENT=$(echo "$FILE_PATH" | grep -oE "/services/[^/]+/[^/]+-svc/" | head -1)
  PARENT_DIR="${FILE_PATH%%${PARENT}*}${PARENT}"
  if [ ! -d "$PARENT_DIR" ]; then
    SVC_NAME=$(echo "$PARENT" | grep -oE "[^/]+-svc")
    if ! grep -q "$SVC_NAME" "${FILE_PATH%/lumie-backend/*}/lumie-backend/settings.gradle.kts" 2>/dev/null; then
      echo "차단: 새 *-svc 디렉토리 생성 금지 (legacy MSA naming)." >&2
      echo "  → 새 모듈은 -svc 접미사 없이 생성하세요." >&2
      echo "  참고: ARCHITECTURE_INTENT.md" >&2
      exit 2
    fi
  fi
fi

exit 0
