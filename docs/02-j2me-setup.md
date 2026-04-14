# J2ME 开发环境搭建指南

## 概述

J2ME 开发需要一套比较古老的开发工具，因为 Oracle 在 2013 年左右就停用了 Java ME。好在我们可以从互联网档案馆找到这些工具。

## 需要下载的工具

### 1. NetBeans IDE 6.9.1

这是最后一个对 J2ME 支持比较好的 NetBeans 版本，新版本不支持 Java ME 插件了。

- **下载地址 (Archive.org)**: https://web.archive.org/web/20110723070513/http://download.netbeans.org/netbeans/6.9.1/final/
- **推荐包**: `netbeans-6.9.1-ml-linux.sh` (Linux) 或者对应 Windows/Mac 版本
- **大小**: ~150MB

### 2. Java ME SDK 3.0

Oracle 官方的 Java ME 开发包。

- **下载地址**: https://web.archive.org/web/20100209031122/http://java.sun.com/javame/downloads/sdk30.jsp
- **Linux 版本**: `sun_java_me_sdk-3_0-linux.bin`
- **Windows 版本**: `sun_java_me_sdk-3_0-windows.exe`

### 3. Sony Ericsson Java ME 开发包 (可选但推荐)

索尼爱立信提供的设备模拟器和特定 API。

- **Sony Ericsson Developer World 设备包**: https://web.archive.org/web/20090501000000/http://developer.sonyericsson.com/site/global/docstools/java/p_java.jsp
- **W995 设备配置**: 包中已经包含，可以直接选择 W995 作为目标设备模拟器

## 安装步骤 (Linux)

### 1. 安装兼容的 JDK

JDK 6/7 都可以兼容，OpenJDK 就可以：

```bash
# Ubuntu/Debian
sudo apt-get install openjdk-7-jdk
```

> 注意: 最新的 JDK 版本可能无法兼容旧版 NetBeans，推荐使用 JDK 7。

### 2. 安装 NetBeans

```bash
chmod +x netbeans-6.9.1-ml-linux.sh
./netbeans-6.9.1-ml-linux.sh
```

跟随安装向导完成即可。

### 3. 安装 Java ME SDK 3.0

```bash
chmod +x sun_java_me_sdk-3_0-linux.bin
./sun_java_me_sdk-3_0-linux.bin
```

### 4. 在 NetBeans 中启用 Java ME

1. 打开 NetBeans → Tools → Plugins
2. 找到 "Java ME SDK" 插件并启用
3. 设置 Java ME SDK 安装路径
4. 重启 NetBeans

### 5. 添加索尼爱立信设备包 (可选)

解压索尼爱立信开发包到 NetBeans 的设备目录，就可以在新建项目时选择 W995 作为目标设备。

## 替代方案: 轻量级命令行构建

如果你不想安装大体积的 NetBeans，也可以使用命令行工具直接编译：

### 使用 javac + Ant

```
ant clean jar
```

项目可以自带 build.xml，这样任何人都可以不用 IDE 直接编译。

### 需要的工具:

- JDK 6 或 7
- Apache Ant 1.8.x
- J2ME WTK (Wireless ToolKit)

## 模拟器测试

安装完成后，可以在 NetBeans 中选择 W995 模拟器运行和调试你的 MIDlet，模拟器可以模拟网络连接。

## 安装到真机

编译得到 `JAD` 和 `JAR` 文件后：

1. 通过蓝牙或存储卡把文件传到手机
2. 在手机文件管理器中点击 JAD 文件安装
3. 允许安装，完成后就可以在应用列表找到 MJGA
4. 首次联网运行时会弹权限确认，选择允许即可

## 代码签名

- 对于个人使用，不需要代码签名，只是每次联网会弹确认框
- 如果需要去掉确认框，可以用自由开发者证书签名（索尼爱立信当时提供免费证书申请，现在只能自签名）
- 部分手机需要修改 `jad` 文件属性添加权限声明

## 工具下载镜像

由于 Archive.org 下载可能较慢，可以参考项目 [tools.md](./tools.md) 整理的国内镜像。

