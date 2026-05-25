// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "flutter_beacon_fence",
    platforms: [
        .iOS("14.0"),
    ],
    products: [
        .library(name: "flutter-beacon-fence", targets: ["flutter_beacon_fence"]),
    ],
    targets: [
        .target(
            name: "flutter_beacon_fence",
            dependencies: [],
            resources: [
                .process("PrivacyInfo.xcprivacy"),
            ],
        ),
    ]
)
