# VH3 项目协作入口

本文件作用于本仓库及子目录。安装内容在 `translate-packs/`，补丁在 `patch-mod/`，安装器在 `installer/`。

1. 新对话依次读取 `project-memory/README.md`、`STATE.md`、`RULES.md`、`DECISIONS.md`、`TODO.md`，以及 README 已归并清单之外的全部 notes。
2. 当前目录、版本和文档从 [Wiki](wiki/README.md) 查阅，并核对实际文件；历史记录只说明当时状态。
3. 需求不清或需要用户决策时先提问并说明原因。TODO 不构成额外任务授权；仅明确要求时提交 Git。
4. 原始输入不改；不记录用户提供的整合包实例路径。PowerShell 使用 `-LiteralPath` 访问含方括号路径。
5. 模块、配置、版本及单 JAR 导出规则见 [RULES](project-memory/RULES.md)。区分实现、离线验证和游戏验收。

用户持续授权维护本项目共享记忆，普通工作新增唯一 note；整理归并前重读最新文件，保留并发增量与历史 notes。不修改全局记忆，不声称聊天全文会自动共享。
