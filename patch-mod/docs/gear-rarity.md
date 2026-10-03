# 装备稀有度与稀有度池

`gear_rarity.json` 以原始英文显示值为键，包含等级名、Scrappy+ 等池名及 Royale_Poor / Royale Poor 等原始写法。初始 21 条全部来自现行 VP；不会按文字 Common/Rare 等在所有界面全局替换。

覆盖 11 方法/11 钩子：VaultGearRarity.getDisplayName、装备未鉴定 tooltip 的 RollType 名称、VaultCharmItem 护符的池名称、商店台装备名称、锻造熟练度池名、幻化筛选的六个等级显示。枚举显示 getter 两端启用，其他入口仅客户端。护符带使用次数，但其稀有度复用 VaultGearRarity，属于本模块；悬赏池、地牢门、宝箱、传奇珍宝自身等级不接管。JewelItem/BasicScavengerItem 由枚举名拼接语言键的逻辑保留，继续由语言资源翻译。

VaultGearTooltipItem 的问题规避方式：仅在 lambda$addTooltipRarity$12 调用 RollType.getName 之后插入显示翻译；不修改 RollType、配置键、VaultGearRarity.name/valueOf/getColor。颜色仍由原 RollType.getColor 提供。原 VP 动态临时规则已移出。本轮尚不能声称查明此前崩溃根因或完成游戏修复验收，统一离线验证已检查真实方法的 ASM 栈分析与颜色逻辑未变，尚未在游戏加载器内执行该 tooltip。

旧规则保存在 translations/vp/gear_rarity.json；混合幻化组只移走稀有度项，All 保留 VP。其他同类固定提示保持原状。更新运行 `python tools/source-audit.py gear_rarity`，读取原始 JAR 与 gear/gear_roll_type.json 重新比较，再审查新增调用点与池名。

模块配置外置，两端首次加载失败阻止启动；客户端 F3+T、服务端重启。1.0.12 实施，1.0.14 统一构建和离线验证通过，详见 [成果报告](takeover-report.md)；未进行游戏验收。
