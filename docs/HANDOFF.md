# MineVM 停更交接与恢复开发

更新日期：2026-11-25。项目进入暂时搁置阶段，保留可继续开发的源码快照，不把当前开发版当作新的稳定 Release。

新会话从本文开始，再检查 Git 状态和实际代码；不需要依赖旧聊天记录。当前开发版本是 `1.1.0-dev`，已有的 `1.0.0` 发布不因本次备份而改变。

## 仓库与环境

- 仓库：`https://github.com/250-BadBrain/MineVM`，当前分支 `master`。
- 原本地目录：`E:/Desktop/MineVM`。可在其他位置克隆，不要依赖旧名称 `Computer in Minecraft`。
- Minecraft `1.20.1`，Forge `47.4.10`，Java `17`，Gradle Wrapper `8.8`。
- 模组 ID 为 `minevm`；Java 包仍为 `com.example.vm`，不是漏改项目名称，不要为了命名再次整体迁移。
- Windows 下使用 PowerShell，优先 `pwsh`。手动编辑源码时保留现有风格，不顺带重构无关模块。
- 最终交付保持单个模组 JAR，外部工具和镜像路径通过 Forge 的 `config/minevm-common.toml` 配置。虚拟磁盘数据独立存储，不放进 JAR。
- QEMU 使用完整安装目录，包括 `qemu-system-x86_64.exe`、`qemu-img.exe`；真实 QEMU 集成自测还使用同目录的 `qemu-io.exe`。不要把这些工具或系统镜像提交到仓库。

本次停更提交包含此前尚未提交的功能、修复、测试和文档。以包含本文的 Git 提交作为恢复点；开始整理前的本地 HEAD 是 `c2b3403`，它本身并不包含后续全部改进。恢复时先看 `git log`，不要误退回该旧提交。

## 已实现的开发内容

1. 台式机、笔记本、显示器、键盘、鼠标、HDMI/USB 线、系统光盘、独立软驱盒、Win95 启动软盘和带独立身份的空白可写软盘。
2. 电脑/软盘物品的 NBT 身份与数据绑定；电脑带 NBT 掉落和创造模式选取。线材在生存模式连接成功后消耗。
3. 物理键鼠的双向连接和设备身份校验。拆除设备后输入失效，同坐标放置新设备不会自动继承旧连接；旧存档的布尔连接状态需要重新接线。
4. BIOS 启动、光盘和软盘独立插拔、QMP 电源/介质控制、VNC 显示和输入。修复过弹盘寻址、异步插盘消耗错误物品、启动介质遗漏、多 VM 接收阻塞、QMP 响应解析及异步生命周期问题。
5. 所有者与授权玩家、独占控制租约、退出/超时/断线释放按键、服务端画面订阅和带宽限制。授权控制不等于磁盘维护权限，也不是完整的领地防破坏系统。
6. 诊断命令、累计性能统计、关机后的硬盘备份/恢复/重置，保留恢复前的磁盘副本并恢复备份的系统档案。
7. 中英文新流程提示、BIOS 标签、首次进入提示及 19 个合成配方。新贴图仍使用临时或复用素材，统一绘制安排在后期。
8. Windows 98 和 32 位 Vista 的实验硬件档案、ISO 配置和光盘物品，以及明确区分实测证据的兼容性表。

画面捕获仍使用 VNC；多人链路使用有界的 RAW/DEFLATE 全帧传输，已拆分 `ScreenTransport` 和 `FrameCodec` 接口。**没有实现 H.264/FFmpeg，也没有引入对应解码器或可执行文件依赖。** 不要把接口准备误写成视频流改造已完成。

## 兼容性与未完成事项

| 系统 | 当前可声明的状态 |
| --- | --- |
| Ubuntu Server / Desktop、Windows XP | 曾由用户报告安装/运行可用，部分镜像修订号未记录；不是本次重新验证 |
| FreeDOS | 用户报告 1.4 LiveCD 完整安装链路通过 |
| Windows 95 | 安装过程多次走到后期，但出现过硬盘无效系统盘和 IOS 保护错误；最终硬盘冷启动回归仍未完成 |
| Windows 98 SE、32 位 Vista | 已增加实验档案，尚未完成真实镜像安装验证 |
| Windows 10 / 11 | 仅有档案与物品基础，安装链路未验证；Win11 固件/TPM 请求不代表完整环境已配置成功 |

恢复开发的优先顺序：

1. 重新构建并跑自动化测试，先确认停更快照在新环境可用。
2. 从全新测试磁盘重跑 Win95 的分区、安装、介质交换、硬盘冷启动、重启和 15 分钟空闲流程，记录镜像版本与哈希。不要把安装界面出现当作完成支持。
3. 用两台真实客户端验证服务端画面、授权交接、指针对齐、断线按键释放、维度/距离/区块边界和限流。当前测试不能替代双客户端实测。
4. 按 Win98 SE、32 位 Vista 的顺序继续安装验证，之后再做 Win10/11。
5. 用已有计数器定位性能瓶颈，再设计 H.264/FFmpeg 传输、进程生命周期和解码集成，保留已知可用的回退路径。
6. 最后统一绘制贴图并进行界面、本地化、配方和生存流程检查，再决定下一个正式版本。

