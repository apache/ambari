// 4.1.10 Reflect.deleteMetadata ( metadataKey, target [, propertyKey] )
// https://rbuckton.github.io/reflect-metadata/#reflect.deletemetadata
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.deleteMetadata", function () {
    it("InvalidTarget", function () {
        chai_1.assert.throws(function () { return Reflect.deleteMetadata("key", undefined, undefined); }, TypeError);
    });
    it("WhenNotDefinedWithoutTargetKey", function () {
        var obj = {};
        var result = Reflect.deleteMetadata("key", obj, undefined);
        chai_1.assert.equal(result, false);
    });
    it("WhenDefinedWithoutTargetKey", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, undefined);
        var result = Reflect.deleteMetadata("key", obj, undefined);
        chai_1.assert.equal(result, true);
    });
    it("WhenDefinedOnPrototypeWithoutTargetKey", function () {
        var prototype = {};
        Reflect.defineMetadata("key", "value", prototype, undefined);
        var obj = Object.create(prototype);
        var result = Reflect.deleteMetadata("key", obj, undefined);
        chai_1.assert.equal(result, false);
    });
    it("AfterDeleteMetadata", function () {
        var obj = {};
        Reflect.defineMetadata("key", "value", obj, undefined);
        Reflect.deleteMetadata("key", obj, undefined);
        var result = Reflect.hasOwnMetadata("key", obj, undefined);
        chai_1.assert.equal(result, false);
    });
});
//# sourceMappingURL=reflect-deletemetadata.js.map