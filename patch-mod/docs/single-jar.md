# 单 JAR 与加载层

自 1.0.16 起，只分发 `vh3_translation_patch-<版本>.jar`。runtime/transformer 源码与 SERVICE/GAME 加载层保持分离。

## 为什么需要定位器

固定 Forge 40.3.11 的早期扫描只识别顶层服务 JAR，并将它们排除在普通 mod 扫描之外。直接平铺 runtime 与 transformer 会丢失普通 mod 入口；仅嵌套也不能同时满足早期服务和后续普通 mod 发现。专用 `IModLocator` 为内层运行模块提供受控入口。

原始四种布局比较、加载器源码散列和最小定位器证据保留在 [评估证据](evidence/jar-merge-assessment.json)。历史复现工具 `tools/assessment/assess-jar-layout.py` 需用 `--runtime`、`--transformer` 显式指定旧双包，不读取当前单包冒充旧版。

## 结构和缓存

- 外层只含转换侧代码、目标清单、`ITransformationService` / `IModLocator` 服务注册。
- 构建先完成 `runtime:reobfJar`，再逐字节嵌入 `META-INF/vh3_translation_patch/runtime.jar`，记录版本和 SHA-256。外层没有运行侧 class 或普通 `mods.toml`。
- `EmbeddedRuntime` 由转换服务和定位器共用，不依赖两入口初始化顺序。校验后提取到 `.vh3_translation_patch/runtime/<版本>-<哈希>/runtime.jar`；原件错误拒绝启动，缓存损坏可重建。
- 同目录临时写入并原子替换；无法创建缓存、移动失败或占用时报错。旧缓存不在运行中自动删除。
- 校验唯一单包、旧独立包残留、内外版本和 helper。核心/方法指纹、VP 冲突、ready/version 握手仍保留。

安装、升级、回退见 [安装说明](../../wiki/install.md)。构建导出和测试位置见 [开发说明](../../wiki/development.md)，不再重复维护版本号和成品数量。

## 验证

`verifyBundle` 检查 SPI、内外版本、reobf 字节与 SHA-256、源码层隔离、未内置 JSON 或测试类。缓存/安装布局回归覆盖重复包、旧包、版本/哈希错误、损坏恢复、不可写、升级与回退。

`tools/assessment/verify-bundle.py` 使用真实 Forge/ModLauncher 扫描器及模块加载器，验证 SERVICE SPI、Forge mod 元数据、客户端/服务端预检和隔离 GAME 类加载；结果输出在 `build/bundle-verification/`。

探针补充 FML 环境夹具，没有实例化 @Mod 或执行 Minecraft 生命周期。它不能替代完整游戏启动、F3+T、客户端和服务器验收。历史固定结果保留于 `docs/evidence/`。
