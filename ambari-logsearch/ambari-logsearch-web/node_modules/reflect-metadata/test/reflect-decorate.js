// Reflect.decorate ( decorators, target [, propertyKey [, descriptor] ] )
"use strict";
require("../Reflect");
var chai_1 = require("chai");
describe("Reflect.decorate", function () {
    it("ThrowsIfDecoratorsArgumentNotArrayForFunctionOverload", function () {
        var target = function () { };
        chai_1.assert.throws(function () { return Reflect.decorate(undefined, target, undefined, undefined); }, TypeError);
    });
    it("ThrowsIfTargetArgumentNotFunctionForFunctionOverload", function () {
        var decorators = [];
        var target = {};
        chai_1.assert.throws(function () { return Reflect.decorate(decorators, target, undefined, undefined); }, TypeError);
    });
    it("ThrowsIfDecoratorsArgumentNotArrayForPropertyOverload", function () {
        var target = {};
        var name = "name";
        chai_1.assert.throws(function () { return Reflect.decorate(undefined, target, name, undefined); }, TypeError);
    });
    it("ThrowsIfTargetArgumentNotObjectForPropertyOverload", function () {
        var decorators = [];
        var target = 1;
        var name = "name";
        chai_1.assert.throws(function () { return Reflect.decorate(decorators, target, name, undefined); }, TypeError);
    });
    it("ThrowsIfDecoratorsArgumentNotArrayForPropertyDescriptorOverload", function () {
        var target = {};
        var name = "name";
        var descriptor = {};
        chai_1.assert.throws(function () { return Reflect.decorate(undefined, target, name, descriptor); }, TypeError);
    });
    it("ThrowsIfTargetArgumentNotObjectForPropertyDescriptorOverload", function () {
        var decorators = [];
        var target = 1;
        var name = "name";
        var descriptor = {};
        chai_1.assert.throws(function () { return Reflect.decorate(decorators, target, name, descriptor); }, TypeError);
    });
    it("ExecutesDecoratorsInReverseOrderForFunctionOverload", function () {
        var order = [];
        var decorators = [
            function (target) { order.push(0); },
            function (target) { order.push(1); }
        ];
        var target = function () { };
        Reflect.decorate(decorators, target);
        chai_1.assert.deepEqual(order, [1, 0]);
    });
    it("ExecutesDecoratorsInReverseOrderForPropertyOverload", function () {
        var order = [];
        var decorators = [
            function (target, name) { order.push(0); },
            function (target, name) { order.push(1); }
        ];
        var target = {};
        var name = "name";
        Reflect.decorate(decorators, target, name, undefined);
        chai_1.assert.deepEqual(order, [1, 0]);
    });
    it("ExecutesDecoratorsInReverseOrderForPropertyDescriptorOverload", function () {
        var order = [];
        var decorators = [
            function (target, name) { order.push(0); },
            function (target, name) { order.push(1); }
        ];
        var target = {};
        var name = "name";
        var descriptor = {};
        Reflect.decorate(decorators, target, name, descriptor);
        chai_1.assert.deepEqual(order, [1, 0]);
    });
    it("DecoratorPipelineForFunctionOverload", function () {
        var A = function A() { };
        var B = function B() { };
        var decorators = [
            function (target) { return undefined; },
            function (target) { return A; },
            function (target) { return B; }
        ];
        var target = function () { };
        var result = Reflect.decorate(decorators, target);
        chai_1.assert.strictEqual(result, A);
    });
    it("DecoratorPipelineForPropertyOverload", function () {
        var A = {};
        var B = {};
        var decorators = [
            function (target, name) { return undefined; },
            function (target, name) { return A; },
            function (target, name) { return B; }
        ];
        var target = {};
        var result = Reflect.decorate(decorators, target, "name", undefined);
        chai_1.assert.strictEqual(result, A);
    });
    it("DecoratorPipelineForPropertyDescriptorOverload", function () {
        var A = {};
        var B = {};
        var C = {};
        var decorators = [
            function (target, name) { return undefined; },
            function (target, name) { return A; },
            function (target, name) { return B; }
        ];
        var target = {};
        var result = Reflect.decorate(decorators, target, "name", C);
        chai_1.assert.strictEqual(result, A);
    });
    it("DecoratorCorrectTargetInPipelineForFunctionOverload", function () {
        var sent = [];
        var A = function A() { };
        var B = function B() { };
        var decorators = [
            function (target) { sent.push(target); return undefined; },
            function (target) { sent.push(target); return undefined; },
            function (target) { sent.push(target); return A; },
            function (target) { sent.push(target); return B; }
        ];
        var target = function () { };
        Reflect.decorate(decorators, target);
        chai_1.assert.deepEqual(sent, [target, B, A, A]);
    });
    it("DecoratorCorrectTargetInPipelineForPropertyOverload", function () {
        var sent = [];
        var decorators = [
            function (target, name) { sent.push(target); },
            function (target, name) { sent.push(target); },
            function (target, name) { sent.push(target); },
            function (target, name) { sent.push(target); }
        ];
        var target = {};
        Reflect.decorate(decorators, target, "name");
        chai_1.assert.deepEqual(sent, [target, target, target, target]);
    });
    it("DecoratorCorrectNameInPipelineForPropertyOverload", function () {
        var sent = [];
        var decorators = [
            function (target, name) { sent.push(name); },
            function (target, name) { sent.push(name); },
            function (target, name) { sent.push(name); },
            function (target, name) { sent.push(name); }
        ];
        var target = {};
        Reflect.decorate(decorators, target, "name");
        chai_1.assert.deepEqual(sent, ["name", "name", "name", "name"]);
    });
    it("DecoratorCorrectTargetInPipelineForPropertyDescriptorOverload", function () {
        var sent = [];
        var A = {};
        var B = {};
        var C = {};
        var decorators = [
            function (target, name) { sent.push(target); return undefined; },
            function (target, name) { sent.push(target); return undefined; },
            function (target, name) { sent.push(target); return A; },
            function (target, name) { sent.push(target); return B; }
        ];
        var target = {};
        Reflect.decorate(decorators, target, "name", C);
        chai_1.assert.deepEqual(sent, [target, target, target, target]);
    });
    it("DecoratorCorrectNameInPipelineForPropertyDescriptorOverload", function () {
        var sent = [];
        var A = {};
        var B = {};
        var C = {};
        var decorators = [
            function (target, name) { sent.push(name); return undefined; },
            function (target, name) { sent.push(name); return undefined; },
            function (target, name) { sent.push(name); return A; },
            function (target, name) { sent.push(name); return B; }
        ];
        var target = {};
        Reflect.decorate(decorators, target, "name", C);
        chai_1.assert.deepEqual(sent, ["name", "name", "name", "name"]);
    });
    it("DecoratorCorrectDescriptorInPipelineForPropertyDescriptorOverload", function () {
        var sent = [];
        var A = {};
        var B = {};
        var C = {};
        var decorators = [
            function (target, name, descriptor) { sent.push(descriptor); return undefined; },
            function (target, name, descriptor) { sent.push(descriptor); return undefined; },
            function (target, name, descriptor) { sent.push(descriptor); return A; },
            function (target, name, descriptor) { sent.push(descriptor); return B; }
        ];
        var target = {};
        Reflect.decorate(decorators, target, "name", C);
        chai_1.assert.deepEqual(sent, [C, B, A, A]);
    });
});
//# sourceMappingURL=reflect-decorate.js.map