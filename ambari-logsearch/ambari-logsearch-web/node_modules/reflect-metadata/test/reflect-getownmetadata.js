// 4.1.7 Reflect.getOwnMetadata ( metadataKey, target [, propertyKey] )
// https://rbuckton.github.io/reflect-metadata/#reflect.getownmetadata
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.getOwnMetadata", function () {
    it("InvalidTarget", function () {
        chai_1.assert.throws(function () { return Reflect.getOwnMetadata("key", undefined, undefined); }, TypeError);
    });
    it("WithoutTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.getOwnMetadata("key", obj, undefined);
        chai_1.assert.equal(result, undefined);
    });
    it("WithoutTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, undefined);
        var result = Reflect.getOwnMetadata("key", obj, undefined);
        chai_1.assert.equal(result, "value");
    });
    it("WithoutTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, undefined);
        var result = Reflect.getOwnMetadata("key", obj, undefined);
        chai_1.assert.equal(result, undefined);
    });
    it("WithTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.getOwnMetadata("key", obj, "name");
        chai_1.assert.equal(result, undefined);
    });
    it("WithTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, "name");
        var result = Reflect.getOwnMetadata("key", obj, "name");
        chai_1.assert.equal(result, "value");
    });
    it("WithTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, "name");
        var result = Reflect.getOwnMetadata("key", obj, "name");
        chai_1.assert.equal(result, undefined);
    });
});
//# sourceMappingURL=reflect-getownmetadata.js.map