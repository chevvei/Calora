---
name: "changelog"
description: "记录大修改行为日志并自动汇总阶段性 changelog。每当完成一次大修改（新增模块/重大重构/接口变动/需求实现/Bug 修复）后必须调用；当用户要求 '总结日志/合并 changelog/出阶段报告' 时也调用。"
---

# Changelog Skill — UMI_Gripper 项目变更记录

本 skill 用于在 UMI_Gripper 项目中**强制性、结构化**地记录每一次大修改，并把若干次散乱的修改自动汇总为一次"完整功能日志"。

## 调用时机（强制）

满足以下任一条件，**必须**调用本 skill：

1. **新增功能模块**：例如新增 UsbReceiver、ImuWaveformPanel、ADS+QWK 集成
2. **重大重构**：替换核心库、调整目录结构、更换框架
3. **接口/协议变动**：UmiProtocol 字段调整、IDataSource 增减信号
4. **需求实现**：完成 `Docs/requirement.md` 中"节点 N"列出的任一指标
5. **Bug 修复**（影响用户可见行为的）：窗口拖拽失效、数据丢帧、CRC 失败
6. **用户显式要求**："写 changelog"、"总结一下今天的修改"、"合并日志"、"出阶段报告"

## 目录结构

```
Docs/changelog/
├── INDEX.md                      # 所有变更的目录索引
├── entries/                      # 单次变更日志（细粒度，每次一份）
│   ├── 2026-05-25_ads-qwk-integration.md
│   ├── 2026-05-25_window-drag-fix.md
│   └── ...
└── releases/                     # 阶段性汇总（粗粒度，由 N 次 entries 合并）
    ├── v0.3.0_ui-panel-system.md
    └── ...
```

## 单次记录（entries/）模板

文件命名：`YYYY-MM-DD_<kebab-case-topic>.md`

```markdown
# <一句话标题>

- 日期：YYYY-MM-DD
- 类型：feature | refactor | bugfix | docs | infra | perf | breaking
- 涉及模块：app/gui, core, device, infra, build, docs (列出实际触达层)
- 关联需求：Docs/requirement.md §<章节> 或 节点 N
- 关联 commit：<sha 或 N/A>

## 1. 背景 / 触发原因
为什么要改？暴露了什么问题？源自哪个用户需求？

## 2. 改了什么（What）
- 文件：[相对路径](file:///abs/path)
- 模块：函数/类/配置项

## 3. 怎么改的（How）
关键代码片段或设计选择。必要时贴差异：
```diff
- old
+ new
```

## 4. 怎么用（Usage）
用户/开发者如何感知/使用这次修改。命令、API、UI 操作步骤。

## 5. 影响面 / 风险
- 向后兼容？
- 性能影响？
- 需要重新构建/迁移？

## 6. 验证（Verification）
- 构建：`bash rebuild.sh` 通过
- 运行：`bash run.sh` 行为符合预期
- 测试用例：列出（若适用）

## 7. 关联
- Closes #<issue>
- 后续 TODO
```

## 阶段汇总（releases/）模板

文件命名：`v<MAJOR>.<MINOR>.<PATCH>_<kebab-case-theme>.md`

```markdown
# v<version> — <主题>

- 周期：YYYY-MM-DD ~ YYYY-MM-DD
- 包含 entries：N 篇
- 对应需求节点：Docs/requirement.md 节点 N

## 总览
本次发布的目标和主要价值（3~5 行）。

## 新功能（Features）
- ✨ <一句话> ← [entry](../entries/xxx.md)

## 重构与优化（Refactor / Perf）
- ♻️ ...

## Bug 修复（Bugfixes）
- 🐛 ...

## 破坏性变更（Breaking）
- ⚠️ ...

## 文档与基础设施（Docs / Infra）
- 📚 ...

## 验收对照
| 需求指标 | 来源 | 状态 |
|---------|------|------|
| 双相机视频流 | requirement.md §视觉 | ✅ |
| 触觉力可视化 | requirement.md §触觉 | 🚧 |

## 升级与迁移说明
用户从上一版本升级需要执行的步骤。

## 鸣谢/合并的 entries
- 2026-05-25_ads-qwk-integration.md
- ...
```

## 工作流程

### A. 单次修改完成后

1. 在 `Docs/changelog/entries/` 下新建一个 markdown 文件
2. 用上面"单次记录模板"填写
3. 在 `Docs/changelog/INDEX.md` 顶部加一行链接

### B. 自动汇总阶段日志

当满足以下任一条件触发汇总：
- 用户要求"总结日志 / 出阶段报告"
- 距上次 release 已积累 ≥ 5 篇 entries
- 完成 `requirement.md` 的"节点 N"全部条目

操作：
1. 读取 `Docs/changelog/entries/` 下自上次 release 之后的所有文件
2. 按类型聚类（feature / refactor / bugfix / breaking / docs）
3. 生成 `Docs/changelog/releases/v<version>_<theme>.md`
4. 更新 `Docs/changelog/INDEX.md` 添加 release 链接

## 规范要点

1. **不写流水账**：每条记录必须给出 What / How / Why / Verification 四要素
2. **链接优先**：所有文件引用用 markdown 链接到绝对路径
3. **中文优先**：项目沟通语言为中文，entries 用中文写作
4. **代码差异片段最长 30 行**：超过用 commit hash 或文件链接代替
5. **不创建空 entry**：没有实质修改就不要记录
6. **与 git 解耦**：本 skill 不依赖 git 历史，纯文档驱动

## 自检清单（每次调用 skill 后）

- [ ] entries/ 下生成了新文件，且文件名符合 `YYYY-MM-DD_*.md`
- [ ] INDEX.md 添加了新条目（最新在最顶部）
- [ ] 关联 `requirement.md` 中的具体章节或节点
- [ ] 列出了验证方式（构建/运行命令或测试编号）
- [ ] 没有遗留 TODO 没标注
