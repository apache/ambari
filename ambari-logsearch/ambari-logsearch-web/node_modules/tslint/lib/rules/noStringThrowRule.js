/**
 * @license
 * Copyright 2016 Palantir Technologies, Inc.
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
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-string-throw",
    description: "Flags throwing plain strings or concatenations of strings " +
        "because only Errors produce proper stack traces.",
    hasFix: true,
    options: null,
    optionsDescription: "Not configurable.",
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Throwing plain strings (not instances of Error) gives no stack traces";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitThrowStatement = function (node) {
        var expression = node.expression;
        if (this.stringConcatRecursive(expression)) {
            var fix = this.createFix(this.createReplacement(expression.getStart(), expression.getEnd() - expression.getStart(), "new Error(" + expression.getText() + ")"));
            this.addFailure(this.createFailure(node.getStart(), node.getWidth(), Rule.FAILURE_STRING, fix));
        }
        _super.prototype.visitThrowStatement.call(this, node);
    };
    Walker.prototype.stringConcatRecursive = function (node) {
        switch (node.kind) {
            case ts.SyntaxKind.StringLiteral:
            case ts.SyntaxKind.NoSubstitutionTemplateLiteral:
            case ts.SyntaxKind.TemplateExpression:
                return true;
            case ts.SyntaxKind.BinaryExpression:
                var n = node;
                var op = n.operatorToken.kind;
                return op === ts.SyntaxKind.PlusToken &&
                    (this.stringConcatRecursive(n.left) ||
                        this.stringConcatRecursive(n.right));
            case ts.SyntaxKind.ParenthesizedExpression:
                return this.stringConcatRecursive(node.expression);
            default:
                return false;
        }
    };
    return Walker;
}(Lint.RuleWalker));
