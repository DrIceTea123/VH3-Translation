# VH3 跨平台汉化安装器

Java 17 + Swing 窗口向导，生成一个内置基础内容的可执行 JAR。Windows、Linux、macOS 使用相同文件，运行端需要 Java 17 或更新版本；没有图形桌面时可使用终端向导。运行时不读取安装器旁的配置文件。

## 快速编辑

| 修改内容 | 唯一编辑位置 |
|---|---|
| 汉化包说明正文 | `resources/notice.txt`，UTF-8 文本，当前故意留空 |
| 各步骤标题、说明、确认文字、按钮、完成提示 | `resources/ui.properties` |
| 整合包版本、the_vault 版本/文件名/SHA-256 | `resources/installer.properties` 的 `pack.*` / `vault.*` |
| 模组显示名称、下载地址、文件名、SHA-256、是否必装 | 同一文件的 `mods` 列表及对应分组 |
| 基础汉化内容及 VTP | `../program/汉化包内容/`，构建时完整内置，不维护副本 |
| 下一次安装器版本 | `version.properties` 的 `next.version` |

`.properties` 使用 `键=值`，中文直接输入；多行文案使用 `\n`。UI 文件开头列有版本占位符，已有提示中的 `{path}`、`{problems}`、`{error}`、`{name}` 由程序填入。说明正文是纯文本，不解析 Markdown/HTML。底层文件系统/网络异常保留详细诊断。

修改下载地址时同时更新 `filename` 和 `sha256`，不能留空或填写 `latest`。地址须为 HTTPS 的 JAR 直链；下载后按固定哈希验证并检查 JAR 结构。可在 PowerShell 使用 `Get-FileHash -LiteralPath '已核实的文件.jar' -Algorithm SHA256` 获取摘要。构建不联网，也不会偷偷把直链模组内置进成品。

当前固定 i18n 3.7.0、VP 1.5.3-hotfix（实际文件名 `1.5.3-fix`）、JECh 1.18.2-4.3.11、Oculus 1.18.2-1.6.4。i18n 与 VP 必装，另外两项默认不选。VMTranslationUpdate 不在安装列表，也不会下载；已存在的 VMCT、旧 i18n、JECh、Oculus 不在本次自动删除范围，升级这些模组时由用户先处理旧版，避免重复模组。

## 一键生成

构建机器安装 **JDK 17 或更新版本**，配置 `JAVA_HOME` 或让 `java` 命令指向 JDK。不需要 Inno Setup、7-Zip、Maven、Gradle 或 Python。

- Windows：双击 `build.cmd`；或在 PowerShell 执行 `./build.ps1`。
- Linux/macOS：在本目录执行 `sh ./build.sh`。
- 只编译和检查：`./build.ps1 -Check` 或 `sh ./build.sh --check`，不生成成品、不增加版本。

一键生成会编译 Java 17 字节码、打包完整基础目录、执行安装回归测试、生成 JAR 并验证内置内容。成功后输出到父工作区：

```text
[发布文件]/VH3-installer/VH3-Translation-Installer-V2.7.0.jar
```

首版为 **V2.7.0**，以后每次成功生成，末位加一：2.7.1、2.7.2……。`last.version` 记录最近成功版本，`next.version` 自动变为下一版；测试、失败构建不递增。修改主/次版本时手动编辑 `next.version`。已有同名成品不会被覆盖。不要在构建过程中编辑基础目录。

成品只需分发这个 JAR，内置所有配置、说明和基础内容；玩家无需项目源码，不能通过旁置配置文件改变安装行为。修改任何发布内容后须重新生成。源码本身使用明文资源打包，不以此承诺防篡改或防反编译。

## 启动与安装

关联了 Java 的系统可双击 JAR，或使用：

```sh
java -jar VH3-Translation-Installer-V2.7.0.jar
```

无图形桌面时使用 `java -jar 文件名.jar --console`。终端模式同样要求确认说明、选择目录、明确强制安装和确认开始，不提供静默安装。`--check` 只校验成品内置资源，不安装、不下载。

1. 展示说明，必须勾选确认才能继续。空说明仍展示占位提示。
2. 默认目录取 **JAR 所在文件夹**，不取终端当前目录。选择已有目录后检查 `mods`、唯一目标 the_vault 文件名及 SHA-256；不匹配弹出“返回重新选择 / 强制安装”。强制安装只绕过兼容性校验，不绕过文件安全检查或下载哈希。
3. 基础汉化（含当前 VTP）、i18n、VP 必装；JECh、Oculus 选装。基础文件同名覆盖，不按时间戳跳过。
4. 先清理 `vaultpatcher/cache`，再覆盖基础内容，然后依次下载校验并写入 `mods`，全部成功后删除旧 VP/VTP。保留这次安装的 VP 和基础目录中的 VTP，不清空 mods，不删除自定义 VP modules。
5. 成功后显示完成弹窗；失败显示原因并可重试。安装期间不允许关闭窗口，避免普通误操作中断。

安装时先在选定根目录建立临时目录并校验内置资源，然后按上述顺序修改。网络失败最多尝试三次；失败会撤销本轮文件写入，已清空的 VP 缓存不恢复，新建的空文件夹可能保留。若权限/磁盘问题导致还原失败，提示保留备份的位置，需手动处理；`backup/recovery.properties` 记录原相对路径与备份编号，`NEW_FILE` 表示本轮新增文件。断电/强杀进程不保证自动恢复，不应在游戏运行时安装。

目录内 `.vh3-installer.lock` 是零字节并发锁标记，文件存在不代表正在安装；是否占用取决于操作系统文件锁。固定标记保留，以避免删除锁文件造成并发穿透。安装路径下的符号链接或 junction 被拒绝；可选择链接指向的实际根目录。

## 验证范围

`src/test/java/.../InstallerTest.java` 覆盖匹配/错误根目录、强制安装、顺序、覆盖、选装、重复安装、精确清理、下载中断还原、哈希/JAR 拒绝、ZIP 路径穿越、并发锁、内置文件逐字节对比及说明确认。链接测试在操作系统不允许创建符号链接时明确跳过。离屏页面图位于 `build/verification/`。

这些是隔离文件测试与离屏界面检查；不等于完整游戏验收，也不代表已在 Linux/macOS 真机操作过。Swing 的跨平台能力见 [Java 17 官方文档](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/package-summary.html)。

发布配置改动后可用 JDK 单独实测下载链路（会下载全部四个模组到指定临时目录，不安装）：

```sh
java --class-path "成品.jar" tools/VerifyDownloads.java "build/download-check"
```
