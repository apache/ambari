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
        return this.applyWithWalker(new UseIsnanRuleWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "use-isnan",
    description: "Enforces use of the `isNaN()` function to check for NaN references instead of a comparison to the `NaN` constant.",
    rationale: (_a = ["\n            Since `NaN !== NaN`, comparisons with regular operators will produce unexpected results.\n            So, instead of `if (myVar === NaN)`, do `if (isNaN(myVar))`."], _a.raw = ["\n            Since \\`NaN !== NaN\\`, comparisons with regular operators will produce unexpected results.\n            So, instead of \\`if (myVar === NaN)\\`, do \\`if (isNaN(myVar))\\`."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Found an invalid comparison for NaN: ";
exports.Rule = Rule;
var UseIsnanRuleWalker = (function (_super) {
    __extends(UseIsnanRuleWalker, _super);
    function UseIsnanRuleWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    UseIsnanRuleWalker.prototype.visitBinaryExpression = function (node) {
        if ((this.isExpressionNaN(node.left) || this.isExpressionNaN(node.right))
            && node.operatorToken.kind !== ts.SyntaxKind.EqualsToken) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING + node.getText());
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    UseIsnanRuleWalker.prototype.isExpressionNaN = function (node) {
        return node.kind === ts.SyntaxKind.Identifier && node.getText() === "NaN";
    };
    return UseIsnanRuleWalker;
}(Lint.RuleWalker));
var _a;
