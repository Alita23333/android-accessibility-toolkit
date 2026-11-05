# DSHA context for Phase 9

日期：2026-10-03。状态：**上下文与源码差异分析完成；Phase 9 未开始实施。**

本轮只创建本文档。没有复制 upstream 文件，没有修改 production runtime、Gateway、journal、workspace、carrier、Accessibility、设置或 APK，没有安装应用、运行设备测试或启用新工具。上游目录作为只读 handoff 使用。本文是接收侧集成建议，不是新的 policy、journal 或 backend 架构裁决。

## 1. 输入、优先级与当前基线

权威输入依次为最新用户决策、接收产品 Phase 8 实测状态、最新上游 Phase 9 实测结果、adjudication、实际交付源码及合同。历史研究用于背景，不能覆盖当前实测。

- 接收状态：`docs/handoff/zcode-integration-status.md`，Phase 8 receiving-product PASS。
- DSHA 评估：`docs/research/dsha-reuse-assessment-2026-10-03.md`，完整读取；SHA-256 `4ED090A05A559D3D9730AB67CA65AAB6221A67712924669AB12EACFA5714428B`。
- Phase 9：`${USER_HOME}/Documents/A!g!E!n!T!/AGENT ON ANDROID workflow/export/codex-phase9-handoff/`。读取包内文档、interfaces、全部 Kotlin/runtime source、测试、SHA 元数据及 residuals；运行时源码核对到具体执行分支。
- 更新载荷：同一 export 下 `codex-runtime-payload/`。检查 manifest、contracts、protocol/env/provider 文档、acceptance procedure、licenses、alignment evidence、配置和载荷文件；二进制以完整文件 SHA 校验，未执行。测试凭据不输出、不复制；未对不变的 14.8 MB minified core 作新的全面语义审计。

DSHA 评估中“Phase 8 final 未完成 / device-blocked”的历史描述已经被最新接收侧 PASS 覆盖。Phase 9 adjudication 引用的接收侧 device-pending 描述同样不是当前产品状态。HANDOFF 中“regression sweep 下一次运行”的描述落后于 phase9-results 中实际记录的 full run。

当前产品仍是 ZCode Agent Core 0.16.9 + APK-owned Node 24.18.0/Bash 5.3.20，runtime4、production Gateway、journal v3、MCP carrier；workspace mutation/backup/rollback 为现有 Gateway 事务路线，设备工具经现有 Android adapter/dispatcher/provider 边界。保留 ZCode 的 Provider/Session/Context/Skill/Tool loop。legacy Kotlin Agent runtime 受 `LEGACY_DIAGNOSTICS=false` 限制，不能恢复第二套内核。

接收侧设备实测来源为 `DEVICE_ANDROID17`（MODEL_ANDROID17、API 37、ARM64）。当前本地文件重新计算的 hash 与 Phase 8 状态记录一致：

| 本地基线 | SHA-256 |
|---|---|
| debug APK | `458773E38DEC05BC9BED5729737057838738D465329AECE8AA0C863D7452DA63` |
| runtime4 manifest | `B5BA0B1E8641B3CC3E86CC112353F8791FB8B4EC6521C4430E0FEAC3D6F2A2D9` |
| product Gateway | `2E657133079CF6773DBBC183ABE621AB5208F218D4A5358C506037316463836A` |
| product bootstrap | `F2E79C11DD79BD605F26CEF234CB95F41DEC16D57018BD71DDC4A55CCCC7707C` |
| product ui-tree | `983179097D1932531A961DF4120F507B32F8DEFB7782F10DE5A3A4398AF43849` |

基线验证沿用已保存证据：26 JVM tests；lint/assemble；设备 WWS 44/44、search 57/57、Gateway 22/22、artifacts 9/9；真实 provider turn/streaming/cancel/reconnect/crash-restart/force-stop/session resume/persisted messages；真实 Tavily、identity、八个 UI 工具及 workspace/rollback。**本轮没有重新运行这些测试，也不把这些结果计作 Phase 9 产品验收。** Phase 8 scroll 的非滚动界面 `at_end` 结果仍不是长列表正向滚动证明。

## 2. DSHA 架构背景和当前复用决定

