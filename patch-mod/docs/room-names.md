# 地图房间名称

`room_names.json` 用原始英文显示文本查表；在 `VaultMapRenderHelper.getTooltipText(ResourceLocation, boolean)` 三个 String 返回点接管。上游提取末段、分类、去掉挑战/终焉房间数字后缀等逻辑保持原样，普通/详细显示均覆盖。详细模式使用 `Common Room ({0})` 和 `Raw Room ({0})` 句式，内层名称独立查表。

调用方是结算 `VaultMapElement.onHoverTooltip` 和游戏内 `IngameVaultMapElement.onHoverTooltip`。二者把 mapData 中每个 Room 的 roomId 交给同一 formatter，显示方法没有排除 dungeon_rooms。如果此 ID 进入地图，模块同样接管其格式化结果；这不表示地牢一定被上游记录到地图，游戏显示仍待验收。正式数据清单包含 `vault/rooms/`、`vault/dungeon_rooms/` 以及起始房间 `vault/starts/`。

数据来源：`origin-3.21.7/config/the_vault/gen/templates.json`。`translations/room-name-inputs.json` 保存 ID 与普通/详细显示键的对应，当前 139 个 ID。初始配置 131 项，其中 43 项缺乏已确认译文，按用户要求保留英文。旧 VP 两组保存于 `translations/vp/room_names.json`，原位置留接管注释。

更新时运行 `python tools/source-audit.py room_names`，查看模板 ID 和目标 class 变化，再重新审查格式化算法、生成输入对应表和补充译名；不要只更新摘要。配置只在客户端读取，F3+T 后新悬浮文本使用新词表，缺项保留原文。工程编译和测试按本轮计划延后统一执行，游戏未验收。
