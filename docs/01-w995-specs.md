# 索尼爱立信 W995 规格详解

## 基本信息

- **型号**: Sony Ericsson W995 (Walkman 系列)
- **发布时间**: 2009 年 Q1
- **外观**: 滑盖设计，物理 T9 键盘 + 方向导航键
- **颜色**: 经典黑、银、红

## Java ME 平台规格

| 参数 | 规格 |
|------|------|
| **Java Configuration** | CLDC 1.1 (Connected Limited Device Configuration) |
| **Java Profile** | MIDP 2.0 (Mobile Information Device Profile) |
| **支持的 JSR** | 见下文详细列表 |
| **最大堆内存** | ~1.5 - 2MB (根据不同区域固件略有差异) |
| **最大 JAR 文件大小** | 通常限制为 1-2MB |

## 连接能力

- **WiFi**: 802.11b/g，支持 WEP, WPA, WPA2-PSK
- **蓝牙**: 2.0 + EDR
- **移动网络**: UMTS/HSDPA (3.5G)，下行可达 7.2Mbps
- **USB**: Mass Storage 模式支持

> 💡 **关键点**: W995 在系统层面已经支持 WiFi，你不需要在 J2ME MIDlet 里做 WiFi 连接管理，MIDlet 只需正常使用 `HttpConnection` API 即可联网。系统会处理连接。

## 显示和输入

- **屏幕尺寸**: 2.6 英寸
- **分辨率**: 240 × 320 像素
- **色彩**: 262,144 色 (18-bit) TFT
- **输入方式**: 物理 T9 键盘 + 方向导航键 + 功能键
- **按键**: 通话键、挂断键、返回键、确认键、音乐控制键独立存在

## 存储

- **内置存储**: 118 MB 用户可用
- **扩展卡**: Memory Stick Micro (M2)，官方支持最大 16GB，实际可以用到更大容量

## 电池

- **型号**: BST-38
- **容量**: 930 mAh
- **待机时间**: 最长 370 小时 (WiFi 持续使用仍然可以撑 1-2 天)
- **通话时间**: 最长 9 小时

## 支持的 Java JSR 列表

W995 支持以下主要 JSR (Java Specification Request):

- **JSR 118**: MIDP 2.0 (核心)
- **JSR 139**: CLDC 1.1 (核心)
- **JSR 185**: JTWI (无线技术，包含 HTTP 联网)
- **JSR 120**: Wireless Messaging API (短信)
- **JSR 135**: Mobile Media API (多媒体播放)
- **JSR 75**: FileConnection and PIM API (访问文件和联系人)
- **JSR 179**: Location API (定位)
- **JSR 205**: SIP API
- **JSR 211**: Content Handler API
- **JSR 226**: Scalable 2D Vector Graphics API

对于我们 MJGA 项目来说，最关键的是 **JSR 185**，它提供了标准的 `HttpConnection` API，可以直接做 HTTP 请求。我们不需要额外的 native 库。

## 固件区域差异

不同地区发售的 W995 在 Java 限制上略有不同：

- **欧版/国际版**: 限制较少，可以直接安装未签名 MIDlet
- **港版/亚太版**: 对某些权限会弹确认框，但不影响使用
- **国行**: 可能对某些 Java 联网有限制，建议刷国际版固件

## 参考链接

- [GSMArena Sony Ericsson W995 官方规格页](https://www.gsmarena.com/sony_ericsson_w995-2674.php)
- [Sony Ericsson W995 Developer Notes (Archive.org)](https://web.archive.org/web/20090601000000/http://developer.sonyericsson.com/)
- [Sony Ericsson W995 Java 能力测试](https://web.archive.org/web/20090701000000/http://developer.sonyericsson.com/getDocument.do?docId=950001)

