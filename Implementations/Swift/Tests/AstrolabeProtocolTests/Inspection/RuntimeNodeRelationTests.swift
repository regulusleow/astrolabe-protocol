import AstrolabeProtocol
import XCTest

final class RuntimeNodeRelationTests: XCTestCase {
    func testNamespacedNodeRelationRoundTrips() throws {
        let relation = RuntimeNodeRelation(
            type: try RuntimeNamespacedIdentifier(rawValue: "vendor.graph.related"),
            sourceNodeID: try RuntimeOpaqueIdentifier(rawValue: "node:view:1"),
            targetNodeID: try RuntimeOpaqueIdentifier(rawValue: "node:layer:1"),
            extensions: try RuntimeExtensionMap(
                values: ["vendor.graph.confidence": .integer(1)]
            )
        )
        let codec = RuntimeMessageCodec()

        XCTAssertEqual(
            relation,
            try codec.decode(RuntimeNodeRelation.self, from: codec.encode(relation))
        )
        XCTAssertEqual(RuntimeCapability.uiGraphRelations.rawValue, "uiGraphRelations")
    }
}
