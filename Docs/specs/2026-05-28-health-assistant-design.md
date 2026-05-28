# 健康助手 + 每日卡片 + AI 记忆 · 设计规格

- 日期：2026-05-28
- 状态：已通过头脑风暴并经用户批准（B 路径：落地规格 + 进入 writing-plans）
- 负责模块：`nutrition` / `assistant`（新增）/ `data/local` / `ui/screen`
- 关联问题来源：用户要求"健康助手 + 每日健康建议 + AI 对话 + 长期记忆"

---

## 1. 目标与边界

### 1.1 目标

1. 首页新增「今日健康助手」折叠区块，包含 1 张主卡（今日健康建议）+ 2-3 张知识卡（横滑）
2. 主卡 / 知识卡内容**完全本地生成**（基于 `HealthAdvisor` 规则 + 今日 `MealRecord`），零网络成本，每日自动刷新
3. 点击主卡进入「健康助手」对话页，复用现有 `LLMConfigStore`，对话中注入【今日饮食摘要 + 长期记忆事实】
4. 退出对话页时后台异步调 LLM 提炼新事实（如"用户乳糖不耐受"），追加进记忆库
5. 提供"记忆管理"页：查看、删除每条事实（编辑列入 P1）

### 1.2 不做（YAGNI 边界）

- 不做远程同步、不做多用户、不做 Tab 重构、不做语音输入
- 不在每日主卡 / 知识卡上花 LLM token
- 不做记忆编辑 UI（仅查看/删除）
- 不做诊断式医疗建议

---

## 2. 模块拆分

```
com.calora/
├── nutrition/
│   ├── HealthAdvisor.kt              （已有，扩展 dailySuggestion / tipDeck）
│   └── DailyAdvisorEngine.kt         （新增：聚合今日 MealRecord → 今日主卡 + 知识卡）
├── assistant/                         （新增模块）
│   ├── memory/
│   │   ├── MemoryFact.kt             （data class，UI 层使用）
│   │   ├── MemoryFactEntity.kt
│   │   ├── MemoryFactDao.kt
│   │   ├── MemoryRepository.kt
│   │   └── MemoryExtractor.kt        （后台提炼：调 LLM JSON 模式）
│   ├── chat/
│   │   ├── ChatMessage.kt            （data class，UI 层使用）
│   │   ├── ChatMessageEntity.kt
│   │   ├── ChatMessageDao.kt
│   │   └── ChatRepository.kt
│   ├── prompt/
│   │   └── AssistantPromptBuilder.kt （SKILL_PROMPT + 今日饮食 + 记忆注入）
│   └── AssistantClient.kt            （复用 LLMClient，新增 chat() 与 extractMemory()）
├── data/local/
│   └── AppDatabase.kt                （升级版本号 +1，新增 MemoryFactEntity / ChatMessageEntity）
└── ui/screen/
    ├── HomeScreen.kt                 （修改：嵌入 HealthAssistantSection）
    ├── HealthAssistantSection.kt     （新增：主卡 + 横滑知识卡）
    ├── AssistantChatScreen.kt        （新增：对话页）
    ├── AssistantChatViewModel.kt     （新增）
    ├── MemoryManageScreen.kt         （新增）
    └── MemoryManageViewModel.kt      （新增）
```

---

## 3. 数据流

```
        ┌──────────────────────────────────────┐
        │  HomeScreen                          │
        │  ┌────────────────────────────────┐  │
        │  │ HealthAssistantSection         │  │
        │  │  ├─ DailyAdvisorEngine.build() │◄─┼─ 本地：MealRecord(今日) + HealthAdvisor 规则
        │  │  ├─ MainCard (今日建议)        │  │
        │  │  └─ KnowledgeCards (横滑)      │  │
        │  └────────────┬───────────────────┘  │
        └───────────────┼──────────────────────┘
                        │ 点击主卡
                        ▼
        ┌──────────────────────────────────────┐
        │  AssistantChatScreen                 │
        │  ┌────────────────────────────────┐  │
        │  │ ChatMessageList ◄── Room       │  │
        │  │ InputBar ──► AssistantClient   │  │
        │  └──────┬─────────────────────────┘  │
        └─────────┼────────────────────────────┘
                  │ 用户发送
                  ▼
        AssistantPromptBuilder.build():
        ┌────────────────────────────────────┐
        │ system = SKILL_PROMPT              │
        │        + 今日 MealRecord 摘要       │
        │        + MemoryFact Top20          │
        │ messages = 最近 8 轮历史 + 当前输入 │
        └────────────────┬───────────────────┘
                         ▼
                    LLMClient
                         │
                         ▼
                    AI 回复 → 入 Room ChatMessage
                         │
                         ▼ （用户退出聊天页 / DisposableEffect.onDispose）
        MemoryExtractor.extractAsync(本次对话窗口)
          └─► LLM JSON mode 返回新事实列表
          └─► 与现有 MemoryFact 去重 / 合并（confidence 增强）
          └─► 写入 Room
```

---

## 4. 数据模型

### 4.1 MemoryFactEntity

```kotlin
@Entity(tableName = "memory_facts")
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,        // health / preference / goal / restriction
    val content: String,         // 如 "乳糖不耐受"
    val confidence: Float,       // 0.0 ~ 1.0
    val createdAt: Long,
    val updatedAt: Long,
    val sourceMsgId: Long? = null
)
```

去重规则：`category + content` 命中视为同一条，更新 `updatedAt`，confidence 取 `max(old, new)` 上限 1.0。

