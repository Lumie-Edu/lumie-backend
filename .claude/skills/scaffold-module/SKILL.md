---
name: scaffold-module
description: Scaffold a new backend domain module following hexagonal layout. Composes backend-scaffolder + lumie-architect + lumie-domain-expert.
user-invocable: true
argument-hint: <module-name> [tier=core|platform|support]
---

Invokes the workspace `backend-scaffolder` agent with $ARGUMENTS, after `lumie-domain-expert` verifies the name and `lumie-architect` confirms tier/placement. Auto-triggers `backend-reviewer` on completion.

See `.claude/agents/backend/backend-scaffolder.md` (workspace) for layout, constraints, and self-verify. Constraints (no `-svc` suffix, canonical naming, hexagonal layout) live in `BACKEND_RULES.md`.