评估固定到 DSHA commit `edba958027a4e9bd32f481a321e02d31b9a99ba9`，而非追踪其未来变化。DSHA 是面向 DSH 的 Android launcher/harness：Java 17、ARM64、两种 flavor；用 proot/proroot 管理 Linux guest，并带终端、更新、进程监管、无线 ADB 与虚拟显示/输入能力。ContainerRuntime、ProotBootstrap、RuntimeHostPorts、HarnessController 之间包含 DSH_HOME、rootfs、插件/配置迁移、浏览器认证和静态全局状态耦合，不能视为现成的通用 Agent SDK。

**保持选择性抽取，不 fork/pivot 到 DSHA。** 可复用安装校验、PTY/终端、可恢复下载、guest watchdog、ADB pairing 以及 display/input 的会话生命周期经验。不能迁移 DSH Agent、Cordis/npm 插件控制、DSH 认证/session/storage 语义，也不能由 DSHA 的 shell whitelist/auth token 替代本产品 Gateway。

继续权威：ZCode runtime/supervisor、production Gateway、journal v3、workspace transaction/backup、rollback、CapabilityResolver 和 AndroidToolDispatcher。Root 已获用户授权且手机已 root，不等于已有 Root production backend，也不扩大 Phase 9 的能力范围。

未来 Ubuntu ToolchainProvider 是独立可选 guest：基础系统、工具包、Agent runtime 分开管理；ZCode 本身继续使用 native Android/Bionic Node，Ubuntu glibc Node 属于另一个 ABI。guest scratch 产物进入正式 workspace 必须经过既有事务边界，不能扩大 shared-storage mounts。proot 同 UID，不承诺安全沙箱。

PTY 的人类终端与模型命令执行分开；backend 由实时健康/身份/作用域选择，不能固定 root→Shizuku→ADB 顺序或静默升级权限。未知执行结果不能自动跨 backend 重放。DSHA updater/watchdog 的复用不得引入第二个 ZCode supervisor 或副作用动作重放。第三方许可与二进制分发条件保留原评估中的边界；本轮不作新的法律结论。

## 3. Phase 9 的实际范围与证明范围

runtime5 增加 `vision-pipeline.cjs`；bootstrap 增加 `ui_see` INLINE pipeline、internal capture/dump helpers 和 lab vision；Gateway 增加三个 classification rows；Kotlin 增加 a11y screenshot primitive 和两个 worker tools。

| canonical name | carrier ID | 执行位置 | 上游既有分类 |
|---|---|---|---|
| `ui.screenshot` | `ui_screenshot` | Android worker / Accessibility screenshot | PRIVILEGED |
| `ui.see` | `ui_see` | bridge INLINE → 用户显式配置的 VLM | NETWORK |
| `ui.tap` | `ui_tap` | Android worker / Accessibility gesture | PRIVILEGED |

沿用 Route A：像素交给指定 vision endpoint，主聊天模型接收规范化文本、bbox、匹配节点与动作提示。未配置 provider 的文件缺失路径返回 `vision_unconfigured`；节点几何相关性使用 Jaccard≥0.5。截图默认 JPEG q80、long edge≤1080、不放大、小型私有 shot store。capture 有独立 callback executor；HARDWARE bitmap 缩放后仍需 `copy(ARGB_8888,false)` 才能读像素。保留这些实测经验，不重新设计 ZCode runtime。

没有 virtual display、virtual input 或 MediaProjection 实现。MediaProjection 明确延后。adjudication 写“Route B built but OFF”，但实际 `android-carrier.cjs` 只发 text content，与 runtime4 的 carrier hash 相同；**本包不能证明存在可启用的 Route B image carrier**。也不能将文档列出的 vision picker、VisionConfigWriter、vision.invoke 和所有 runbook guards 视作已实现（第 6 节）。

上游设备为 `DEVICE_ANDROID15`、MODEL_ANDROID15、Android 15/HyperOS，不是接收产品的 API 37 设备。按最新 phase9-results 精确记录：

