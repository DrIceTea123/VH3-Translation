# 主世界铭文预览名称

`overworld_names.json` 接管 `StructurePreviewElement.renderPreview` 内 `getDisplayName()` 的显示调用结果，在测量文字宽度、截断和绘制之前翻译。只对命名空间 the_vault、路径以 overworld/ 开头的结构生效；模板 ID、原 getter、结构读取及其他结构预览不变。

真实输入在 `origin-3.21.7/config/the_vault/gen/templates.json`（基线不存在根级 templates.json）。取 ID 最后一级，将下划线换为空格，只将首字符转大写；不是每个词都转大写。原 VP 的 8 个多词名称大小写不匹配，已为实际输出新增对应键并沿用已确认译文；没有新增猜测译名。38 个源 ID，46 个配置键，无英文占位。

旧规则保存在 translations/vp/overworld_names.json，ID/显示键清单在 translations/overworld-name-inputs.json。更新运行 `python tools/source-audit.py overworld_names`，核对新增/删除/变化后人工更新输入清单与配置；不自动重写译文。模块仅客户端、F3+T 重载、缺项原文回退。

版本 1.0.10；1 个目标方法/1 个钩子。统一构建与离线验证已通过；VP 的 Loading/Unknown/Not Found 状态提示继续保留。详见 [成果报告](../../wiki/history.md)，尚未游戏验收。
