# 安装器维护

Java 17 Swing 向导，无外部构建依赖。生成单个可执行 JAR，完整内置 `translate-packs/`、配置和说明；运行时不读取旁置覆盖文件。

## 唯一编辑位置

| 内容 | 位置 |
|---|---|
| 安装前说明正文 | [notice.txt](../installer/resources/notice.txt)，纯文本，保留原发布文案 |
| 页面、校验错误、风险确认、进度和按钮文字 | [ui.properties](../installer/resources/ui.properties) |
| 汉化包/整合包版本、核心文件及哈希 | [installer.properties](../installer/resources/installer.properties)，`translation.version` / `pack.*` / `vault.*` |
| 导出文件名 | 同文件 `export.filename` |
| 模组名称、直链、文件名、哈希和默认必装标注 | 同文件 `mods` 及各模组分组 |
| 下一次导出序列号 | [export.properties](../installer/export.properties)，`next.serial` |
| 全部基础文件与 VTP | [translate-packs](../translate-packs/)，打包时读取，不另存副本 |

properties 使用 UTF-8，`\n` 写作反斜杠加 n 表示换行。界面支持 `{translationVersion}`、`{exportSerial}`、`{modpackVersion}`（或 `{pack}`）、`{vault}`；特定提示还支持 `{path}`、`{count}`、`{filename}`、`{problems}` 等，见原键值。操作系统/网络异常继续保留详细诊断。

## 文件名与导出

当前模板：

```text
宝藏猎人3汉化安装器-{modpackVersion}-VM汉化组-V{translationVersion}-{exportSerial}.jar
```

首次新制导出为 `宝藏猎人3汉化安装器-3.21.7-VM汉化组-V2.7-1.jar`，输出至父工作区 `[发布文件]/`。`.jar` 后缀不可省略；未知占位符、路径分隔符和非法文件名会被拒绝。

序列号是从 1 开始的正整数。成功编译、测试、成品自检和复制后，写入 `last.serial`，并将 `next.serial` +1；仅检查或失败不递增。已有同名文件不覆盖。汉化包版本不会自动递增；旧安装器语义版本文件及规则已移除。

| 系统 | 构建 | 仅检查 |
|---|---|---|
| Windows | 双击 `installer/build.cmd` 或执行 `./installer/build.ps1` | `./installer/build.ps1 -Check` |
| Linux/macOS | `sh installer/build.sh` | `sh installer/build.sh --check` |

构建使用 JDK 17+，由 `JAVA_HOME` 或 PATH 中的 Java 提供；没有 Maven/Gradle/网络下载步骤。构建期间不要修改基础输入。修改任何文案、译文或下载配置后均须重新导出。

## 下载和安装行为

固定 i18n 3.7.0、VP 1.5.3-hotfix、JECh 1.18.2-4.3.11、Oculus 1.18.2-1.6.4。地址使用 HTTPS JAR 直链，并同时维护文件名和 SHA-256；下载校验哈希及 JAR 结构后再替换文件，最多重试三次。VMTranslationUpdate 不列入下载项。

下载前检查 `mods/<配置文件名>`：普通文件通过配置 SHA-256 和 JAR 结构校验即跳过下载，不重写文件；缺失、文件名不同、损坏或无法校验时继续原下载流程。跳过日志使用 ui.properties 的 `install.mod.skip`，四个下载模组均适用。复用当前 VP 不影响旧 VP 清理。

`required=true` 控制“必装”字样和默认勾选，用户仍可取消。基础文件始终安装。无论 VP 是否勾选，旧 VP 清理都保留配置指定的目标文件名并删除已知错误版本；不触碰无关模组。实际安装顺序、备份和恢复见 [安装说明](install.md)。

目录错误可返回；强制安装先选择“强制安装”，再确认风险弹窗。终端模式同样要求 `FORCE` 和 `CONFIRM` 两步。首次说明确认及最终安装动作仍需用户明确选择。

界面主标题 20、页面标题 18、正文和控件 16 号字。较长说明支持滚动。文件校验、下载和解压在后台执行，界面更新使用 EDT。

## 验证边界

`InstallerTest` 在 build 隔离目录验证目录检查、两次确认、取消下载、VP 版本清理、覆盖/还原、哈希、ZIP 越界和并发锁；`BuildTest` 验证文件名展开与路径边界。链接创建受系统权限限制时明确报告跳过。

离屏图片在 `installer/build/verification/`，完整内置文件逐字节校验。不把这些测试写成真实桌面操作或 Linux/macOS 真机验收。发布直链未变时不重复下载；需重新核实时可运行 `java --class-path "成品.jar" installer/tools/VerifyDownloads.java "下载检查目录"`，仅下载验证，不安装。
