# Fresh export checks — 2026-11-05

PASS: extracted Android Accessibility library assembleDebug. Existing raw-to-normalized node IDs/continuation assertions pass against original ui-tree.cjs. No physical-device rerun or new screenshot acceptance.

Host tools: Node24.19.0, JDK17, Gradle8.9, Android SDK35 where applicable. Historical product runtime remains Node24.18.0; host tests do not revalidate that binary ABI.
Logs and available JUnit XML are in verification/. Android builds used the existing workspace Gradle8.9 launcher, whose JAR is not redistributed.
Build configuration and source layout are archival extraction changes. Source hashes and byte identity are listed separately; no new phone behavior is claimed.
