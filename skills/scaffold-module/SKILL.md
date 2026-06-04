---
name: scaffold-module
description: Scaffold a new backend domain module under modules/. Composes backend-scaffolder + lumie-architect + lumie-domain-expert.
---

Invokes the `backend-scaffolder` agent with $ARGUMENTS, after `lumie-domain-expert` verifies the name and `lumie-architect` confirms placement. Auto-triggers `backend-reviewer` on completion.

See `lumie-backend/.codex/agents/backend-scaffolder.md` for layout, constraints, and self-verify. Constraints (canonical naming, hexagonal layout) live in `BACKEND_RULES.md`.