- main matrix：31 PASS / 0 FAIL；其五个 device suites 为 vision 20/0、WWS 44/0、search 57/0、Gateway 22/0、artifacts 9/0。
- full run：34 PASS / 1 FAIL；phase4 7/0、phase6 36/0、phase7 80/0、phase8 48/0。唯一失败 V6.9 为 regression cleanup 清除了 a11y，使返回 `ui_a11y_disabled`；脚本调整后单项重测为 `ui_screenshot_secure_window`。
- 不宣称修正后的完整 matrix 已有一次 35/0 重跑，也不把 runbook 的 100 项设计矩阵当作实际执行数量。实际 script 没有运行 Phase 5 独立回归。
- mock VLM 与 fixture 证明 pipeline/坐标落点，不能证明真实 VLM bbox 准确性。上游无真实 VLM credential，真实 VLM 结果未测；0.5 阈值未校准。
- 实际 `verify-phase9.sh` 多数检查直接调用 `/carrier/call`。V1.4 检查的是 `/status.labVision`，不是模型 tools[]；没有新的真实模型发起的 screenshot/see/tap transcript + final-answer 全链路验收。这是脚本证明范围，不能据 results 中“model-visible”措辞扩张。

## 4. Phase 9 × DSHA 的交集

| DSHA 候选能力 | Phase 9 的影响 | 后续处理建议 |
|---|---|---|
| Screenshot capture | 原“截图待构建”边界已被 a11y capture 填充 | 以 Phase 9 为主路径；DSHA 截图实现只能作为未来独立 provider 候选，不替换当前主线 |
| Vision fallback | Phase 9 已定义指定 VLM、bbox、相关性、错误与 journal owner | 不再引入 DSHA/DSH 的独立 vision loop；沿用当前 ZCode loop 和 Phase 9 边界修复缺口 |
| Coordinate tapping | Phase 9 已通过 Accessibility gesture 实现坐标 tap | 不将 DSHA 注入直接接入模型；未来 virtual-input backend 仍需 adapter、Gateway、设备独占锁和可信观察标识 |
| Virtual display | Phase 9 无新增 display lifecycle，当前捕获使用 DEFAULT_DISPLAY | 保持延期；未来 displayId/session/generation/frameSeq、heartbeat/revocation 应与 observation ID 对接，不能将 treeId 或 shotId 当 display session |
| Virtual input | Phase 9 是当前物理 display 的 a11y gesture，不是 InputManager/ADB/root inject | DSHA 会话鉴权与输入生命周期经验仍有价值，但未被 Phase 9 自动实现 |
| MediaProjection / capture providers | G5 明确未实现；运行时 capability stub 也不完整 | 后续独立 capture provider + 真实 consent 生命周期；不能自动切换去绕开 secure-window 或未授权 a11y capture |

总体复用决定不变，**只是截图/视觉/普通坐标动作的首选实现来源由未来 DSHA 候选变为 Phase 9 主线**。Virtual display/input 的后续需求仍存在。

下列能力与 Phase 9 基本正交，仍全部延期：PTY、Ubuntu ToolchainProvider、Root backend、Shizuku backend、ADB backend、updater/watchdog reuse。它们共享 artifact/cancel/device-resource/权限边界，因而不是零耦合；但 Phase 9 不改变它们的 ABI、guest isolation、身份检测或生命周期方向，也不是现在实施它们的授权。

## 5. Phase 8 产品修复 vs Phase 9 实际源码

三项均为 **STILL_PRODUCT_DIVERGENCE**。依据是执行源码，而不是 HANDOFF 的“Consumed/noted”反馈表。没有一项可以凭上游 regression green 就删除产品修复。

