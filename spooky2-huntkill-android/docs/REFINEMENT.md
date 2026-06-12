# Hunt & Kill — Refinement Generations (narrow-around-hits)

This documents the **refinement** feature: after the first biofeedback scan finds
hit frequencies and treats ("kills") them, each subsequent generation re-scans a
**narrow window around each previous hit** at a **halved step**, instead of
re-sweeping the whole range. This is the original Spooky2 "Hunt and Kill"
behavior ("scan, treat, refine scan, treat, refine … until you quit").

## Source of truth

Two sources, cross-checked:

1. **Spooky2 Users Guide** (`Spooky2_Users_Guide_20250124.pdf`), Biofeedback Scan pane:
   - #23 **Refine +/-** (`r`): "For every found frequency `f` in the initial scan,
     given a refine value `r`, the range from `f-r` through `f+r` is scanned."
   - #29 **Continue Refining Hits**: "after the first scan and treatment, the
     subsequent scans are refinement of the previous scan."
   - #31 **Run Cycles** (`BFB_Repeat_BFB`): number of scan/treat pairs; `0` = repeat
     until stopped.

2. **Decompiled `Spooky.exe` (VB6)** — `Main.frm` (VB-Decompiler dump). Evidence:

| Fact | Evidence (`Main.frm`) |
|---|---|
| Per-hit window `newStart = CLng(hit − r)`, `newFinish = CLng(hit + r)` | lines 70211 / 70215 |
| Step **halved** each refining generation: `step = step / 2` | lines 64374, 68301 |
| Bounds clamped (lower floored, upper clamped to ceiling) | 70802–70806 (cleaner copy) |
| Each generation **replaces** the hit list; per-hit windows concatenated | 66509–66560 |
| Mode flag: 3 = initial/grade scan, 2 = refine scan | 64559/64563, 65056, 65127 |
| Repeat-cycle counter (`BFB_Repeat_BFB`) | 32785, 68336, 68344 |
| `Refine +/-` = preset key `BFB_Include_x_Hz_In_Search` | 48545–48550, read 68284 |

Canonical preset **GX Hunt and Kill (C) - JW**: `BFB_Continue_Refining_Hits=1`,
`BFB_Repeat_BFB=0`, `BFB_Include_x_Hz_In_Search=0`, `BFB_Initial_Step_Size_Hz=100`
(percentage step actually used = 0.025%), `BFB_Max_Hits_To_Find=10`,
range 41000–1800000.

## Not bit-recoverable (flagged)

The VB-Decompiler **dropped the assignment** of the half-width temp (`var_1EC`) on the
`r == 0` path in both decompiled copies (only the `hit ± var_1EC` arithmetic
survived). So the exact window when `BFB_Include_x_Hz_In_Search = 0` — the canonical
preset value — is **not** bit-provable from the dump.

The surrounding evidence (the sweep is step-based and the step is explicitly halved
each generation) ties the window to the step size. We therefore **derive**, when
`refinePlusMinusHz == 0`:

```
r = REFINE_WINDOW_STEPS × localCoarseStep(hit)
localCoarseStep(hit) = hit × stepSizePercent/100   (percentage mode)
                     = stepSizeHz                   (linear mode)
REFINE_WINDOW_STEPS  = 10   (each side)
```

`localCoarseStep` is always computed from the **original generation-1 step** (the
half-width basis passed to `RefinementPlanner.planNextGeneration`), so the window
width is CONSTANT across generations — ±250 Hz around a 1 MHz hit with the 0.025%
step, every generation. Only the sweep step halves. (Deriving it from the current
generation's already-halved step would shrink the window each pass and eventually
miss the refined hit.)

Runaway guard: `frequencyStepsFor` aborts (returns empty, ending the refinement
loop) if the halved step falls below `MIN_STEP_HZ` (1 µHz) — below that,
double-precision addition can no longer advance the frequency and the grid loop
would otherwise spin forever in non-suspending code.

This is a **reasoned default**, flagged for later hardware/screenshot verification —
consistent with how this port already documents other unproven constants
(`rampSteps`, `dwellSeconds`). When a user supplies an explicit `refinePlusMinusHz > 0`,
that value is used verbatim (the bit-proven contract `f-r … f+r`).

## Algorithm (as implemented)

```
genParams = parameters
hits = runBiofeedbackScan(genParams)                 # generation 1: full range
kill(hits)
generation = 1
while continueRefining and active and not cycle-capped:
    plan = RefinementPlanner.planNextGeneration(hits, genParams)
        # per hit: window [hit-r, hit+r] clamped to [start,end]; overlapping windows merged
        # next step = current step / 2
    freqs = RefinementPlanner.frequencyStepsFor(plan, genParams)   # concatenated, halved step
    if freqs empty: break
    genParams = genParams.copy(step halved)          # cumulative halving across generations
    hits = runBiofeedbackScan(genParams, frequencyOverride = freqs)   # narrowed sweep
    if hits empty: break
    kill(hits)
    generation++
finish (clear + restore amplitude)
```

- **Replace, not union**: each generation's hits come solely from that generation's
  scan (matches the decoded "fresh CSV re-analysis per generation").
- **Cumulative step halving**: gen 2 = step/2, gen 3 = step/4, … (decoded `step/2`
  applied per generation).
- **Cycle cap**: `repeatBfbCycles` (`BFB_Repeat_BFB`); `0` = until stopped/cancelled.
- **Clamping**: windows are clamped to `[startFrequency, endFrequency]`; degenerate
  windows are dropped.

The first generation and the detection/kill math are unchanged — the golden replay
test (`HuntAndKillReplayTest`) still passes bit-for-bit.
