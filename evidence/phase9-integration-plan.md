# Phase 9 receiving-product integration plan

Authorized 2026-10-03: user says the ZCode Phase 9 is complete and this product now commands integration. This supersedes the earlier analysis-only stop. Phase 8 remains the last accepted release until new acceptance passes.

1. Preserve runtime4/APK/source identity before changes. Pin upstream handoff/runtime5 SHA manifests. Review primary source against the prior analysis; no blind replacement.
2. Merge upstream bootstrap changes onto the measured product bootstrap; add Gateway Phase 9 classification and label reader onto the measured product Gateway. Retain node normalization/mapping and rollback fixes. Keep core unchanged and unrelated plugins disabled.
3. Adapt upstream capture primitive and tool definitions into current Accessibility/worker boundary. Implement bounded compressed frames, private host-owned shot index, session/generation/rotation identity, purge, cancellation and stale/out-of-display refusal. Do not expose lab pixels to production models.
4. Adapt the supplied vision pipeline at its existing INLINE seam: explicit Keystore-backed host config, reused egress guards, bounded results/response, pre-egress no-key/region refusal, real shot metadata, cancellation/deadlines, and journal failure handling. Internal capture must have Gateway authorization rather than an implicit internal bypass.
5. Add canonical mappings and settings/capability integration; manifest enumerates required assets/endpoints and product SHA pins merged sources. Keep journal v3/workspace/rollback contracts unchanged.
6. Validate pure boundary behaviors and all three Phase 8 repairs; run JVM/build/lint and previous runtime/search/workspace/UI regression. Device `DEVICE_ANDROID17` only for canonical acceptance; no Root/Shizuku backend implementation.
7. Save physical frame/action/journal evidence, real model-issued screenshot/see/tap events and final answers; separate mock pipeline from real VLM accuracy. No accepted label while a required check is missing/failing. Update CURRENT_STATE and integration status with exact test results, hashes, remaining residuals and next boundary.

Decision source: `dsha-context-for-phase9.md` includes handoff hashes and per-source omissions. Product changes implement those existing contracts without introducing a second policy or architecture. Distribution licensing remains independent of technical acceptance.
