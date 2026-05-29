# 营养精细化 + 记录详情页 设计规格

> 日期：2026-05-28
> 关联前置规格：`2026-05-28-health-assistant-design.md`（独立模块，不互相阻塞）
> 状态：待用户审查

---

## 1. 目标与边界

### 目标
1. 修正"识别成功但 kcal=0 / 蛋白/碳水/脂肪=0"的现象。
2. 把"识别大类"升级为"细分类 + 精确营养"：字典优先、LLM 补盲、缓存回写。
3. 单餐结果展示「4-4-9 三大产能营养素占比」+「占每日 RDA」+ 可信区间。
4. 首页历史记录可点击进入详情页回看。

### 非目标（YAGNI）
- 不替换 TFLite 模型（不重训、不引入大端侧多模态）。
- 不做单张拍照 / 连拍模式切换。
- 不做手动输入食物搜索框。
- 不做营养值手动编辑（P1）。

---

## 2. 决策记录

| 项 | 决策 | 理由 |
|---|---|---|
| 识别路线 | 字典为主 + LLM 补盲 | 0 网络成本 + 长尾覆盖 |
| 占比口径 | 4-4-9 三大产能营养素 | 营养学标准 |
| 精度目标 | 可信区间（如 240±20 kcal） | 符合项目规则第 5 条 |
| LLM 频率 | 字典 / 缓存未命中才调 | 配额可控、离线友好 |
| 范围 | A（识别+营养）+ B（详情页） 合并一个计划 | 用户决策 |

---

## 3. 模块拆分

### 新增
| 文件 | 职责 |
|---|---|
| `inference/LabelMapper.kt` | 加载 `nutrition_label_map.json`，英文 label → 中文名 + 同义词 |
| `nutrition/NutritionResolver.kt` | 三级查询：字典 → 缓存 → LLM；负责回写缓存 |
| `nutrition/MacroComposer.kt` | 4-4-9 占比 + RDA 占比聚合 |
| `data/local/NutritionCacheEntity.kt` | LLM 结果缓存实体 |
| `data/local/NutritionCacheDao.kt` | 缓存 DAO |
| `ui/screen/RecordDetailScreen.kt` | 历史记录详情页（复用 NutritionCard） |
| `ui/component/NutritionCard.kt` | 结果页 + 详情页共用营养卡 |
| `ui/component/MacroRing.kt` | Canvas 绘制 4-4-9 占比环 |
| `app/src/main/assets/nutrition_label_map.json` | 中英映射 ~200 条（MVP），P1 扩到 500+ |

### 修改
| 文件 | 改动 |
|---|---|
| `llm/LLMClient.kt` | 新增 `queryNutrition(name: String, bitmap: Bitmap?): NutritionQueryResult` |
| `inference/FoodClassifier.kt` | `composeLive` 改走 `NutritionResolver.resolve()` 替代直查字典 |
| `data/local/AppDatabase.kt` | version +1，新增 `NutritionCacheEntity` |
| `data/local/MealRecordDao.kt` | 新增 `suspend fun getById(id: Long): MealRecordEntity?` |
| `data/repository/MealRepository.kt` | 转发 `getById` |
| `ui/screen/HomeScreen.kt` | `MealRecordItem` 加 `onClick` 回调 |
| `ui/screen/ResultScreen.kt` | 用 `NutritionCard` + `MacroRing` 替换现有展示块 |
| `ui/navigation/NavGraph.kt` | 新路由 `RECORD_DETAIL/{recordId}` |
| `core/AppModule.kt` | Provides：`NutritionResolver`、`NutritionCacheDao`、`LabelMapper`、`MacroComposer` |
| `core/Routes.kt` | 加 `RECORD_DETAIL = "record_detail/{recordId}"` 及构造函数 |

---

## 4. 数据流

```
拍照 / 实时帧
    │
    ▼
TFLite (food_v1) ── english label + conf
    │
    ▼
LabelMapper ────── 中文名 + 同义词列表
    │
    ▼
NutritionResolver.resolve(name, bitmap?)
    │
    ├─ ① 本地字典 NutritionEstimator       命中 → per100g (HIGH)
    ├─ ② Room NutritionCache (by name)     命中 → per100g (MEDIUM)
    └─ ③ LLMClient.queryNutrition()
           成功 → 写入 NutritionCache，返回 per100g 区间 (MEDIUM)
           失败 → 回落"未知食物"占位 100kcal (LOW)
    │
    ▼
SizeEstimator → gramsLow / gramsHigh
    │
    ▼
MacroComposer.compose()
   - kcalLow, kcalHigh
   - protein/carbs/fat 克数（中值）
   - macroPercent{P%, C%, F%}      4-4-9 标准化
   - rdaPercent{...}                复用现有 HealthAdvisor.getRdaPercentages
   - confidenceTier
    │
    ▼
ResultScreen / RecordDetailScreen
    └─ 复用 NutritionCard + MacroRing
```

