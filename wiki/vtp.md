# VTP 模块与译表

VTP 在已审查的显示入口翻译，保留上游算法、业务 ID、存档键和数值。当前 14 个模块；配置唯一位置为 [translate-packs/config/vh3_translation_patch](../translate-packs/config/vh3_translation_patch/)。源码和 JAR 不保存译文副本。

| 模块 | 内容 / 键 | 运行端与细节 |
|---|---|---|
| `mob_names` | 结算与经验怪物名；实体 ID 冒号后的路径 | 客户端；[文本获取](../patch-mod/docs/text-capture.md)、[经验入口](../patch-mod/docs/vault-xp.md) |
| `sound_names` | 声音设置名；ModSounds 原始字段名 | 客户端；[文本获取](../patch-mod/docs/text-capture.md) |
| `research_names` | 原始英文研究名、限制和解锁提示 | 两端；[研究](../patch-mod/docs/research-names.md) |
| `chest_names` | 宝箱/桶、稀有度、经验与预览 | 两端，UI 入口限客户端；[宝箱](../patch-mod/docs/chest-names.md) |
| `card_text` | 卡牌名称、条件、效果与物品提示 | 两端，UI 入口限客户端；[卡牌](../patch-mod/docs/card-text.md) |
| `crystal_stats` | 水晶结算统计句式 | 客户端；[水晶统计](../patch-mod/docs/crystal-stats.md) |
| `theme_names` | 原始英文主题名；结算、物品与虚空坩埚 | 两端；[主题](../patch-mod/docs/crystal-stats.md) |
| `room_names` | 普通/详细地图房间名称 | 客户端；[地图](../patch-mod/docs/room-names.md) |
| `overworld_names` | 主世界铭文预览名称 | 客户端；[铭文](../patch-mod/docs/overworld-names.md) |
| `bestiary_groups` | 图鉴族类显示名，查找仍使用原始 ID | 客户端；[图鉴](../patch-mod/docs/bestiary-groups.md) |
| `gear_rarity` | 装备稀有度与池名称，不含宝箱/悬赏 | 两端；[稀有度](../patch-mod/docs/gear-rarity.md) |
| `quest_names` | 原始 Quest 标题、完成提示 | 两端；[任务](../patch-mod/docs/quest-names.md) |
| `gear_affixes` | 完整装备词缀句式与样式 | 两端；[词缀](../patch-mod/docs/gear-affixes.md) |
| `talent_affixes` | 天赋等级句式，与技能等级分开 | 两端；[词缀](../patch-mod/docs/gear-affixes.md) |

## 配置约定

每个 `<模块ID>.json` 是 UTF-8 平面字符串对象。禁止注释、重复键、非字符串值及尾随内容；`{}` 不覆盖，空字符串是显式空译文。新增既有模块条目只需改 JSON。

默认用原始英文精确查表，不跨语言、不改全局语言键。怪物使用实体路径，例如 `aggressive_cow`；声音使用原始字段，例如 `RAFFLE_SFX`。其他模块的特殊格式以对应文档为准。

句式中的 `{0}` 等参数需完整保留；源参数从 0 连续编号、不可重复或相邻、最多 8 个。允许重排参数，保留组件颜色、悬浮与点击信息。完整精确键优先于句式及子组件处理。

宝箱完整名优先，未命中再组合稀有度与基础名；不能从已汉化 getter 反推英文键。怪物未知 ID 不使用注册表默认实体；分组、宠物外观和玩家皮肤名不混入实体名称表。

经验入口已经拆到 `mob_names`、`chest_names` 与 VP，不再读取 `combat_stats.json` 或 `vault_xp.json`。个人旧项按真实 ID/现行键格式迁入；不能按英文猜实体 ID。

未确认房间名继续保留英文，见 [名称清单](../patch-mod/docs/unconfirmed-names.md)。重载和安装行为见 [安装与使用](install.md)，新增模块见 [架构](../patch-mod/docs/modules.md)。
