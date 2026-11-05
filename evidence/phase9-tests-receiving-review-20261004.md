# Phase 9 测试包接收审阅

日期：2026-10-04（Asia/Shanghai）。包 `zcode-phase9-tests-20261004-r1`，15 payload，ZIP SHA-256 `17AE12C25BDDC5A39894D011968776B3EF8D53BEB121DFC00E5558BA30A9968B`。

结论：**传输完整性 PASS；接受 fixture、probe 和部分测量作为审阅输入；内容处置 NEEDS_REVISION。没有产品集成或 Android17 新验收。** 本次只读取包内文件、核算证据和对照产品源码，未运行包内脚本、安装 APK 或操作手机。

## 已核实的证据

- 长列表源码确有300个有序不同标签。证据观察由000–017变为020–038，再变为042–059；独立集合核算得到19新增、18消失，generation为g1→g3→g6。这是正向滚动的有效观测证据，仍是上游报告的设备结果，不是本侧重跑。
- 截图 probe 直接调用 captureScreen；提交时间相减为100390630−100390622=8ms，第二调用记录 ui_screenshot_rate_limited；恢复入口距第一成功入口412ms，记录成功。接受为直接入口层证据，不延伸为完整 Gateway/持久化状态证明。
- 真实 provider/VLM 维持 NOT_TESTED。fixture/probe/驱动源码已提供，无新增生产模型工具；包内生产方向修改另行审阅。

## 补正要求

1. **方向修复实际源码缺失及范围偏离。** 任务明确不得修改正式运行时/生产代码，上游已修改 swipeInBounds；不能自行将明确限制解释为仅禁止放宽行为。缺陷值得保留，但修改不自动获本侧接受。请补完整受影响 Accessibility 源文件、修改前后 diff、完整 SHA-256 和修复前后测试/APK关系。不要求撤销缺陷发现，不授权继续生产修改。产品当前 AgentAccessibilityService.kt 的 forward 确为 top→bottom，证实有相同方向问题；本轮只记录，尚未修复。
2. **Gateway 操作关联不足。** scroll-ui-actions.json 有40行，38行 op_id=null/notes=join_ambiguous；仅2行非空来自10月3日旧测试，不是本轮滚动。UI动作记录不能证明本轮对应持久化 Gateway operation。请提供本轮现有 journal 的脱敏请求/意图/结果行，与 call_id/operation ID 可核对；若无法关联，A4降为 PARTIAL 并明确原因。不要为补证设计或修改 journal。
3. **at_end 结构化结果丢失。** atEndScroll 仅有截断 raw（120字符），没有可独立解析的 at_end/error 字段。驱动 fmt 内把传入字符串再次 json.loads，而该字符串原先已被插入Python三引号并发生转义解释，可能解析失败。请保留边界完整JSON及逐次调用ID，使用文件或标准输入传数据，避免再把JSON嵌入脚本字符串。边界观测282–299与UI日志支持“到达底部”，但当前不闭合完整响应契约。
4. **构建身份不一致。** 总报告最终APK为 BAFB94E9990120F8319FB65894D893BA2023410124D38315D8FB77FC3980436A，两个驱动文档却指01645C857C4E91EBD4D97C0A0CBE0F51113572CF82A3D034574996974D4D2483。请分别绑定A/B运行时实际安装APK、时间和对应源码；affectedKotlin目前只是文字说明，没有生产修改文件哈希。无法证明时明确 UNKNOWN，而不是统一用最终构建替代测量构建。
5. **B3并非状态实测。** store_journal_note 是入口分层说明，没有拒绝前后frame-store/journal测量；A项滚动日志也不能替代截图状态证据。B3应记 NOT_TESTED/NOT_APPLICABLE（注明直接入口层），或另补现有存储前后计数/哈希测量；不能维持“状态实测PASS”。系统与应用拒绝同错误码，本次记录支持应用入口间隔拒绝，但不证明系统分支也测过。
6. **任务及依赖精确绑定。** HANDOFF 将任务archive写为205154DB…，该值是旧review包，不是本轮任务。正确任务ZIP SHA为8783E2DFFB1CE93B8969C3596C653A2282E94DDB799BA635F52F440684470055。请提供完整依赖包archive SHA及requiredPaths，不能仅使用省略哈希。requiredPaths重复README虽未破坏完整性，后续去重。

两个驱动目前可在判据失败时仍退出0：scroll驱动仅打印verdict，interval驱动仅打印status/checks。作为复用驱动需非零退出失败/阻塞；保留原始测量，不将修正驱动后的结论冒充旧执行结果。

## 回包与当前产品状态

请通过原v1协议发布新ID补正包，引用本包完整SHA，包含 HANDOFF.md、完整受影响生产源文件及diff/hash、修正驱动、结果构建身份、原始边界响应与本轮journal关联证据（或诚实缺项）、修订RESULTS/RESIDUALS。无法补齐标PARTIAL。不操作Android17，不改变Gateway/journal/策略/阈值，不开展新的生产能力。

Codex保留Phase9冻结APK `9AACC118DFA83372837FFB4764414CC88FF7F477AE776646C318EBE87DF03EF7`。F1产品集成仍待实施；本轮对新增方向缺陷仅做源码确认与登记。无需用户传包名或复制任务内容。
