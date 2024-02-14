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
var LEGAL_TYPEOF_RESULTS = new Set(["undefined", "string", "boolean", "number", "function", "object", "symbol"]);
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
    ruleName: "typeof-compare",
    description: "Makes sure result of `typeof` is compared to correct string values",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "'typeof' expression must be compared to one of: " + Array.from(LEGAL_TYPEOF_RESULTS).map(function (x) { return "\"" + x + "\""; }).join(", ");
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitBinaryExpression = function (node) {
        var operatorToken = node.operatorToken, left = node.left, right = node.right;
        if (Lint.getEqualsKind(operatorToken) && (isFaultyTypeof(left, right) || isFaultyTypeof(right, left))) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING);
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    return Walker;
}(Lint.RuleWalker));
function isFaultyTypeof(left, right) {
    return left.kind === ts.SyntaxKind.TypeOfExpression && isFaultyTypeofResult(right);
}
function isFaultyTypeofResult(node) {
    switch (node.kind) {
        case ts.SyntaxKind.StringLiteral:
            return !LEGAL_TYPEOF_RESULTS.has(node.text);
        case ts.SyntaxKind.Identifier:
            return node.originalKeywordKind === ts.SyntaxKind.UndefinedKeyword;
        case ts.SyntaxKind.NullKeyword:
        case ts.SyntaxKind.NumericLiteral:
        case ts.SyntaxKind.TrueKeyword:
        case ts.SyntaxKind.FalseKeyword:
        case ts.SyntaxKind.ObjectLiteralExpression:
        case ts.SyntaxKind.ArrayLiteralExpression:
            return true;
        default:
            return false;
    }
}
