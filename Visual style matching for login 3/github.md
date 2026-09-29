repo: QuyenLuvBunBo/dms
branch: main
path: frontend

## Last sync
date: 2026-09-27T19:44:17Z

### Updated in this project
- Redesigned for the new process (updated CLAUDE.md / BUILD_PLAN.md + HUST K71 notice): rounds, declaration, 4-step wizard, hold & payment, residence, check-in, round setup
- Sidebar nav updated for new roles (BUILDING_MANAGER replaces AFFAIRS) — repo navigation.ts not yet updated
- Font: Be Vietnam Pro

## Screen map
| Project screen | Repo files |
|---|---|
| Sidebar.dc.html | frontend/src/layout/Sidebar.tsx, frontend/src/layout/navigation.ts, frontend/src/api/types.ts |
| DMS Registration Flow.dc.html (layout, cards, forms, alerts) | frontend/src/layout/AppLayout.tsx, frontend/src/pages/HomePage.tsx, frontend/src/pages/LoginPage.tsx, frontend/src/index.css |

## Sync history
### 2026-09-26T21:03:20Z
- Phase 2–3 mockups (scoring-based; since discarded)
- Sidebar recreated as a reusable component from Sidebar.tsx + navigation.ts
