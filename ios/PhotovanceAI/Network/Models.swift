import Foundation

struct StatusResponse: Codable {
    let ready: Bool
    let models: [String: ModelInfo]?
    let defaultNegativePrompt: String?

    enum CodingKeys: String, CodingKey {
        case ready
        case models
        case defaultNegativePrompt = "default_negative_prompt"
    }
}

struct ModelInfo: Codable, Identifiable {
    let id: String
    let name: String
    let desc: String?
    let type: String?
}

struct UploadRequest: Codable {
    let data: String
}

struct UploadResponse: Codable {
    let imageId: String?
    let imageUrl: String?
    let width: Int?
    let height: Int?
    let error: String?

    enum CodingKeys: String, CodingKey {
        case imageId = "image_id"
        case imageUrl = "image_url"
        case width
        case height
        case error
    }
}

struct SampleResponse: Codable {
    let imageId: String
    let imageUrl: String
    let width: Int?
    let height: Int?

    enum CodingKeys: String, CodingKey {
        case imageId = "image_id"
        case imageUrl = "image_url"
        case width
        case height
    }
}

struct ChangeDressRequest: Codable {
    let imageId: String
    let prompt: String
    let negativePrompt: String
    let modelKey: String
    let mode: String
    let aspectRatio: String
    let target: String
    let expand: Int
    let keepFace: Bool
    let variations: Int?
    let garmentId: String?

    enum CodingKeys: String, CodingKey {
        case imageId = "image_id"
        case prompt
        case negativePrompt = "negative_prompt"
        case modelKey = "model_key"
        case mode
        case aspectRatio = "aspect_ratio"
        case target
        case expand
        case keepFace = "keep_face"
        case variations
        case garmentId = "garment_id"
    }
}

struct ChangeDressResponse: Codable {
    let success: Bool
    let resultUrl: String?
    let previewUrl: String?
    let elapsed: Double?
    let modelName: String?
    let mode: String?
    let coverage: Double?
    let results: [String]?
    let error: String?

    enum CodingKeys: String, CodingKey {
        case success
        case resultUrl = "result_url"
        case previewUrl = "preview_url"
        case elapsed
        case modelName = "model_name"
        case mode
        case coverage
        case results
        case error
    }
}

struct HistoryItem: Codable, Identifiable {
    let id: String
    let url: String
    let prompt: String
    let model: String?
    let mode: String?
    let elapsed: Double?
}
