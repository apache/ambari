// 4.1.8 Reflect.getMetadataKeys ( target [, propertyKey] )
// https://rbuckton.github.io/reflect-metadata/#reflect.getmetadatakeys
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.getMetadataKeys", function () {
    it("KeysInvalidTarget", function () {
        // 1. If Type(target) is not Object, throw a TypeError exception.
        chai_1.assert.throws(function () { return Reflect.getMetadataKeys(undefined, undefined); }, TypeError);
    });
    it("KeysWithoutTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.getMetadataKeys(obj, undefined);
        chai_1.assert.deepEqual(result, []);
    });
    it("KeysWithoutTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, undefined);
        var result = Reflect.getMetadataKeys(obj, undefined);
        chai_1.assert.deepEqual(result, ["key"]);
    });
    it("KeysWithoutTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, undefined);
        var result = Reflect.getMetadataKeys(obj, undefined);
        chai_1.assert.deepEqual(result, ["key"]);
    });
    it("KeysOrderWithoutTargetKey", function () {
        var obj = {};
        Reflect.defineMetadata("key1", "value", obj, undefined);
        Reflect.defineMetadata("key0", "value", obj, undefined);
        var result = Reflect.getMetadataKeys(obj, undefined);
        chai_1.assert.deepEqual(result, ["key1", "key0"]);
    });
    it("KeysOrderAfterRedefineWithoutTargetKey", function () {
        var obj = {};
        Reflect.defineMetadata("key1", "value", obj, undefined);
        Reflect.defineMetadata("key0", "value", obj, undefined);
        Reflect.defineMetadata("key1", "value", obj, undefined);
        var result = Reflect.getMetadataKeys(obj, undefined);
        chai_1.assert.deepEqual(result, ["key1", "key0"]);
    });
    it("KeysOrderWithoutTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        Reflect.defineMetadata("key2", "value", prototype, undefined);
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key1", "value", obj, undefined);
        Reflect.defineMetadata("key0", "value", obj, undefined);
        var result = Reflect.getMetadataKeys(obj, undefined);
        chai_1.assert.deepEqual(result, ["key1", "key0", "key2"]);
    });
    it("KeysWithTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.getMetadataKeys(obj, "name");
        chai_1.assert.deepEqual(result, []);
    });
    it("KeysWithTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, "name");
        var result = Reflect.getMetadataKeys(obj, "name");
        chai_1.assert.deepEqual(result, ["key"]);
    });
    it("KeysWithTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, "name");
        var result = Reflect.getMetadataKeys(obj, "name");
        chai_1.assert.deepEqual(result, ["key"]);
    });
    it("KeysOrderAfterRedefineWithTargetKey", function () {
        var obj = {};
        Reflect.defineMetadata("key1", "value", obj, "name");
        Reflect.defineMetadata("key0", "value", obj, "name");
        Reflect.defineMetadata("key1", "value", obj, "name");
        var result = Reflect.getMetadataKeys(obj, "name");
        chai_1.assert.deepEqual(result, ["key1", "key0"]);
    });
    it("KeysOrderWithTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        Reflect.defineMetadata("key2", "value", prototype, "name");
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key1", "value", obj, "name");
        Reflect.defineMetadata("key0", "value", obj, "name");
        var result = Reflect.getMetadataKeys(obj, "name");
        chai_1.assert.deepEqual(result, ["key1", "key0", "key2"]);
    });
});
//# sourceMappingURL=reflect-getmetadatakeys.js.map