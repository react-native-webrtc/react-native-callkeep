// swift-tools-version: 6.0

import Foundation
import PackageDescription

// React Native resolves these paths through build/generated/autolinking/libs.
let usedRootEntries: Set<String> = ["Package.swift", "ios", "include"]
let unusedRootEntries = try! FileManager.default.contentsOfDirectory(atPath: Context.packageDirectory)
    .filter { !usedRootEntries.contains($0) }
    .sorted()

let package = Package(
    name: "RNCallKeep",
    platforms: [.iOS("15.1")],
    products: [
        .library(name: "RNCallKeep", targets: ["RNCallKeep"]),
    ],
    dependencies: [
        .package(name: "ReactNative", path: "../../../../xcframeworks"),
        .package(name: "React-GeneratedCode", path: "../../../ios"),
    ],
    targets: [
        .target(
            name: "RNCallKeep",
            dependencies: [
                .product(name: "ReactHeaders", package: "ReactNative"),
                .product(name: "ReactNativeHeaders", package: "ReactNative"),
                .product(name: "ReactNativeDependenciesHeaders", package: "ReactNative"),
                .product(name: "ReactAppHeaders", package: "React-GeneratedCode"),
            ],
            path: ".",
            exclude: unusedRootEntries + ["ios/RNCallKeep.xcodeproj"],
            sources: ["ios/RNCallKeep"],
            resources: [.copy("ios/PrivacyInfo.xcprivacy")],
            publicHeadersPath: "include",
            cSettings: [.define("RCT_NEW_ARCH_ENABLED", to: "1")],
            cxxSettings: [.define("RCT_NEW_ARCH_ENABLED", to: "1")],
            linkerSettings: [
                .linkedFramework("UIKit"),
                .linkedFramework("Foundation"),
                .linkedFramework("CallKit"),
                .linkedFramework("Intents"),
                .linkedFramework("AVFoundation"),
            ]
        ),
    ],
    cxxLanguageStandard: .cxx20
)
