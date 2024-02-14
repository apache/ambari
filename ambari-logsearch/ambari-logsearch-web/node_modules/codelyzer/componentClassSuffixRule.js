"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var Lint = require("tslint");
var sprintf_js_1 = require("sprintf-js");
var walkerFactory_1 = require("./walkerFactory/walkerFactory");
var walkerFn_1 = require("./walkerFactory/walkerFn");
var function_1 = require("./util/function");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super.apply(this, arguments) || this;
    }
    Rule.validate = function (className, suffixList) {
        return suffixList.some(function (suffix) { return className.endsWith(suffix); });
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(Rule.walkerBuilder(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
Rule.FAILURE = 'The name of the class %s should end with the suffix %s (https://goo.gl/5X1TE7)';
Rule.walkerBuilder = walkerFn_1.all(walkerFn_1.validateComponent(function (meta, suffixList) {
    return function_1.Maybe.lift(meta.controller)
        .fmap(function (controller) { return controller.name; })
        .fmap(function (name) {
        var className = name.text;
        var _suffixList = suffixList.length > 0 ? suffixList : ['Component'];
        if (!Rule.validate(className, _suffixList)) {
            return [new walkerFactory_1.Failure(name, sprintf_js_1.sprintf(Rule.FAILURE, className, _suffixList))];
        }
    });
}));
exports.Rule = Rule;
