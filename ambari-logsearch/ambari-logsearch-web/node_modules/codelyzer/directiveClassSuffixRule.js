"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var Lint = require("tslint");
var sprintf_js_1 = require("sprintf-js");
var ng2Walker_1 = require("./angular/ng2Walker");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super.apply(this, arguments) || this;
    }
    Rule.validate = function (className, suffix) {
        return className.endsWith(suffix);
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new ClassMetadataWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
Rule.FAILURE = 'The name of the class %s should end with the suffix %s (https://goo.gl/5X1TE7)';
exports.Rule = Rule;
var ClassMetadataWalker = (function (_super) {
    __extends(ClassMetadataWalker, _super);
    function ClassMetadataWalker() {
        return _super.apply(this, arguments) || this;
    }
    ClassMetadataWalker.prototype.visitNg2Directive = function (meta) {
        var name = meta.controller.name;
        var className = name.text;
        var suffix = this.getOptions()[0] || 'Directive';
        if (!Rule.validate(className, suffix)) {
            this.addFailure(this.createFailure(name.getStart(), name.getWidth(), sprintf_js_1.sprintf.apply(this, [Rule.FAILURE, className, suffix])));
        }
    };
    return ClassMetadataWalker;
}(ng2Walker_1.Ng2Walker));
exports.ClassMetadataWalker = ClassMetadataWalker;
