/*
 * Copyright 2026 adalseno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import AVFoundation
@preconcurrency import Shared
import UIKit
import VisionKit

/// iOS barcode scanner, built on VisionKit's DataScannerViewController (iOS 16+, A12 or newer).
/// Implements the Kotlin `IosScannerBridge` interface declared in shared/src/iosMain.
// @unchecked Sendable: all mutable state is touched only on the main actor.
final class VisionKitScanner: NSObject, IosScannerBridge, @unchecked Sendable {
    /// Keeps the delegate alive while the scanner is on screen (the delegate property is weak).
    @MainActor private var activeSession: ScanSession?

    func scan(callback: IosScanCallback) {
        Task { @MainActor in self.start(callback) }
    }

    @MainActor
    private func start(_ callback: IosScanCallback) {
        guard DataScannerViewController.isSupported else {
            callback.onNotSupported()
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            present(callback)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                Task { @MainActor in
                    if granted { self.present(callback) } else { callback.onPermissionDenied() }
                }
            }
        default:
            callback.onPermissionDenied()
        }
    }

    @MainActor
    private func present(_ callback: IosScanCallback) {
        // isAvailable is false when camera access is denied or restricted (e.g. Screen Time).
        guard DataScannerViewController.isAvailable else {
            callback.onPermissionDenied()
            return
        }
        guard let presenter = Self.topViewController() else {
            callback.onFailed(message: "No window to present the scanner from")
            return
        }

        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.ean13, .ean8, .upce])],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isHighlightingEnabled: true
        )
        let session = ScanSession(callback: callback)
        session.onFinish = { [weak self] in self?.activeSession = nil }
        session.scanner = scanner
        scanner.delegate = session
        activeSession = session

        scanner.navigationItem.leftBarButtonItem = UIBarButtonItem(
            systemItem: .cancel,
            primaryAction: UIAction { _ in session.finish(.cancelled) }
        )
        let navigation = UINavigationController(rootViewController: scanner)
        navigation.modalPresentationStyle = .fullScreen

        presenter.present(navigation, animated: true) {
            do {
                try scanner.startScanning()
            } catch {
                session.finish(.failed(error.localizedDescription))
            }
        }
    }

    @MainActor
    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let windows = scenes.flatMap { $0.windows }
        var top = (windows.first { $0.isKeyWindow } ?? windows.first)?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

@MainActor
private final class ScanSession: NSObject, DataScannerViewControllerDelegate {
    enum Outcome {
        case scanned(String)
        case cancelled
        case failed(String)
    }

    let callback: IosScanCallback
    weak var scanner: DataScannerViewController?
    var onFinish: (() -> Void)?
    private var finished = false

    init(callback: IosScanCallback) {
        self.callback = callback
    }

    func dataScanner(
        _ dataScanner: DataScannerViewController,
        didAdd addedItems: [RecognizedItem],
        allItems: [RecognizedItem]
    ) {
        for item in addedItems {
            if case .barcode(let barcode) = item,
               let value = barcode.payloadStringValue,
               !value.isEmpty {
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                finish(.scanned(value))
                return
            }
        }
    }

    func dataScanner(
        _ dataScanner: DataScannerViewController,
        becameUnavailableWithError error: DataScannerViewController.ScanningUnavailable
    ) {
        finish(.failed(error.localizedDescription))
    }

    /// Dismisses the scanner, then reports the outcome to Kotlin exactly once.
    func finish(_ outcome: Outcome) {
        guard !finished else { return }
        finished = true
        scanner?.stopScanning()

        let deliver = { [callback, onFinish] in
            switch outcome {
            case .scanned(let code): callback.onScanned(code: code)
            case .cancelled: callback.onCancelled()
            case .failed(let message): callback.onFailed(message: message)
            }
            onFinish?()
        }
        if let navigation = scanner?.navigationController, navigation.presentingViewController != nil {
            navigation.dismiss(animated: true, completion: deliver)
        } else {
            deliver()
        }
    }
}
