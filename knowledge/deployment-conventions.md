<!-- SOURCE: user-template v3; do not edit in-project, edit user-level then re-sync -->

# 🚀 Deployment Conventions (condensed B10 matrix)

Seed page for this project's `knowledge/` bundle. **Platform-selection source:**
`C:\Dev\JARVIS\kb\dev\deployment-conventions.md` (the full B10 decision matrix) —
this copy exists so deployment choices in this project start from the house
conventions instead of being re-derived. For a guided choice, run
**`/deploy-advisor`** and record the decision in this bundle.

## TL;DR

Frontend → **Vercel**. Python API → **Cloud Run Service** (or all-in **HF Space**
for demos). Training **never runs inside an HTTP request** — app-triggered GPU
work uses the **dispatch pattern** to a serverless job tier (**Modal** = primary),
with **mandatory checkpointing** for hours-long runs. **Firebase** is a services
menu (Firestore/Auth/FCM from any host), not a hosting choice. Tabular/clinical
models need **T4/L4-class GPUs**, not A100s.

## Condensed matrix

| Platform | Use for | Watch out |
|---|---|---|
| Vercel | Frontend + light serverless; container images run **as Functions** | Function duration/memory limits — not for training or fat torch images |
| Cloud Run **Service** | Containerized APIs, scale-to-zero | ~60 min request cap → serving only |
| Cloud Run **Job** | Run-to-completion **training** (~24 h, GPU-capable) | Pair with Services doing the serving |
| HF Spaces | ML demos; ZeroGPU for inference demos | GPU tier only if the app needs GPU continuously |
| Modal | **Primary for app-triggered GPU jobs** — per-second billing, ~24 h timeouts, spot-safe | Requires checkpointing for spot safety |
| RunPod / Vast | Very long / budget training pods | More ops burden |
| Colab / Kaggle | Interactive human GPU only | No job API / quotas — never an app backend |

## Hours-long training rules

1. Never inside an HTTP request, on any platform.
2. Use a job tier: Modal (to ~24 h) or **Cloud Run Jobs**; very long/cheap →
   rented pods.
3. **Checkpointing is mandatory** — crash/preemption resume + enables cheap spot
   instances; checkpoints go to the artifact/DVC store.
4. Cost sanity: T4 ≈ $0.4–0.6/hr, A10G/L4 ≈ $0.7–1.2/hr.

## Dispatch pattern (app-triggered GPU)

```
CPU backend ──enqueue──► serverless GPU job (Modal / Cloud Run Job)
     ▲                         │  pushes artifacts → DVC store / registry
     └──── polls job status ◄──┘  (status JSON → UI progress)
```

<!-- APPLICATION-DELIVERY:BEGIN -->
## Three application delivery modes

1. **D01 - Every application and update.** Deliver three modes: hosted/web online,
   complete offline Docker, and complete offline without Docker. Every later
   feature, fix, dependency or security update must reach all three.
2. **D02 - One implementation, preserved behavior and data.** Build all modes from
   one canonical implementation and the same application version/source revision.
   Preserve functionality, choices, features and private library data through
   updates. Document infrastructure differences; they cannot erase functionality.
3. **D03 - Complete offline packages.** Bundle runtime, locked dependencies,
   assets/models, local service replacements and startup instructions. Docker
   includes loadable images and orchestration. Standalone Python includes a base
   interpreter and local dependencies to create a venv on the target; other stacks
   include their appropriate standalone runtime. Required host prerequisites must
   also be available offline under existing approval/licensing gates. Declare the
   supported OS, architecture and hardware. A source-only ZIP, developer cache,
   Internet image pull or remote provider API is not an offline delivery.
4. **D04 - Versioned handover.** Provide versioned package/download locations and
   installation, start, stop, persistence and upgrade instructions. Record the
   common application version/source revision, mode-specific build/configuration
   and artifact hashes; protect existing user data.
5. **D05 - Prove first delivery and every update.** Build/rebuild all three and
   execute their applicable acceptance/tests at every existing layer. Verify
   matching identity and behavior using the same synthetic scenarios, including
   save/reopen and upgrade data preservation. Test each offline package's
   installation, first start and complete workflows with Internet/provider access
   blocked. Permit loopback/local services and hardware I/O required by the
   documented offline topology. Bind evidence to the actual artifacts/configuration;
   stale, missing or unverified modes remain work left. Existing comparable-evidence
   reuse applies only to unchanged checks; changed packages still require fresh
   install/start and affected acceptance.
6. **D06 - Preserve gates and expose feasibility conflicts.** This requirement
   authorizes no provider deployment, release publication, spending, credential
   disclosure, private-data transfer or additional access. Existing privacy, cost,
   licensing and approval gates still apply. Use the registered synthetic identity
   for acceptance. Native/mobile applications and provider-dependent features
   remain in scope: report a concrete feasibility conflict for the user's decision,
   without inventing exemptions, dropping features or claiming all modes done.
<!-- APPLICATION-DELIVERY:END -->

## Deployment and repair verification

1. Prefer available, reliable API/CLI/MCP checks for release identity, file hashes,
   status, uploads, polling, access control, archives and exports. Use browser
   control for UI-only authentication and visible behavior; verify the current
   tool contract before choosing a route.
2. Before deploying a new hosted runtime or changing SDK, provider-default or
   process-lifecycle configuration, preflight the relevant startup seam. After
   a hosted startup fault, reproduce the smallest failing seam before another
   provider retry. Exercise the pinned SDK registration hooks, provider defaults,
   actual mounted lifespan, process/port ownership and clean shutdown. Reuse a
   valid unchanged preflight. State controlled substitutions and untested hosted
   behavior; local CPU readiness proves neither hosted acceptance nor GPU allocation.
3. Bind evidence to the relevant source, input, configuration, environment and
   release. Report publication/hash checks, server readiness, the user's
   end-to-end outcome and measured timings as separate results.
4. Automate the applicable acceptance sequence: authorized sign-in, stable
   upload/retry, progressive processing and result/provenance, owner and
   anonymous-access checks, explicit archive save/reopen, then exports. Retain
   artifact IDs and checkpoints so a retry resumes without duplicating writes;
   protect credentials and private payloads.
5. Reuse completed evidence only while its relevant code, input, configuration
   and environment remain comparable. SDK/default/lifecycle changes invalidate
   startup evidence; detector/tracking/motion/input changes invalidate affected
   analysis; render/unit changes invalidate visual/export evidence; resource or
   hardware changes invalidate performance, GPU and cost claims. Rerun or
   recompute affected consumers and preserve unchanged processing baselines.
6. Use focused red/green checks during repair, then complete every existing
   required test layer and regression obligation before declaring the change
   done. A stale pass cannot verify changed behavior; distinguish inherited
   evidence, newly run checks and absent layers in the repair record.
7. Preserve strict failures and raw outputs. Compare differences at the actual
   consumer in explicit units under the accepted comparison policy; do not
   introduce an unapproved tolerance or infer physical accuracy. Inspect actual
   rendered playback, fullscreen behavior and PDF pages when applicable: API,
   DOM or screenshot success alone does not prove dynamic or native fullscreen
   behavior. Keep unsupported or denied checks unverified.
8. Keep existing publication, privacy, approval and no-added-cost constraints.
   Availability, RUNNING status or displayed quota does not prove allocation or
   billing enforcement. Claim a speedup or a dominant source of delay only from
   measured, comparable evidence.

## Decisions taken in this project

<!-- Append one dated line per deployment decision, with the /deploy-advisor
     reasoning or a link to the fuller decision page in this bundle. -->
- *(none yet)*
