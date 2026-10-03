# 特殊装备词缀句式

1.0.14 将完整显示组件交给 `gear_affixes`（29 个方法）和 `talent_affixes`（1 个方法）重排。技能和天赋的英文句式相同但中文类别不同，因此配置分离；两者共用 TemplateModule 的参数匹配与样式保留机制。所有配置都在 program 的 vh3_translation_patch 目录，不内置译表。

## 范围

- 技能冷却、魔力消耗、效果范围、技能等级与天赋等级。
- 搜刮回魔、搜刮/受击施放技能，奥术新星、蛛网新星、无序束缚中毒与霜冻新星易伤。
- Increased/Reduced 属性语序、横扫伤害、红心碎片、连击叠伤、第三次攻击、荆棘幸运一击、壁垒。
- getConfigDisplay 的完整范围预览也接管；搜刮回魔保留概率区间与回复量区间，不能将两者当成单个回复量。

只在 MutableComponent 返回处注入，空返回保持为空，不改变数值符号、公式、技能/天赋 ID、触发概率、序列化数据或存档。上游前缀作为参数保留；参数移动保留颜色、点击和悬浮样式。技能与宝箱名通常已由原工程配置翻译，模板保留传入名称；少量独立术语使用现有译名。CooldownPercent 上游始终生成 Lowers 语句，此处仅翻译，不自行修正上游算法。

## VP 与更新

旧片段保存在 translations/vp/gear_affixes.json 和 talent_affixes.json。原类级规则收窄到仍存在原文的非接管方法，主要是构造器固定标题和 serializeTextElements；这些序列化片段仍由 VP 负责。本模块接管显示组件，不重新定义上游 JSON 导出格式。

目标签名、摘要、返回数和端位于 transformer 的 patches 清单；原核心类、旧规则及散列列在 translations/source-inputs.json 与 sources/。运行 tools/source-audit.py gear_affixes talent_affixes 重新比较来源；源码更新时先审阅差异，不直接刷新指纹。

统一离线验证与游戏验收状态以 takeover-report.md 为准。
