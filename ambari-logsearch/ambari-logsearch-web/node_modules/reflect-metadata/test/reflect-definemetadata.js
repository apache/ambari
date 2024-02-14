// 4.1.2 Reflect.defineMetadata ( metadataKey, metadataValue, target, propertyKey )
// https://rbuckton.github.io/reflect-metadata/#reflect.definemetadata
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.defineMetadata", function () {
    it("InvalidTarget", function () {
        chai_1.assert.throws(function () { return Reflect.defineMetadata("key", "value", undefined, undefined); }, TypeError);
    });
    it("ValidTargetWithoutTargetKey", function () {
        chai_1.assert.doesNotThrow(function () { return Reflect.defineMetadata("key", "value", {}, undefined); });
    });
    it("ValidTargetWithTargetKey", function () {
        chai_1.assert.doesNotThrow(function () { return Reflect.defineMetadata("key", "value", {}, "name"); });
    });
});
//# sourceMappingURL=reflect-definemetadata.js.map