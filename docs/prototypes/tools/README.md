# Prototype tools

## `shoot.ps1` — render a prototype to a PNG so the agent can look at it

Built during [`C12` #31](https://github.com/idomarhaim/Android_Final_Project/issues/31), after
three revisions of a 3D chart were argued in prose and rejected on sight. With it, ten review
rounds ran in a single turn and **eight of the nine defects found had been invisible in the
source** — including two the human had not reported.

```powershell
cd docs\prototypes\tools
./shoot.ps1 -Page ..\2026-08-11-visual-styles\index.html -Out page.png `
            -Query "style=darkneo&arc=raised&canvas=native" -Width 520 -Height 1180
```

`-Probe "<js>"` appends a script to a **throwaway copy** of the page (the prototype is never
modified) so a single component can be rendered large and close up — that is what makes small
defects visible. The page's own functions and state are reachable from the probe because
top-level `let` in a classic script is shared across `<script>` tags.

**The rule this exists to serve:** when the acceptance criterion is visual, render and *look*
between revisions. A round whose render is unchanged is a **result** — it falsifies the fix
immediately, which prose review cannot do.
