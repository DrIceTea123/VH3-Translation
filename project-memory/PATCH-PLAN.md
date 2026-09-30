# the_vault 专用汉化补丁方案

日期：2026-09-30。状态：用户接受的方向；实现细节需用真实原型验证。

## 目标与非目标

以具体核心 JAR 为基线，修复 VP 难以表达的显示逻辑；保留已完成的翻译，减少组合输出穷举与重复维护。开发阶段用反编译/字节码查看逻辑；玩家启动时只进行字节码转换。第一版不提供任意 Java 源码运行时编辑器，不重建整个 the_vault，也不先复制 VP 的所有跨加载器能力。

## 三层设计

1. 构建工具：导入旧 VP 文件；按功能拆分固定文本、共享词典和方法补丁清单；保留源规则语义和溯源；生成发布资源与验证报告。
2. 加载期转换器：针对清单中的类注册 ModLauncher ITransformationService/ITransformer<ClassNode>；处理常量替换、指定调用修改和方法体替换。
3. Java 汉化辅助类：实体/矿石名称、稀有度与容器模板、声音词典、房间名称等。译文与逻辑分离，多处显示复用。

固定字符串仍数据驱动。组合输出和参数顺序交给 Java。只有控制流程真正需要改变才替换完整方法。整类替换可用于特殊情况，但要检查其他转换器/Mixin 对同类的改动。

## 方法级补丁

补丁清单包含：唯一 ID、核心版本/哈希、类内部名、方法名、描述符、客户端/服务端范围、操作、预期命中数、原方法摘要及说明。

示例目标：

- 类：iskallia/vault/client/gui/screen/summary/element/CombatStatsContainerElement
- 方法：formatMobName
- 描述符：(Ljava/lang/String;)Ljava/lang/String;
- 操作：方法体转交给补丁 mod 的实体名称辅助方法，再返回结果。

目标类无需整体重新编译。转换器只生成参数加载、静态方法调用和返回等短指令，复杂逻辑用正常 Java 编写。依赖原对象私有字段或局部变量的特殊情况需专门设计访问或局部转换，不能假定外部 helper 可直接访问。

任意方法体移植不能只复制 instructions：标签、异常表、局部变量、栈帧、lambda/匿名类/合成方法和访问权限都要检查。修改后计算必要的 maxs/frames 并使用 ASM 校验；保持描述符和返回类型正确。

## 首批目标

### 经验提示

ClientVaultXpTracker.formatChestName：稀有度、类型、Chest/Barrel 分别翻译，用模板排列。数量由 R×T×2 的组合变为约 R+T+2 个词条和例外。formatOreName 根据完整 ID 复用方块/物品语言或专用覆盖；formatMobName 复用实体名称解析。兼顾 createPreviewNotifications。只改显示，不改 XP、tick 与统计逻辑。

### 结算怪物名

CombatStatsContainerElement.formatMobName 接收完整实体 ID。按“专用覆盖 → 实体实际语言键 → 原英文逻辑”解析。未知 ID 先检查注册表是否存在，避免默认条目误导显示。不能直接删除 237 条映射；aggressive_cow 和 aggressive_cow_boss 的差异要保留。

### 声音名称

formatSoundName 接收 Java 字段名，collectSoundEntries 同时有 SoundEvent。第一版可使用字段词典，更稳妥的是将调用改为传声音 ID；先验证实际映射。203 个声音译名依然需要词典，不承诺改代码后全部消失。

### 后续目标

VaultMapRenderHelper.getTooltipText 保留路径与类别，适合模板化。研究和任务名称在界面/通知生成处转换，避免全局改 Research.getName 等业务名称；术语仍共享词典。

## 环境与加载

- MC 1.18.2、Java 17、整合包实际 Forge 版本。
- 以本地原始 JAR 为 ForgeGradle 依赖，需引用目标类型时用 fg.deobf 配置；没有源码不阻止引用公开 API。
- 优先让 helper 使用 String、基础类型和 Minecraft API，减少 the_vault 复杂依赖。
- 早期转换器仅操作 ASM 节点及字符串名称，不提前加载目标类；game-side helper 与转换器的打包/类加载器边界必须实测。
- 开发映射和发布映射分别处理。不要直接把 IDE 中的 Minecraft 方法名写死到生产字节码。
- 明确与 VP、目标 mod 自身 Mixin 和整合包其他转换器的顺序；同一显示补丁只保留一个所有者。

## 迁移阶段

1. 原型：保留 VP 1.4.4+3；打通一个方法补丁，再完成经验提示、结算怪物名、声音名称和变化检查。
2. 编辑工具：把既有 VP 翻译导入功能文件。保留 method/local/ordinal、部分匹配、返回值/局部变量等语义。未支持的规则报告并留给 VP。
3. 扩展：逐步接管地图/研究/任务和动态规则，移除重复作用位置。
4. 去依赖评估：只有旧规则等效覆盖、动态补救处理、other_mods 承接、服务端验证都具备后，才决定是否彻底移除 VP。

## 更新与验证

每个核心 JAR 记录版本和 SHA-256；扫描签名；对目标方法计算忽略行号等调试内容的规范化字节码摘要；生成未变化/内部变化/签名变化/消失报告。不要用易变的指令绝对序号作为唯一定位依据。

固定规则检查目标和命中数；方法补丁检查描述符、预期原形及重复应用；导出转换类供复核；进行字节码检查和完整整合包功能测试。绑定版本是允许的，但上游新增业务逻辑不能被旧方法体无意覆盖。

## 性能与语言

按类索引补丁，不在热路径遍历全部规则；词典查找与生成文本只放在必要位置，避免全局渲染正则、调用栈查询和重复对象分配。缓存随资源/语言重载失效，不在语言系统初始化前永久缓存译文。

客户端显示使用语言资源；服务端消息优先保留翻译键及参数，1.18.2 可用 TranslatableComponent，不得在专用服务端直接调用客户端 I18n。纯 String 接口另行设计回退。不要假定替换为翻译组件后可继续使用不受支持的格式说明符。

Mixin 不是因为数量多就严重拖慢运行；ASM 不自动更快。此路线为批量规则和组织方式服务，性能结论要测量。用户未禁止在特殊位置评估少量 Mixin。

## 参考

- https://github.com/3093FengMing/VaultPatcher （初次检查 master 提交 affc2d99c62ed6f5f7d152f0a2f4bde9495a93f4，不能假定与 VP 1.4.4+3 功能完全相同）
- https://docs.minecraftforge.net/en/fg-5.x/dependencies/
- https://docs.minecraftforge.net/en/1.18.x/gettingstarted/
- https://docs.minecraftforge.net/en/1.18.x/concepts/internationalization/
- https://github.com/SpongePowered/Mixin/wiki/Introduction-to-Mixins---Understanding-Mixin-Architecture
- https://asm.ow2.io/faq.html