---

## 5. 数据模型

### 5.1 NutritionCacheEntity

```kotlin
@Entity(tableName = "nutrition_cache")
data class NutritionCacheEntity(
    @PrimaryKey val name: String,         // 标准中文名
    val kcalLow: Float, val kcalHigh: Float,
    val proteinLow: Float, val proteinHigh: Float,
    val carbsLow: Float, val carbsHigh: Float,
    val fatLow: Float, val fatHigh: Float,
    val servingLow: Float, val servingHigh: Float,
    val source: String,                   // "llm" | "dict_override"
    val updatedAt: Long
)
```

### 5.2 MacroBreakdown

```kotlin
enum class ConfidenceTier { HIGH, MEDIUM, LOW }

data class MacroBreakdown(
    val kcalLow: Int, val kcalHigh: Int,
    val proteinG: Float, val carbsG: Float, val fatG: Float,
    val proteinPct: Float, val carbsPct: Float, val fatPct: Float,
    val rda: RdaPercentages,
    val tier: ConfidenceTier
)
```

### 5.3 NutritionQueryResult（LLMClient 返回）

```kotlin
sealed class NutritionQueryResult {
    data class Success(
        val name: String,
        val per100gKcal: ClosedFloatingPointRange<Float>,
        val per100gProtein: ClosedFloatingPointRange<Float>,
        val per100gCarbs: ClosedFloatingPointRange<Float>,
        val per100gFat: ClosedFloatingPointRange<Float>,
        val typicalServingG: ClosedFloatingPointRange<Float>,
        val confidence: Float
    ) : NutritionQueryResult()
    data class Error(val message: String, val networkUnreachable: Boolean = false) : NutritionQueryResult()
}
```

### 5.4 DB 升级

- 当前 version=1 → 本规格升到 version=2（Health-Assistant 规格执行后会再 +1 到 3）。
- 用 `fallbackToDestructiveMigration()`，MVP 阶段可接受清空历史。
- AppDatabase 实体表：`MealRecordEntity`、`NutritionCacheEntity`。

---

## 6. 4-4-9 算法

```kotlin
fun macroPercent(p: Float, c: Float, f: Float): Triple<Float, Float, Float> {
    val theoretical = p * 4 + c * 4 + f * 9
    val basis = if (theoretical <= 0f) 1f else theoretical
    val pp = (p * 4 / basis) * 100f
    val cp = (c * 4 / basis) * 100f
    val fp = (f * 9 / basis) * 100f
    val sum = pp + cp + fp
    return if (sum <= 0f) Triple(0f, 0f, 0f)
    else Triple(pp / sum * 100f, cp / sum * 100f, fp / sum * 100f)
}
```

- 分母用「理论 kcal」，避免占比和 ≠ 100% 的视觉误导。
- 强制归一到 100%（防浮点误差）。

---

## 7. UI 规范

### 7.1 NutritionCard 布局

```
┌─────────────────────────────┐
│  🍚  番茄炒蛋          ⚪⚪⚪ │  ← 右上角置信度小点(填实数 = tier)
│  约 240 kcal (180-280)      │
│  ─────────────────────────  │
│       [ 4-4-9 占比环 ]      │  ← MacroRing
│   蛋白 28% 碳水 22% 脂肪 50%│
│  ─────────────────────────  │
│  蛋白 16g · 碳水 12g · 脂肪 14g│
│  ─────────────────────────  │
│  占今日 RDA  ▓▓▓░░ 12%       │
└─────────────────────────────┘
```

### 7.2 MacroRing

- Compose `Canvas`，三段彩色环（蛋白 蓝 #4A90E2 / 碳水 黄 #F5A623 / 脂肪 红 #D0021B）。
- 中间显示总 kcal（取中值）。
- 起始角度 -90°，顺时针。

### 7.3 详情页

- 顶部 TopAppBar：返回 + 标题"饮食详情"。
- 主体：`NutritionCard`（同结果页）。
- 底部：删除按钮（确认弹窗）。

### 7.4 首页改动

- `MealRecordItem` 加 `onClick: (Long) -> Unit` 参数 → 导航到 `record_detail/{id}`。
- 整张 Card 可点击（`Modifier.clickable`）。

---

## 8. LLM Prompt

### 8.1 SYSTEM

```
你是中文营养师。严格返回 JSON，禁止任何额外文字、解释或 Markdown 包裹。
```

### 8.2 USER（含图）

```
图片中识别的食物名称：<name>
请基于图片估算典型营养，返回 JSON：
{
  "name": "标准中文名",
  "per_100g": {
    "kcal":     [low, high],
    "protein_g":[low, high],
    "carbs_g":  [low, high],
    "fat_g":    [low, high]
  },
  "typical_serving_g": [low, high],
  "confidence": 0~1
}
```