| 修复 | 上游实际源码证据（相对于 Phase 9 包） | 当前产品证据 | 分类与结论 |
|---|---|---|---|
| Gateway lifecycle/hash/rollback | `reusable-source/runtime/gateway.cjs:802` 的 `_terminalTxn` 仍以 latest sparse row 的 `txn.before_hash` 为默认，没有 intent fallback，也不保存 before_size/after_size；`:1502` 注释写 executed→verified，但实际直接进入 terminal，没有两条 rollback lifecycle insert；`:1515` 的 original rolled_back marker 未带原有 hash/backup | `app/src/main/assets/zcode-runtime/gateway.cjs:794` 的 terminal 有 intent fallback、sizes、observed/verify；rollback 显式写 executed、verified、terminal 并保留 original hashes/backup | STILL_PRODUCT_DIVERGENCE；runtime5 Gateway 必须按源码 merge，不能覆盖。original rolled_back latest op_id 为 null 的已知报告限制仍独立保留，不能自行升级 schema |
| abandoned `/carrier/next` timer | `reusable-source/runtime/bootstrap.cjs:1047` 每 500 ms `setTimeout(wait)`，没有 response close cleanup、stopped/destroyed/writableEnded 检查 | product bootstrap `/carrier/next` 保存 pollTimer、res.close cleanup 和终止条件 | STILL_PRODUCT_DIVERGENCE；保留本产品避免失联旧 poll 消费新队列项的修复，新 internal calls 也要验证取消/timeout queue 清理 |
| normalized/raw UI-node mapping | upstream `ui-tree.cjs` hash 与原 runtime4 文件完全相同，没有 `_rawIndex/nodeTargets`；`ZcodeAccessibilityService.kt:295` 直接把 emitted idx 当 raw preorder；`CarrierWorker.kt:222` 只返回 render response，不安装 mapping | product ui-tree 保留 raw preorder WeakMap/index；bootstrap 返回 host-only nodeTargets；AgentAccessibilityService 的 emittedToRaw + installNodeTargets；worker 安装 mapping | STILL_PRODUCT_DIVERGENCE；必须保留四处联动。Phase 9 bbox→node correlation 会继续使用 emitted ID，删除修复会使视觉后的 node click 也选错 |

产品 `ZCodeCarrierWorker` post-action render envelope 提取与正向 long-press UiLab fixture 同样应保留；上游 UiLab 没有产品的 longClick listener。它们不是新的独立架构，也不能被整个 Kotlin 文件替换丢失。

## 6. 在生产集成前必须说明/解决的交付差异

以下是本轮静态源码发现，未执行漏洞利用、网络请求或设备实验。推荐修补既有边界以满足已裁决合同，不新建 policy matrix 或第三套架构。

### 6.1 Gateway 与动作安全

- `gateway.cjs:177` 的 `uiHardConfirmHit` 仍仅检查 `input.confirm || input.text`，**没有** runbook A12d 所要求的 `input.label`。三个新 classification rows 已有，但不能由此声称 destructive coordinate target 在所有模式都进入 hard-confirm。worker `doTap` 对 destructive label 直接拒绝，不能替代 Gateway 的不可绕过确认语义。
- `CarrierWorker.kt:648` 的 `doTap` 不读取 schema 已声明的 `shotId`，没有 generation/rotation 检查，也未按实际 display bounds 拒绝坐标。timeoutMs 未使用，longPress 不在交付 schema 内且执行固定 false；没有 frame/corr join descriptor。不要对外承诺 runbook stale-frame、rotation 或 ROI verification 已存在。
- `bootstrap.cjs:947` internalCall 将 `ui_screenshot/ui_dump` 直接放进 worker queue，不经过新的 `handlePermission`/intent claim；`ui_see` 的 NETWORK permission 是否可覆盖其内部 PRIVILEGED capture 必须按现有 host policy ceiling 明确绑定、测试。不能把 internal 标记当授权。沿用现有 Gateway 安全 invariants，不在本报告另定审批模型。
- 网络结果 journal 在 `httpJson` 之后才写；internal queue 超时只移除 pending map、不清理已入队项。后续应验证 pre-execution intent、journal failure、取消及迟到 worker 不执行弃用操作，不能从正常成功路径推断这些性质。

### 6.2 Capture、retention 和元数据

