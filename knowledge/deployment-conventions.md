<!-- SOURCE: user-template v1; do not edit in-project, edit user-level then re-sync -->

# 🚀 Deployment Conventions (condensed B10 matrix)

Seed page for this project's `knowledge/` bundle. **Source of truth:**
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

## Decisions taken in this project

<!-- Append one dated line per deployment decision, with the /deploy-advisor
     reasoning or a link to the fuller decision page in this bundle. -->
- *(none yet)*
