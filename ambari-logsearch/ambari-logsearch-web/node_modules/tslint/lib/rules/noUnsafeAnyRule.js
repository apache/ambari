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
    ruleName: "no-unsafe-any",
    description: (_a = ["\n            Warns when using an expression of type 'any' in an unsafe way.\n            Type casts and tests are allowed.\n            Expressions that work on all values (such as '\"\" + x') are allowed."], _a.raw = ["\n            Warns when using an expression of type 'any' in an unsafe way.\n            Type casts and tests are allowed.\n            Expressions that work on all values (such as '\"\" + x') are allowed."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Unsafe use of expression of type 'any'.";
exports.Rule = Rule;
// This is marked @internal, but we need it!
var isExpression = ts.isExpression;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitNode = function (node) {
        if (isExpression(node) && isAny(this.getType(node)) && !this.isAllowedLocation(node)) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING);
        }
        else {
            _super.prototype.visitNode.call(this, node);
        }
    };
    Walker.prototype.isAllowedLocation = function (node) {
        var parent = node.parent;
        switch (parent.kind) {
            case ts.SyntaxKind.ExpressionStatement: // Allow unused expression
            case ts.SyntaxKind.Parameter: // Allow to declare a parameter of type 'any'
            case ts.SyntaxKind.TypeOfExpression: // Allow test
            case ts.SyntaxKind.TemplateSpan: // Allow stringification (works on all values)
            // Allow casts
            case ts.SyntaxKind.TypeAssertionExpression:
            case ts.SyntaxKind.AsExpression:
                return true;
            // OK to pass 'any' to a function that takes 'any' as its argument
            case ts.SyntaxKind.CallExpression:
            case ts.SyntaxKind.NewExpression:
                return isAny(this.getTypeChecker().getContextualType(node));
            case ts.SyntaxKind.BinaryExpression:
                var _a = parent, left = _a.left, right = _a.right, operatorToken = _a.operatorToken;
                // Allow equality since all values support equality.
                if (Lint.getEqualsKind(operatorToken) !== undefined) {
                    return true;
                }
                switch (operatorToken.kind) {
                    case ts.SyntaxKind.InstanceOfKeyword:
                        return true;
                    case ts.SyntaxKind.PlusToken:
                        return node === left ? this.isStringLike(right) : this.isStringLike(left);
                    case ts.SyntaxKind.PlusEqualsToken:
                        return node === right && this.isStringLike(left);
                    default:
                        return false;
                }
            // Allow `const x = foo;`, but not `const x: Foo = foo;`.
            case ts.SyntaxKind.VariableDeclaration:
                return Lint.hasModifier(parent.parent.parent.modifiers, ts.SyntaxKind.DeclareKeyword) ||
                    parent.type === undefined;
            case ts.SyntaxKind.PropertyAccessExpression:
                // Don't warn for right hand side; this is redundant if we warn for the left-hand side.
                return parent.name === node;
            default:
                return false;
        }
    };
    Walker.prototype.isStringLike = function (node) {
        return Lint.isTypeFlagSet(this.getType(node), ts.TypeFlags.StringLike);
    };
    Walker.prototype.getType = function (node) {
        return this.getTypeChecker().getTypeAtLocation(node);
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
function isAny(type) {
    return type !== undefined && Lint.isTypeFlagSet(type, ts.TypeFlags.Any);
}
var _a;
