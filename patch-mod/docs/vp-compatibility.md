# VP 冲突预检范围

VTP 1.0.18 对齐官方 VaultPatcher **1.5.3-hotfix**。1.0.15 曾修复“扫描所有旧文件而误报冲突”；本轮在此基础上同步新版实际加载路径和条目格式。

## 启用文件

总配置仍是 `config/vaultpatcher_asm/config.json`，行为依据官方发布 JAR 的 `VaultPatcherConfig.readConfig`、`VaultPatcher._init` 和 `VaultPatcherModule.read`：

- 使用 `modules` 列表；旧 `mods` / `m` 已不被新版识别，VTP 也忽略它们。重复 `modules` 最后列表生效。
- 默认 `load_all_modules=false`。每个名称追加 `.json`，从游戏根目录 `vaultpatcher/modules/` 读取；支持显式子目录，不解释名称通配符或移除已有后缀。
- 选中的新文件不存在时，VP 会迁移旧配置目录中相同**文件名**的模块。VTP 只读检查这个回退文件，不执行移动；新文件存在时旧文件不参与检查。
- `load_all_modules=true` 时，按 VP 的 `DirectoryStream("*.json")` 检查新模块目录第一层，不递归，不追加显式列表或旧目录松散文件。
- 总配置缺失或默认空列表时不扫描松散文件；选中的模块两处都不存在时不创建文件，VP 自行生成空模板。
- 已启用的冲突和损坏文件仍阻止启动；未启用的备份或坏文件不参与预检。

## 条目识别

新版 `target_class` / `target_classes` / `t` / `s` 类名数组和 `info` / `i` 被转换为 VTP 模块既有判定模型。支持 `m/l/o` 定位别名和 `p/k/v` 翻译别名；累加类名与 pairs，info 字段按读取顺序覆盖。空 method/local 按 VP 的空限制处理，多类组不能绕过冲突检测。旧对象格式仍支持，供旧目录迁移与历史导入使用。

判定不修改输入。兼容副本仍保留原格式和未接管内容，多目标混合组拒绝不安全的整组删除。正式输入已移除接管规则，`prepare-vp` 输出应无条目删除，生成位置改为 `vaultpatcher/modules/`。

## 验证

`VpEnabledModulesTest` 覆盖显式列表、旧别名忽略、子目录、空配置、损坏文件、缺失模块、load-all 非递归及新文件优先/旧文件回退；配置列表和顺序用官方 JAR 的读取器对照。

`Vp153FormatTest` 逐组调用官方发布解析器，核对类名、定位条件和全部 pairs，覆盖动态局部替换；比较全部模块的新旧格式与多类输入冲突判定；真实核心的 41 个共享类先经过 VTP 再经过 VP，并对写出的全部方法执行 ASM 分析。`XpVpRulesTest` 继续核实矿石调用后的钩子及四个固定标签。

这是离线验证，未执行 Minecraft 客户端/服务端生命周期、F3+T 或整合包游戏验收。外部 VP 插件在加载期间任意改写规则不属于静态预检范围。
