# MJGA 项目开发规则

## 技术栈
- **Java**: JDK 6/7
- **IDE**: Trae IDE + Ant 构建
- **目标**: W995 (MIDP 2.0 + CLDC 1.1)
- **内存**: 1-2MB 堆限制

## 代码规范
- **类名**: 帕斯卡 (`HttpClient`)
- **方法/变量**: 驼峰 (`sendMessage()`)
- **常量**: 全大写下划线 (`MAX_RETRY`)
- **缩进**: 4空格, UTF-8

## J2ME 规范
- MIDlet 继承 `javax.microedition.midlet.MIDlet`
- UI 用 `javax.microedition.lcdui`
- 网络用 `javax.microedition.io.HttpConnection`
- UI 操作须在主线程
- 网络异常捕获 `IOException`

## 项目结构
```
src/com/mjga/
├── midlet/MJGAMidlet.java
├── ui/, network/, util/
res/, build.xml, jad.xml
```

## API 限制
- 可用: lcdui, midlet, io
- 禁用: Thread.stop(), 反射, 大型库

## 性能
- 内存: `StringBuffer`, 对象池, 及时释放
- 网络: 连接复用, 超时, 重试(≤3次)

## 构建
```bash
ant clean jar  # 开发
ant dist       # 发布
```
输出: `dist/MJGA.jad`, `dist/MJGA.jar`
