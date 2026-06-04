---
name: project_analysis_svc_refactor
description: analysis-svc was decomposed from a 412-line monolithic main.py into canonical per-service layout (main/config/wiring/schema/usecase/adapters) in May 2026
metadata:
  type: project
---

The 412-line monolithic `services/analysis/main.py` was split into the canonical layout:
- `main.py` — thin FastAPI entry point + lifespan
- `src/config.py` — Pydantic BaseSettings (llm_api_key has no default — fail-fast)
- `src/wiring.py` — Container dataclass + build_container()
- `src/schema.py` — all Pydantic v2 models
- `src/usecase.py` — AnalysisUseCase with prompt builders
- `src/adapters/llm.py` — LlmAdapter wrapping AsyncOpenAI

**Why:** monolith was untestable and violated WORKER_RULES layout canonical.

**How to apply:** When reviewing or scaffolding analysis-svc, use this layout as the baseline. Five blockers were found in the refactor — see feedback_analysis_svc_review_blockers.

Known issues found in the refactor (2026-05-17):
- `pydantic-settings` missing from requirements.txt and pyproject.toml (C3 — build-breaking)
- `APIError` re-wrapped in `APIError` constructor in usecase.py — TypeError at runtime (W3)
- `assert` used as production guard instead of explicit HTTPException (C1)
- `except Exception` without logging (C2)
- `message.content` is Optional[str] with no None guard (W2)
