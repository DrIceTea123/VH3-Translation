# VH3 汉化安装器

Java 17+，完整内置 [translate-packs](../translate-packs/)。配置、文案、下载、命名及序列号规则统一见 [安装器 Wiki](../wiki/installer.md)；玩家使用方式见 [安装说明](../wiki/install.md)。

- Windows：双击 `build.cmd`，或执行 `./build.ps1`；`-Check` 仅检查。
- Linux/macOS：`sh build.sh`；`--check` 仅检查。

成品输出到父工作区 `[发布文件]/`，仅导出 JAR 并同步三个独立启动脚本，不生成启动包 ZIP。运行 `windows系统点我启动.cmd`、`macOS系统点我启动.command` 或 `Linux系统点我启动.sh`；脚本优先选择汉化包版本最高的安装器，版本相同再比较导出序列号。不依赖 JAR 文件关联，仍需 Java 17+。启动入口源码在 `launchers/`。

汉化包项目版本为 V2.7，导出序列号从 1 开始；JAR 和脚本成功导出才 +1。使用 JDK 编译，无网络构建依赖。