### 4.2 ChatMessageEntity

```kotlin
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,            // user / assistant
    val content: String,
    val createdAt: Long
)
```

### 4.3 Database 升级

- 版本号 +1
- MVP 阶段使用 `fallbackToDestructiveMigration()`（项目尚未上架，可接受历史 MealRecord 清空）
- 上架后由 P1 切到真 Migration

---

## 5. Prompt 设计

### 5.1 SKILL_PROMPT（系统级常驻）

```
你是 Calora 健康营养助手，遵循中国居民膳食指南(2023)。
风格要求：
- 简洁、专业、emoji 分段、总字数 ≤ 200
- 总是结合用户长期事实做个性化回答
- 不诊断疾病，遇医疗问题引导就医
- 用户今日饮食已注入，建议中应体现
```

### 5.2 用户上下文段（每次拼接）

```
[今日饮食]
- 总热量: {kcal}
- 总蛋白/碳水/脂肪: ...g / ...g / ...g
- 餐次: 早 / 午 / 晚 / 加餐 详情...

[用户长期事实]（Top N by confidence DESC，N ≤ 20）
- [health] 乳糖不耐受 (0.95)
- [goal] 减脂 (0.90)
- [restriction] 不吃猪肉 (0.85)
- ...
```

### 5.3 提炼 Prompt（后台异步）

```
请从以下对话中提取关于用户的"长期事实"，仅输出 JSON 数组：
[{"category":"...","content":"...","confidence":0.0-1.0}]

规则：
- category 只能取：health / preference / goal / restriction
- 不要提炼一次性问题或临时情绪
- confidence < 0.6 不输出
- 若无值得记忆的事实，返回 []

对话：
<对话窗口>
```

调用方完成 JSON 解析失败时静默丢弃，不影响用户主流程。

---

## 6. 技术选型理由

| 决策 | 理由 |
|---|---|
| 复用 `LLMClient`（HttpURLConnection） | 项目已有，无需新引入 OkHttp/Retrofit；保持 APK 体积稳定 |
| Room 存事实 + 历史 | 项目已用 Room，仅升级 `AppDatabase` 版本号 |
| 每日卡本地生成 | 用户明选 MVP，零成本零延迟；AI 用在对话场景 |
| 退出聊天后台提炼 | 不阻塞用户、不污染聊天输出；用户无感知 |
| 短期上下文 = 最近 8 轮 | token 可控（≈1-2K），多数对话足够 |
| Top 20 记忆事实注入 | 按 confidence DESC，防止 prompt 爆炸 |
| 后台提炼用 `DisposableEffect.onDispose` 触发 | 与 Compose 生命周期一致，无需 WorkManager |

---

## 7. 风险与缓解

| 编号 | 风险 | 缓解 |
|---|---|---|
| R1 | 用户首次点开聊天页未配置 API | 检测到 `LLMConfigStore.current().isValid()=false` 时展示空态卡片，引导跳转「设置→LLM」 |
| R2 | 提炼噪音事实污染记忆库 | confidence 阈值 ≥ 0.6；记忆管理页可手动删除 |
| R3 | DB destructive migration 清空历史 | MVP 阶段可接受；上架前必须切真 Migration |
| R4 | 网络失败 | 聊天页错误气泡兜底，本地卡不受影响 |
| R5 | 隐私 | 所有记忆与历史只存本地 Room，不上传任何云端 |
| R6 | 提炼 LLM 调用失败 | 静默丢弃，下次退出再尝试 |
| R7 | 聊天历史无限增长 | MVP 阶段不做清理；P1 提供"清空历史"按钮 |

---

## 8. MVP 与后续迭代

### 8.1 MVP（本期）

1. `DailyAdvisorEngine` + `HealthAssistantSection`（首页主卡 + 知识卡）
2. `AssistantChatScreen`（含历史持久化 + 记忆注入 + 今日饮食注入）
3. 退出后台事实提炼（`MemoryExtractor`）
4. `MemoryManageScreen`（列表 + 删除）
5. `AssistantClient.chat()` 与 `extractMemory()`

### 8.2 P1（下期）

- 记忆编辑（不仅删除）
- 知识卡轮播 + 收藏
- 聊天页快捷问题按钮
- 清空对话历史
- Database 真 Migration

### 8.3 P2

- 记忆 confidence 周期性衰减
- 健康偏好向导（首次向导填写）
- 多会话 / 话题分组

---

## 9. 验收要点（实施后必须满足）

1. 首页可见"今日健康助手"区块，含 1 主卡 + ≥2 知识卡
2. 主卡内容随今日 `MealRecord` 变化而变化
3. 点击主卡进入聊天页，能发送、能收到 AI 回复
4. API 未配置时聊天页显示引导卡，不闪退
5. 退出聊天页后，重新进入下次对话，AI 体现记忆（例：用户曾说"我乳糖不耐受"，下次问"喝什么"时 AI 主动避开牛奶）
6. 记忆管理页能列出所有事实，能删除
7. `./gradlew :app:assembleRelease -x lint` 通过
8. 真机（小米 15）能完整跑通：进入聊天 → 对话 → 退出 → 重进 → AI 体现记忆

---

## 10. 可执行的下一步

1. 本规格提交到 Git
2. 进入 `sp-writing-plans`，产出分步实施计划（含每步验收点 + Stop Points）
3. 用户审查实施计划
4. 按计划执行，每完成一阶段编译验证
