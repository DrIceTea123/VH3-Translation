# 图鉴族类显示与查找分离

`bestiary_groups.json` 有 14 个英文族类名称键，沿用旧译文。四个方法、五处修改：

- GroupUtils.getEntityGroupNames 的两个 stream mapper 改为 PartialEntityGroup.getId → 独立 lookupName；结果仍是原文查找名称，fighter 特例仍为 Dweller。不再通过 Component 的已翻译文字生成查找键。基线 loadEntityGroups 只向 ENTITY_GROUPS 放入 PartialEntityGroup。
- GroupListElement 只在 TextComponent 构造时翻译标签；原 groupName 仍用于 getFilterByName 和点击回调。
- TextUtil.formatLocationPathAsProperNoun 在组件构造时翻译族类，GroupUtils.getEntityName 的 Dweller 特例同样翻译。这保留原来悬赏等显示调用的族类中文；非族类悬赏种类/维度键仍由 VP 翻译。

EntityGroupElement 继续用原文 groupName 查找，标题从 getEntityName 获取显示组件。旧 GroupListElement / EntityGroupElement 反向翻译组已删除；不会把任何中文字符串交给 ResourceLocation 反解析。没有反向词典，同名译文也不影响查找。未知族类显示回退原文，实体名称分支、隐藏组检查和 ID/存档不变。

此模块仅客户端。F3+T 重载词表，重开图鉴重建标签。来源为真实 GroupUtils、TextUtil、两处图鉴类与 translations/vp/bestiary_groups.json；执行 `python tools/source-audit.py bestiary_groups` 重新比对来源。核心变化须重新核对 ENTITY_GROUPS 的键类型及 formatter；不能仅更新摘要。

VTP 1.0.11，按任务指令暂未运行工程编译和测试，统一验证时需特别执行 LambdaMetafactory 链路与中文同名回归；尚未游戏验收。
