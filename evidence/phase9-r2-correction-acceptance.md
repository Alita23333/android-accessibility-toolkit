# r2 补正通知接收结果

2026-10-04。用户仅提醒 ZCode 已完成任务，Codex 自动发现 `zcode-phase9-r2-corrections-20261004-r1`。ZIP SHA `E8E62A46B39514E539308B16FBF5FFBF6D1CFAC1DB1D4CE3090E0DCB20745DE5`，1个 notice payload，v1完整性校验通过。

**接受本次补正通知，上一轮两个 NEEDS_REVISION 声明项已闭合。** 不把这一结论写成产品 runtime/APK 已集成。

1. Kotlin dependency 更正为 `dev/zcode/nodepoc/UiLabActivity.kt`。在原已封存 Kotlin 包中核验6个 requiredPaths均存在，两个依赖包 archiveSha256 与主包声明匹配。旧错误路径仅由新通知替代，未修改历史包。
2. ZCode 撤回 v1例外请求，接受两个不变文件的 receiving-baseline dependency：本产品 Core SHA `B1DF2EF3E5BD76C4AF3ECB296BC003A10D3F13191A26610BD0BA940FEADAD529`、lab TLS fixture SHA `F3D6E7EA9CE6067EDE04D7CBFA478B153837F0093AA0B9518F91656AB96BBEE8`。两者当前本地实际哈希均匹配；没有输出或转发私钥。
3. 补正通知引用的 Codex 审阅反馈 ZIP SHA `205154DB3C3985E3D552928C3ECA68F997D00D658CF264001D6F44B5ABCDFDFE` 已独立复核，关联正确。

证据：[correction-baseline-check.json](evidence/phase9-r2-review/correction-baseline-check.json)。之前源级F1/local fault9、portable6、vision20证据不变；本次无源码变更，不重复跑相同测试。

接收后的 r2 有效输入是原主包 + Kotlin包 + drivers包 + 本次补正通知 + 两项已核验本地基线。它是可审阅的组合交付，**不是自包含 COMPLETE payload**。长列表fixture缺项、上游350ms refusal未测、真实provider未测仍保留。主包原 completion=PARTIAL与历史NEEDS_REVISION回执不修改。

v1 discover目前不解析跨包补正关系，仍可能显示旧主包NEEDS_REVISION；内容裁决以本通知的绑定回执与此报告为准。不为改变显示而伪造INTEGRATED或覆盖旧回执。

后续产品工作为 F1最小修复与Android17独立验收，当前 frozen APK仍是 `9AACC118DFA83372837FFB4764414CC88FF7F477AE776646C318EBE87DF03EF7`。本轮生产代码变化0、手机操作0、协议/工具变化0。无需ZCode再次为这两项发修订包；正式反馈已放入codex-to-zcode，用户只需提醒查收。
