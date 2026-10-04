# VaultPatcher 1.5.3-hotfix 格式升级

2026-10-04。按用户指定目标完成 VP 配置迁移，VTP 配套适配推进至 **1.0.18**。保留当前译文、main/ulti 分类、模块顺序与 72 条注释；没有继续执行后续文档整并任务。

## 发布依据

[官方 Modrinth 1.5.3-hotfix](https://modrinth.com/mod/vault-patcher/version/oATpDq2Q) 是核验时最新版本，实际文件名为 `vaultpatcher-all-1.5.3-fix.jar`，197444 字节。SHA-256：

`d79668dd66f3edeffa347917ba10ff7930c9d09788aa86528d4af6514fc6af52`

下载同时与官方 API 的 SHA-512 一致。查阅 [官方源码](https://github.com/3093FengMing/VaultPatcher)（本轮取得 master `affc2d99c62ed6f5f7d152f0a2f4bde9495a93f4`）辅助定位，最终以**发布 JAR 的字节码和真实解析器/转换器测试**为准，不把 master 当作发布版本。

## 配置迁移

总配置保持旧路径，`mods` 改为 `modules`，明确 `load_all_modules=false`、`default_language=zh_cn`。四个模块移动至游戏根目录 `vaultpatcher/modules/`；元数据保留并明确 `i18n=false`，译文继续作为文字值读取，不转为语言键。

所有目标改为字符串数组，定位信息独立放入 `info`；顶层 key/value 改为 pairs。原多目标拆分后，715 组有效目标 / 4166 对翻译逐项一致，详见 [分类与统计](../../docs/maintenance/vp-asm-layout.md) 和 [机器可读迁移记录](evidence/vp-1.5.3-migration.json)。`local` 字母、方法名、ordinal、Unicode 控制字符及动态 `@` 标记均保留。

旧动态条目 `\'\'I got a rock\'\'` 使用 Gson 可接受但严格 JSON 不合法的单引号转义，现改为同一字符串内容 `''I got a rock''`。`hide_pairs` 改为新版 `pairs_hide_limit`；移除新版不读取的 `output_format` / `missing_warn`。缓存、调试开关等受支持值保留。

历史 `patch-mod/translations/vp/` 文件不是运行配置，继续保留旧格式用于追溯。迁移工具 [upgrade-1.5.3.py](../../tools/vp/upgrade-1.5.3.py) 只读旧输入、输出到独立游戏目录，并断言展开后的目标/选择器/pairs 顺序与内容完全一致；本轮旧输入来源 Git `b0ed6e7`。

## 安装与构建

- 安装端更新 VP 下载 URL、实际文件名及下载 SHA-256，清理已知旧 VP JAR；VTP 输出更新为单 JAR 1.0.18。
- 只清理 `vaultpatcher/cache/`，保留模块、插件等其他子目录。覆盖版客户端和服务端说明同步，不能按旧说明删除整个 `vaultpatcher/`。
- 同步安装总配置、新模块目录与 VTP JAR；不能只替换其中一项。安装目录原有自定义规则请先备份并审阅合并，不能把已由 VTP 接管的旧规则加回启用列表。
- 测试参考 JAR 保存在 Git 忽略的 `local-deps/vp-1.5.3/`，运行仓库根的 `tools/vp/fetch-reference.ps1` 获取并验证；Gradle 在测试编译前再次检查固定 SHA-256。不将第三方 JAR 打入 VTP 或 Git。
- VTP 的启用文件与条目预检详见 [兼容预检](vp-compatibility.md)。

## 验证边界

完整离线 build、222 项 Java 测试、109 个 VTP 目标方法/242 处修改的核心哈希与字节码检查、单包内外版本与哈希、实际 VP 解析与 41 个共享类连续变换通过。另运行 Forge/ModLauncher 单包加载探针，确认客户端和服务端预检可读取新配置目录。

未启动 Minecraft、未构建或执行安装器。真实界面显示、完整第三方转换器链、客户端/服务端联测与 F3+T 仍待游戏验收。
