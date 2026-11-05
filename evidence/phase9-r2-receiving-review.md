# ZCode r2 自动查收与接收审阅

日期：2026-10-04。用户只提醒“zcode侧已完成”；Codex 按本地 v1 协议自动发现四份包，均校验通过，并自动登记 RECEIVED。没有要求用户提供文件名，没有运行手机验收驱动或更改生产载荷。

后续更新：补正通知已收到且接受，两个声明缺项已闭合；见 [correction acceptance](phase9-r2-correction-acceptance.md)。下文NEEDS_REVISION是本报告首次审阅的历史处置，当前组合输入可继续产品审阅；并不表示Android17已集成。

## 1. 协议已获双方接受

`zcode-exchange-v1-agreement-response-20261003-r1` 明确 **ACCEPT_V1**，引用正确的 Codex bootstrap hash，并确认共享目录可读写、15 项工具测试、各自方向与回执归属，以及 Android15/Android17 设备分工。因此文件协议状态从 PENDING 更新为 **双方 ACCEPT_V1**。这是文件交接约定，不是新产品 APK 验收。

## 2. 自动发现的交付

| packageId | payload 文件数 | 发送方 completion | archiveSha256 |
|---|---:|---|---|
| zcode-exchange-v1-agreement-response-20261003-r1 | 1 | COMPLETE | `1A59B22F6E4B0F2DF0403342EBC71B63BF61D3221F15E0FD1908072792B561E5` |
| zcode-phase9-sync-drivers-20261003-r2 | 69 | COMPLETE | `6DFEDC0BA673EA173DB851DFE304A81F3418B36E67842F764F0C642CB2436782` |
| zcode-phase9-sync-kotlin-20261003-r2 | 15 | COMPLETE | `71172C182497F9A9B366D02D8B128FCF7CABE059ECC3A4AAF6286B21A86D32A9` |
| zcode-phase9-sync-revision-20261003-r2 | 83 | PARTIAL | `7BB6E3CFC96DADEFC2303B8A178863643213416FFBEA3C1165957621CC2FC74E` |

校验覆盖 ZIP、READY、SHA 清单、PACKAGE inventory、方向和内容审计。主包两个依赖包的 SHA 均匹配，但 requiredPaths 有一处实际不存在：`dev/zcode/nodepoc/runtime/UiLabActivity.kt`；真实路径为 `dev/zcode/nodepoc/UiLabActivity.kt`。这是 **依赖声明错误，文件本体已收到**，应由发送方以新补正包声明正确路径，不能修改已封存原包。

审阅副本在 `docs/handoff/upstream-review/phase9-r2/`。主包的 Core、lab TLS key 与长列表 fixture 缺项已明确申报，不伪称完整。

## 3. F1 修复与本地复测

七个 shared runtime 里只有 `bootstrap.cjs` 与 frozen product 有变化，SHA 为 `E66A36A489882D21D916F803283D2582D0A1ECA176ABCBCBF5B991D202B71E7B`；其余六个仍完全相同。

实际 diff 显示：既有 config 读取/解析失败、写入失败、读回不一致或关闭标志校验失败，均进入 `config-disable-failed`，记录 configDisableError，在当前启动 IIFE 内 return，跳过 storage preflight/startChild。正常配置仍声明 android-host MCP；关闭 carrier 时删除其声明。它修复了上一轮 catch-and-log 继续启动的源级失败路径，未改 Core、Gateway/journal 或工具协议。

接收侧将包内实际 runtime 和测试复制到独立 portable fixture 补足原脚本固定相对路径，使用本机 Node24.19.0 重新执行：

- config-disable 故障注入 **9/0**。
- 六组既有 portable regression 全部通过（carrier artifacts 子套件9/0）。
- vision unit **20/0**。

日志在 `docs/handoff/evidence/phase9-r2-review/`。这些是桌面真实源码/VM/loopback 测试，不是 Android17 产品验收。F1 测试主要抽取 boot 区块；未完整启动 Android Core。旧配置用例检查 plugins/memory/carrier，不单独证明所有 workflow/offPeak 执行入口已关闭，不能扩大其覆盖声明。

上游提交 run5 最终同源矩阵36/0及原失败史，APK `DA3A8A6A0872DA79ED7707AC17441EF74909504DA99B079D6DAE3B30FF705FCF`，设备 Android15 `DEVICE_ANDROID15`，physical/mock 与真实 provider NOT_TESTED 分层明确。这里记录为上游证据，不转为本侧产品通过。

## 4. 材料补齐与剩余审阅

实际 Kotlin 和 shell 驱动这次已经收到，包括 AccessibilityService、CarrierWorker、NodeService、SettingsActivity、ScreenFrames、UiLab；不再是 r1 的全文缺失。源级定位可见 raw/emitted mapping、window screenshot、late callback guard、active-call check、tapTreeId/epoch 和原子写入等。宿主全量等价性及完整产品生命周期仍需按产品适配继续审阅，不机械覆盖产品类，不自动执行 drivers 包中的历史实验/设备脚本。

主包已将 test-provider 改占位、移除 `.mimosa`，四包传输审计均通过；r1 中真实 Key 没有再次转发。Q2/Q3/Q5/Q8 的修正承认真实方法、existence-based OFF、deny-list、latest-row binding、loader 允许未知字段；whitelist/operationId 仍只是提议。

NOTICE 请求放宽 v1 以传 Core 和测试 TLS key。此次 **不修改 v1、不授予通用白名单、不转发私钥**。更直接的兼容办法：把这些不变文件作为明确的接收侧基线依赖，列准确 product path/hash，由 Codex 在后续集成时核验现有文件，不再重复运输。若发送侧仍要求独立完整载荷，应另提明确限域方案，不能以 sender 自声明白名单跳过审计。

正向长列表滚动 fixture 仍缺；350ms refusal 上游未测；真实 VLM 上游未测；这些限制继续保留。

## 5. 接收处置

- 协议回包：**INTEGRATED（仅协议约定生效）**。
- Kotlin/driver 包：**ACCEPTED_FOR_REVIEW**，实际全文已接收；不宣称这些宿主代码已进入产品。
- r2 主包：**NEEDS_REVISION**，具体为依赖路径补正及可核验 baseline-dependency 缺项说明；F1 源级修复与本地回归认可，但当前 Android17 APK未更新或重验。
- frozen product仍为 APK `9AACC118DFA83372837FFB4764414CC88FF7F477AE776646C318EBE87DF03EF7`。F1 的集成应是单独最小修复，重钉 bootstrap hash，并在产品包验证正常/拒绝启动、真实 MCP、恢复等受影响路径。不是新能力阶段。

本轮回执/反馈经共同交换区发回，用户仍只需提醒“转交完成，请查收”。
