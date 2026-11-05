# Phase 9 同步包接收审阅

日期：2026-10-03。接收裁决：**共享 runtime 源码同步已核实；整包交付 PARTIAL，不能据此宣称完整安全合同已闭环。** 本轮没有修改生产源码、安装 APK、操作手机、调用测试 API 或写入 Workflow 目录。

## 1. 输入身份与完整性

只读原件：`AGENT ON ANDROID workflow/export/codex-phase9-sync-handoff.zip`。审阅副本位于 `docs/handoff/upstream-review/phase9-sync-97f7ef8a/codex-phase9-sync-handoff/`，不是产品载荷或可再分发的干净包。

| 项目 | 实际结果 |
|---|---|
| ZIP 大小 | 39,343,984 bytes |
| ZIP SHA-256 | `97F7EF8AE2F94246337A997D0704BF1F51C7CDD7EC8D83289585EA2267E6CA50` |
| ZIP CRC | 全部通过 |
| 根 SHA 清单 | 90/90 匹配；清单 SHA `7462D3587E7E70E946CE4150A3841CDFFDC31279E44EF31F79EF18CA771718D2` |
| runtime-payload SHA 清单 | 43/43 匹配；清单 SHA `5EF2B5A2E51124CAA953A6C3FACF5535FCD2BE7436334B2F6F20FA3F1BE55A87` |
| sync manifest SHA | `8C06D484300F85731CB77B0B5EF350DFD6DAF69666E1B1AABB4BD8B37C7BBC49` |
| 实际文件数 | 94；根清单不覆盖自身、payload 的 SHA 清单以及两个 `.mimosa` 执行器状态文件 |
| 与产品载荷比较 | 34 个对应文件中 33 个字节相同，唯一不同为 runtime-manifest.json |
| 原生 ELF | 17/17 与产品相同；独立 ELF64 静态解析确认全部 PT_LOAD alignment≥16384、offset/vaddr 的 16KB 余数一致。未测试真实 16KB 内核启动 |

`HANDOFF.md` 所说“SHA 覆盖交付包除自身外全部文件”不准确。payload 自身清单完整，但根清单还缺 payload 清单文件的身份关联，并存在不应交付的 `.mimosa/hook-state` 与 `hook-status`。

机器可读证据：[integrity-review.json](evidence/phase9-sync-review/integrity-review.json)。校验脚本：[verify-phase9-sync-review.py](../../tools/handoff/verify-phase9-sync-review.py)，仅输出元信息，不输出凭据。

## 2. 可以确认的同步结果

以下七个 shared runtime 模块与当前 Phase 9 PASS 产品逐字节相同：`bootstrap.cjs`、`gateway.cjs`、`android-carrier.cjs`、`ui-tree.cjs`、`vision-pipeline.cjs`、`web-search.cjs`、`workspace-files.cjs`。包内 `source/runtime/`、测试路径 shim 和 payload 对应副本须保持这一身份关系；本次测试执行的是包内 shim。

Core `zcode.cjs`、17 个 ELF、builtin provider 数据、空 personal provider 模板、三个 contracts 和 lab TLS fixture 均与产品相同。没有新 Core patch、journal schema、工具协议或新能力。

| 修复 | 接收侧可核验结论 |
|---|---|
| P8-GW | **ADOPTED_UPSTREAM**：实际源码相同，sparse before hash/size、observed/verify、rollback 五阶段和原备份保留回归通过 |
| P8-POLL | **ADOPTED_UPSTREAM**：实际源码相同，断开轮询释放 timer、不消费新调用回归通过 |
| P8-NODE | runtime 部分 **ADOPTED_UPSTREAM**：隐藏/折叠 wrapper/raw preorder/分页回归通过。上游 Kotlin mapping 调用链缺完整源码，原生等价实现只能登记为上游报告，尚未独立核验 |
| P9 AUTH/PURGE/EGRESS/VISION/CANCEL/TRACE 等 runtime 部分 | 共享实现已同步，相关可移植回归通过；没有据此增加真实模型/真机覆盖声明 |
| P9 CAPTURE/TAP 和原生 FRAME 边界 | 产品实现保持权威。上游仅提供完整 ScreenFrames.kt，其余 worker/service/settings 实际函数未交付，描述和设备日志不能替代原生源码审阅 |
| P9-CONFIG | shared snapshot/materialization 合同可对照；Keystore 仍属产品。不以 debug host 的 0600 文件存储替换产品加密存储 |