### 8.3 调用参数

- `temperature = 0.2`
- `max_tokens = 256`
- 与现有 `analyzeFood` 复用 HTTP 连接逻辑，仅替换 prompt 和解析器。
- 解析失败 / 字段缺失 → 返回 `Error`，由 Resolver 走兜底。

---

## 9. 缓存策略

- 命中字典（HIGH）：不查缓存、不查 LLM。
- 未命中字典：先查 `NutritionCacheDao.getByName(name)`；命中即返回 MEDIUM。
- 缓存未命中：调 LLM；成功后 `INSERT OR REPLACE`，返回 MEDIUM。
- LLM 失败：返回兜底「未知食物 100kcal / 5/15/3 g」，tier=LOW；**不写缓存**。
- 失败重试间隔：同一 `name` 5 分钟内不再调 LLM（内存级 Map，进程重启清空）。

---

## 10. 路由

| 路由 | 参数 |
|---|---|
| `record_detail/{recordId}` | `recordId: Long` |

NavGraph 用 `navArgument("recordId") { type = NavType.LongType }`。

---

## 11. 风险

| ID | 风险 | 缓解 |
|---|---|---|
| R1 | LLM 返回非 JSON | 严格解析；失败走兜底；日志记录原文前 300 字 |
| R2 | LLM 配额耗尽 | 缓存优先；失败 5 分钟黑名单；config.enabled=false 时直接走兜底 |
| R3 | DB destructive 清掉历史记录 | MVP 可接受；规格中明示用户首次升级会清空 |
| R4 | LabelMapper 漏映射 | MVP 200 条覆盖高频；漏映射时直接用模型原 label 查询，仍可走 LLM |
| R5 | 4-4-9 与显示 kcal 不一致 | 用理论 kcal 算占比；UI 显式标"估算" |
| R6 | 详情页 / 结果页样式漂移 | 强制走同一 `NutritionCard` 组件 |
| R7 | 离线 + 字典未命中 → 体验差 | 走「未知食物」兜底 + 显式提示「未联网，营养为估算值」 |
| R8 | DB 升级与 Health-Assistant 规格相互冲突 | 本规格 v1→v2；Health-Assistant 规格 v2→v3。先执行哪个都正确（destructive） |

---

## 12. MVP / P1 / P2

### MVP（本规格交付）
- LabelMapper + `nutrition_label_map.json`（~200 条高频）
- NutritionResolver 三级查询
- NutritionCacheEntity / Dao
- MacroComposer 4-4-9
- NutritionCard + MacroRing
- ResultScreen 改造
- 记录点击 → RecordDetailScreen
- LLMClient.queryNutrition()

### P1
- 标签映射远程拉取 / 增量更新
- 营养值手动编辑
- 多食物画面同时识别

### P2
- 份量手势调整（拖动改克重）
- 历史记录搜索 / 按周分组

---

## 13. 验收标准

1. 拍 1 张白米饭照片 → 结果页显示 `~116 kcal (90-140)`，4-4-9 占比环正确（蛋白 ≈9% / 碳水 ≈89% / 脂肪 ≈2%）。
2. 拍 1 张字典外食物（如"佛跳墙"）→ 联网时触发 LLM，结果展示且 DB 出现缓存行；再次拍秒出（无 LLM 调用）。
3. 结果页"占每日 RDA"条形条与现有 `getRdaPercentages` 输出一致。
4. 首页点击"番茄炒蛋"记录卡 → 进入详情页，展示与结果页完全相同的 `NutritionCard`。
5. 关网络拍照 → 走字典 / 走兜底，不闪退；离线 + 字典未命中时显示「未联网，营养为估算值」提示。
6. LLM 返回乱字符 → 不闪退，回落兜底，Logcat 出现 `[LLM] nutrition parse failed: ...`。
7. 离线连续拍 10 张已缓存食物 → 全部秒出（验证缓存命中、无 LLM 调用）。
8. 详情页删除按钮 → 弹确认 → 删除后回到首页，列表少一条。

---

## 14. 测试边界 / Mock 点

- `LabelMapper` 可独立 unit test：输入英文 label → 输出中文名。
- `MacroComposer.macroPercent` 可独立 unit test：边界（全 0、单一营养素）。
- `NutritionResolver` 可注入假的 `Dictionary` / `Dao` / `LLMClient` 来 unit test 三级路径。
- `LLMClient.queryNutrition` 走集成层，单元测仅测 JSON 解析。

---

## 15. 下一步

1. 本规格提交到 Git。
2. 用户审查规格。
3. 通过后调用 `sp-writing-plans` 产出分步实施计划（含每步 Stop Points）。
4. 按计划逐步实施，最后真机回归 8 条验收。