- `captureScreen` 有 dedicated executor、HardwareBuffer.use、软件 bitmap copy、350 ms floor 和 all-black refusal，可作为实测起点。
- 源码是一次缩放/一次 compress；没有 JPEG quality + WebP + rescale ladder，也没有 200 KiB **base64** hard cap。script V2.1 仅比较 raw bytes<204800。runbook 尺寸预算不能由这一检查证明。
- black probe 只采样 bitmap 最前面至多 4096 像素的部分点，不能证明全图黑色。明确 code 6 secure-window mapping 未实现，平台 error6 会落为 `ui_screenshot_error_code_6`；decode exception 又把细节拼进 code string。需在目标 ROM 按冻结错误合同验证，不能只依赖上游锁屏返回黑图的一个路径。
- `finishShot:621` 写 `sN.ext` + 保留最近两文件；没有 session key/index/generation/rotation 绑定。service `onDestroy:112` 只清 instance，NodeService shutdown 也没有 shot purge；脚本 cleanup 才主动清空 store。两文件 pruning 不等于 session-scoped retention。counter 随进程重置，不能承诺旧 shotId 在重启后不会指到新图。
- screenshot 返回 `performed:true, verified:true`，缺少 runbook observation-only `verified:false/verify:skipped`、treeId、capture timestamp；字段实际是 w/h/screenW/screenH/sha256_16、scale string，而非文档 width/height/...。边界应明确选择并映射真实合同，不能静默更名后丢失精度。
- model-facing screenshot schema 仍含 lab `raw`，uilab flag 打开后可返回 b64；后续 debug fixture 与 production surface 必须隔离。没有完整 magnification/IME/display-offset 防护。上述均不能靠未来 DSHA virtual-display 代码自动解决。

### 6.3 Vision pipeline、网络和凭据

- `vision-pipeline.cjs:httpJson` 直接调用 node http/https，无 `web-search` 的 normalizeUrl/public-host/connect-time DNS guard；本文件没有对应 import/helper。labvision flag 只决定 mock server 启动，不是此 transport 的 loopback 限制。Phase 7 search guards 通过不证明 Phase 9 vision egress 安全。
- `bootstrap:977` readShot/shotMeta 以未经 live-index 验证的模型 shotId 拼接路径。没有严格 ID/containment 检查，也没有对应帧的真实元数据；reuse 路径 metadata 硬编码 1080×2400、width/height=0。在本产品 DEVICE_ANDROID17 上不能用这个 fallback 计算动作坐标。需要沿现有 artifact/host-owned path 约束绑定真实 shot ref；禁止将字符串视作任意读路径。
- loadVisionConfig 只检查文件存在/JSON/enabled；providerId/modelId/key 缺失不一定返回 vision_unconfigured。SettingsActivity 只有 status/privacy + autofill seed 分支，`:572` 直接 writeText 明文 config；没有交付 VisionConfigWriter/Keystore sealed keys/atomic fsync rename readback/provider picker。NodeService 也不 materialize vision config。不要迁入新的 secret 存储路线；适配现有 SecureSettings/provider writer 模式，排除 backup。
- actual constants VISION_SESSION_CAP=30（runbook 为每滚动小时20）；bootstrap 是 session default key 的累积计数，不是 rolling-hour timestamps。没有 interpretation cache；`reused` 只表示传入了 shotId，仍发送新的网络请求。不要将它描述为零 egress idempotency。
- normalizer 对每个 0<x≤1.5 的坐标自动乘1000、拒绝 inverted bbox；runbook 的严格单位、orient、min-edge、actionable/depth tie-break 与实际有差异。文本超过预算按 JS字符截断，structuredContent.text 仍保存完整 parsed.text；没有总结果上限、HTTP response body 上限或 cancellation signal。correlation 使用 dump 返回的预算内节点，不是完整未截断节点集。
- `android-carrier.cjs:119` reply 没有 `_meta:{zcode/errorPresentation:"message-only"}`；缺配置的 text 只是 `ui_see failed: vision_unconfigured`，完整说明位于 structured error。不能声称 unwrapped anti-hallucination refusal 已经过真实模型测试。
- StandardAndroidProvider 只有 screenshot row，没有独立 MediaProjection provider 或 vision.invoke；NodeService 在 restart cycle publish，不证明 a11y/vision 状态改变即刷新。

### 6.4 验收与载荷文档差异

`gateway-unittest.cjs` 仍是22项原断言，没有三个新工具分类及 label hard-confirm pins。`verify-phase9.sh` 未覆盖 runbook stale frame、rotation、purge、SSRF/DNS、partial-config no-key、journal failure/no execution、真实模型发起/最终回答等关键项。timeout check 甚至允许 `d.ok` 为通过，锁屏 check 接受 rate_limited；这些不能算对应的拒绝/超时语义证明。测试有写 config/注册 tool/修改 journal/device posture 的行为，本轮未运行。

包内缺少 backup-rule 文件与 VisionConfigWriter，NodeService 还依赖发送方 RuntimeEnv/provider writers/capability classes；测试硬编码 `dev.zcode.nodepoc`、`scripts/` 和原 spike 目录，不能直接当本产品脚本运行。未来须适配 `dev.alita.androidagent` 和现有 dispatcher/provider contracts，不迁入发送方整个 app。

