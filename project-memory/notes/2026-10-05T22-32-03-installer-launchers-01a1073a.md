# 三系统独立启动文件

- 日期：2026-10-05 22:32，Asia/Shanghai。
- 来源：用户要求解决部分用户不能双击 JAR 的问题，提供简单名称的跨系统启动方式；补充要求直接放在父工作区 [发布文件]，尽量不附带 launcher 文件夹等额外内容。
- 状态：实现、构建、Windows 实际启动自检和 POSIX Shell 逻辑验证完成；未提交 Git、未操作游戏实例。

最终发布结构：windows系统点我启动.cmd、macOS系统点我启动.command、Linux系统点我启动.sh 三个独立文件直接位于 [发布文件]。Windows CMD 内置 PowerShell 并按 UTF-8 读取；两个 Unix 文件由同一 POSIX sh 源生成，不需要辅助文件夹、配置、旁置说明。只有一个同目录 JAR 则直接启动，有多个时列编号由用户选择，无 JAR/非法选择明确退出。无须配置 JAR 文件关联。

Java 17+ 检测依次尝试用户自行提供的同目录 runtime、JAVA_HOME、PATH，macOS 额外查系统 Java；不自带或自动下载 Java。支持 --check、--console，失败保留退出码与提示。Windows 对选中的文件先检查 ZIP 和安装器主类，避免损坏文件触发原生 Java 启动器的乱码诊断。

构建继续保留原名独立 JAR，新增同名 ZIP。ZIP 严格只有 JAR 和三个脚本，Unix 权限 755、LF 无 BOM；三个独立脚本也同步导出至 [发布文件]。成功写入 JAR、ZIP 和脚本后才推进序列号；失败撤销本轮成品并恢复原脚本。

发现并修复目录名以 ! 结尾时 JAR 资源 URL 解析失败：Config.resource 在标准读取未命中时按真实代码来源打开同一 JarFile，流关闭同步释放文件句柄。--check / --console 错误只输出终端，不弹出阻塞对话框。增加隔离 JAR 加载回归，覆盖中文、空格、&、括号及 ! 路径。

验证：8 项构建文件名检查、启动包结构/编码检查、26 项安装器测试通过，1 项符号链接权限跳过；227 个内置文件一致，成品自检成功。真实 Windows CMD/PowerShell/Java 17 覆盖特殊路径、不同当前目录、缺 Java、缺 JAR、损坏 JAR、多文件有效/无效选择。Git Bash 下用 Java 版本/参数夹具执行两种 Unix 入口，覆盖 Java 8 拒绝并回退 17、缺 Java、参数转发和退出码 7、多文件选择；不是 macOS/Linux 真机验收，未声称通过原生桌面测试。

最终 ZIP 解压后再用真实 Windows 入口 --check 验证通过；独立脚本与 ZIP 内文件字节一致，ZIP 四条目且没有文件夹、Unix 权限 755。证据在 installer/build/verification/launchers.json、final-launchers.json。Wiki/README 同步启动方法、Java 要求及无执行权限时的 sh 命令。

开始时用户已导出至序列 15。最终成品序列 18，next.serial=19：

- JAR：宝藏猎人3汉化安装器-3.21.7-VM汉化组-V2.7-18.jar，1203393 字节，SHA-256 2f6b0e7d676574fb8707d47c38029cebb440919a5fd4ebcfe905996205a3ba42。
- ZIP：同名 .zip，1206838 字节，SHA-256 58e2aae80de35c64fa383de73507aa4cfd7aa3e19e5513e7a5fac3fee2cdd314。

本轮中间产物 16/17 的 JAR/ZIP 经哈希确认后移入 installer/build/verification/intermediate-launchers，避免带辅助目录的中间方案混入最终发布；未改用户原有序列 15 成品、汉化包 ZIP、指南与 buildOutput.cmd。保留上一轮未提交的图标/Markdown/编码修改；未改 VTP。
