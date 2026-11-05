# Android Accessibility Toolkit — research archive

可单独研究的 Android 无障碍执行基础与语义 UI 树：有界节点采集、raw/normalized 节点关联、查询、点击/输入/滚动验证及截图。

## 内容与使用

accessibility/ 为 Android library，源码保持原实现；runtime/ui-tree.cjs 为无第三方运行时依赖的 Node 模块；reference/ 保存受控 UiLab 和原集成测试。
JDK17、SDK35、Gradle8.9：`gradle :accessibility:assembleDebug`。
Node 语义树映射回归：`node tests/tree-mapping.cjs`，只运行已有测试中的纯树段，不重复测 carrier 定时器。
宿主应提供自己的设置入口、显式系统授权和动作权限决策。原 XML 的 settingsActivity 是产品类名，接入其他应用时需替换。

## 边界

截图依赖系统 API 和厂商行为，已测主动窗口截图条件保留在报告中；不表示任意窗口/安全窗口可捕获。
模型、视觉供应商、图片上传、Gateway、权限 UI 均属于宿主集成；这里不自动配置或发送图片。
Android17 历史结果和 Android15 未闭合的只读验收分别登记，不将截图文件存在等同于完整验收 PASS。
虚拟显示、虚拟输入、MediaProjection 后端不是本仓库已实现能力。

# 本仓库的身份 / Identity

结项日期：2026-11-05。项目已停止持续开发，本仓库为冻结的研究成果。
This is a frozen research archive, with no maintenance or production-safety commitment.

自有代码 MIT；第三方依赖保留原许可。源码与原产品的对应关系见 SOURCE_MAP.json。
设备验收报告是指定历史源码/APK/设备上的证据，不代表拆分后新跑了同一真机测试。
本次新跑的导出测试会单列在 EXPORT_VERIFICATION.md；没有测试的内容明确列出。
主索引：https://github.com/Alita23333/android-agent-research
