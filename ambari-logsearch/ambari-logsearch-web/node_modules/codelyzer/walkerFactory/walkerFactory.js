"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var ng2Walker_1 = require("../angular/ng2Walker");
function allNg2Component() {
    return new Ng2ComponentWalkerBuilder();
}
exports.allNg2Component = allNg2Component;
var Failure = (function () {
    function Failure(node, message) {
        this.node = node;
        this.message = message;
    }
    return Failure;
}());
exports.Failure = Failure;
var Ng2ComponentWalkerBuilder = (function () {
    function Ng2ComponentWalkerBuilder() {
    }
    Ng2ComponentWalkerBuilder.prototype.where = function (validate) {
        this._where = validate;
        return this;
    };
    Ng2ComponentWalkerBuilder.prototype.build = function (sourceFile, options) {
        var self = this;
        var e = (function (_super) {
            __extends(class_1, _super);
            function class_1() {
                return _super.apply(this, arguments) || this;
            }
            class_1.prototype.visitNg2Component = function (meta) {
                var _this = this;
                self._where(meta).fmap(function (failure) {
                    _this.addFailure(_this.createFailure(failure.node.getStart(), failure.node.getWidth(), failure.message));
                });
                _super.prototype.visitNg2Component.call(this, meta);
            };
            return class_1;
        }(ng2Walker_1.Ng2Walker));
        return new e(sourceFile, options);
    };
    return Ng2ComponentWalkerBuilder;
}());
