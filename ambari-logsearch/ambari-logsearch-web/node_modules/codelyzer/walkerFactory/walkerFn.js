"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var ng2Walker_1 = require("../angular/ng2Walker");
var function_1 = require("../util/function");
function validate(syntaxKind) {
    return function (validateFn) { return ({
        kind: 'Node',
        validate: function (node, options) { return (node.kind === syntaxKind) ? validateFn(node, options) : function_1.Maybe.nothing; },
    }); };
}
exports.validate = validate;
function validateComponent(validate) {
    return {
        kind: 'Ng2Component',
        validate: validate,
    };
}
exports.validateComponent = validateComponent;
function all() {
    var validators = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        validators[_i] = arguments[_i];
    }
    return function (sourceFile, options) {
        var e = (function (_super) {
            __extends(class_1, _super);
            function class_1() {
                return _super.apply(this, arguments) || this;
            }
            class_1.prototype.visitNg2Component = function (meta) {
                var _this = this;
                validators.forEach(function (v) {
                    if (v.kind === 'Ng2Component') {
                        v.validate(meta, _this.getOptions()).fmap(function (failures) { return failures.forEach(function (f) { return _this.failed(f); }); });
                    }
                });
                _super.prototype.visitNg2Component.call(this, meta);
            };
            class_1.prototype.visitNode = function (node) {
                var _this = this;
                validators.forEach(function (v) {
                    if (v.kind === 'Node') {
                        v.validate(node, _this.getOptions()).fmap(function (failures) { return failures.forEach(function (f) { return _this.failed(f); }); });
                    }
                });
                _super.prototype.visitNode.call(this, node);
            };
            class_1.prototype.failed = function (failure) {
                this.addFailure(this.createFailure(failure.node.getStart(), failure.node.getWidth(), failure.message));
            };
            return class_1;
        }(ng2Walker_1.Ng2Walker));
        return new e(sourceFile, options);
    };
}
exports.all = all;
