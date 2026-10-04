# 项目 Wiki

VM 汉化组的 Vault Hunters 3rd Edition 汉化工程，包含游戏资源、配置译文、硬编码补丁与跨平台安装器。

## 版本与适配

| 项目 | 当前值 | 权威配置 |
|---|---|---|
| 汉化包项目 | **V2.7** | [installer.properties](../installer/resources/installer.properties) 的 `translation.version` |
| Vault Hunters 整合包 | **3.21.7** | 同文件 `pack.version` |
| Minecraft / Forge | 1.18.2 / 40.3.11 | [gradle.properties](../patch-mod/gradle.properties) |
| the_vault 核心 | 1.18.2-3.21.6.6884 | 安装器校验配置与 VTP 目标清单 |
| VTP 定向补丁 | **1.0.19**，单 JAR | `patch-mod/gradle.properties` 的 `mod_version` |
| VaultPatcher | **1.5.3-hotfix**（文件名 `1.5.3-fix`） | 安装器 `vp.*` |
| 安装器导出 | 从 **1** 起的整数序列号 | [export.properties](../installer/export.properties) |
| Java | 游戏/VTP 基线 JDK 17；安装器 Java 17+ | [构建说明](development.md) |

汉化包版本、VTP 版本和导出序列号各自独立。安装器不再维护单独的语义版本。

## 内容与分工

| 目录 | 内容和用途 |
|---|---|
| `translate-packs/` | 唯一安装输入；安装器完整内置后复制到整合包根目录 |
| `translate-packs/config/the_vault/` | 原模组配置、名称与描述；编辑时保留业务字段 |
| `translate-packs/config/openloader/` | 汉化资源包和进度数据包；ZIP 是有效安装输入 |
| `translate-packs/patchouli_books/` | 指南书内容 |
| `translate-packs/scripts/` | JEI 信息/配方及现有修正脚本 |
| `translate-packs/vaultpatcher/modules/` | VP 静态、动态和其他模组硬编码规则 |
| `translate-packs/config/vh3_translation_patch/` | VTP 的 14 份外部译表 |
| `translate-packs/mods/` | 当前 VTP 单 JAR，构建完成后自动导出 |
| `patch-mod/` | VTP 源码、目标指纹、测试与来源证据 |
| `installer/` | Java Swing 安装向导、文案、下载配置和一键构建 |
| `project-memory/` | Codex 当前状态、约束与历史来源 |

根目录图标及整合包信息也随基础内容原样安装。原始核心、旧译文、公告、参考材料、checker、工具和发布成品留在父工作区。

## 阅读入口

- [安装与使用](install.md)：玩家安装、升级和故障定位。
- [翻译维护](translation.md)：资源、VP 规则、VTP 译表的编辑位置。
- [VTP 模块](vtp.md)：14 个模块的作用、输入格式与运行端。
- [安装器维护](installer.md)：文案、版本、下载项、文件名与导出序列号。
- [开发与验证](development.md)：构建命令、源码职责和验收边界。
- [变更摘要](history.md)：关键迁移及历史证据。

当前完成了离线构建、字节码和加载层验证。真实客户端、专用服务器、F3+T 与全部界面仍需游戏验收，详见 [待办](../project-memory/TODO.md)。
