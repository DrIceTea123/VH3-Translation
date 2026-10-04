# VH3 汉化安装器

Java 17+，完整内置 [translate-packs](../translate-packs/)。配置、文案、下载、命名及序列号规则统一见 [安装器 Wiki](../wiki/installer.md)；玩家使用方式见 [安装说明](../wiki/install.md)。

- Windows：双击 `build.cmd`，或执行 `./build.ps1`；`-Check` 仅检查。
- Linux/macOS：`sh build.sh`；`--check` 仅检查。

成品输出到父工作区 `[发布文件]/`。汉化包项目版本为 V2.7，导出序列号从 1 开始；成功导出才 +1。使用 JDK 编译，无网络构建依赖。
