# Phase 9 receiving-product acceptance — PASS

Date: 2026-10-03. Canonical device: `DEVICE_ANDROID17`, model `MODEL_ANDROID17`, Android 17 / API 37, ARM64. Only this phone was operated. The tested, installed product APK is frozen at `evidence/phase9/final/phase9-pass.apk`.

Phase 9 is accepted on this device. This supersedes the analysis-only stop and the Phase 8 current-baseline label. Phase 8 evidence and a recoverable APK/source snapshot remain under `evidence/phase8/` and `evidence/phase9/baseline/`. Technical acceptance does not resolve the existing distribution licensing questions.

## Product architecture

```text
ZCode 0.16.9 model/session/context/skill/tool loop
  → model-visible android-host MCP stdio carrier
  → permission broker + existing production Gateway / host policy ceiling
      ├─ device identity → AndroidToolDispatcher → local JSON-RPC provider
      ├─ semantic UI / screenshot / coordinate tap → existing Kotlin carrier worker → Accessibility
      ├─ ui_see → existing INLINE seam → Gateway-authorized dump/capture
      │                                → explicitly configured HTTPS image provider
      ├─ real Web Search → existing INLINE search pipeline
      └─ workspace tools / rollback → existing workspace transaction implementation
  → bounded structured/text result → ZCode final answer
```

One ZCode Core, one Gateway and journal schema **3** remain authoritative. ZCode Protocol, MCP carrier and Android Local Tool Protocol JSON-RPC 2.0 remain separate. No Route C runtime patch, second Kotlin agent loop, new policy matrix, new journal schema or replacement rollback was introduced. Root was used for test setup, starting private test activities and crash/force-stop instrumentation; the product did not gain a Root/Shizuku/ADB execution backend.

## Exact identities

| Object | Version / SHA-256 |
|---|---|
| Product runtime | `0.16.9-runtime5`, receiving-product source merge |
| APK-owned native runtime | Node `24.18.0`, Bash `5.3.20`; unchanged native payload |
| Installed and frozen APK | `9AACC118DFA83372837FFB4764414CC88FF7F477AE776646C318EBE87DF03EF7` |
| Product runtime manifest | `38735F3D4CFDA8E05D45AAA4B310473D34CD9816E9E1C67C9BF2B9C41E1B73AD` |
| Product payload SHA manifest | `D84E42D4CE5AEF3F76B9E298EC292DD45FF3E9B0B7647F220845EF6A8180FC66` |
| Product bootstrap | `49C4B05166597866056896123494C9C72EB944D45DDBF5986907014242BCE613` |
| Production Gateway, journal v3 | `6C9B8D50A9043ABF412A67FA36EFFAB902646A4776409E48429267842342C336` |
| Product MCP carrier | `8453662E1BA721FA8C622672E9F6D13FBE6D850E54E4D23E70E1D38FF7F0EA0C` |
| Product vision pipeline | `4BD2E3D62472A70A5D4F16BA29D14284F79CCB2EBAA83ABE14459CC7BA158DB6` |
| Retained product ui-tree | `983179097D1932531A961DF4120F507B32F8DEFB7782F10DE5A3A4398AF43849` |
| Unmodified ZCode Core | `B1DF2EF3E5BD76C4AF3ECB296BC003A10D3F13191A26610BD0BA940FEADAD529` |
| Consumed Phase 9 package SHA manifest | `8BFECC03DBD30C59E64468CE46349375DD280247D04137EEF9AC28A93F469403` |
| Consumed runtime payload SHA manifest | `C9CD8FAF808D3A4112A2893A4B692073B2B4CC0AE85287FA7B7E4DA10EAD4DCF` |
| Supplied runtime5 manifest | `8FB44BF100962AED7811BEB358031549CAE929DCA24C0E54916730028DBF318F` |

`final/release-identity.json` contains all payload identities and the measured installed APK hash. All **34/34 packaged payload entries** match their product hashes; **33/33 immutable installed entries** also match. The remaining packaged entry is the host-materialized personal-provider configuration: its release template is verified, while its installed content intentionally differs under the existing validator's documented exclusion. No provider credentials were copied into evidence. Materialized vision configuration permissions are `0600`; app backup/transfer exclusions are retained.

Upstream package validation remains exactly the finding in `dsha-context-for-phase9.md`: Phase 9 27/27 matches; payload 43/44 matches, with the single mismatch in unrelated `.mimosa/hook-state` export metadata. It was not packaged. Product changes have new product hashes and are not represented as byte-identical upstream assets. No native payload was rebuilt; the supplied native alignment evidence applies to the unchanged native hashes.

## Final-APK acceptance results

All reports below are from the final APK identified above. Intermediate reports in the parent `phase9/` directory document development and must not be substituted for `phase9/final/` evidence.

