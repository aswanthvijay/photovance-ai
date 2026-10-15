import Foundation
import UIKit

public enum IosDeviceTier {
    case flagshipNpu   // A17 Pro / A18 / A18 Pro (iPhone 15 Pro, 16 series), >= 7.5GB RAM
    case standardCloud // All other iOS devices (routes to Cloud 8K GPU)
}

public struct IosDeviceSpec {
    public let tier: IosDeviceTier
    public let modelIdentifier: String
    public let marketingName: String
    public let ramGigabytes: Double
    public let supportsLocalNpuStudio: BooleanLiteralType
}

public class DeviceCapabilityDetector {
    public static let shared = DeviceCapabilityDetector()

    public let currentSpec: IosDeviceSpec

    private init() {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }

        let ramBytes = ProcessInfo.processInfo.physicalMemory
        let ramGb = Double(ramBytes) / (1024.0 * 1024.0 * 1024.0)

        // Flagship A17 Pro / A18 identifiers:
        // iPhone16,1 = iPhone 15 Pro
        // iPhone16,2 = iPhone 15 Pro Max
        // iPhone17,1 = iPhone 16 Pro
        // iPhone17,2 = iPhone 16 Pro Max
        // iPhone17,3 = iPhone 16
        // iPhone17,4 = iPhone 16 Plus
        let isA17OrA18 = identifier.hasPrefix("iPhone16,") || identifier.hasPrefix("iPhone17,")
        let hasFlagshipRam = ramGb >= 7.2 // 8GB RAM iPhones report ~7.5 - 7.8 GB usable

        let isFlagship = isA17OrA18 || hasFlagshipRam

        let marketingName: String = {
            switch identifier {
            case "iPhone16,1": return "iPhone 15 Pro (A17 Pro)"
            case "iPhone16,2": return "iPhone 15 Pro Max (A17 Pro)"
            case "iPhone17,1": return "iPhone 16 Pro (A18 Pro)"
            case "iPhone17,2": return "iPhone 16 Pro Max (A18 Pro)"
            case "iPhone17,3": return "iPhone 16 (A18)"
            case "iPhone17,4": return "iPhone 16 Plus (A18)"
            default: return UIDevice.current.model
            }
        }()

        currentSpec = IosDeviceSpec(
            tier: isFlagship ? .flagshipNpu : .standardCloud,
            modelIdentifier: identifier,
            marketingName: marketingName,
            ramGigabytes: (ramGb * 10).rounded() / 10,
            supportsLocalNpuStudio: isFlagship
        )
    }
}
