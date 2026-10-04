# V2.7 路径同步、图鉴修复、安装器及 Wiki

- 日期：2026-10-04 23:22，Asia/Shanghai。
- 来源：用户要求执行父工作区《给Codex的工作指令.md》五项任务；随后确认导出序列从 1 开始及中文名称模板，并补充“跳过 VP 时若版本不对仍需删除”。
- 类型：用户决定、实现、文档与项目记忆归并、离线验证。
- 状态：已修复、构建并导出；没有暂存/提交 Git，没有操作实际整合包实例或启动 Minecraft。保留任务开始时 notice.txt 的用户修改。

## 本轮结果

- 当前安装输入为 translate-packs；同步 VTP 构建/导出、运行与转换测试、安装器打包、加载探针、Git 忽略例外和协作入口。旧 program 引用不再用于当前维护。
- 汉化包项目版本 V2.7，唯一配置为 installer/resources/installer.properties 的 translation.version；VTP 从 1.0.18 推进到 1.0.19，同轮重试未重复递增。
- 图鉴崩溃为详情返回把 getEntityName 的中文“集群怪物”送入 getFilterByName，再构造非法资源位置。返回回调改为原始 group ID → lookupName；同时覆盖带 predicate 的 BestiaryScreen 构造入口，只对真实族类转换查找键，保留其他 predicate 回退。显示标题、筛选算法、业务 ID 与正式译表保持原值。细节见 patch-mod/docs/bestiary-groups.md。
- 安装器根目录校验文字全部提取到 ui.properties；强制安装新增第二次风险确认，终端同步 FORCE/CONFIRM；主标题 20、页面标题 18、正文/控件 16 号字。
- 所有下载模组允许取消，保留“必装”标注和默认勾选。按最终用户补充，跳过 VP 仍清理已知错误版本，只保留配置目标文件名；不下载、重写目标版本，不清理其他无关模组。文件按现有命名模式识别，自行改名的旧 JAR 仍需人工核对。
- 移除安装器语义版本和 version.properties。export.properties 保存 last.serial/next.serial；成功导出递增，失败/仅检查不递增。名称模板为“宝藏猎人3汉化安装器-{modpackVersion}-VM汉化组-V{translationVersion}-{exportSerial}.jar”。
- 新建 wiki，集中版本/目录、安装、翻译、VTP 模块、安装器、开发与历史；五份已完成评估/计划/成果报告归并到现行说明，VP 维护页移入 Wiki。保留独立技术证据、来源、原始文件及所有历史 notes。现行 Markdown 本文不含历史 notes 的体积约减少 26%。
- 重新读取并归并此前 25 份 notes，更新 README 已归并清单及 STATE/RULES/DECISIONS/TODO，删除已被后续实现取代的待办描述；本 note 为新的验证增量。全局记忆未修改。

## 验证与交付

- VTP 完整离线 build：223 项 Java 测试（runtime 62、transformer 161），零失败/错误/跳过；111 个目标方法、245 处修改通过固定核心哈希、方法指纹、命中数和 ASM 检查。执行真实详情返回回调及列表 lambda，检查重复返回、不同原始 ID、Dweller 与非族类回退。
- 最终单包通过实际 Forge/ModLauncher SERVICE SPI、mod 元数据和独立 GAME 类加载；客户端/服务端预检分别注册 14/8 模块、90/43 类。探针仍用环境夹具，没有执行完整 Minecraft 生命周期。持久证据：patch-mod/docs/evidence/single-jar-1.0.19.json。
- VTP 安装输入 translate-packs/mods/vh3_translation_patch-1.0.19.jar，142619 字节；与 distribution 完全一致，SHA-256 b37d3d835fbc7a014431bbfc28a630666388f9c41ebe1d5e1a718386fe6f19dd。图鉴新增来源基线重新审阅并记录，复核 unchanged。
- 安装器 JDK 21 与 JDK 17 检查均为 21 项通过、1 项符号链接测试因系统权限跳过；另有 8 项文件名展开/越界检查通过。四页离屏渲染中的说明页、目录页、模组页和进度页已查看；尚未真实桌面点击或 Linux/macOS 真机验收。
- 新成品在父工作区 [发布文件]/宝藏猎人3汉化安装器-3.21.7-VM汉化组-V2.7-1.jar，1190214 字节；SHA-256 a4747ed6e58b7fdbd1e8a83306255f6b67b5fceed3aa0b94824d17366216fb93。内置 227 个文件与 translate-packs 逐一字节一致，notice.txt 与当前用户正文一致，无测试类。
- 成品自检通过；成功导出后 last.serial=1、next.serial=2。随后仅检查保持 2；隔离构建中故意引入编译失败，确认不生成成品、不推进序列号。未重复下载未变更的四个固定官方文件。
- Markdown 本地链接、历史 notes 字节不变、父入口与模板一致、git diff 空白检查通过。原始崩溃 ZIP 仅仅读取，未提取或记录实例路径。

## 待验收

用户仍需在真实游戏验证图鉴详情返回、带族类直接打开及重载。完整客户端/专用服务器、其他模块界面、VP 转换器链与 F3+T 尚未验收；离线通过不等于游戏问题已由用户确认消失。剩余事项已在 TODO 中合并保留。
