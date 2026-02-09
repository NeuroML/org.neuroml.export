CA1 OMV comparison (baseline vs modified)
Date: 2026-02-09

Output dirs:
- baseline: coreprojects/CA1PyramidalCell/neuroConstruct/omv_rerun_20260209/baseline
- modified: coreprojects/CA1PyramidalCell/neuroConstruct/omv_rerun_20260209/modified

Spike detection: threshold crossing; spike time taken at sample point (same as OMV default).

Test: .test.jnmlnrn.omt (Current clamp, single soma)
Expected spikes (ms): [25.351, 43.583, 61.895, 80.213]

| Run      | Spikes (ms)                              | best_tol |
|----------|-------------------------------------------|----------|
| baseline | [25.351, 43.583, 61.895, 80.213]           | 1.7716398483041409e-16 |
| modified | [25.347, 43.574, 61.881, 80.194]           | 0.000236869335394403   |

Test: .test.ca1.jnmlnrn.omt (BigCA1)
Expected spike times from .test.ca1.mep (ms):
Seg0=4.43675, Seg14=4.1665, Seg2031=5.1875, Seg2056=6.2374

| Segment | Baseline spike (ms) | Baseline best_tol | Modified spike (ms) | Modified best_tol | Outcome |
|---------|---------------------|-------------------|---------------------|-------------------|---------|
| Seg0    | 4.448               | 0.002535639826449437 | 4.44            | 0.0007325181720855174 | improved |
| Seg14   | 4.172               | 0.0013200528021122056 | 4.168          | 0.00036001440057603667 | improved |
| Seg2031 | 5.204               | 0.003180722891566386  | 5.194          | 0.0012530120481927616 | improved |
| Seg2056 | 6.24                | 0.0004168403501459194 | 6.228          | 0.0015070381889890495 | worse |

Seg2056 meaning:
- Segment id=2056 (name=Seg1_user5_28) in CA1.cell.nml
- Recorded quantity: CA1_CG/0/CA1/2056/v
- Output file: CA1_CG_0.2056.dat

Seg2056 delta vs expected:
- baseline: +0.0026 ms (6.240 - 6.2374)
- modified: -0.0094 ms (6.228 - 6.2374)

Likely reason Seg2056 got worse:
- Gate multipliers (fcond) are now computed after SOLVE, which advances channel currents slightly.
- This tends to move spikes earlier. Seg2056 was already close to expected; the earlier shift overshoots.
- Seg2056 is on dendrite branch user5_28 (in dendrite_group with non-uniform kad/kap/hd distributions), so it is more sensitive to timing shifts.
