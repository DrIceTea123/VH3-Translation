# Vault Hunters 3rd Edition 翻译工程

当前工程包含 VP 翻译、资源包、安装素材和独立补丁 VTP。运行基线与验证状态统一见 [项目状态](project-memory/STATE.md)；尚未完成整合包游戏联测。

## 从哪里开始

| 任务 | 入口 |
|---|---|
| 修改既有翻译、制作汉化包 | `program/`；打包方式见下文 |
| 编辑 VTP 怪物名、宝箱名、声音名、研究名 | [补丁使用与构建](patch-mod/README.md) |
| 新增补丁模块、修改 ASM | [模块开发](patch-mod/docs/modules.md) |
| 理解原始文本与注入位置 | [文本获取分析](patch-mod/docs/text-capture.md)、[研究名覆盖](patch-mod/docs/research-names.md)、[经验提示](patch-mod/docs/vault-xp.md)、[宝箱名称模块](patch-mod/docs/chest-names.md) |
| 维护 VP main / long / complex | [VP 分类规则](docs/maintenance/vp-asm-layout.md) |
| 项目规则、决策与待办 | [共享记忆](project-memory/README.md)；协作从 [AGENTS.md](AGENTS.md) 开始 |
| 同步父目录入口 | [工作区入口模板](docs/workspace-entry/README.md) |

玩家汉化说明书 DOCX 位于 `program/`；该目录下的许可证、安装前提示、覆盖版说明是独立发布入口，不能按重复维护文档删除。

## 编辑与打包

- 当前安装输入在 `program/`。其中 `基础+硬编码汉化/config/vh3_translation_patch/` 是 VTP 运行配置的唯一维护位置，`mods/` 放配套 JAR。
- 使用 Inno Setup 打开 `program/package.iss`；保持原编码。脚本从父工作区 `必要文件/iss-plugin-7z/` 读取工具，输出到 `[发布文件]/`。尚未重新编译安装包。
- OpenLoader 的两个 ZIP 和工程 mods 中导出的两个 VTP 配套 JAR 是必需安装输入，受 Git 管理。其他第三方 JAR、7-Zip、构建缓存和发布成品不入库。克隆仓库不会自动获得父目录依赖。
- 发布前按 [待办](project-memory/TODO.md) 完成整合包联测，并同步玩家说明；现有安装提示和说明书不能当作 VTP 已验收的证据。

原始输入、旧译文、历史公告、参考表格、checker、工具和成品留在父工作区。日常从本仓库运行 `git status`、`git diff`；仅在用户明确要求时提交 Git。
