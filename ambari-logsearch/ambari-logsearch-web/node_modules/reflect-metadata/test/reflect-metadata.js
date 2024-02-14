// 4.1.2 Reflect.metadata ( metadataKey, metadataValue )
// https://rbuckton.github.io/reflect-metadata/#reflect.metadata
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.metadata", function () {
    it("ReturnsDecoratorFunction", function () {
        var result = Reflect.metadata("key", "value");
        chai_1.assert.equal(typeof result, "function");
    });
    it("DecoratorThrowsWithInvalidTargetWithTargetKey", function () {
        var decorator = Reflect.metadata("key", "value");
        chai_1.assert.throws(function () { return decorator(undefined, "name"); }, TypeError);
    });
    it("DecoratorThrowsWithInvalidTargetKey", function () {
        var decorator = Reflect.metadata("key", "value");
        chai_1.assert.throws(function () { return decorator({}, {}); }, TypeError);
    });
    it("OnTargetWithoutTargetKey", function () {
        var decorator = Reflect.metadata("key", "value");
        var target = function () { };
        decorator(target);
        var result = Reflect.hasOwnMetadata("key", target, undefined);
        chai_1.assert.equal(result, true);
    });
    it("OnTargetWithTargetKey", function () {
        var decorator = Reflect.metadata("key", "value");
        var target = {};
        decorator(target, "name");
        var result = Reflect.hasOwnMetadata("key", target, "name");
        chai_1.assert.equal(result, true);
    });
});
//# sourceMappingURL=reflect-metadata.js.map