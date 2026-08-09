// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "astrolabe-protocol",
    platforms: [
        .iOS(.v15),
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "AstrolabeProtocol",
            targets: ["AstrolabeProtocol"]
        )
    ],
    targets: [
        .target(
            name: "AstrolabeProtocol",
            path: "Implementations/Swift/Sources/AstrolabeProtocol"
        ),
        .testTarget(
            name: "AstrolabeProtocolTests",
            dependencies: ["AstrolabeProtocol"],
            path: "Implementations/Swift/Tests/AstrolabeProtocolTests"
        )
    ]
)
