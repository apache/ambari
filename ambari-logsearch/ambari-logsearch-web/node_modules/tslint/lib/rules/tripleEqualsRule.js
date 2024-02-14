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
var OPTION_ALLOW_NULL_CHECK = "allow-null-check";
var OPTION_ALLOW_UNDEFINED_CHECK = "allow-undefined-check";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var comparisonWalker = new ComparisonWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(comparisonWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "triple-equals",
    description: "Requires `===` and `!==` in place of `==` and `!=`.",
    optionsDescription: (_a = ["\n            Two arguments may be optionally provided:\n\n            * `\"allow-null-check\"` allows `==` and `!=` when comparing to `null`.\n            * `\"allow-undefined-check\"` allows `==` and `!=` when comparing to `undefined`."], _a.raw = ["\n            Two arguments may be optionally provided:\n\n            * \\`\"allow-null-check\"\\` allows \\`==\\` and \\`!=\\` when comparing to \\`null\\`.\n            * \\`\"allow-undefined-check\"\\` allows \\`==\\` and \\`!=\\` when comparing to \\`undefined\\`."], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [OPTION_ALLOW_NULL_CHECK, OPTION_ALLOW_UNDEFINED_CHECK],
        },
        minLength: 0,
        maxLength: 2,
    },
    optionExamples: ["true", '[true, "allow-null-check"]', '[true, "allow-undefined-check"]'],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.EQ_FAILURE_STRING = "== should be ===";
Rule.NEQ_FAILURE_STRING = "!= should be !==";
exports.Rule = Rule;
var ComparisonWalker = (function (_super) {
    __extends(ComparisonWalker, _super);
    function ComparisonWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.allowNull = _this.hasOption(OPTION_ALLOW_NULL_CHECK);
        _this.allowUndefined = _this.hasOption(OPTION_ALLOW_UNDEFINED_CHECK);
        return _this;
    }
    ComparisonWalker.prototype.visitBinaryExpression = function (node) {
        var eq = Lint.getEqualsKind(node.operatorToken);
        if (eq && !eq.isStrict && !this.isExpressionAllowed(node)) {
            this.addFailureAtNode(node.operatorToken, eq.isPositive ? Rule.EQ_FAILURE_STRING : Rule.NEQ_FAILURE_STRING);
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    ComparisonWalker.prototype.isExpressionAllowed = function (_a) {
        var _this = this;
        var left = _a.left, right = _a.right;
        var isAllowed = function (n) {
            return n.kind === ts.SyntaxKind.NullKeyword ? _this.allowNull
                : _this.allowUndefined &&
                    n.kind === ts.SyntaxKind.Identifier &&
                    n.originalKeywordKind === ts.SyntaxKind.UndefinedKeyword;
        };
        return isAllowed(left) || isAllowed(right);
    };
    return ComparisonWalker;
}(Lint.RuleWalker));
var _a;