runtime5 manifest 的 assets.files/modules 已补齐 ui-tree、workspace-files、vision-pipeline，值得采用其描述增量；但 `bridge.endpoints` 仍为较早列表，bridgeEndpoints 摘要没有完整 `/vision/status`；实际 bootstrap 也无该 route。README、env-contract 的 `shell/lib` vs 实际 `shell-lib`、contracts/journal.sql v1 注释 vs actual gateway v3 均属历史差异。**不能因此删除产品 fail-closed 校验或让静态 SQL 覆盖现有 journal。**

## 7. 可以安全集成的增量及必须保留的产品部分

可以在下一次获得实施指令后作为起点：三个 canonical→carrier mappings、既有风险分类下的增量工具定义、独立 callback capture primitive、HARDWARE bitmap repair、无放大缩放、明确缺配置 refusal、bbox/Jaccard 与 pre-egress region soft gate 的基本路径、mock fixtures 及新 manifest module 描述。这里的“可以”是源码复用候选，不是未关闭缺口的生产上线批准。

必须保留产品实现：

1. 三项 Phase 8 measured repairs 及其本地 regressions；不得用 runtime5 原文件整体覆盖。
2. ZCodeRuntimeBundle 的 asset/native/install integrity、safeRelative/containment、原子安装与 explicit runtime version 验证；后续需显式支持 runtime5 和 vision module，并为合并后的文件重新生成 **product** hash，不伪装成 upstream byte-identical。
3. 当前 ZCode supervisor、Activity reconnect/session persistence/restart、SecureSettings、provider materialization、Compose UI、legacy diagnostics gate；上游 NodeService/SettingsActivity 仅参考增量。
4. AndroidToolDefinition/Request/Result/Error、ArtifactRef、Cancellation、Dispatcher、ZCodeToolMapping、identity/local provider 注册和已有 UI mappings。ZCode NDJSON protocol、MCP carrier 与 Android Local Tool Protocol JSON-RPC 2.0 的边界保持分开。
5. production Gateway policy ceiling、hard-confirm、journal v3、write_txn/backup/rollback、残余路径 kill-switch/regression。任何 capture/vision/gesture 也要共用既有取消/日志/授权/独占资源路径。

## 8. 推荐的下一次 Phase 9 集成顺序（本轮未执行）

1. 固定本次 upstream hashes，以当前 Phase 8 PASS 为 merge base；先确认第 6 节源码/合同差异的有效处置，尤其 label hard-confirm、shot identity/staleness/path containment、session cleanup、vision network guards/secret writer 和 internal-capture authorization。不得通过 DSHA 引入替代架构来绕开这些问题。
2. source-level 三方 merge bootstrap/Gateway/ui-tree；保留三项产品修复及 Kotlin mapping链路。显式升级 bundle manifest/layout/checksum验证；保持 zcode.cjs 不变，禁止 Route C。
3. 将 screenshot 与 tap 适配到现有 AndroidToolDispatcher/provider，ui_see 沿已裁决 INLINE boundary；接入现有 SecureSettings 配置与 backup exclusion，debug hooks 不能扩大 production surface。
4. 在目标 DEVICE_ANDROID17 上先做 capture capability、secure-window 两种拒绝路径、portrait/landscape/insets/rotation、byte/base64 budgets 与 callback/thread/cancel probes，保存 ROM 实测。root 只用于既有调试设备准备，不作为模型可见 backend。
5. 先跑原 Phase 8 repairs 的专用回归与 journal hash/lifecycle audit，再跑 runtime/session/search/identity/八UI/workspace/rollback回归。补齐第6节行为断言，保持 journal schema3，保留旧记录。
6. 最终执行真实模型→MCP tool event→carrier→dispatcher/Gateway→provider→physical result→model final answer：截图、see和tap分别有证据；测无vision/key缺失零egress、不确定/取消不迟执行、帧过期拒绝、hard-confirm不可绕过。mock与真实VLM分开报告；未测真实VLM准确性就记录未测。
7. 只有接收侧完整验收后更新 `zcode-integration-status.md` 为 Phase 9；否则继续 Phase 8 baseline 并记录 blocked case。此时仍不实施 PTY/Ubuntu/virtual display/input/MediaProjection/Root/Shizuku/ADB/updater/watchdog。

