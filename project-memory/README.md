# 项目共享记忆

唯一维护位置为本仓库 `project-memory/`。用户持续授权记录本项目的规则、决定、事实、验证和待办；此授权不延伸到全局记忆，也不授权自动执行 TODO。

## 读取与分工

每个新对话依次读取 [STATE](STATE.md)、[RULES](RULES.md)、[DECISIONS](DECISIONS.md)、[TODO](TODO.md)，再读取下方清单以外的全部 notes。不能只看入口或按修改时间猜测哪些记录已归并。

| 文档 | 唯一职责 |
|---|---|
| STATE | 当前实现、基线和验证边界 |
| RULES | 持续有效的用户约束与维护规则 |
| DECISIONS | 方案选择及理由 |
| TODO | 剩余工作与验收状态 |
| notes | 各轮来源与证据，保留当时状态 |

技术细节不在记忆中重复维护：使用与构建见 [补丁 README](../patch-mod/README.md)，架构见 [模块开发](../patch-mod/docs/modules.md)，原始文本见 [文本获取](../patch-mod/docs/text-capture.md)，VP 分类见 [分类文档](../docs/maintenance/vp-asm-layout.md)。旧 PATCH-PLAN 已归并到模块开发、DECISIONS 与 TODO。

## 记录与归并

有实质增量时，新建 `notes/YYYY-MM-DDTHH-mm-ss-主题-唯一后缀.md`。记录日期（Asia/Shanghai）、来源对话 ID 或需求摘要、类型、状态、关联 ID、决定/事实、验证与限制、对旧记录及 TODO 的影响；不得编造来源 ID。

普通对话只追加自己的 note。归并前重读最新文件，保留并发增量，更新快照及本清单；历史 notes 不删除、不重写成当前状态。明确区分用户要求、建议、推测、离线验证和游戏验收；最新明确指令优先，冲突无法判断时提问。

共享文件不等于自动共享聊天全文或实时通知；其他对话需重新读取才能获知变化。不可读写时如实说明，不声称已记住。

## 已归并清单

快照基线：2026-10-01。以下 10 份记录已归并，日常无需全部重读；需要历史依据时再打开。新加入而不在此清单的记录仍必须读取。

- [2026-09-30T11-10-00-initial-handoff-18925497af78.md](notes/2026-09-30T11-10-00-initial-handoff-18925497af78.md)
- [2026-09-30T11-53-11-git-preparation-d8b74750.md](notes/2026-09-30T11-53-11-git-preparation-d8b74750.md)
- [2026-09-30T11-57-58-git-layout-d8b74750.md](notes/2026-09-30T11-57-58-git-layout-d8b74750.md)
- [2026-09-30T12-09-43-patch-start-d8b74750.md](notes/2026-09-30T12-09-43-patch-start-d8b74750.md)
- [2026-09-30T16-04-32-patch-foundation-d8b74750.md](notes/2026-09-30T16-04-32-patch-foundation-d8b74750.md)
- [2026-09-30T16-30-28-vp-asm-split-d8b74750.md](notes/2026-09-30T16-30-28-vp-asm-split-d8b74750.md)
- [2026-09-30T21-38-18-patch-modules-d8b74750.md](notes/2026-09-30T21-38-18-patch-modules-d8b74750.md)
- [2026-09-30T22-02-57-version-export-vp-d8b74750.md](notes/2026-09-30T22-02-57-version-export-vp-d8b74750.md)
- [2026-10-01T03-16-09-sound-module-d8b74750.md](notes/2026-10-01T03-16-09-sound-module-d8b74750.md)
- [2026-10-01T03-55-00-external-module-config-a93f418c.md](notes/2026-10-01T03-55-00-external-module-config-a93f418c.md)

首次方案来源：`01a0f032-400a-7990-93fc-18925497af78`。旧记录可能使用历史路径及已取代的方案，当前布局与规则以快照和更新增量为准。
