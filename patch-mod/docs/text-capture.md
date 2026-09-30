# 两个模块的原始文本与截取位置

核对基线：Minecraft 1.18.2、Forge 40.3.11、整合包 3.21.7、核心 `the_vault-1.18.2-3.21.6.6884.jar`。本文按该 JAR 的 `javap -p -c` 字节码还原逻辑；代码片段是等价示意，不是上游发布的 Java 源码。以下数字是**原始方法内的字节码偏移**，不是源码行号，也不是插入补丁后重新计算的位置。

## 配置维护

正式映射只保存在 `program/基础+硬编码汉化/config/vh3_translation_patch/` 下：

| 文件 | 键的形式 | 示例 |
|---|---|---|
| `combat_stats.json` | 实体 ID 的路径部分，通常小写加下划线 | `"aggressive_cow": "战斗牛"` |
| `sound_names.json` | `ModSounds` 中原始 Java 字段名，保留大小写和下划线 | `"RAFFLE_SFX": "速通音效"` |

给现有两个模块增加词条只改 JSON，不改源码，不重编译 JAR。安装时将配置随汉化工程复制到游戏的同名目录；游戏读取的是已安装的配置，工程文件与游戏实例之间没有自动同步。编辑游戏配置后按 F3+T，重新打开声音设置；结算界面也建议重新打开以重新构建显示项。

配置必须为 UTF-8 标准 JSON 对象，键和值均为字符串，不允许注释、重复键或尾随内容。首次缺失或损坏会阻止客户端启动并指出文件；重载失败保留该模块上次有效快照。空对象 `{}` 合法，表示不提供专用译文；删除整个文件不会自动生成。配置只影响所属模块，不覆盖全局语言资源。

## 结算怪物名称：combat_stats

目标类：`iskallia.vault.client.gui.screen.summary.element.CombatStatsContainerElement`。

目标方法：`private String formatMobName(String)`，描述符 `(Ljava/lang/String;)Ljava/lang/String;`。

### 上游输入与原算法

结算构造逻辑从实体统计的键中取得 `ResourceLocation`，调用 `toString()`，再传给 `formatMobName`。构造方法中一处对应偏移为 `563: ResourceLocation.toString` → `566: formatMobName`。

因此传入值类似 `the_vault:aggressive_cow`、`the_vault:aggressive_cow_boss`、`minecraft:zombie`。它是完整实体标识，既不是 `Aggressive Cow`，也不是 `entity.the_vault.aggressive_cow` 这样的语言键。

上游方法的等价逻辑如下，标出了两个返回位置：

```java
private String formatMobName(String id) {
    String[] parts = id.split(":");           // 偏移 0–7
    if (parts.length < 2) {
        return id;                          // 原始 ARETURN @15
    }
    String name = parts[1];                 // 偏移 16–19：原方法去除命名空间
    name = name.replace("_", " ");          // 偏移 20–30
    StringBuilder result = new StringBuilder();
    for (String word : name.split(" ")) {
        if (word.length() > 0) {
            result.append(word.substring(0, 1).toUpperCase());
            if (word.length() > 1) result.append(word.substring(1));
            result.append(" ");
        }
    }
    return result.toString().trim();         // trim @140，原始 ARETURN @143
}
```

例如原方法会把 `the_vault:aggressive_cow` 格式化为 `Aggressive Cow`。上游首字母处理使用默认区域设置；VTP 查配置时不依赖这个英文结果。

### VTP 如何取得配置键

VTP 保留上述方法，在 **两个 ARETURN 之前**分别调用：

```java
CombatStatsModule.translate(id, originalResult)
```

`id` 从局部变量槽位 1 读取。这两个目标方法都没有改写槽位 1，因此即使已执行完原格式化算法，仍可取到原始参数。原结果作为第二参数保留，供未命中时回退。

运行侧 `CombatStatsModule.mappingKey` 自行寻找**第一个冒号**，取它后面的全部内容；没有冒号则使用完整输入。不会从英文显示名反推 ID，不会生成语言键，也不会把 ID 转大写或去掉下划线。

