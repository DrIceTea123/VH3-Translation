# 安装器图标、Markdown 首页与 CMD 中文输出

- 日期：2026-10-05 09:24，Asia/Shanghai。
- 来源：用户要求图标改用 translate-packs/icon.png，第一页说明改 Markdown，修复 build.cmd 在 CMD 下的乱码。
- 状态：实现、构建检查、离屏预览和新安装器导出完成；未暂存或提交 Git，未操作真实整合包实例。

构建时将 translate-packs/icon.png 原样内置为安装器资源，窗口图标与首页标题旁图标共用该资源，不在源码 resources 维护图片副本。现有 notice.txt 迁移为 resources/notice.md，保留声明正文，增加小节标题、重点粗体和列表。Texts 读取新文件，Swing 首页使用只读 HTML 组件渲染轻量 Markdown：标题、段落、换行、单层列表、强调、链接、代码、引用和分隔线；不声称完整 CommonMark/GFM。原始 HTML 转义，不加载图片，HTTP(S) 链接仅点击时通过系统浏览器打开；终端显示 Markdown 原文。Wiki 已同步唯一编辑位置与支持语法。

乱码修复：build.cmd 保存原代码页，构建时切换 65001，暂停后恢复且保留退出码；build.ps1 临时统一 OutputEncoding 与 Console.OutputEncoding；构建及测试/成品自检 Java 子进程同时指定新旧 JDK 的 stdout/stderr UTF-8 属性，避免仅设 file.encoding 时标准流仍使用原控制台编码。PowerShell 脚本结束恢复原输出编码。

验证：直接 CMD → Windows PowerShell → Java 的 build.cmd -Check 成功，中文输出正常且序列未递增；随后指定 JDK 17，从 CMD 代码页 936 运行完整 build.cmd，捕获输出以 UTF-8 严格解码，确认中文日志、中文路径及下次序列号正确，无替代字符，结束恢复 936。日志在 installer/build/verification/cmd-jdk17-utf8.log。8 项构建命名检查、25 项安装器测试通过，1 项符号链接测试因系统权限跳过；227 个内置基础文件逐字节一致，成品自检通过。查看首页离屏图片确认图标、标题、强调、正文与滚动区域；未进行真实桌面点击、完整安装或跨平台真机验收。

本轮开始用户序列状态 last.serial=10、next.serial=11，沿用未回退。导出 [发布文件]/宝藏猎人3汉化安装器-3.21.7-VM汉化组-V2.7-11.jar，1201790 字节，SHA-256 5d5896c229a5795bedaaa225111f72e8abb29048db6bdffc67c5a84c30055b52；成功后 last.serial=11、next.serial=12。额外检查成品 icon.png / notice.md 与源文件字节一致、不残留 notice.txt。保留用户新增的 VMTU 配置及其他当前发布内容，VTP 未修改或重新编译。
