import Foundation

// MARK: - Request from JS

struct BridgeRequest: Codable {
    let id: String
    let method: String
    let params: [String: AnyCodable]
}

// MARK: - Response to JS

struct BridgeResponse: Codable {
    let id: String
    let type: String
    let success: Bool
    var data: [String: AnyCodable]?
    var error: String?

    static func success(id: String, data: [String: AnyCodable] = [:]) -> BridgeResponse {
        BridgeResponse(id: id, type: "response", success: true, data: data, error: nil)
    }

    static func failure(id: String, error: String) -> BridgeResponse {
        BridgeResponse(id: id, type: "response", success: false, data: nil, error: error)
    }
}

// MARK: - Event to JS

struct BridgeEvent: Codable {
    let type: String
    let event: String
    let data: [String: AnyCodable]

    init(event: String, data: [String: AnyCodable] = [:]) {
        self.type = "event"
        self.event = event
        self.data = data
    }
}

// MARK: - Type-erased Codable wrapper

struct AnyCodable: Codable {
    let value: Any

    init(_ value: Any) {
        self.value = value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            value = NSNull()
        } else if let bool = try? container.decode(Bool.self) {
            value = bool
        } else if let int = try? container.decode(Int.self) {
            value = int
        } else if let double = try? container.decode(Double.self) {
            value = double
        } else if let string = try? container.decode(String.self) {
            value = string
        } else if let dict = try? container.decode([String: AnyCodable].self) {
            value = dict.mapValues { $0.value }
        } else if let array = try? container.decode([AnyCodable].self) {
            value = array.map { $0.value }
        } else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported type")
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch value {
        case is NSNull:
            try container.encodeNil()
        case let bool as Bool:
            try container.encode(bool)
        case let int as Int:
            try container.encode(int)
        case let double as Double:
            try container.encode(double)
        case let string as String:
            try container.encode(string)
        case let dict as [String: Any]:
            try container.encode(dict.mapValues { AnyCodable($0) })
        case let array as [Any]:
            try container.encode(array.map { AnyCodable($0) })
        default:
            try container.encodeNil()
        }
    }
}
