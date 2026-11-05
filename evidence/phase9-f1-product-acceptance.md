# Phase 9 F1 产品修复与验收

2026-10-04（Asia/Shanghai）。Android17 产品设备 `DEVICE_ANDROID17`（MODEL_ANDROID17），Android15 `DEVICE_ANDROID15` 未操作。

**F1 产品集成及本轮验收 PASS。** 仅合入已审阅上游 bootstrap.cjs 的启动配置写入/读回/语义校验失败即停止 Core 修复，并更新该文件的载荷SHA。没有改变 Gateway、journal schema3、workspace、carrier、Accessibility、模型配置或授权架构；没有新增生产能力。

## 来源与当前制品

- 采用 `zcode-phase9-sync-revision-20261003-r2`（ZIP SHA `7BB6E3CFC96DADEFC2303B8A178863643213416FFBEA3C1165957621CC2FC74E`）的 source/runtime/bootstrap.cjs，字节一致。
- bootstrap SHA：`E66A36A489882D21D916F803283D2582D0A1ECA176ABCBCBF5B991D202B71E7B`。
- runtime版本继续为 `0.16.9-runtime5`，manifest SHA保持 `38735F3D4CFDA8E05D45AAA4B310473D34CD9816E9E1C67C9BF2B9C41E1B73AD`。
- 新payload SHA清单哈希：`766C7B9174CAA83CADD5880CB8F03DACB11975F934A4246DCF84B66C401EF010`；34项APK载荷与33项已安装不可变文件均匹配。宿主个人provider配置例外保持。
- 新已安装APK：`evidence/phase9-f1/product-f1.apk`，SHA `E05A8B64C6B0853497B577903BFBFDA52C613EE17C50F5DFADC5CDA9538D4EB7`。
- 原Phase9冻结APK SHA `9AACC118DFA83372837FFB4764414CC88FF7F477AE776646C318EBE87DF03EF7`仍匹配，原验收文件未覆盖。

## 实际测量

| 层 | 结果 | 范围 |
|---|---|---|
| 源级故障注入 | 9 PASS / 0 FAIL | 配置读写失败、损坏、部分写回、正常配置和carrier开关；采用实际产品源码块 |
| 现有源级边界 | 4 suites PASS | Phase8 repairs、内部授权边界、carrier取消、vision产品边界 |
| 构建/静态/单元 | PASS | assembleDebug、lintDebug；JVM30/0失败/0错误/0跳过 |
| 真机F1 | 4 PASS | 可读取但chmod400文件写入EACCES→Core不启动；损坏配置→Core不启动；删除测试配置后重建恢复；carrier.disabled关闭时MCP声明移除 |
| 运行时 | 6 PASS | 真实provider turn及流式、取消、Activity重连、runtime崩溃重启、会话恢复、force-stop/relaunch及持久化消息 |
| 真实既有工具 | 3 PASS | model-issued设备身份、界面读取、Tavily搜索 |
| 真实视觉 | 3 PASS | model-issued截图、当前真实多模态provider视觉、坐标点击并核验count=1 |
| 工作区与回滚 | 5 PASS | 模型create/overwrite/append/delete；物理回滚验证且生成新operation；测试文件最后删除 |

故障测试使用应用私有的生成配置，原文件备份留在设备私有目录，finally字节核验后恢复，再启动正常运行时。没有导出配置、凭据或真实图像。诊断HTTP保持可用，但失败时Core未启动；不是宣称全部HTTP宿主路由被关闭。

工具回归首次设备身份测试在 provider/testModelConnectivity 前发生 SSE socket hang up/timeout。运行时随后状态ready，对只读用例重新连接后通过。保存 `device-identity-first-attempt.err`；根因尚未隔离，不抹除首次失败。故障驱动开发时修正了非exported Activity需root启动，以及carrier.disabled位于files根目录的测试路径错误；这些是测试适配，不是生产修复。

## 证据与限制

证据目录 `docs/handoff/evidence/phase9-f1/` 含artifact-identity、installed-hashes、device-faults、unit-results、四组summary、模型/工具结果及构建日志。工具回归结果17项加F1真机4项；source fault9独立计数，不混成真实模型测量。

本轮没有重复此前29项完整物理视觉矩阵或APK-owned Node三套旧单元；此前结果保留为历史基线，本轮明确覆盖上表。force-stop后Accessibility权限仍需恢复，验收用已授权root调试设置，未增加生产自动恢复。新发现的滚动方向问题仍未修改：产品forward当前top→bottom，待上游完整生产源文件/补正证据与本侧专门验收。截图直接间隔测量、正向长列表目前只在上游Android15测过，不据此宣称Android17产品PASS。

## ZCode 下一步指示

1. 查收本通知，记录Android17已采用你方F1源码且本轮验收通过；不要重复修改或重新发布相同F1生产实现。
2. 继续处理 `codex-phase9-tests-review-20261004-r1`（ZIP SHA `D94D416BE65E655D5B2E82D3D7105A76E35318B50FBED474F94658C8126D76E2`）中的测试补正：交完整方向修改源文件/diff/hash、精确APK与运行身份、边界完整响应、可核对journal关联或诚实PARTIAL、B3状态声明和完整依赖哈希。
3. 不开展新的生产能力，不修改Gateway/journal/策略/350ms阈值，不操作Android17。源码交付供审阅，不自动获得产品集成批准。
4. 通过原v1协议发布新ID回包。用户仅提醒“转交完成，请查收”；缺项标PARTIAL，保持真实VLM NOT_TESTED（除非实际在你方测量），不引用本侧证据冒充你方测量。
