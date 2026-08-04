# Astrolabe Protocol

[English](README.md) | 简体中文

Astrolabe Protocol 定义 Astrolabe Host 与各平台 Runtime SDK 共享的平台无关 Wire Contract。

## 内容

- `Contract/` 下版本化的规范 Wire Contract。
- `Implementations/` 下地位对等的 Swift 与 Kotlin 实现。
- 请求与响应封装、错误、版本协商和帧编解码器。
- 每个 Contract 版本下的 JSON Schema 与跨语言 Fixture。
- [Contract/v2/PROTOCOL.md](Contract/v2/PROTOCOL.md) 中的 Wire Protocol 2.0 规范。
- [Contract/v1/PROTOCOL.md](Contract/v1/PROTOCOL.md) 中归档的 Wire Protocol 1.0 文档。

UIKit、Android View、Transport Listener、设备发现、截图、CLI 命令和 MCP Tools 均不属于本仓库。

## 安装

通过 Swift Package Manager 添加 Package：

```swift
.package(
    url: "https://github.com/regulusleow/astrolabe-protocol.git",
    exact: "2.0.0"
)
```

依赖 `AstrolabeProtocol` Product，并通过以下方式导入：

```swift
import AstrolabeProtocol
```

## Wire 格式

每条消息由一个使用网络字节序的四字节无符号 Payload 长度和随后的 UTF-8 JSON 对象组成。当前
Wire Protocol 版本为 `2.0`。

Swift 类型只是该协议的一种实现。其他语言的实现应以规范、Schema、Fixture 和文档约定的 Wire
行为作为兼容性事实源。

## 仓库目录

```text
Contract/          规范协议文档、Schema 和 Fixture
Implementations/   地位对等的 Swift 与 Kotlin 协议实现
Tooling/           Contract 校验、发布自动化和测试
```

根目录的 SwiftPM、Gradle 和 npm 清单只是各生态的标准入口，通过显式路径映射到实现与 Tooling，
不表示任一语言是仓库的主实现。

双端实现使用相同的概念分域：`Core`、`Attributes`、`Framing`、`Messaging`、`Negotiation`、
`Inspection` 和 `Patching`。Swift 在 Inspection 内继续按 `Application`、`Hierarchy` 和
`NodeDetail` 建立子目录；Kotlin 保持公开 package `dev.astrolabe.protocol` 扁平，不为追求物理目录
对称而制造新的 package 边界。

## 开发

安装协议校验器并运行全部检查：

```bash
npm ci
npm test
swift test --parallel
swift build -c release
```

校验流程会编译全部 Draft 2020-12 Schema，检查有效和无效 Fixture，执行 JSON Schema 无法表达的
语义规则，并验证 Swift DTO 对相同 Payload 的接受和拒绝行为保持一致。

`Contract/v2/manifest.json` 通过 `fixtureRoots` 声明递归 Fixture 根目录。根目录下的每个 JSON
Fixture 都必须在 `cases` 中且仅有一条注册记录。

## 许可证

Astrolabe Protocol 使用 [Apache License 2.0](LICENSE) 许可。