| 传入参数 | 配置键 | 配置命中结果示例 |
|---|---|---|
| `the_vault:aggressive_cow` | `aggressive_cow` | 战斗牛 |
| `the_vault:aggressive_cow_boss` | `aggressive_cow_boss` | 战斗牛首领 |
| `minecraft:zombie` | `zombie` | 使用该键的配置值 |
| `aggressive_cow` | `aggressive_cow` | 战斗牛 |

按既定方案，命名空间不参与配置匹配：不同模组的同名路径共享这一条映射。缺失条目时，依次尝试已注册实体的 `getDescriptionId()` 对应当前游戏语言译名，再退回上游格式化结果。该语言键只用于回退，不是 JSON 的键。

## 声音设置名称：sound_names

目标类：`iskallia.vault.client.gui.screen.accessibility.VaultSoundOptionsScreen`。

目标方法：`private String formatSoundName(String)`，描述符同上。

### 上游输入与原算法

`collectSoundEntries()` 反射枚举 `iskallia.vault.init.ModSounds` 的字段，筛选 `SoundEvent` 类型并取字段值。跳过空值，以及资源标识等于 `BOSS_FIGHT_1` 至 `BOSS_FIGHT_4` 的声音。随后执行如下调用链：

```java
String displayName = formatSoundName(field.getName());
// 后续以 soundEvent、displayName 和音量创建 SoundEntryInfo。
```

具体位置是 `collectSoundEntries` 内的 `141: Field.getName()` → `144: formatSoundName(String)` → `147: ASTORE 7`。所以原始输入是字段名，例如 `RAFFLE_SFX`，不是声音资源 ID，不带 `the_vault:`，也不是已经格式化的 `Raffle SFX`。

上游方法等价逻辑如下：

```java
private String formatSoundName(String fieldName) {
    String[] words = fieldName.toLowerCase().split("_"); // 偏移 0–10
    StringBuilder result = new StringBuilder();
    for (String word : words) {
        if (word.equals("sfx")) {
            result.append("SFX");                       // 此分支不额外追加空格
        } else {
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1)).append(" ");
        }
    }
    return result.toString().trim();                    // trim @105，ARETURN @108
}
```

### VTP 如何取得配置键

在唯一的 **ARETURN @108 之前**调用 `SoundNamesModule.translate(fieldName, originalResult)`。原始字段名同样来自槽位 1；原方法的 `toLowerCase()` 返回新字符串，不会改写原始参数。

声音模块直接使用完整字段名查表，不截取、不转大小写、不把下划线替换为空格：

| 原始输入／配置键 | 上游英文结果 | 配置命中结果示例 |
|---|---|---|
| `RAFFLE_SFX` | `Raffle SFX` | 速通音效 |
| `GRASSHOPPER_BRRR` | `Grasshopper Brrr` | 蚱蜢：咕咕 |
| `VAULT_AMBIENT_LOOP` | `Vault Ambient Loop` | 宝库环境循环 |

未配置的字段直接使用上游英文结果。原界面构造方法在偏移 67 调用 `collectSoundEntries()`，在 70 存入 `allSoundEntries`；显示名被缓存。因此按用户确认的行为，修改配置并按 F3+T 后，需要关闭并重新打开声音设置。

## “截断点”的准确含义与边界

当前实现没有删除或提前终止上游源码段。所谓截取分成两件事：**取参数**是在原方法返回前从槽位 1 读取；**截取文本**只有结算模块取冒号后路径，声音模块不截取文本。实际注入指令为：

```text
原操作数栈：[originalResult]
ALOAD 1                         → [originalResult, originalInput]
SWAP                            → [originalInput, originalResult]
INVOKESTATIC <模块>.translate    → [translatedResult]
ARETURN                         → 将最终名称返回给原调用方
```

两个方法的原算法都会先运行；若上游自身对非法输入抛异常，返回钩子不会提前拦截。正常输入由实体统计或字段枚举提供。注入点按方法摘要和 ARETURN 指令定位，不在源码中硬编码上述偏移数字。

新增 JSON 条目能覆盖所有**实际到达这两个方法**的相应输入，配置中没有编译期白名单或固定条目上限。但它不会新增实体统计项、注册声音，或让上游排除的声音重新出现在设置页。要接管新界面、新方法或改变筛选流程，仍需编写／调整模块。