不再把三项 Phase 8 **shared runtime** 修复描述为相对此次 sync 包的产品独有差异。相对原消费的旧 Phase 9 包，它们确实仍曾是差异，历史证据保留。

## 3. 本地重新验证与上游实测的口径

接收侧此次使用 Windows Node **24.19.0**，没有启动 Android/Bionic Node。六组测试均 exit 0：

1. `phase9-repair-regression.cjs`。
2. `ui-boundary-regression.cjs`。
3. `internal-boundary-regression.cjs`。
4. `carrier-cancellation-regression.cjs`。
5. `carrier-product-regression.cjs`，内部 9/0。
6. `vision-product-regression.cjs`。

另外 `vision-unittest.cjs` **20/0**。日志保存在 `evidence/phase9-sync-review/`；这些是独立本地源级、临时目录或 loopback fixture 验证，没有真实公网模型调用。

上游提供的 `run4-final-36-0.log` 记录：设备 `DEVICE_ANDROID15`，MODEL_ANDROID15/API35；最终矩阵 36/0，包含 vision20/WWS44/search57/Gateway22/artifacts9 以及 phase4/6/7/8 回归摘要。失败历史 4/1、23/13、33/3 和 phase7 重跑日志均保留，最终全量重跑符合任务书对失败历史的要求。

但是不能从交付包独立复现该设备矩阵：`verify-phase*.sh`、主要 Kotlin 源码和测试 Activity 缺失；声称驱动“hash-pinned”的位置没有实际驱动 SHA 条目；最终 APK 只提供 hash，未提供本体。记录为 **UPSTREAM_REPORTED**，不冒充接收侧重新实测。

真实 provider/真实 VLM 明确未在上游重测，mock 不能证明图像理解能力。正向长列表滚动仍未完成（任务书 C.10 未交付 fixture）；350ms 拒绝路径改为“顺序截图不同且有界”，原拒绝项未测。上游锁屏观察到 geometry_changed 并拒绝是有效 fail-closed 结果，但该日志不等价于 FLAG_SECURE fixture 的完整验证。

## 4. 必须纠正的接口与安全保证

以下是实际源码/隔离注入发现，**并非本包新引入的生产变化**：shared runtime 与当前产品相同，所以源码缺口同样存在于冻结产品。原 Phase 9 已执行矩阵 PASS 保留，但不延伸为这些此前未测场景的 PASS。

### F1：安全配置写入失败没有终止启动（优先处理）

`bootstrap.cjs` 的 boot hardening 区块对 `.zcode/cli/config.json` 写入异常执行 `catch (e) { log("config-disable failed: " + e); }`，随后仍进入 endpoint materialization、storage preflight 和 Core 启动流程。哈希校验静态 config-disable-pack 文件不能证明这次生成的 Core 配置已成功写入、读取并应用。

本次从实际源码提取该完整 hardening 区块，在隔离 VM 中将 `writeFileSync` 注入失败，验证仍能走到区块之后。证据：[contract-fault-injection.log](evidence/phase9-sync-review/contract-fault-injection.log)，脚本：[phase9-sync-contract-review.cjs](../../tools/handoff/phase9-sync-contract-review.cjs)。这是区块级复现，没有故意让真实 Core 带失败配置启动。

因此 Q2 的“kill-switch/config 失败在启动时 fail-closed”不可接受。下一补正应在现有 boot preflight 实现写入/读取/校验失败即不启动 Core，并测试旧 config 启用 plugins/workflow 等状态、不可写目录、截断/损坏配置、读取失败；不要新建 policy/Gateway，也不要以 catch-and-log 掩盖失败。

### F2：Q2 的方法名、kill-switch 语义、RPC ceiling 类型错误

- 实际反向权限请求是 **`interaction/requestPermission`**，不是答案时序中的 `permission/request`。包内 `agent-core-stdio.md` 本身也与答案冲突。
- `CARRIER_OFF` 是环境 `CARRIER_ENABLED==="0"` **或 `carrier.disabled` 文件存在**；没有“正向 enable 文件必须存在且 parse 成功”的代码。文件不存在时默认启用 carrier。
- `Gateway.rpcGuard` 是 **deny-list ceiling**，未匹配的任意 method 返回 `{allowed:true}`，不是 RPC whitelist。隔离直接调用未知 method 已确认 allowed；此结果不等于证明 Core 支持该未知 method，也不等于证明已有任意执行利用。若声称有效 whitelist，必须交付确切宿主入口/允许方法集/绕过回归。

