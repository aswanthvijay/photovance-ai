import Foundation
import UIKit

class ApiClient: ObservableObject {
    static let shared = ApiClient()
    private let prefsKey = "photovance_base_url"
    static let defaultUrl = "http://192.168.1.230:7860/"

    @Published var baseUrl: String = defaultUrl

    private lazy var session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 180
        config.timeoutIntervalForResource = 300
        return URLSession(configuration: config)
    }()

    func initialize() {
        if let saved = UserDefaults.standard.string(forKey: prefsKey), !saved.isEmpty {
            baseUrl = saved.hasSuffix("/") ? saved : "\(saved)/"
        }
    }

    func saveBaseUrl(_ newUrl: String) {
        let formatted = newUrl.hasSuffix("/") ? newUrl : "\(newUrl)/"
        baseUrl = formatted
        UserDefaults.standard.set(formatted, forKey: prefsKey)
    }

    func getFullUrl(_ relativeUrl: String) -> URL? {
        let clean = relativeUrl.hasPrefix("/") ? String(relativeUrl.dropFirst()) : relativeUrl
        return URL(string: "\(baseUrl)\(clean)")
    }

    func getStatus() async throws -> StatusResponse {
        guard let url = URL(string: "\(baseUrl)api/status") else {
            throw URLError(.badURL)
        }
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode(StatusResponse.self, from: data)
    }

    func getSample() async throws -> SampleResponse {
        guard let url = URL(string: "\(baseUrl)api/sample") else {
            throw URLError(.badURL)
        }
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode(SampleResponse.self, from: data)
    }

    func uploadImage(image: UIImage) async throws -> UploadResponse {
        // Downscale image to max 1536px to prevent OOM and ensure sub-second upload
        let optimized = image.resized(maxDimension: 1536)
        guard let jpegData = optimized.jpegData(compressionQuality: 0.92) else {
            throw URLError(.cannotDecodeContentData)
        }
        let base64String = jpegData.base64EncodedString()
        let dataUri = "data:image/jpeg;base64,\(base64String)"

        guard let url = URL(string: "\(baseUrl)api/upload") else {
            throw URLError(.badURL)
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(UploadRequest(data: dataUri))
        request.timeoutInterval = 60

        let (data, _) = try await session.data(for: request)
        return try JSONDecoder().decode(UploadResponse.self, from: data)
    }

    func changeDress(request: ChangeDressRequest) async throws -> ChangeDressResponse {
        guard let url = URL(string: "\(baseUrl)api/change-dress") else {
            throw URLError(.badURL)
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONEncoder().encode(request)
        req.timeoutInterval = 180

        let (data, _) = try await session.data(for: req)
        return try JSONDecoder().decode(ChangeDressResponse.self, from: data)
    }
}

// UIImage Downscale Extension
extension UIImage {
    func resized(maxDimension: CGFloat) -> UIImage {
        let currentMax = max(size.width, size.height)
        guard currentMax > maxDimension else { return self }
        let scale = maxDimension / currentMax
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: newSize, format: format).image { _ in
            self.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