## 最近的验证证据

2026-10-25 的最终组合验证通过：完整构建、真实 QEMU 介质与硬盘维护自测，以及独立 Forge 服务端的 5 项 GameTest。66 个 JSON 资源可解析，中英文键名匹配，`git diff --check` 通过。

真实 QEMU 测试验证了空白 MBR、空软驱、可写/只读软盘交换、实际弹盘、运行中拒绝维护、关机备份/重置/恢复和系统档案恢复。它没有安装 Windows，也不证明 GUI 指针对齐或多人操作已通过。

2026-11-25 整理停更快照时不重跑 Gradle，避免重新生成刚清理的构建产物；仅做源码/资源和 Git 静态检查。历史 JAR 校验值属于历史构建，不作为未来重构建的预期值。

详细结果见 [改进验证报告](audits/2026-10-25-improvements-validation.md) 和 [代码审计记录](audits/2026-10-25-project-review.md)。报告中的“未提交”“磁盘保留”等措辞是当时的历史状态；当前以本交接和实际文件系统为准。

## 清理与数据状态

2026-11-25 已检查以下路径不存在：项目内的 `build`、`bin`、`.gradle`、`logs`、`run/logs`、`run/crash-reports`、`run/saves`、`run/win10-probe-current`、`run-gametest/logs`、`run-gametest/world`，以及原虚拟机存储目录 `E:/MinecraftVMs`。

先前助手的删除调用曾被执行器拦截，之后提供了手动清理命令；本次记录的是后续检查到这些目标均已不存在的事实，不声称助手成功执行了被拦截的命令。**旧测试世界和两台旧虚拟机数据不应被当成可恢复开发的前提；它们未随源码备份。**

仍可能存在忽略的 `run/config`、运行选项和诊断截图；它们不是远端仓库的一部分。新克隆不包含个人配置、ISO、外部工具、存档或虚拟硬盘。启动时重新生成配置，并按实际环境设置路径。不要读取旧文档中的示例路径后直接删除对应目录。

## 恢复开发命令

在仓库根目录使用 Java 17；第一次构建需要联网获取依赖：

```powershell
git status --short
.\gradlew.bat build --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat runQemuMediaIntegrationSelfTest '-PqemuSystemPath=D:/qemu/qemu-system-x86_64.exe' --console=plain
.\gradlew.bat runClient --console=plain
```

将集成自测中的 QEMU 路径替换成本机实际安装。该测试使用临时磁盘，`runGameTestServer` 使用独立的 `run-gametest`，不要拿真实用户存档代替测试环境。构建输出为 `build/libs/minevm-1.1.0-dev.jar`。

历史环境发生过 ForgeGradle 远端连通性探测阻塞：先检查网络和代理；仅在依赖已缓存时才考虑报告里记录的临时离线验证方式，不要把跳过探测写成全局关闭 TLS 校验。

游戏内常用命令：`/minevm help`、`doctor`、`compatibility`、`status`、`access grant/revoke <玩家名>`、`disk backup`、`disk restore confirm`、`disk reset confirm`；客户端统计为 `/minevmclient stats`。磁盘维护要求授权玩家看向电脑并确认关机，恢复和重置会保留旧盘副本，但不是异地备份。

## 代码和文档入口

- `src/main/java/com/example/vm/MineVM.java`：注册与服务器生命周期。
- `block`、`blockentity`、`item`、`floppy`：硬件实体、NBT、接线和软盘物品。
- `computer`、`config`：BIOS/电源/介质操作、权限、命令、配置与启动预检。
- `vm`、`vm/backend/local`、`qmp`：异步 VM 管理、启动参数、磁盘维护和 QMP。
- `screen`、`input`、`client`：VNC、帧缓冲、纹理、沉浸式输入和性能计数。
- `network`：协议版本 3 的服务端授权、订阅、控制租约、帧包和限流；多人两端必须使用匹配版本。
- `src/main/java/com/example/vm/test`、`build.gradle`：自测入口、构建校验和 GameTest。
- [使用与配置](GETTING_STARTED.zh-CN.md)、[兼容性与安装清单](COMPATIBILITY.md)、[测试与多人清单](TESTING.md)：恢复开发时一起阅读。
- `docs/superpowers/plans`：历史设计与实现计划，仅作背景，不覆盖最新代码和本交接的状态。

新会话可使用这段开场要求：

> 继续开发 MineVM。先阅读 docs/HANDOFF.md、docs/COMPATIBILITY.md 和 docs/TESTING.md，再检查 Git 状态和实际源码。不要假设旧存档、虚拟机或构建产物仍存在；不要把实验档案当作安装已验证。先重新构建测试，再从 Windows 95 最终硬盘启动回归开始。
