---
name: scaffold-module
description: Scaffold a new backend domain module following hexagonal layout. Compose backend-scaffolder + lumie-architect + lumie-domain-expert verification.
user-invocable: true
argument-hint: <module-name> [tier=core|platform|support]
---

Scaffold a new backend module.

## Steps

1. If $ARGUMENTS empty, ask user for module name + tier.
2. Invoke `lumie-domain-expert` to verify name against DOMAIN_GLOSSARY (refuse if forbidden alias).
3. Invoke `lumie-architect` for tier + placement decision.
4. Invoke `backend-scaffolder` with verified name + tier:
   - Generate `services/{tier}/{module}/` directory (NO `-svc` suffix per HARD_RULES).
   - Hexagonal layout: `domain/{entity,vo,repository,exception}`, `application/{service,port,dto}`, `adapter/{in/web,out/persistence}`.
   - `{Module}ErrorCode` enum extending `BusinessException`.
   - Initial `*Controller.java` skeleton with `/v1/...` mapping.
   - Entity extends `BaseEntity`, has `@Version` for user-mutable.
   - `build.gradle.kts` with proper deps (Lombok, validation; NO unused MapStruct).
5. Add module to `settings.gradle.kts`.
6. Auto-trigger `backend-reviewer` to validate.
7. Output summary with self-verify checklist.

## When to use

- Adding a new domain area (e.g., `meeting`, `survey`, `feedback`).
- Splitting an oversized module.

## When NOT to use

- Adding a controller/service inside an existing module (use `add-endpoint`).
- Renaming an existing module (use `/refactor`).

## Constraints

- NO `-svc` suffix on new module directory.
- Module name MUST be canonical per DOMAIN_GLOSSARY.
- `backend-reviewer` MUST report ✅ before claiming done.
