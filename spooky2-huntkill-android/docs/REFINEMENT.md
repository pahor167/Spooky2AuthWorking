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

2. **Decompiled `Spooky.exe` (VB6)** — Ghidra decompile + `Main.frm` (VB-Decompiler dump):

| Fact | Status | Evidence |
|---|---|---|
| Step **halved** each refining generation: `step = Val(Text19)/2` | **binary-proven** | Ghidra `FUN_0084b3b0`; `Main.frm` 64374, 68301 |
| Repeat-cycle counter (`BFB_Repeat_BFB`) | binary-supported | `Main.frm` 32785, 68336, 68344 |
| `Refine +/-` = preset key `BFB_Include_x_Hz_In_Search` | preset/guide | 48545–48550, read 68284 |
| Per-hit window `[hit−r, hit+r]` | **guide only** | Users Guide field #23; NOT located in the binary |

> **RETRACTED misattribution** (2026-06-12 Ghidra pass): `Main.frm` lines
> 70211/70215 (`var_84 = CLng(var_448 − var_1EC)` …) were previously cited as the
> per-hit frequency window. Raw disassembly at 0x855D62/0x855EB1 proves `var_1EC`
> = `Val(Text5.Text)` = **`BFB_RA_Window_1`** (default 20) and the expressions are
> **array indices of the running-average smoothing window inside hit DETECTION**.
> The "destroyed assignment" claim was wrong — the assignment exists and was
> recovered. The same applies to the 70802–70806 "clamping" (array-bounds clamps)
> and 66509–66560 (detection loop). None of those lines are refine-grid math.

Canonical preset **GX Hunt and Kill (C) - JW**: `BFB_Continue_Refining_Hits=1`,
`BFB_Repeat_BFB=0`, `BFB_Include_x_Hz_In_Search=0`, `BFB_Initial_Step_Size_Hz=100`
(percentage step actually used = 0.025%), `BFB_Max_Hits_To_Find=10`,
range 41000–1800000.

## Per-hit window — BINARY-PROVEN (2026-06-14 Ghidra pass)

A deeper Ghidra read of `FUN_0084b3b0` (the refine handler) recovered the window
build directly. With `param_1[0x6d8]` = the found-hits array and `param_1[0x6e1]` =
the scan-state struct:

```
start  = hit − V          ; dec_FUN_0084b3b0:1159-1160  (param_1[0x6e1]+0x18)
finish = hit + V          ; dec_FUN_0084b3b0:1180-1181  (param_1[0x6e1]+0x1a)
step   = V / 10.0         ; dec_FUN_0084b3b0:1357,1462  (const @ VA 0x405b48 = 10.0)
```

So **`V = 10 × step`** — the window is **±10 sweep-steps per hit (~20 points/hit)**,
and because the step halves each generation, the window zooms in proportionally.
When `refinePlusMinusHz == 0` the port computes exactly this:

```
step  = previousStep / 2                       (binary: FUN_0084b3b0:2319, /2.0)
r = REFINE_WINDOW_STEPS × localStep(hit, step)  REFINE_WINDOW_STEPS = 10
localStep(hit) = hit × stepSizePercent/100   (percentage mode)
               = stepSizeHz                   (linear mode)
window = [hit − r, hit + r], swept at `step`
```

**Two earlier mistakes, now corrected:**
1. `Main.frm:70211/70215` (`var_1EC`) was cited as the per-hit window — it is the
   `BFB_RA_Window_1` running-average window inside hit DETECTION (`var_1EC =
   Val(Text5.Text)`, default 20). Retracted.
2. A follow-up pass then claimed "no per-hit window in the binary" and the window was
   recalibrated to ~60 steps from the observed ~3-minute timing. ALSO wrong — the
   window IS in the binary (above), at ±10 steps. The ~3 minutes is **not** the refine
   scan (which is ~20 points/hit ≈ 10–15 s) — it is the **kill dwell** on the refined
   hits that follows each refine generation.

`param_1[0x6ed]` (once misread as the radius) is the **generation counter** — reset
at scan start (`FUN_008378b0:2629`), `+1` per pass (`:433`), compared `≤ maxRepeats`.

`localCoarseStep` is always computed from the **original generation-1 step** (the
half-width basis passed to `RefinementPlanner.planNextGeneration`), so the window
width is CONSTANT across generations — ±15 kHz around a 1 MHz hit with the 0.025%
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
