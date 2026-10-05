# 翻译维护

资源和配置在 `translate-packs/`。普通资源键编辑 OpenLoader 包，模组配置编辑 `config/the_vault/`，指南书编辑 `patchouli_books/`。保留业务 ID、结构、数值及脚本语义。VTP 领域译文见 [模块索引](vtp.md)，其他硬编码按下文维护 VP。

## VP 配置格式与接管

当前安装目标为 **VaultPatcher 1.5.3-hotfix**（官方文件名 `vaultpatcher-all-1.5.3-fix.jar`），VTP 为 1.0.20。详细来源、验证与升级步骤见 [格式升级报告](../patch-mod/docs/vp-1.5.3-upgrade.md)。

## 当前布局

相对于 `translate-packs/`（安装时复制到游戏根目录）：

- `config/vaultpatcher_asm/config.json`：总配置，`modules` 明确列出四个模块，`load_all_modules=false`。
- `vaultpatcher/modules/the_vault-asm_main.json`：主要 ASM 规则。
- `vaultpatcher/modules/the_vault-asm_ulti.json`：用户现有独立长表布局。
- `vaultpatcher/modules/other_mods.json`：其他模组规则。
- `vaultpatcher/modules/the_vault-dynamic.json`：动态替换规则。

先前 main/long/complex 分类是历史布局，已被用户调整；维护时保持当前 main/ulti 分类和顺序，不再套用旧重排规则。接管注释全部保留，历史被接管规则仍在 `patch-mod/translations/vp/`，只供导入和回归，不能复制回游戏。

## 条目格式

```json
{
    "target_class": ["iskallia.vault.client.data.ClientVaultXpTracker"],
    "info": {"method": "handleDeltas", "local": "MformatOreName"},
    "pairs": [{"key": "Example", "value": "示例"}]
}
```

模块数组第一项仍为元数据，明确 `dynamic` 和 `i18n=false`。`target_class` 使用类名字符串数组；`method/local/ordinal` 放入 `info`；单条翻译也放入 `pairs` 数组。原多个目标若有独立定位条件，拆成单目标组，避免共用 `info` 后扩大范围。动态多类同样拆分，因为新版动态读取器只保留最后一个类名。通配动态项保持空类名数组。

截至 2026-10-04，按拆分后的有效目标统计（注释不算规则）：

| 文件 | 规则组 | 译文对 | local 组 | ordinal 组 |
|---|---:|---:|---:|---:|
| asm_main | 654 | 2819 | 36 | 1 |
| asm_ulti | 22 | 1241 | 3 | 0 |
| other_mods | 35 | 86 | 1 | 0 |
| dynamic | 4 | 20 | 0 | 0 |
| 合计 | 715 | 4166 | 40 | 1 |

旧文件按未展开多目标组统计是 657 组 / 3915 对；展开后与新文件的 715 组 / 4166 对逐项一致。数量增长来自目标拆分复用 pairs，没有新增译文。

## 维护边界

普通界面标签、矿石提示与其余硬编码由 VP 负责；14 个 VTP 模块的显示接管边界见 [补丁 README](../patch-mod/README.md)。例如 Tracker 的矿石调用结果与四个固定标签仍由 VP 处理；装备属性序列化保留的名称片段与 VTP 显示钩子分开。

新接管应同步更新冲突判定、外部译表和原位注释。新旧格式可用于历史导入；正式发布只使用上述新目录和新格式。运行预检只检查新版 VP 实际启用的文件，详见 [兼容预检](../patch-mod/docs/vp-compatibility.md)。更新配置后只清理 `vaultpatcher/cache/`，不能删除整个 `vaultpatcher/`。