| Check | Exact result / evidence |
|---|---|
| Build, JVM, lint | BUILD SUCCESSFUL; JVM **30 tests, 0 failures, 0 errors**; lint passes. Existing warnings remain. `evidence/phase9/build.log` and XML reports |
| Real provider turn + streaming | PASS; expected `STREAM_ACCEPTANCE_OK`, multiple streamed updates, persisted messages. `final/real-turn.json` |
| Cancellation | PASS; stop accepted and cancellation observed. `final/cancellation.json` |
| Activity reconnect | PASS; background/foreground reconnect retains same runtime and readable messages. `final/activity-reconnect.json` |
| Runtime crash/restart | PASS; supervisor restarts after bootstrap process kill. `final/runtime-crash-restart.json` |
| Session resume | PASS; completed assistant message and at least three messages persisted. `final/session-resume.json` |
| Force-stop/relaunch | PASS; bridge unreachable after stop, fresh ready runtime, same persisted session/messages restored. `final/force-stop-relaunch.json` |
| Model-issued screenshot | PASS; visible ZCode tool result, metadata-only carrier event, Gateway rows, real compressed frame metadata, final model answer. `final/model-screenshot.json` |
| Model-issued real vision | PASS; `deepseek-flash` at `api.deepseek.com` actually receives the compressed image through the explicit vision route and reads `count=0` / `INCREMENT`; final answer reports the returned interpretation. `final/model-real-vision.json` |
| Model-issued coordinate tap | PASS; model obtains dump and fresh screenshot, taps center `(600,265)` once, physical counter becomes `count=1`, verified result/final answer, `retries=0`. `final/model-coordinate-tap.json` |
| Physical boundary and legacy UI regression | **29/29 PASS**. `final/device-boundaries.json` |
| Real identity / UI inspection | PASS; model-visible tool, carrier/physical result and Gateway trace. `final/device-identity.json`, `final/ui-inspect.json` |
| Real Tavily-backed Web Search | PASS; existing network journal, returned results and cited URL. `final/web-search.json` |
| Real model workspace tools | PASS create, overwrite, append, delete; dedicated `phase9/model-visible-regression.txt` fixture only. `final/workspace-{create,overwrite,append,delete}.json` |
| Physical workspace rollback | PASS; new Gateway operation, restored `created-by-zcode` pre-image. `final/workspace-rollback.json` |
| Previous suites on APK-owned Node | WWS **44/0**, Gateway **22/0**, Web Search **57/0**, in isolated test directories/journal. `final/device-suites.json` and corresponding logs |
| Carrier artifacts regression | **9/0**, actual product HTTP handlers in isolated fixture. `carrier-artifacts.log` |
| MCP cancellation regression | PASS; actual carrier process receives MCP cancellation notification and closes pending HTTP request. `carrier-cancellation.log` |
| Three measured Phase 8 repairs | PASS lifecycle/hash/rollback assertions, normalized/raw mapping and abandoned-poll timer assertions. `phase8-repair.log`, `ui-boundary-regression.log` |
| Vision negative/protocol tests | PASS actual pipeline source: bbox parsing, all-field hard safety, no configuration, traversal, generation/epoch/rotation staleness, bounded output, cache/quota, private/HTTP/IPv6 refusal, pre-egress journal failure, oversized response and cancellation. Mock evidence is separated from real image-provider results. `vision-regression.log` |
| Internal approval / cleanup execution | PASS actual host functions: missing/mismatched/denied operation or journal failure cannot queue; preflight carries the exact operation ID and real critical label; cancellation clears pending calls, frames and cache. `internal-boundary.log` |
| Production controls | On final running product, plugins disabled, memory feature/extraction disabled; only `android-host` MCP stdio server remains configured and real tools remain model-visible. `final/release-identity.json` |

The 29 physical checks include: screenshot byte budget and identity; accessibility-first region refusal; verified tap and long press; old-frame and out-of-display refusal; maximum two live frames; real image-provider reading; cache with no extra network; started/completed network audit; `FLAG_SECURE` refusal; all-black refusal; critical target escalated despite benign model label and no mutation on denial; bound Gateway operation IDs; no observable change reported unverified with zero retry; all eight legacy UI tool paths; visible-keyboard screenshot/tap refusal; session-stop frame purge and old-frame replay refusal. Legacy scroll is the same honest `at_end` non-scrollable-fixture case as Phase 8; this is not a positive long-list scrolling acceptance.

## Source adaptations and retained divergences

The supplied runtime is the starting point. These corrections close contracts at existing boundaries rather than establish another runtime design:

