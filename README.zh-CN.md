# Astrolabe Protocol

[English](README.md) | 简体中文

Astrolabe Protocol 定义 Astrolabe Host 与各平台 Runtime SDK 共享的平台无关 Wire Contract。

## 内容

- `AstrolabeProtocol` Product 中的 Swift DTO 和强类型协议模型。
- 请求与响应封装、错误、版本协商和帧编解码器。
- `Schemas/` 下带版本的 JSON Schema。
- `Fixtures/` 下用于跨语言验证的有效和无效示例。
- [PROTOCOL-2.0.md](PROTOCOL-2.0.md) 中的 Wire Protocol 2.0 规范。
- [PROTOCOL.md](PROTOCOL.md) 中归档的 Wire Protocol 1.0 文档。

UIKit、Android View、Transport Listener、设备发现、截图、CLI 命令和 MCP Tools 均不属于本仓库。

## 安装

通过 Swift Package Manager 添加 Package：

```swift
.package(
    url: "https://github.com/regulusleow/astrolabe-protocol.git",
    exact: "1.0.0"
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

## 许可证

Astrolabe Protocol 使用 [Apache License 2.0](LICENSE) 许可。
