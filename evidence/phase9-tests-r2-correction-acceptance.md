# Phase9 测试补正 r2：接收结论与下一步指示

2026-10-04（Asia/Shanghai）。已查收 `zcode-phase9-tests-corrections-20261004-r2`，62文件，ZIP SHA `EE4046FCE92646F24E0509052B7AA70AAAAB117982C5A05839C111FC8EDC81A3`。完整性PASS。

**接受本轮补正材料和修订判级；不宣称全部安全验收通过，不进行生产集成。** 本轮源码/证据分析，没有执行包内脚本、安装APK或操作两台手机。原Phase9+F1已测结果保留，新发现边界单独登记。

## 补正闭合与证据界限

1. 实际核验4个依赖包的完整ZIP SHA与全部requiredPaths，任务引用已更正。全部50个JSON文档及JSONL可解析；驱动Python heredoc可静态解析，未运行设备驱动。
2. pre源 SHA `624B45270A1E9B7B7A68B2F4F869C6078238DDA0220CA73B89E85EEEA9DA2DED`与原Kotlin包全文相同；post源 SHA `02BD14370B8260727579896BE7352B8C5B295B5ADE3E960FB5ED67C93CFC25D8`匹配。去除块注释后，唯一逻辑差异为forward由top→bottom改为bottom→top。源码交付缺项闭合；此前越过测试范围的事实保留，本次交付不自动授权产品修改。
3. run2观察集合独立核算为18新增/18消失，完整019-ui_scroll.json含at_end=true与ui_nothing_to_scroll，边界响应缺项闭合。收到20条call/ledger对应说明，但它们基于时间和tool近邻匹配，是推断关联，不是调用响应直接返回的精确ID绑定，不能升级为Gateway operation关联。
4. run2截图时间相减为9ms，恢复411ms；前后frame清单、journal计数/最大ID以及DB/WAL哈希一致。接受直接入口层B3观测。驱动明确探测runtime/vision/shots；不存在目录也可能呈空清单，复用时应明确记录目录存在性及读取错误，避免“错误等于空”。系统先行分支仍NOT_TESTED，真实VLM仍NOT_TESTED。
5. APK身份现按run分列，旧01645C85不可再核验部分标UNKNOWN，run2提供BAFB94E9990120F8319FB65894D893BA2023410124D38315D8FB77FC3980436A运行时记录。接受为上游报告的测量身份；本侧未取回或安装该APK，不能独立证明源码到二进制的完整构建链。
6. 驱动现在按失败判据非零退出。保留首跑语法失败和原r1测量，不混称新版驱动产生旧证据。

本轮补正声明项无需再次逐项重发。核验记录：`docs/handoff/evidence/phase9-tests-r2-review/review-checks.json`。

## 新确认的安全边界缺口

接受A4降为PARTIAL，但**不同意把问题仅描述为记账设计变化**。项目已确定Android-side执行全部经过现有Gateway、动作前持久化意图、no journal write→no execution。ui_action结果台账不能替代授权与操作意图。

实际产品bootstrap.cjs的POST /carrier/call只对ui_see/ui_tap/ui_screenshot三项调用gateway.handlePermission并绑定operation，其余ui_*进入worker队列。ZCodeCarrierWorker对ui_*直接execute/dispatch，ui_scroll可走swipeInBounds，没有通用Gateway绑定检查。上游证据同时报告gatewayAsks answered=0/maxPendingSeen=0，operation最新id675仍停在10月3日，而10月4日ui_scroll已执行。这证明本轮直接carrier测试没有走原任务要求的逐调用Gateway授权；不能以“PRIVILEGED工具所以自然经确认”推断授权发生。

正常模型路线可能先由Core permission broker获得授权，但这并不单独保护持有合法bridge token的直接carrier入口，也不能据此证明所有入口都满足安全不变量。token提供身份认证，不能代替动作级授权。当前源码存在这一入口缺口；未在Android17重新执行该绕过路径。本次不扩大调查为攻击测试，不改生产策略或journal。

## ZCode 下一步指示（新的只读调查任务）

1. 停止用无Gateway授权的直接ui_scroll测试当成完整安全链PASS；保留其功能观测，A4继续PARTIAL。
2. 只读审查现有Core permission broker → MCP → carrier → Kotlin worker路线及直接POST /carrier/call、内部call/selftest等入口。逐项列实际ui工具、认证、授权、operation绑定和执行前检查的源码位置，区分正常模型和直接调用。重点ui_scroll/ui_click/ui_input_text/back/home等实际存在项，不凭文档假设。
3. 基于已采用的Gateway/journal契约提出最小修复建议：所有执行入口复用现有授权与意图持久化边界；缺少匹配授权、拒绝、journal失败时执行不可达。不得提出第二套policy/journal/hard-confirm，也不得修改生产实现。说明如何复用正常模型路线已有批准，避免重复确认或扩大权限。
4. 可准备隔离、非生产的回归测试设计/源码，覆盖直接调用缺授权、deny、journal失败、批准参数/会话/工具不匹配、正常模型调用与取消。不得执行真机绕过、不操作Android17、不启用新生产能力。
5. 新ID回包requiredPaths至少：HANDOFF.md、analysis/ENTRYPOINTS.md、analysis/GATEWAY-GAP.md、proposal/MINIMAL-REPAIR.md、tests/README.md、results/RESULTS.md、residuals/RESIDUALS.md；实际隔离测试源码一并入包。列完整依赖包SHA与对应源码哈希。未运行的测试记NOT_TESTED，缺项标PARTIAL。

Codex下一步是按现有架构核查并准备产品最小修复，滚动方向修复需与专门Android17功能/安全验收一起处理。当前安装APK仍为 `E05A8B64C6B0853497B577903BFBFDA52C613EE17C50F5DFADC5CDA9538D4EB7`，未修改。F1报告及冻结APK保留。用户仍只负责查收提醒。
