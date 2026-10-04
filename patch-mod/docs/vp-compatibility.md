# VP 冲突预检范围

1.0.15 修复：VTP 曾递归扫描 vaultpatcher_asm 目录内所有 JSON，把未启用旧文件、备份甚至无关 JSON 也当成补丁，从而误报冲突或解析错误。

现在先读取 config/vaultpatcher_asm/config.json，仅检查 mods 列表选中的文件。行为依据本工作区参考 JAR vaultpatcher-all-1.4.4+1 的 VaultPatcherConfig.readConfig、VaultPatcher._init 和 VaultPatcherModule.read 字节码：

- 支持 mods 的别名 m；若出现多次，最后出现的列表生效。
- 每项按 VP 的行为直接追加 `.json`，支持显式子目录；不递归搜索、不解释通配符、不自动移除已有后缀。
- 配置缺失、无 mods/m 字段或列表为空时，没有启用文件，松散 JSON 不参与检查。
- 启用文件尚不存在时不检查，也不由 VTP 创建；VP 自身会生成空模块模板。
- 启用文件中的冲突仍阻止启动；加载列表或启用文件损坏仍报告错误。未启用文件即使含旧冲突或损坏内容也不影响预检。

正式 VP 配置及用户译文不改动。旧测试改为读取当前启用列表，构建兼容副本的输入改为现有 asm_main，避免引用用户已删除的 asm_complex。特殊词缀语序测试使用独立夹具；正式外置译表继续做语义合法性校验，用户修改译文无需修改测试预期。

新增 VpEnabledModulesTest 共 8 项，覆盖启用切换、显式子目录、空/缺失配置、m 别名及先后顺序、追加后缀、启用文件损坏、缺失模块和配置元数据；别名和顺序用 VP 原配置读取器对照。完整构建通过后导出 1.0.15 配套双 JAR；游戏启动验收仍需另行执行。
