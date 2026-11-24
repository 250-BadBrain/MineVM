# MineVM 使用与维护

本开发版面向 Minecraft 1.20.1、Forge 和 Java 17。仍然是一个模组 JAR，外部工具路径集中在 `config/minevm-common.toml`。虚拟硬盘和软盘数据当然仍需独立磁盘空间，不能封装在 JAR 内。

## 配置

首次启动游戏生成配置，关闭游戏后编辑，再重新启动。下面的路径只是示例，需要改成本机实际路径。Windows 路径可用正斜线，避免 TOML 反斜线转义错误。

```toml
[vm]
enableLocalQemu = true
qemuSystemPath = "D:/qemu/qemu-system-x86_64.exe"
qemuImgPath = "D:/qemu/qemu-img.exe"
vmStorageDirectory = "E:/MinecraftVMs"
accelerator = "whpx"
diskSizeGb = 20
guestOperatingSystem = "UBUNTU_SERVER"
isoPath = ""
ubuntuServerIsoPath = ""
ubuntuDesktopIsoPath = ""
freedosIsoPath = "E:/ISOs/FD14LIVE.iso"
windows95IsoPath = "E:/ISOs/windows95.iso"
windows95BootFloppyPath = "E:/ISOs/disk0.img"
windows98IsoPath = ""
windowsXpIsoPath = ""
windowsVistaIsoPath = ""
windows10IsoPath = ""
windows11IsoPath = ""
sharedViewing = true
networkFramesPerSecond = 5
networkBytesPerSecond = 524288
networkFrameCodec = "deflate"
```

没有镜像的条目留空。不要给不同系统共用一个不匹配的 `isoPath`；这是旧版兼容回退配置。启用本地 QEMU 前确认整套 QEMU 安装完整。Win95 的兼容链路还会使用与 `qemu-img.exe` 同目录的 `qemu-io.exe`，不要只单独复制两个 EXE。现代 UEFI 档案还需要对应固件，不能据此认为 Win10/11 已完成支持。

多人服务器仅在服务器配置 QEMU 和镜像，客户端不需要这些路径；所有端都必须安装本次相同版本的模组。服务器运行的老系统仍可能通过 QEMU 用户态网络访问网络，不要拿它处理真实账号、密码或不可信程序。

## 连接与安装

1. 放置机箱、显示器、键盘、鼠标。笔记本具有内置屏幕和输入设备。
2. 持 HDMI 线右键机箱，再右键显示器。持 USB 线先右键已经放置的键盘，再右键机箱；鼠标重复一次。生存模式成功连接会消耗线材。
3. Win95 另放软驱盒，用 USB 线先选软驱，再选机箱。需要重新连接软驱时，先关闭相关虚拟机。
4. 将系统光盘插入机箱；启动软盘插入软驱盒。光盘和软盘可以独立取出，不能把两者当作一个介质。
5. 打开机箱和显示器电源，空手右键屏幕，进入 BIOS 并从安装介质启动。`Ctrl+Alt` 退出控制模式。
6. 按安装程序要求分区、格式化、安装。要求制作启动盘时，使用“空白可写软盘”，不要用只读的启动软盘；要求取出启动软盘时，只取出软驱里的盘。安装光盘需要读取文件时应继续保留。
7. 安装后从硬盘启动，并按兼容性清单检查冷启动、重启、输入和长时间空闲。

软盘右键软驱插入，`Shift+右键` 软驱弹出。空白可写软盘第一次插入时生成独立 UUID，对应存储目录的 `floppies/<UUID>.img`。挖下或移动物品时保留其 NBT 身份；复制同一份带 NBT 的软盘意味着共享同一份镜像，不是复制数据。

键盘、鼠标现在通过方块身份和双向连接验证。挖掉后连接失效，同坐标放新设备不会自动复用旧连接。旧存档中仅记录“已连接”的布尔状态不能证明设备身份，需要重新接线一次。所有者和授权列表保存在电脑 NBT 内，机箱移动时会随物品保留。原所有者可继续管理，OP 可协助维护。

## 诊断与授权

```text
/minevm help
/minevm doctor
/minevm compatibility
/minevm status
/minevmclient stats
/minevm access grant 玩家名
/minevm access revoke 玩家名
```

`doctor` 检查后端配置和各镜像文件是否存在，不代表镜像可引导、版本匹配或安装已验证。它仅允许 OP、服务端控制台或单人世界主人运行，避免把服务器路径诊断公开给其他玩家。

`status`、磁盘维护和授权命令需要所有者或 OP 看向附近的电脑或其显示器。`minevmclient stats` 为本地客户端命令，进入控制模式后也可使用，显示本机累计帧数和纹理上传统计。`status` 在服务端输出 QEMU 进程号、累计 CPU 毫秒、帧处理/发送统计和最近日志尾部。累计 CPU 时间不是瞬时 CPU 百分比；多人客户端的帧处理时间包含解码。

授权在线玩家后，他们可以控制系统；授权不赋予重置硬盘、弹出介质或管理其他授权的权限。默认允许附近玩家观看画面，但同一台电脑同一时间只能有一个控制者。设置 `sharedViewing = false` 可关闭普通旁观者观看，此时仅所有者、授权玩家和 OP 能订阅画面。

## 备份、恢复与重新安装

先在客户机内正常关机，然后在游戏里确保机箱已关闭。不要在安装程序写盘中途强行关电后立即备份。看向电脑，使用：

```text
/minevm disk backup
/minevm disk restore confirm
/minevm disk reset confirm
```

- `backup` 将当前硬盘复制为同目录的 `.backup`。这是一个备份槽，下一次备份会替换它。
- `restore confirm` 从备份槽恢复，并将恢复前的硬盘移动为 `.retained-<UUID>`，不直接删除原盘。
- `reset confirm` 先备份并保留旧盘，再创建全新空白 qcow2，供重新安装。它会更新上述备份槽。
- 同时记录系统档案；恢复时恢复对应档案。UEFI 档案会备份和恢复变量存储，不会把 BIOS 档案错当成 UEFI 档案。
- 运行中或正在开关机的电脑不能维护。后端还会再次检查 QEMU 进程是否存活，不能仅靠方块电源标记绕过。

重置/恢复输出中的路径指向保留副本或备份文件。`.retained-*` 不会自动清理。磁盘很大时需要足够空间；这些同盘副本不是异地备份，不能防止整块宿主机硬盘损坏。需要长期保存时，由服务器管理员另做完整存储目录备份。不要手动同时启动同一个硬盘文件的另一个 QEMU 实例。

本次开发验证只使用临时虚拟机目录，没有重置已有玩家硬盘。Win95、Win98、Vista 的真实安装仍需按 [兼容性表](COMPATIBILITY.md) 测试。
