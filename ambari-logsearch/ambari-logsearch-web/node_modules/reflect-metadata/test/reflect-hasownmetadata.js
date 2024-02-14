// 4.1.5 Reflect.hasOwnMetadata ( metadataKey, target [, propertyKey] )
// https://rbuckton.github.io/reflect-metadata/#reflect.hasownmetadata
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.hasOwnMetadata", function () {
    it("InvalidTarget", function () {
        chai_1.assert.throws(function () { return Reflect.hasOwnMetadata("key", undefined, undefined); }, TypeError);
    });
    it("WithoutTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.hasOwnMetadata("key", obj, undefined);
        chai_1.assert.equal(result, false);
    });
    it("WithoutTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, undefined);
        var result = Reflect.hasOwnMetadata("key", obj, undefined);
        chai_1.assert.equal(result, true);
    });
    it("WithoutTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, undefined);
        var result = Reflect.hasOwnMetadata("key", obj, undefined);
        chai_1.assert.equal(result, false);
    });
    it("WithTargetKeyWhenNotDefined", function () {
        var obj = {};
        var result = Reflect.hasOwnMetadata("key", obj, "name");
        chai_1.assert.equal(result, false);
    });
    it("WithTargetKeyWhenDefined", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, "name");
        var result = Reflect.hasOwnMetadata("key", obj, "name");
        chai_1.assert.equal(result, true);
    });
    it("WithTargetKeyWhenDefinedOnPrototype", function () {
        var prototype = {};
        var obj = Object.create(prototype);
        Reflect.defineMetadata("key", "value", prototype, "name");
        var result = Reflect.hasOwnMetadata("key", obj, "name");
        chai_1.assert.equal(result, false);
    });
});
//# sourceMappingURL=reflect-hasownmetadata.js.map