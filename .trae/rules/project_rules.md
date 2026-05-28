# Calora 项目规则（Trae 自动加载）

> 本文件由 Trae IDE 在每次会话开始时自动读取。
> Calora：面向健康饮食场景的 Android Food / Nutrition Agent。

## 1. 沟通规则
- 与用户交流使用中文
- 大规模代码修改前必须先给出中文方案（目标/模块/风险/收益/验证）
- 未经用户确认不直接执行大规模修改
- 回答方案类问题时，默认优先给可快速落地的 MVP 路线
- 涉及系统设计、技术架构、模块拆分时，默认输出结构化清单，并补充 ASCII 图或分层图

## 2. 项目定位
- 项目名称：Calora
- 项目方向：健康相关的 Food / Nutrition Agent
- 核心价值：识别食物 → 估算营养 → 给出健康建议 → 记录饮食
- 目标平台：Android
- 首要设备：小米 15（MIUI / HyperOS 系）
- 当前目标：先用免费模型把 MVP 跑到 Android 手机上
- 产品原则：先验证自动识别 + 健康营养建议 + 饮食记录闭环，再逐步增强

## 3. 技术路线约束
- 优先使用 Android 原生 Kotlin
- 优先使用免费、可本地部署的方案
- 模型优先级：TensorFlow Lite > ML Kit > ONNX Runtime Mobile
- 第一阶段优先轻量模型，不默认引入大体积端侧多模态模型
- 优先前台摄像头页面内的低频智能识别，不做后台常驻摄像头方案
- 输出技术方案时，优先考虑小米 15 的性能、温控、功耗与 MIUI 后台限制

## 4. 默认技术栈
- 语言：Kotlin
- UI：Jetpack Compose（如发现项目已有 View 体系，则遵循现有实现）
- 摄像头：CameraX
- 本地推理：TensorFlow Lite
- OCR：ML Kit Text Recognition
- 本地存储：Room
- 并发：Kotlin Coroutines + Flow / StateFlow
- 网络层：Retrofit（若项目已使用其他方案，遵循现有约定）

## 5. 架构与实现偏好
- 优先模块化设计，至少按 UI / camera / inference / nutrition / data 拆分思路组织代码
- 优先单向数据流与清晰状态管理
- 不写无必要的兜底兼容代码
- 不默认引入复杂微服务、重后端或云端依赖
- 第一阶段营养建议优先使用规则引擎，不默认引入 LLM 作为核心依赖
- 第一阶段热量与营养估算允许区间值，不追求伪精确克重

## 6. 方案输出要求
当用户要求产品方案、技术方案、系统设计、开发规划时，尽量包含：
- 目标与边界
- 模块拆分
- 数据流
- 技术选型理由
- 风险点
- MVP 与后续迭代划分
- 可直接执行的下一步
- 如适合，补充 ASCII 结构图

## 7. 编码前检查
在修改 Android 项目相关代码前，优先检查：
- settings.gradle / settings.gradle.kts
- build.gradle / build.gradle.kts
- app/build.gradle / app/build.gradle.kts
- AndroidManifest.xml
- 现有 package 结构与命名风格
- 已使用的 UI、DI、网络、存储、测试框架

## 8. 代码风格
- 遵循现有项目代码风格与目录结构
- 默认使用 Kotlin 强类型与明确数据模型
- 避免过度抽象
- 严禁提交 secrets、keys、签名文件或敏感配置
- 未经用户要求，不新增注释

## 9. 测试与验证
- 涉及硬件、摄像头、模型推理、OCR 等能力时，优先设计可 mock 的边界
- 如果项目存在测试命令，完成任务后必须运行 lint、typecheck、test 或对应 Android 校验命令
- 如果项目尚未确定验证命令，先检查 Gradle 任务；若仍不明确，再询问用户
- 典型候选命令包括：
  - `./gradlew tasks`
  - `./gradlew lint`
  - `./gradlew test`
  - `./gradlew assembleDebug`

## 10. Git 提交格式
```
[<module>][<type>]: 中文描述 / English description
```

type：feat / fix / refactor / perf / docs / style / test / breaking

## 11. 当前阶段优先级
- 第一优先级：Android 端 MVP 快速落地
- 第二优先级：免费模型在真机可运行
- 第三优先级：围绕小米 15 实机体验做性能与功耗优化
- 第四优先级：沉淀技术路线、系统设计、架构图与开发清单

## 12. 明确不优先做的事项
- 不优先全场景连续视频流理解
- 不优先后台常驻摄像头
- 不优先超大端侧 VLM 直上生产
- 不优先复杂多 Agent 编排
- 不优先重运营后台和复杂云服务

## 13. 命名方案
- 项目文件夹：Calora
- GitHub / Gitee 仓库名：calora
- Android applicationId：com.calora.app
- Android namespace：com.calora
- App 展示名：Calora
- Git 默认分支：main
- 远程仓库：GitHub（origin）+ Gitee（gitee），双仓库同步推送