1. Preserve all three Phase 8 receiving-product repairs. Classification against the consumed upstream sources remains **STILL_PRODUCT_DIVERGENCE** for Gateway lifecycle/hash/rollback, abandoned `/carrier/next` timer cleanup and normalized/raw-node mapping. The corresponding executable regressions pass against the merged sources.
2. Host-only bounded frame store: random unique shot ID, compressed JPEG <=153600 bytes, two live frames, content hash, session ownership, age <=30 seconds, actual display geometry/rotation and service observation epoch. Process/session stop/change and restart purge frames/cache/pending calls. Old frames cannot survive a service generation reset simply by reusing `g1`.
3. On this phone, the default-display screenshot API did not provide dependable secure-window refusal in the test. The product instead uses `takeScreenshotOfWindow` on API 34+, checks geometry, and pads excluded bars/overlays to preserve display coordinates. The final target refuses the secure fixture through system error 6. All-black and keyboard-obscured cases also refuse. No privileged screenshot fallback exists. [Android API documentation](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#takeScreenshotOfWindow(int,%20java.util.concurrent.Executor,%20android.accessibilityservice.AccessibilityService.TakeScreenshotCallback)) documents the window API and secure-window error.
4. Explicit encrypted vision configuration and atomic private materialization are supplied by the receiving app. The user's confirmation that the current test API is native multimodal was followed by an explicit settings action copying the saved current model into the independent vision configuration. Normal chat remains the Route A text interface; images go only to the explicitly configured vision provider, whose actual image request was verified. No key is placed in model context, journal or reports.
5. Internal dump/capture requests are Gateway-authorized; P9 carrier calls also bind to durable exact approval arguments. Fresh full native labels beneath tap coordinates, including ancestors, participate in hard safety. A benign model label cannot override `Factory Reset`; the test denied the hard confirmation and measured no mutation. Config identity/destination are bound at approval; the private config snapshot used by the request cannot silently change afterward.
6. Reuse existing public-egress controls with DNS pinning, HTTPS-only production calls, no redirects/private destinations, bounded image/result/HTTP body, cancellation and deadlines. Network intent is committed before the HTTP request. Explicit lab loopback allowance is narrow and disabled in real acceptance. Cache identity includes provider configuration; cached results do not consume another request and remain usable at the session network cap.
7. MCP cancellation maps to carrier HTTP cancellation, pending worker cleanup and vision abort. INLINE and worker dispatch now both emit metadata-only carrier-call events. Argument contents and image bytes do not enter these observability events. No independent local-provider protocol was created.

## Measured limits and deferred work

- This is receiving-product acceptance on `DEVICE_ANDROID17`, not acceptance on every Android ROM. Screenshots require the window API on API 34+; lower versions fail closed. Non-fullscreen window geometry, magnification and active IME are conservatively refused/limited rather than captured with root.
- This ROM removes the app's enabled Accessibility service after force-stop. Runtime/session recovery passed independently; test instrumentation explicitly restored the service before UI checks. The production app requires the user to re-enable the system service when the ROM removes it. It does not silently restore the grant using root.
- Vision accuracy was demonstrated on the physical UiLab fixture, not measured as a general visual benchmark. Observation generation/epoch/rotation and a short lifetime reduce stale actions; they cannot prove that every pixel-only canvas change emits an accessibility event. A gesture with no observable tree/window change is returned unverified and is not retried automatically.
- The all-eight-UI regression uses the stable receiving fixture; positive long-list scrolling remains unmeasured. Virtual displays/input, MediaProjection and other capture providers are not implemented.
- One read-only provider-connectivity check after debug restart timed out with an SSE disconnect before any device tool was sent. The final retry passed without production changes; no uncertain device mutation was replayed. The isolated timeout is not attributed to a verified backend or supervisor cause.
- DSHA remains a selective component donor, no fork or agent-runtime pivot. PTY, optional Ubuntu ToolchainProvider, Root/Shizuku/ADB production backends and updater/watchdog are still deferred. They need their own scope and acceptance; Phase 9 does not authorize a parallel Gateway/journal/policy.
- ZCode/Bash distribution licensing remains the existing independent question. The debug APK is a technical acceptance artifact.

## ZCode-side synchronization recommendation

The next useful upstream task is to consume this report and inspect the merged boundary corrections and their regressions, then ship a revised hash-pinned handoff that adopts them. In particular: carry the three Phase 8 repairs forward; align secure capture and observation epoch semantics; preserve cleanup/cancellation and pre-egress auditing; bind config/destination and actual target labels; include real model traces and a full clean regression run. Re-check source adoption rather than accepting prose claims. Product integration should keep the present accepted artifact frozen while that synchronization happens.

Files to inspect: product `bootstrap.cjs`, `gateway.cjs`, `ui-tree.cjs`, `vision-pipeline.cjs`, `android-carrier.cjs`; `AgentAccessibilityService.kt`; `ZCodeCarrierWorker.kt`, `ZCodeScreenFrames.kt`, `ZCodeVisionConfigWriter.kt`, `ZCodeHostService.kt`, `ZCodeRuntimeBundle.kt`; `MainActivity.kt` / `MainViewModel.kt`; capability providers and accessibility/backup XML; `Phase9VisionBoundaryTest.kt`; `tools/phase9/` and `tools/phase8/ui-boundary-regression.cjs`. No message was sent to another project/thread; this report is the reviewable local synchronization artifact.
