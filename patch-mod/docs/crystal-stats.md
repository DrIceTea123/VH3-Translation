# 水晶统计与主题名称

`crystal_stats.json` 接管结算水晶统计标签、目标、房间分类、地牢难度/怪物类型和神明声望；String 参数在统计 builder 或 TextComponent 构造处翻译。业务分类、Map 键和统计数值不修改。包含旧 main/complex/dynamic_2 的相关规则，中文句式用 `{0}` 参数重排，不保留 VP 的 `@` 标记。

`theme_names.json` 是独立主题名称词表，以原始英文名称为键。原始来源 `origin-3.21.7/config/the_vault/gen/themes.json`；初始译文逐 ID 提取自汉化工程的同名文件，再将工程注册表的 name 字段恢复原英文，其他字段不动。名称翻译只保留在 VTP 配置。系列名/背景描述继续读取已有 theme_augment_lore 配置。

主题显示覆盖结算、水晶物品描述、主题强化物品描述和服务端入场消息悬浮。`ThemeKey.getName()` 本身与 `ThemeEntityRegistry.buildEntityToThemesMap` 保持原逻辑；不将译名写入内部索引。主题模块两端初始化，统计模块仅客户端。未配置使用原文，首次配置失败阻止启动，F3+T 失败保留旧值；服务端重启重载。

统计模块 4 方法/45 钩子，主题模块 7 方法/8 钩子。精确签名/摘要/端信息见各自 properties。共享 TemplateModule 处理完整句式和组件样式，卡牌模块复用同一实现且继续使用独立配置。

文本来源及更新：`translations/source-inputs.json` 定义输入，`translations/sources/` 保存文本/类摘要基线；运行 `python tools/source-audit.py crystal_stats theme_names` 重新读取并报告新增、删除及变化。审核新版 JAR 的调用路径和配置后才能用 `--record` 更新基线，不因摘要变化自动放行；工具不覆盖译文。旧 VP 原始组留在 `translations/vp/crystal_stats.json`。

七部分完成后已通过统一构建和离线验证，详见 [成果报告](../../wiki/history.md)。未做游戏验收。

## 1.0.17 虚空坩埚漏项

Andersite Caves、Easter、Raid Vault Orcish 等译文一直存在于 theme_names.json。遗漏的是 `VoidCrucibleScreen$ThemeSelect.getThemes` 和 `initializeViewTheme`：两者通过 `NamedKey::getName` 的 invokedynamic 方法引用取名，原来仅拦截直接 `ThemeKey.getName` 调用的补丁没有覆盖它们。

现仅在这两个显示方法中包装名称 Function，列表（含排序）和详情标题使用同一译表。ID 键、选择回调、黑名单包、ThemeKey/NamedKey getter 和注册表不变；自定义房间显示名不经该 mapper，继续使用已有配置。核对工程 gen/themes 的所有名称均有 VTP 配置，不需要再复制或修改主题译表。

F3+T 后重新打开坩埚界面以重建缓存列表/标题。新增测试执行真实方法引用及其注入包装，并验证名称 getter 保持原值、译表重载和未知名称回退；完整游戏界面验收仍待进行。
