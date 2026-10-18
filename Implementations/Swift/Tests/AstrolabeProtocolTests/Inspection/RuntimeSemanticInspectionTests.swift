import AstrolabeProtocol
import Foundation
import XCTest

final class RuntimeSemanticInspectionTests: XCTestCase {
    private let codec = RuntimeMessageCodec()

    func testSemanticRequestPreservesExplicitWindow() throws {
        let source = Data(#"{"semanticCapture":{"windowID":"window:1"}}"#.utf8)
        let request = try codec.decode(RuntimeHierarchySnapshotParameters.self, from: source)
        let encoded = try codec.encode(request)
        XCTAssertEqual(try JSONSerialization.jsonObject(with: encoded) as? NSDictionary,
                       try JSONSerialization.jsonObject(with: source) as? NSDictionary)
    }

    func testDetailRequestPreservesFrozenSnapshotIdentity() throws {
        let source = Data(#"{"nodeID":"semantic:1","snapshotID":"snapshot:1"}"#.utf8)
        let request = try codec.decode(RuntimeNodeDetailParameters.self, from: source)
        let encoded = try codec.encode(request)
        XCTAssertEqual(try JSONSerialization.jsonObject(with: encoded) as? NSDictionary,
                       try JSONSerialization.jsonObject(with: source) as? NSDictionary)
    }

    func testSemanticCaptureKeepsUnknownFrameAndPartialFacts() throws {
        let source = try hierarchy(capture: capture())
        let payload = try codec.decode(RuntimeHierarchySnapshotPayload.self, from: source)
        XCTAssertEqual(try JSONSerialization.jsonObject(with: codec.encode(payload)) as? NSDictionary,
                       try JSONSerialization.jsonObject(with: source) as? NSDictionary)
    }

    func testSemanticCaptureRejectsInvalidIdentityTreeCoordinatesAndBudgets() throws {
        var invalid: [[String: Any]] = []
        var value = capture(); value["source"] = "unnamespaced"; invalid.append(value)
        value = capture(); value["visitedNodeCount"] = -1; invalid.append(value)
        value = capture(); value["visitedNodeCount"] = 2001; invalid.append(value)
        value = capture(); value["visitedNodeCount"] = 0; invalid.append(value)
        value = capture(); value["roots"] = [node(), node()]; invalid.append(value)
        var badNode = node(); badNode["parentID"] = "missing"
        value = capture(); value["roots"] = [badNode]; invalid.append(value)
        badNode = node(); badNode["siblingIndex"] = 1
        value = capture(); value["roots"] = [badNode]; invalid.append(value)
        badNode = node(); badNode["frameInScreen"] = ["x": 0, "y": 0, "width": -1, "height": 1, "coordinateSpace": "screen", "unit": "logical"]
        value = capture(); value["roots"] = [badNode]; invalid.append(value)
        badNode = node(); badNode["frameInScreen"] = ["x": 0, "y": 0, "width": 1, "height": 1, "coordinateSpace": "local", "unit": "logical"]
        value = capture(); value["roots"] = [badNode]; invalid.append(value)
        value = capture(); value["issues"] = [["code": "bad"]]; invalid.append(value)
        value = capture(); value["issues"] = [["code": "inspection.limit", "path": String(repeating: "a", count: 1025)]]; invalid.append(value)
        badNode = node(); badNode["accessibility"] = ["element": true, "identifier": NSNull(), "label": NSNull(), "value": NSNull(), "hint": NSNull(), "traits": ["button", "button"]]
        value = capture(); value["roots"] = [badNode]; invalid.append(value)
        for (index, candidate) in invalid.enumerated() {
            XCTAssertThrowsError(try codec.decode(RuntimeHierarchySnapshotPayload.self,
                                                 from: hierarchy(capture: candidate)), "invalid case \(index)")
        }
    }

    func testFrozenDetailsPreserveTypedCandidateAndRejectInvalidTime() throws {
        let context: [String: Any] = [
            "snapshotID": "snapshot:1", "capturedAtUnixTime": 123,
            "source": "ios.swiftui", "availability": "partial", "issues": [], "wasTruncated": false,
            "textCandidates": [["source": "ios.swiftui.attachmentsStorage", "path": "attachment.text", "issues": [],
                                "fields": [["path": "font.size", "attribute": ["identifier": "common.text.fontSize", "value": ["type": "measurement", "value": ["value": 28, "unit": "scaledLogical"]]]]]]]
        ]
        let source = try JSONSerialization.data(withJSONObject: ["nodeID": "semantic:1", "sections": [], "semanticContext": context])
        let payload = try codec.decode(RuntimeNodeDetailPayload.self, from: source)
        XCTAssertEqual(try JSONSerialization.jsonObject(with: codec.encode(payload)) as? NSDictionary,
                       try JSONSerialization.jsonObject(with: source) as? NSDictionary)
        var badContext = context; badContext["capturedAtUnixTime"] = -1
        XCTAssertThrowsError(try codec.decode(RuntimeNodeDetailPayload.self, from:
            JSONSerialization.data(withJSONObject: ["nodeID": "semantic:1", "sections": [], "semanticContext": badContext])))
    }

    func testOldRenderingGeometryRemainsRequired() throws {
        var source = try JSONSerialization.jsonObject(with: hierarchy(capture: nil)) as! [String: Any]
        var roots = source["roots"] as! [[String: Any]]
        roots[0]["geometry"] = NSNull(); source["roots"] = roots
        XCTAssertThrowsError(try codec.decode(RuntimeHierarchySnapshotPayload.self, from: JSONSerialization.data(withJSONObject: source)))
    }

    func testInspectionTargetsDecodeAndRejectMalformedKnownExtension() throws {
        let source = Data(#"{"common.inspectionTargets":[{"windowID":"window:1","runtimeType":{"name":"UIWindow","ancestors":[]}}]}"#.utf8)
        let extensions = try codec.decode(RuntimeExtensionMap.self, from: source)
        XCTAssertEqual(try extensions.inspectionTargets()?.first?.windowID.rawValue, "window:1")
        let windows = try XCTUnwrap(extensions.inspectionTargets())
        XCTAssertEqual(try RuntimeExtensionMap(inspectionTargets: windows), extensions)
        XCTAssertThrowsError(try codec.decode(RuntimeExtensionMap.self, from: Data(#"{"common.inspectionTargets":"opaque"}"#.utf8)))
    }

    func testContainerCountAndExactDepthBoundary() throws {
        var nodes = (0..<2000).map { index -> [String: Any] in
            var value = node(); value["nodeID"] = "container:\(index)"; value["siblingIndex"] = index
            return value
        }
        var value = capture(); value["roots"] = nodes; value["visitedNodeCount"] = 2000
        XCTAssertNoThrow(try codec.decode(RuntimeHierarchySnapshotPayload.self, from: hierarchy(capture: value)))
        nodes.append(nodes[0]); value["roots"] = nodes
        XCTAssertThrowsError(try codec.decode(RuntimeHierarchySnapshotPayload.self, from: hierarchy(capture: value)))
        var child = node()
        child["nodeID"] = "depth:19"; child["parentID"] = "depth:18"
        for depth in (0..<19).reversed() {
            var parent = node(); parent["nodeID"] = "depth:\(depth)"
            parent["parentID"] = depth == 0 ? NSNull() : "depth:\(depth - 1)" as Any
            parent["children"] = [child]; child = parent
        }
        value = capture(); value["roots"] = [child]; value["visitedNodeCount"] = 20
        XCTAssertNoThrow(try codec.decode(RuntimeHierarchySnapshotPayload.self, from: hierarchy(capture: value)))
        child["parentID"] = "extra"
        var root = node(); root["nodeID"] = "extra"; root["children"] = [child]
        value["roots"] = [root]; value["visitedNodeCount"] = 21
        XCTAssertThrowsError(try codec.decode(RuntimeHierarchySnapshotPayload.self, from: hierarchy(capture: value)))
    }

    func testPathUsesUTF8BudgetAndTextAttributesRejectInvalidValues() throws {
        XCTAssertThrowsError(try RuntimeInspectionIssue(code: RuntimeNamespacedIdentifier(rawValue: "inspection.limit"), path: String(repeating: "界", count: 342)))
        XCTAssertNoThrow(try RuntimeInspectionIssue(code: RuntimeNamespacedIdentifier(rawValue: "inspection.limit"), path: String(repeating: "a", count: 1024)))
        let identifier = try RuntimeAttributeIdentifier(rawValue: "common.text.content")
        XCTAssertThrowsError(try RuntimeSemanticTextField(path: "text", attribute: RuntimeAttribute(identifier: identifier, value: .string(String(repeating: "界", count: 5462)))))
        XCTAssertThrowsError(try RuntimeSemanticTextField(path: "color", attribute: RuntimeAttribute(identifier: identifier, value: .color(RuntimeColor(colorSpace: "sRGB", red: 1.01, green: 0, blue: 0, alpha: 1)))))
        XCTAssertThrowsError(try RuntimeSemanticTextField(path: "size", attribute: RuntimeAttribute(identifier: identifier, value: .measurement(RuntimeMeasurement(value: .infinity, unit: .scaledLogical)))))
        XCTAssertThrowsError(try RuntimeSemanticTextField(path: "extra", attribute: RuntimeAttribute(identifier: identifier, value: .object(["text": .string(String(repeating: "x", count: 16385))]))))
        XCTAssertThrowsError(try RuntimeSemanticTextField(path: "frame", attribute: RuntimeAttribute(identifier: identifier, value: .rect(RuntimeCoordinateRect(x: 0, y: 0, width: -1, height: 1, coordinateSpace: .local, unit: .logical)))))
    }

    func testOldAndEmptySemanticRequestsRemainDistinct() throws {
        let old = try codec.encode(RuntimeHierarchySnapshotParameters())
        let semantic = try codec.encode(RuntimeHierarchySnapshotParameters(semanticCapture: RuntimeSemanticCaptureOptions()))
        XCTAssertEqual(String(decoding: old, as: UTF8.self), "{}")
        XCTAssertEqual(String(decoding: semantic, as: UTF8.self), #"{"semanticCapture":{}}"#)
        XCTAssertEqual(RuntimeCapability.semanticInspection.rawValue, "semanticInspection")
    }

    private func node() -> [String: Any] {
        ["nodeID": "semantic:1", "parentID": NSNull(), "siblingIndex": 0, "role": "text",
         "runtimeType": ["name": "AXElement", "ancestors": []], "availableDetailCategories": ["common.text"], "children": []]
    }

    private func capture() -> [String: Any] {
        ["windowID": "window:1", "source": "ios.accessibility", "availability": "partial",
         "issues": [["code": "inspection.unsupportedLayout"]], "visitedNodeCount": 1,
         "wasTruncated": false, "roots": [node()]]
    }

    private func hierarchy(capture: [String: Any]?) throws -> Data {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        let fixture = try Data(contentsOf: root.appendingPathComponent("Contract/v2/Fixtures/valid/hierarchy-snapshot-response.json"))
        var payload = (try JSONSerialization.jsonObject(with: fixture) as! [String: Any])["payload"] as! [String: Any]
        payload["semanticCapture"] = capture
        return try JSONSerialization.data(withJSONObject: payload)
    }
}
