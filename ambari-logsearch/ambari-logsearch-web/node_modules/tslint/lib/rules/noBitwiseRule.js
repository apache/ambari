/**
 * @license
 * Copyright 2013 Palantir Technologies, Inc.
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
        return this.applyWithWalker(new NoBitwiseWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-bitwise",
    description: "Disallows bitwise operators.",
    descriptionDetails: (_a = ["\n            Specifically, the following bitwise operators are banned:\n            `&`, `&=`, `|`, `|=`,\n            `^`, `^=`, `<<`, `<<=`,\n            `>>`, `>>=`, `>>>`, `>>>=`, and `~`.\n            This rule does not ban the use of `&` and `|` for intersection and union types."], _a.raw = ["\n            Specifically, the following bitwise operators are banned:\n            \\`&\\`, \\`&=\\`, \\`|\\`, \\`|=\\`,\n            \\`^\\`, \\`^=\\`, \\`<<\\`, \\`<<=\\`,\n            \\`>>\\`, \\`>>=\\`, \\`>>>\\`, \\`>>>=\\`, and \\`~\\`.\n            This rule does not ban the use of \\`&\\` and \\`|\\` for intersection and union types."], Lint.Utils.dedent(_a)),
    rationale: (_b = ["\n            Bitwise operators are often typos - for example `bool1 & bool2` instead of `bool1 && bool2`.\n            They also can be an indicator of overly clever code which decreases maintainability."], _b.raw = ["\n            Bitwise operators are often typos - for example \\`bool1 & bool2\\` instead of \\`bool1 && bool2\\`.\n            They also can be an indicator of overly clever code which decreases maintainability."], Lint.Utils.dedent(_b)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Forbidden bitwise operation";
exports.Rule = Rule;
var NoBitwiseWalker = (function (_super) {
    __extends(NoBitwiseWalker, _super);
    function NoBitwiseWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoBitwiseWalker.prototype.visitBinaryExpression = function (node) {
        switch (node.operatorToken.kind) {
            case ts.SyntaxKind.AmpersandToken:
            case ts.SyntaxKind.AmpersandEqualsToken:
            case ts.SyntaxKind.BarToken:
            case ts.SyntaxKind.BarEqualsToken:
            case ts.SyntaxKind.CaretToken:
            case ts.SyntaxKind.CaretEqualsToken:
            case ts.SyntaxKind.LessThanLessThanToken:
            case ts.SyntaxKind.LessThanLessThanEqualsToken:
            case ts.SyntaxKind.GreaterThanGreaterThanToken:
            case ts.SyntaxKind.GreaterThanGreaterThanEqualsToken:
            case ts.SyntaxKind.GreaterThanGreaterThanGreaterThanToken:
            case ts.SyntaxKind.GreaterThanGreaterThanGreaterThanEqualsToken:
                this.addFailureAtNode(node, Rule.FAILURE_STRING);
                break;
            default:
                break;
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    NoBitwiseWalker.prototype.visitPrefixUnaryExpression = function (node) {
        if (node.operator === ts.SyntaxKind.TildeToken) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING);
        }
        _super.prototype.visitPrefixUnaryExpression.call(this, node);
    };
    return NoBitwiseWalker;
}(Lint.RuleWalker));
var _a, _b;
