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
    /* tslint:enable:object-literal-sort-keys */
    Rule.FAILURE_STRING = function (negate) {
        return "This expression is unnecessarily compared to a boolean. Just " + (negate ? "negate it" : "use it directly") + ".";
    };
    Rule.prototype.applyWithProgram = function (sourceFile, langSvc) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-boolean-literal-compare",
    description: "Warns on comparison to a boolean literal, as in `x === true`.",
    hasFix: true,
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitBinaryExpression = function (node) {
        this.check(node);
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    Walker.prototype.check = function (node) {
        var comparison = deconstructComparison(node);
        if (comparison === undefined) {
            return;
        }
        var negate = comparison.negate, expression = comparison.expression;
        var type = this.getTypeChecker().getTypeAtLocation(expression);
        if (!Lint.isTypeFlagSet(type, ts.TypeFlags.Boolean)) {
            return;
        }
        var deleted = node.left === expression
            ? this.deleteFromTo(node.left.end, node.end)
            : this.deleteFromTo(node.getStart(), node.right.getStart());
        var replacements = [deleted];
        if (negate) {
            if (needsParenthesesForNegate(expression)) {
                replacements.push(this.appendText(node.getStart(), "!("));
                replacements.push(this.appendText(node.getEnd(), ")"));
            }
            else {
                replacements.push(this.appendText(node.getStart(), "!"));
            }
        }
        this.addFailureAtNode(expression, Rule.FAILURE_STRING(negate), this.createFix.apply(this, replacements));
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
function needsParenthesesForNegate(node) {
    switch (node.kind) {
        case ts.SyntaxKind.AsExpression:
        case ts.SyntaxKind.BinaryExpression:
            return true;
        default:
            return false;
    }
}
function deconstructComparison(node) {
    var left = node.left, operatorToken = node.operatorToken, right = node.right;
    var eq = Lint.getEqualsKind(operatorToken);
    if (!eq) {
        return undefined;
    }
    var leftValue = booleanFromExpression(left);
    if (leftValue !== undefined) {
        return { negate: leftValue !== eq.isPositive, expression: right };
    }
    var rightValue = booleanFromExpression(right);
    if (rightValue !== undefined) {
        return { negate: rightValue !== eq.isPositive, expression: left };
    }
    return undefined;
}
function booleanFromExpression(node) {
    switch (node.kind) {
        case ts.SyntaxKind.TrueKeyword:
            return true;
        case ts.SyntaxKind.FalseKeyword:
            return false;
        default:
            return undefined;
    }
}
