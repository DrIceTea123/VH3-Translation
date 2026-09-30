# VH3 项目协作入口

本文件作用于本仓库及子目录。仓库为工作区下的 `[vh3-translation]/`，主工程为 `program/`，补丁工程为 `patch-mod/`。

## 开始工作

1. 每个新对话先读 `project-memory/README.md`，再读 `STATE.md`、`RULES.md`、`DECISIONS.md`、`TODO.md`，以及 README 清单之外的所有 notes。
2. 按任务查阅根 README 的文档索引。补丁架构见 `patch-mod/docs/modules.md`，文本获取见 `patch-mod/docs/text-capture.md`。版本、路径与实现状态须重新核对实际文件。
3. 历史 notes 只证明当时状态；最新明确用户要求优先。TODO 不构成额外任务授权。

## 必须遵守

- 不清晰的功能需求或需要决策的部分，先提问并说明原因。新内容放入合适位置。
- 只有用户明确要求时才提交 Git；开发、修复、初始化和整理均不自动授权提交。
- 原始输入保持原样。不得在文档、记忆或 Git 中记录用户提供的原始整合包实例路径。
- 具体工程规则统一维护于 `project-memory/RULES.md`，包括模块配置、VP 接管、版本递增和每轮 mod 修改后的双 JAR 导出要求。
- 区分实现、离线验证与游戏验收；不得将前两者写成已完成游戏验证。
- PowerShell 对含方括号的路径使用 `-LiteralPath`；保留既有安装脚本编码。

## 项目记忆

用户持续授权维护本仓库的 `project-memory/`，不涉及全局记忆。每轮有实质增量时，按 README 协议新增唯一命名的 note，记录来源、决定、验证和待办变化。

普通工作不批量改写快照；用户要求整理或归并时，先重读最新文件，保留其他对话的增量，更新快照及 README 已归并清单。历史 notes 保留溯源。记忆不可读写时如实说明，不声称已记录。
