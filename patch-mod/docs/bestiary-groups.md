# 图鉴族类：显示与查找分离

`bestiary_groups.json` 维护英文族类显示名称，模块仅在客户端使用。当前六个目标方法、八处修改；词表不参与查找键生成。

| 入口 | 处理 |
|---|---|
| `GroupUtils.getEntityGroupNames` | 两个 stream mapper 改为 PartialEntityGroup.getId → lookupName，保留原文查找键 |
| `GroupListElement.<init>` | 只翻译 TextComponent 标签；原 groupName 用于筛选和回调 |
| `GroupUtils.getEntityName`、`TextUtil.formatLocationPathAsProperNoun` | 翻译族类显示及 Dweller 特例；具体实体分支不变 |
| `EntityDefinitionElement.lambda$new$0` | 详情返回直接用 group.getId → lookupName → selectGroup |
| `BestiaryScreen.<init>(EntityPredicate)` | 在 selectGroup 前仅对真实 PartialEntityGroup 使用原始 ID；其他 predicate 保留原结果 |

`lookupName` 按 ID 路径拆分下划线、首字母大写；fighter 特例为 Dweller。无中文反向词典，同名译文和重载均不改变查找。F3+T 后重开图鉴重建显示。

## 1.0.19 崩溃修复

2026-10-04 用户报告详情页点击返回崩溃。报告链为 `EntityDefinitionElement.lambda$new$0 → BestiaryScreen.selectGroup → EntityGroupElement → GroupUtils.getFilterByName`，错误资源位置为 `the_vault:集群怪物`。旧补丁只隔离列表入口，漏掉返回和带 predicate 的构造入口。

返回路径改用已持有的原始 ID；构造入口用 Class.isInstance 和受控方法引用区分族类，避免强转任意 predicate。标题仍可汉化，getFilterByName、ID、存档与筛选算法未改。不通过吞异常或中文反查掩盖问题。

目标签名、固定核心摘要和命中数见 `transformer/src/main/resources/patches/bestiary_groups.properties`。来源审计补充详情页、族类页和主界面；`python tools/source-audit.py bestiary_groups` 可复核。

测试执行真实变换后的列表 lambda 和详情返回回调，覆盖重复返回及不同 ID；运行侧检查原始键、Dweller、中文显示和非族类回退。还校验非目标指令还原、摘要/命中数失败、VP 冲突和两端过滤。未执行完整游戏 UI；返回、带族类直接打开和重载仍需游戏确认。
