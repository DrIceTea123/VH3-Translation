# Vault Hunters 3rd Edition 翻译工程

此目录是本地 Git 仓库。Minecraft 1.18.2、Forge 40.3.11；整合包 3.21.7，核心 mod `the_vault-1.18.2-3.21.6.6884.jar`。版本信息已经只读核实，未进行游戏运行验证。

## 目录

| 位置 | 用途 |
|---|---|
| `[PROGRAM]/` | 当前翻译配置、Patchouli 内容、OpenLoader 资源包、安装脚本与配套素材 |
| `docs/maintenance/` | 文本问题、测试笔记、上游沟通草稿；内容沿用原文件，不代表已解决或已发送 |
| `docs/release/` | 当前版本汉化说明书源文件 |
| `docs/workspace-entry/` | 仓库外工作区入口的版本管理副本 |
| `project-memory/` | 规则、已接受方案、状态快照与独立对话增量 |

历史公告、参考表格、版本对照表、`必要文件/` 参考素材、`origin-*`、`Translated-旧版/`、checker 工具和其他参考文件继续留在父目录，不属于本仓库。

## 编辑和打包

编辑从本仓库的 `[PROGRAM]/` 开始。PowerShell 访问含方括号路径时使用 `-LiteralPath`。现有文件保持原始字节、换行和编码；`bzlr.iss` 使用旧编码，不要未经确认将其整体转换为 UTF-8。

用 Inno Setup 打开 `[PROGRAM]/bzlr.iss`。脚本所需的 7-Zip 位于工作区根目录 `../7z/`；发布输出位于工作区根目录 `../[【发布文件】]/`。脚本中的两处相对路径已随目录迁移调整；当前尚未重新编译安装包。

OpenLoader 下的两份 ZIP 是安装输入，纳入 Git；不能将所有 ZIP 一并忽略。7-Zip 和上游 mod JAR 等第三方工具与依赖不入库。单独克隆本仓库不会自动获得父目录中的依赖或参考输入。

## Git 操作

在本目录运行 `git status`、`git diff`、`git log --oneline`；在工作区父目录可运行 `git -C vh3-translation status`。

本次建立本地 `main` 分支和初始提交，不配置远程仓库。后续修改可按功能提交；推送或发布的目标尚未指定。

## 项目记忆

从 `AGENTS.md` 和 `project-memory/README.md` 读取。早期快照描述的是迁移前布局，必须同时读取未归并 notes；Git 迁移记录提供新的路径与状态。新的规则、结论和 TODO 变化写入本仓库 `project-memory/notes/`。
