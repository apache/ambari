/**
 * @license
 * Copyright 2014 Palantir Technologies, Inc.
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
var utils_1 = require("../language/utils");
var ALLOW_FAST_NULL_CHECKS = "allow-fast-null-checks";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoUnusedExpressionWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-unused-expression",
    description: "Disallows unused expression statements.",
    descriptionDetails: (_a = ["\n            Unused expressions are expression statements which are not assignments or function calls\n            (and thus usually no-ops)."], _a.raw = ["\n            Unused expressions are expression statements which are not assignments or function calls\n            (and thus usually no-ops)."], Lint.Utils.dedent(_a)),
    rationale: (_b = ["\n            Detects potential errors where an assignment or function call was intended."], _b.raw = ["\n            Detects potential errors where an assignment or function call was intended."], Lint.Utils.dedent(_b)),
    optionsDescription: (_c = ["\n            One argument may be optionally provided:\n\n            * `", "` allows to use logical operators to perform fast null checks and perform\n            method or function calls for side effects (e.g. `e && e.preventDefault()`)."], _c.raw = ["\n            One argument may be optionally provided:\n\n            * \\`", "\\` allows to use logical operators to perform fast null checks and perform\n            method or function calls for side effects (e.g. \\`e && e.preventDefault()\\`)."], Lint.Utils.dedent(_c, ALLOW_FAST_NULL_CHECKS)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [ALLOW_FAST_NULL_CHECKS],
        },
        minLength: 0,
        maxLength: 1,
    },
    optionExamples: ["true", "[true, \"" + ALLOW_FAST_NULL_CHECKS + "\"]"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "expected an assignment or function call";
exports.Rule = Rule;
var NoUnusedExpressionWalker = (function (_super) {
    __extends(NoUnusedExpressionWalker, _super);
    function NoUnusedExpressionWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.expressionIsUnused = true;
        return _this;
    }
    NoUnusedExpressionWalker.isDirective = function (node, checkPreviousSiblings) {
        if (checkPreviousSiblings === void 0) { checkPreviousSiblings = true; }
        var parent = node.parent;
        if (parent === undefined) {
            return true;
        }
        var grandParentKind = parent.parent == null ? null : parent.parent.kind;
        var isStringExpression = node.kind === ts.SyntaxKind.ExpressionStatement
            && node.expression.kind === ts.SyntaxKind.StringLiteral;
        var parentIsSourceFile = parent.kind === ts.SyntaxKind.SourceFile;
        var parentIsNSBody = parent.kind === ts.SyntaxKind.ModuleBlock;
        var parentIsFunctionBody = grandParentKind !== null && parent.kind === ts.SyntaxKind.Block && [
            ts.SyntaxKind.ArrowFunction,
            ts.SyntaxKind.FunctionExpression,
            ts.SyntaxKind.FunctionDeclaration,
            ts.SyntaxKind.MethodDeclaration,
            ts.SyntaxKind.Constructor,
            ts.SyntaxKind.GetAccessor,
            ts.SyntaxKind.SetAccessor,
        ].indexOf(grandParentKind) > -1;
        if (!(parentIsSourceFile || parentIsFunctionBody || parentIsNSBody) || !isStringExpression) {
            return false;
        }
        if (checkPreviousSiblings) {
            var siblings_1 = [];
            ts.forEachChild(parent, function (child) { siblings_1.push(child); });
            return siblings_1.slice(0, siblings_1.indexOf(node)).every(function (n) { return NoUnusedExpressionWalker.isDirective(n, false); });
        }
        else {
            return true;
        }
    };
    NoUnusedExpressionWalker.prototype.visitExpressionStatement = function (node) {
        this.expressionIsUnused = true;
        _super.prototype.visitExpressionStatement.call(this, node);
        this.checkExpressionUsage(node);
    };
    NoUnusedExpressionWalker.prototype.visitBinaryExpression = function (node) {
        _super.prototype.visitBinaryExpression.call(this, node);
        switch (node.operatorToken.kind) {
            case ts.SyntaxKind.EqualsToken:
            case ts.SyntaxKind.PlusEqualsToken:
            case ts.SyntaxKind.MinusEqualsToken:
            case ts.SyntaxKind.AsteriskEqualsToken:
            case ts.SyntaxKind.SlashEqualsToken:
            case ts.SyntaxKind.PercentEqualsToken:
            case ts.SyntaxKind.AmpersandEqualsToken:
            case ts.SyntaxKind.CaretEqualsToken:
            case ts.SyntaxKind.BarEqualsToken:
            case ts.SyntaxKind.LessThanLessThanEqualsToken:
            case ts.SyntaxKind.GreaterThanGreaterThanEqualsToken:
            case ts.SyntaxKind.GreaterThanGreaterThanGreaterThanEqualsToken:
                this.expressionIsUnused = false;
                break;
            case ts.SyntaxKind.AmpersandAmpersandToken:
            case ts.SyntaxKind.BarBarToken:
                if (this.hasOption(ALLOW_FAST_NULL_CHECKS) && isTopLevelExpression(node)) {
                    this.expressionIsUnused = !hasCallExpression(node.right);
                    break;
                }
                else {
                    this.expressionIsUnused = true;
                    break;
                }
            default:
                this.expressionIsUnused = true;
        }
    };
    NoUnusedExpressionWalker.prototype.visitPrefixUnaryExpression = function (node) {
        _super.prototype.visitPrefixUnaryExpression.call(this, node);
        switch (node.operator) {
            case ts.SyntaxKind.PlusPlusToken:
            case ts.SyntaxKind.MinusMinusToken:
                this.expressionIsUnused = false;
                break;
            default:
                this.expressionIsUnused = true;
        }
    };
    NoUnusedExpressionWalker.prototype.visitPostfixUnaryExpression = function (node) {
        _super.prototype.visitPostfixUnaryExpression.call(this, node);
        this.expressionIsUnused = false; // the only kinds of postfix expressions are postincrement and postdecrement
    };
    NoUnusedExpressionWalker.prototype.visitBlock = function (node) {
        _super.prototype.visitBlock.call(this, node);
        this.expressionIsUnused = true;
    };
    NoUnusedExpressionWalker.prototype.visitArrowFunction = function (node) {
        _super.prototype.visitArrowFunction.call(this, node);
        this.expressionIsUnused = true;
    };
    NoUnusedExpressionWalker.prototype.visitCallExpression = function (node) {
        _super.prototype.visitCallExpression.call(this, node);
        this.expressionIsUnused = false;
    };
    NoUnusedExpressionWalker.prototype.visitNewExpression = function (node) {
        _super.prototype.visitNewExpression.call(this, node);
        this.expressionIsUnused = false;
    };
    NoUnusedExpressionWalker.prototype.visitConditionalExpression = function (node) {
        this.visitNode(node.condition);
        this.expressionIsUnused = true;
        this.visitNode(node.whenTrue);
        var firstExpressionIsUnused = this.expressionIsUnused;
        this.expressionIsUnused = true;
        this.visitNode(node.whenFalse);
        var secondExpressionIsUnused = this.expressionIsUnused;
        // if either expression is unused, then that expression's branch is a no-op unless it's
        // being assigned to something or passed to a function, so consider the entire expression unused
        this.expressionIsUnused = firstExpressionIsUnused || secondExpressionIsUnused;
    };
    NoUnusedExpressionWalker.prototype.checkExpressionUsage = function (node) {
        if (this.expressionIsUnused) {
            var expression = node.expression;
            var kind = expression.kind;
            var isValidStandaloneExpression = kind === ts.SyntaxKind.DeleteExpression
                || kind === ts.SyntaxKind.YieldExpression
                || kind === ts.SyntaxKind.AwaitExpression;
            if (!isValidStandaloneExpression && !NoUnusedExpressionWalker.isDirective(node)) {
                this.addFailureAtNode(node, Rule.FAILURE_STRING);
            }
        }
    };
    return NoUnusedExpressionWalker;
}(Lint.RuleWalker));
exports.NoUnusedExpressionWalker = NoUnusedExpressionWalker;
function hasCallExpression(node) {
    var nodeToCheck = utils_1.unwrapParentheses(node);
    if (nodeToCheck.kind === ts.SyntaxKind.CallExpression) {
        return true;
    }
    if (nodeToCheck.kind === ts.SyntaxKind.BinaryExpression) {
        var operatorToken = nodeToCheck.operatorToken;
        if (operatorToken.kind === ts.SyntaxKind.AmpersandAmpersandToken ||
            operatorToken.kind === ts.SyntaxKind.BarBarToken) {
            return hasCallExpression(nodeToCheck.right);
        }
    }
    return false;
}
function isTopLevelExpression(node) {
    var nodeToCheck = node.parent;
    while (nodeToCheck.kind === ts.SyntaxKind.ParenthesizedExpression) {
        nodeToCheck = nodeToCheck.parent;
    }
    return nodeToCheck.kind === ts.SyntaxKind.ExpressionStatement;
}
var _a, _b, _c;
