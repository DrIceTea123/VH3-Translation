# VTP 合并可行性评估

- 日期：2026-10-04（Asia/Shanghai）。来源：本对话用户明确要求“接下来优先完成评估合并 VPT 的可能性”，按现有项目理解为 VTP 双 JAR 合并。调整顺序为先评估合并，VP 格式升级后移。
- 类型：评估、源码事实、离线探针。状态：评估完成，等待用户确认；不等于授权实施。未提交 Git。
- 结论：可研究实现单文件分发，推荐外层 transformer＋内嵌 runtime JAR＋SERVICE 层 IModLocator。源码职责与加载层仍然分离；不推荐简单平铺或仅启用标准 Jar-in-Jar。
- 证据：核对固定 Forge 40.3.11 / ModLauncher 9.1.3 源码并调用真实扫描器。平铺外壳被排除普通 mod 扫描；两种纯嵌套布局均缺其中一侧入口；最小定位器可识别从外壳提取的真实 runtime 为 MOD，modId 正确。
- 限制：定位器是直接实例化的测试类，版本元数据由夹具补充；尚未验证实际 SERVICE SPI 注册、GAME 层加载、@Mod 构造、客户端/服务端启动和 F3+T。不把扫描证明写成合并成品通过。
- 方案、改动范围与验收门槛见 patch-mod/docs/jar-merge-assessment.md；复现工具在 tools/assessment，证据在 docs/evidence/jar-merge-assessment.json。缓存产物仅在 build/merge-assessment。
- 生产源码、构建导出、版本号及两个正式 1.0.15 JAR 未改；成品散列已与前一轮核对一致。前一轮 1.0.15 VP 检测修复仍处于未提交状态，不混合提交。
- 对旧决定 D-007 的澄清：默认普通扫描确实排除早期服务 JAR，但这不等于不能用专用定位器实现单文件分发。原双 JAR 决定在新方案获准实施前仍有效。报告后按当前工作指令等待用户确认。
