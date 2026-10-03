# Quest 名称显示

`quest_names.json` 保留原始英文标题为键，84 个唯一名称；逐条对照原始 quests.json 与 sky_quests.json 共 160 个任务记录，无未确认英文占位。ID、正文、任务进度、奖励与 locale 数据保持原样；不修改 Quest.getName。

四个方法/四个钩子：QuestButtonElement 悬浮、QuestOverviewElementScreen 标题、QuestState 完成时发送的通知标题、ClientboundToastMessage.toast 的标题。后者仅精确查表，沿用旧 VP 的标题范围，为只安装客户端补丁时保留通知翻译。服务端已有中文不会重复翻译；通知正文留原有 VP。

QuestState 入口两端启用，三个 UI/接收入口仅客户端；模块配置两端读取。客户端 F3+T 后重建标题，服务端重启；已发出的通知不会追溯改写。

来源清单：translations/quest-name-inputs.json，旧规则 translations/vp/quest_names.json。更新运行 `python tools/source-audit.py quest_names`，重新读取两份原始任务文件及目标类；审核新增/删除/改名后更新配置，不由 ID 猜测译文。

VTP 1.0.13；工程编译/测试按本轮任务延后统一执行，未进行游戏验收。
