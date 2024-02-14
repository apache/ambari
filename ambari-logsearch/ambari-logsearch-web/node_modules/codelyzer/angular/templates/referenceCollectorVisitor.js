"use strict";
var ReferenceCollectorVisitor = (function () {
    function ReferenceCollectorVisitor() {
        this._variables = [];
    }
    ReferenceCollectorVisitor.prototype.visit = function (node, context) {
        node.visit(this, context);
        return this._variables;
    };
    ReferenceCollectorVisitor.prototype.visitBoundText = function (text, context) { };
    ReferenceCollectorVisitor.prototype.visitElementProperty = function (prop, context) { };
    ReferenceCollectorVisitor.prototype.visitReference = function (ast, context) { };
    ReferenceCollectorVisitor.prototype.visitNgContent = function (ast, context) { };
    ReferenceCollectorVisitor.prototype.visitVariable = function (ast, context) { };
    ReferenceCollectorVisitor.prototype.visitAttr = function (ast, context) { };
    ReferenceCollectorVisitor.prototype.visitText = function (text, context) { };
    ReferenceCollectorVisitor.prototype.visitDirective = function (ast, context) { };
    ReferenceCollectorVisitor.prototype.visitDirectiveProperty = function (ast, context) { };
    ReferenceCollectorVisitor.prototype.visitEvent = function (ast, context) { };
    ReferenceCollectorVisitor.prototype.visitEmbeddedTemplate = function (ast, context) {
        var _this = this;
        var references = ast.references.map(function (r) { return r.name; });
        ast.children.forEach(function (e) { return _this.visit(e, context); });
        this._variables = this._variables.concat(references);
    };
    ReferenceCollectorVisitor.prototype.visitElement = function (element, context) {
        var _this = this;
        var references = element.references.map(function (r) { return r.name; });
        element.children.forEach(function (e) { return _this.visit(e, context); });
        this._variables = this._variables.concat(references);
    };
    Object.defineProperty(ReferenceCollectorVisitor.prototype, "variables", {
        get: function () {
            return this._variables;
        },
        enumerable: true,
        configurable: true
    });
    return ReferenceCollectorVisitor;
}());
exports.ReferenceCollectorVisitor = ReferenceCollectorVisitor;