需要 ZCode 侧确认的问题（仅列入本地报告，未发送消息）：交付来源是否遗漏 A12d/A13/purge/guard 等已裁决增量；runbook与source冲突时哪些字段/错误/预算确为冻结合同；internal screenshot 的授权范围如何按既有 ceiling闭合；是否有修正后完整full-run与真实model trace；缺失 VisionConfigWriter/backup exclusion 等是否另有交付。收到更正包时先核对 hash 和源码，不能据回复措辞判定修复已采用。

## 9. 包完整性与版本证据

本轮只读 SHA 校验：Phase 9 清单27/27匹配（含包内hook-state）；runtime payload清单44项，43/44匹配。唯一不匹配为 `.mimosa/hook-state/sess_dacf9ed0-f35e-4eb7-a70f-21c9fa4f3433.json`，属于导出工具状态元数据；全部runtime/native/doc合同载荷项匹配。应记录这一差异，不能宣布44/44；不把无关hook-state作为production runtime安装对象。两个包六份重叠 runtime source 的hash全部相同。

| 检查对象 | SHA-256 |
|---|---|
| phase9 SHA256SUMS.txt | `8BFECC03DBD30C59E64468CE46349375DD280247D04137EEF9AC28A93F469403` |
| runtime5 payload SHA256SUMS.txt | `C9CD8FAF808D3A4112A2893A4B692073B2B4CC0AE85287FA7B7E4DA10EAD4DCF` |
| runtime5 runtime-manifest.json | `8FB44BF100962AED7811BEB358031549CAE929DCA24C0E54916730028DBF318F` |
| HANDOFF.md | `96F1FF922EBE81D36DDB6B141AB5C74052D8EF4E239D544E04647F7465C13797` |
| phase9-results.md | `3BC64FC78E3E1875B56FF4D6E2609D1B46F2EC6B366FC949024BC5D9B9CAC3CA` |
| phase9-adjudication.md | `BBCE6979E69E5C000E748315722ABEB447B97D8BBBF28AF5289F366E60795B12` |
| phase9-runbook.md | `37F04E21DC36F7CDCB183CDF6DE0644DAF91A2E970365114D05C45CB7DA33BAC` |
| upstream runtime5 gateway.cjs | `EA982BB09568B852660955C3AA532AD107BBC6CD09628FE46EE155F35D25D36E` |
| upstream runtime5 bootstrap.cjs | `EDAD8861C324FDC60A54E56E706CBC94BE35B31B3248BBFE277D9DBE220A6253` |
| upstream ui-tree.cjs（仍为原runtime4） | `2FD62B17BDDBA8558D094C9AFC6E74299A65A6A61F03977F3F79BF8CD6361F78` |
| vision-pipeline.cjs | `FC2C3F8A3EF06FE29519AE0C8B60DC0CF2C6C5803F65451D18694C756E726233` |
| unmodified zcode.cjs | `B1DF2EF3E5BD76C4AF3ECB296BC003A10D3F13191A26610BD0BA940FEADAD529` |

供后续协调检查的产品文件：`ZCodeRuntimeBundle.kt`、`ZCodeCarrierWorker.kt`、`ZCodeProviderConfigWriter.kt`、`zcode/adapter/{AndroidToolContracts,ZCodeToolMapping}.kt`、`device/AgentAccessibilityService.kt`、`model/SecureSettings.kt`、`zcode/capability/Capability.kt`、三个修复后的 runtime assets、`res/xml/{accessibility_service_config,data_extraction_rules}.xml`、`tools/phase8/{ui-boundary-regression,carrier-product-regression,product-journal-audit}.cjs`、`docs/handoff/evidence/phase8/` 和最新 integration status。以上 Kotlin 路径均在 `app/src/main/java/dev/alita/androidagent/` 下，runtime assets 在 `app/src/main/assets/zcode-runtime/` 下。

**停止点：仅分析与报告。Phase 8 继续作为 canonical receiving baseline；下一次实施需从本报告的源码差异出发，不能直接覆盖为 runtime5。**
