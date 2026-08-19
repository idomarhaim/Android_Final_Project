# `new-machine-template-sync` — 2026-08-19

> **Summary:** First session on the new machine (old one in repair). Verbatim template projections brought current from the JARVIS library — `general.instructions.md` v17→v18, `testing.instructions.md` v2→v3 (both applied by `Update-TemplateConsumers.ps1`, provenance-verified blob-SHA upgrades). Machine-side, not in this commit: git hooks reinstalled (`Install-GitHooks.ps1`), `local.properties` regenerated, both AVDs (`Pixel_10_Pro_XL`, `Pixel_10_Pro_XL_B`) recreated on API 35 and verified to full boot.

**Branch:** `feat/goalpilot-implementation`.
**Scope:** template projections only — no app code, no resources.

---

Migration bookkeeping. The clone was already current with origin (the branch tip
`d707336` was pushed before the old machine went to repair), but two verbatim
template projections were one library release behind, and everything that lives
outside git — hooks, `local.properties`, the AVDs — had to be rebuilt by hand on
the new machine. This entry exists so the day the repo moved machines is visible
in its own history.
