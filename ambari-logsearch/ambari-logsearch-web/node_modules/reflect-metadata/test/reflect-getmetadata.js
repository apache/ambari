// 4.1.5 Reflect.getMetadata ( metadataKey, target [, propertyKey] )
// https://rbuckton.github.io/reflect-metadata/#reflect.getmetadata
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.getMetadata", function () {
    it("InvalidTarget", function () {
        chai_1.assert.throws(function () { return Reflect.getMetadata("key", undefined, undefined); }, TypeError);
    });
    it("WithoutTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.getMetadata("key", obj, undefined);
        chai_1.assert.equal(result, undefined);
    });
    it("WithoutTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, undefined);
        var result = Reflect.getMetadata("key", obj, undefined);
        chai_1.assert.equal(result, "value");
    });
    it("WithoutTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, undefined);
        var result = Reflect.getMetadata("key", obj, undefined);
        chai_1.assert.equal(result, "value");
    });
    it("WithTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.getMetadata("key", obj, "name");
        chai_1.assert.equal(result, undefined);
    });
    it("WithTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, "name");
        var result = Reflect.getMetadata("key", obj, "name");
        chai_1.assert.equal(result, "value");
    });
    it("WithTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, "name");
        var result = Reflect.getMetadata("key", obj, "name");
        chai_1.assert.equal(result, "value");
    });
});
//# sourceMappingURL=reflect-getmetadata.js.map