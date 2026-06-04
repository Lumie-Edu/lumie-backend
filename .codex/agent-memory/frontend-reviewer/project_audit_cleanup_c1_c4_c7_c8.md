---
name: project-audit-cleanup-c1-c4-c7-c8
description: FSD cycle fix, staff rename, verb-resource slice restructure, shadcn migration, landing widget move — key patterns and carry-forward issues from the ~142-file audit sweep
metadata:
  type: project
---

Multi-item audit cleanup landed together (~142 files, 2026-05-20).

**Why:** Cumulative FSD/arch debt: sessionCache in entities broke shared layer, employee→staff domain rename, flat feature dirs, Lumie Button/Card/Input diverged from shadcn, landing components in app/.

**How to apply:** These are done items. Future reviews should check:
1. `grade-management`, `grade-omr` query files lack `staleTime` — pre-existing, not introduced here. Next PR touching those files should add it (Warning-level per api-queries.md).
2. `src/features/grade-management/list-exams/api/queries.ts` and `view-grade-detail/api/queries.ts` carry `'use client'` — pre-existing carry-over; hooks use TanStack so it's technically benign but unusual for non-UI files.
3. `create-staff` and `list-staff` slices have no `index.ts` barrel — consumed directly via intra-feature path or root feature barrel. Acceptable shortcut but diverges from AGENTS.md slice convention.
4. `SESSION_ME_KEY = ['session', 'me']` in `shared/lib/sessionCache.ts` is an inline copy of `sessionKeys.me()` from `entities/session/model/queryKeys.ts`. A comment warns of the sync requirement; flag if either side changes.
5. Side-effect import at `app/layout.tsx:12` — `import "@/src/entities/session/lib/sessionCache"` — intentional bootstrapping; the shim re-exports from `shared/lib/sessionCache`.
6. `widgets/landing` components use standard Tailwind breakpoints (`sm:`, `md:`, `lg:`) — pre-existing, not new violations. Sweep will flag them; a lint exception is already in place per [[feedback_shadcn_sweep_breakpoint_conflict]].
