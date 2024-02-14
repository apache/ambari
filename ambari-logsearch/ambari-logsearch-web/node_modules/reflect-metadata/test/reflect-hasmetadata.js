// 4.1.4 Reflect.hasMetadata ( metadataKey, target [, propertyKey] )
// https://rbuckton.github.io/reflect-metadata/#reflect.hasmetadata
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.hasMetadata", function () {
    it("InvalidTarget", function () {
        chai_1.assert.throws(function () { return Reflect.hasMetadata("key", undefined, undefined); }, TypeError);
    });
    it("WithoutTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.hasMetadata("key", obj, undefined);
        chai_1.assert.equal(result, false);
    });
    it("WithoutTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, undefined);
        var result = Reflect.hasMetadata("key", obj, undefined);
        chai_1.assert.equal(result, true);
    });
    it("WithoutTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, undefined);
        var result = Reflect.hasMetadata("key", obj, undefined);
        chai_1.assert.equal(result, true);
    });
    it("WithTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.hasMetadata("key", obj, "name");
        chai_1.assert.equal(result, false);
    });
    it("WithTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, "name");
        var result = Reflect.hasMetadata("key", obj, "name");
        chai_1.assert.equal(result, true);
    });
    it("WithTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, "name");
        var result = Reflect.hasMetadata("key", obj, "name");
        chai_1.assert.equal(result, true);
    });
});
//# sourceMappingURL=reflect-hasmetadata.js.map