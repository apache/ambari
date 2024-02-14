/**
 * @license
 * Copyright 2017 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.applyWithProgram = function (srcFile, langSvc) {
        return this.applyWithWalker(new Walker(srcFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-unbound-method",
    description: "Warns when a method is used as outside of a method call.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Avoid referencing unbound methods which may cause unintentional scoping of 'this'.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitPropertyAccessExpression = function (node) {
        if (!isSafeUse(node)) {
            var symbol = this.getTypeChecker().getSymbolAtLocation(node);
            var declaration = symbol && symbol.valueDeclaration;
            if (declaration && isMethod(declaration)) {
                this.addFailureAtNode(node, Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitPropertyAccessExpression.call(this, node);
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
function isMethod(node) {
    switch (node.kind) {
        case ts.SyntaxKind.MethodDeclaration:
        case ts.SyntaxKind.MethodSignature:
            return true;
        default:
            return false;
    }
}
function isSafeUse(node) {
    var parent = node.parent;
    switch (parent.kind) {
        case ts.SyntaxKind.CallExpression:
            return parent.expression === node;
        case ts.SyntaxKind.TaggedTemplateExpression:
            return parent.tag === node;
        // E.g. `obj.method.bind(obj)`.
        case ts.SyntaxKind.PropertyAccessExpression:
            return true;
        // Allow most binary operators, but don't allow e.g. `myArray.forEach(obj.method || otherObj.otherMethod)`.
        case ts.SyntaxKind.BinaryExpression:
            return parent.operatorToken.kind !== ts.SyntaxKind.BarBarToken;
        default:
            return false;
    }
}