### F3：unknown-field/configured 的合同过度声明

`vision-pipeline.loadVisionConfig` 只校验 enabled 和必需字段 truthy，随后返回原对象；没有拒绝 unknown fields 的代码。本次用纯 fixture 带未知字段的配置验证其保留。writer 生成有限字段、请求参数严格验证和 transport egress 检查是不同层，不能统称 runtime 配置 strict unknown-field rejection。

同样，“configured”是显式配置可用性，不是支持 image-input 的探测或能力验证。保留产品已有真实 image-input 证据，但答复应明确各层实际保证，不能把模型自述或配齐配置当作多模态能力确认。

### F4：operationId 没有新的 Gateway 返回接口

当前 `boundInternalApproval` 仍从 `journalView(1)[0]` 核对 tool/session/args，再把该 row.id 放进 pending；`handlePermission` 未返回稳定 operationId。这是产品原有实现，也被 sync 保留，并不是本轮新增的稳定关联接口。并发合法调用可能 fail-closed 拒绝的可用性残余仍存在；提议 additive operationId 仅是建议，不能写成已实现。本次不修改 journal 或授权契约。

## 5. 交付卫生与文档残余

1. `runtime-payload/test-credentials/test-provider.json` 实际含**非空、凭据形态的 API Key**。未验证其有效性、未调用、未输出。HANDOFF 的“no API keys”错误，任务书禁止交付凭据的要求未满足。应由上游导出占位模板，凭据保留在既有安全配置；此次审阅目录不应作为新包再转发。
2. 根清单不覆盖两份 `.mimosa` 状态文件及 payload SHA 清单，需删除 executor state、把 nested manifest 纳入根清单，然后重新生成干净 ZIP 和完整校验。不能修改此次原件后继续沿用原 hash。
3. 缺 AccessibilityService、CarrierWorker、SettingsActivity 等实际 Kotlin 实现和 shell 驱动；上游提到 packaging hook 阻止交付不构成已完整交付。只要求工作区中独立、安全的方法导出；不要求绕过安全机制。若仍无法交付，准确标 PARTIAL 和缺失项。
4. `changes-and-source-map.md` 写 payload44 entries，而实际43。adoption-matrix 仍说 interval refusal 已验证；run-summary/residuals 明确未测。contract-conflicts C10 未列最终接受的 geometry_changed。需统一为最终实际执行口径。
5. sync manifest 与产品差异涉及 **6 个顶层字段**，不只是 revision 和两个 endpoint。`bridge.endpoints` 仍是旧列表，`bridgeEndpoints` 使用分组字符串，不能直接作为精确允许端点列表。env-contract 中 `runtime/shell/lib` 与载荷实际 `runtime/shell-lib` 也不一致。保持产品当前结构化 manifest，不盲目覆盖或降低严格校验。
6. Core/原生精确构建 provenance、license/source 元数据缺项已坦诚记录；本轮不作新的分发许可结论。外部发布仍未进入当前 scope。

## 6. 接收决定和后续顺序

- **认可**这次实际源码同步；不重复移植相同七个 `.cjs`，不重装相同 native/Core，不修改现有 manifest/payload pins/APK。
- **不把整个包认定为最终完整接收 PASS**；状态 PARTIAL。当前产品 frozen APK SHA 仍为 `9AACC118DFA83372837FFB4764414CC88FF7F477AE776646C318EBE87DF03EF7`，本次重新计算已确认。
- 上游先补正 F1 的既有 preflight 缺口、F2/F3/F4 答案和 clean export，再交付所缺原生源码/驱动与全量最终代码回归。产品侧收到修订后，按边界审阅并重跑受影响的 runtime/kill-switch/真实 carrier 验收，不能把旧 PASS 直接赋给新 APK。
- 没有推进 Phase10、PTY/Ubuntu/Root/Shizuku/ADB、虚拟显示/输入、MediaProjection 或 DSHA 实施。

可转交的补正要求：[ZCODE_PHASE9_SYNC_REVIEW_FOLLOWUP.md](ZCODE_PHASE9_SYNC_REVIEW_FOLLOWUP.md)。未向其他任务自动发送消息。
