# 当前状态

归并日期：2026-10-04。已归并来源见 [README](README.md)，后续增量以未归并 notes 为准。

- 汉化包 **V2.7**；VH3 **3.21.7**；MC 1.18.2 / Forge 40.3.11 / JDK 17；the_vault 1.18.2-3.21.6.6884。配置权威入口见 [Wiki](../wiki/README.md)。
- 安装输入已迁到 `translate-packs/`；`patch-mod/` 构建导出与 `installer/` 打包均已同步。旧 program 路径失效。
- VTP **1.0.19**，14 个模块、单 JAR 分发；runtime/transformer 和 SERVICE/GAME 仍分层。配置全部外置；客户端 14、专用服务端 8 模块。
- VP **1.5.3-hotfix**，总配置使用 modules，规则在 vaultpatcher/modules；只检查实际启用文件，保留 main/ulti/other_mods/dynamic 布局。
- 图鉴返回和带族类的构造入口改用原始 ID 查找，显示中文不再参与资源 ID 解析。实际游戏复测仍待用户确认。
- 安装器为 Java 17+ Swing；基础内容完整内置，四个模组可选择下载。“必装”只控制默认勾选和标注；取消 VP 仍清理已知错误版本、保留目标版本。
- 安装器语义版本已移除，改为从 1 开始的整数导出序列；汉化包版本和三占位符名称在 installer.properties，成功导出才递增。强制安装有两次确认。
- Wiki 是项目介绍和维护索引；记忆仅维护当前事实、约束、决策与待办。旧 notes、原始来源和验证证据保留。

本轮离线构建与安装器验证结果见最新 note。**没有完整游戏验收**：真实加载生命周期、VP/其他转换器共存、图鉴与其他界面、F3+T、真实专用服务器、Linux/macOS 安装器仍需验证。不能把探针或离屏 UI 当作游戏/真机验收。
