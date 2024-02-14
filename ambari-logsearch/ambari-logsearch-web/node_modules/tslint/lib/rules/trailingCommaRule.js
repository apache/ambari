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
        return this.applyWithWalker(new TrailingCommaWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "trailing-comma",
    description: (_a = ["\n            Requires or disallows trailing commas in array and object literals, destructuring assignments, function and tuple typings,\n            named imports and function parameters."], _a.raw = ["\n            Requires or disallows trailing commas in array and object literals, destructuring assignments, function and tuple typings,\n            named imports and function parameters."], Lint.Utils.dedent(_a)),
    hasFix: true,
    optionsDescription: (_b = ["\n            One argument which is an object with the keys `multiline` and `singleline`.\n            Both should be set to either `\"always\"` or `\"never\"`.\n\n            * `\"multiline\"` checks multi-line object literals.\n            * `\"singleline\"` checks single-line object literals.\n\n            A array is considered \"multiline\" if its closing bracket is on a line\n            after the last array element. The same general logic is followed for\n            object literals, function and tuple typings, named import statements\n            and function parameters."], _b.raw = ["\n            One argument which is an object with the keys \\`multiline\\` and \\`singleline\\`.\n            Both should be set to either \\`\"always\"\\` or \\`\"never\"\\`.\n\n            * \\`\"multiline\"\\` checks multi-line object literals.\n            * \\`\"singleline\"\\` checks single-line object literals.\n\n            A array is considered \"multiline\" if its closing bracket is on a line\n            after the last array element. The same general logic is followed for\n            object literals, function and tuple typings, named import statements\n            and function parameters."], Lint.Utils.dedent(_b)),
    options: {
        type: "object",
        properties: {
            multiline: {
                type: "string",
                enum: ["always", "never"],
            },
            singleline: {
                type: "string",
                enum: ["always", "never"],
            },
        },
        additionalProperties: false,
    },
    optionExamples: ['[true, {"multiline": "always", "singleline": "never"}]'],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_NEVER = "Unnecessary trailing comma";
Rule.FAILURE_STRING_ALWAYS = "Missing trailing comma";
exports.Rule = Rule;
var TrailingCommaWalker = (function (_super) {
    __extends(TrailingCommaWalker, _super);
    function TrailingCommaWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    TrailingCommaWalker.prototype.visitArrayLiteralExpression = function (node) {
        this.lintChildNodeWithIndex(node, 1);
        _super.prototype.visitArrayLiteralExpression.call(this, node);
    };
    TrailingCommaWalker.prototype.visitArrowFunction = function (node) {
        this.lintChildNodeWithIndex(node, 1);
        _super.prototype.visitArrowFunction.call(this, node);
    };
    TrailingCommaWalker.prototype.visitBindingPattern = function (node) {
        if (node.kind === ts.SyntaxKind.ArrayBindingPattern || node.kind === ts.SyntaxKind.ObjectBindingPattern) {
            this.lintChildNodeWithIndex(node, 1);
        }
        _super.prototype.visitBindingPattern.call(this, node);
    };
    TrailingCommaWalker.prototype.visitCallExpression = function (node) {
        this.lintNode(node);
        _super.prototype.visitCallExpression.call(this, node);
    };
    TrailingCommaWalker.prototype.visitClassDeclaration = function (node) {
        this.lintNode(node);
        _super.prototype.visitClassDeclaration.call(this, node);
    };
    TrailingCommaWalker.prototype.visitConstructSignature = function (node) {
        this.lintNode(node);
        _super.prototype.visitConstructSignature.call(this, node);
    };
    TrailingCommaWalker.prototype.visitConstructorDeclaration = function (node) {
        this.lintNode(node);
        _super.prototype.visitConstructorDeclaration.call(this, node);
    };
    TrailingCommaWalker.prototype.visitConstructorType = function (node) {
        this.lintNode(node);
        _super.prototype.visitConstructorType.call(this, node);
    };
    TrailingCommaWalker.prototype.visitEnumDeclaration = function (node) {
        this.lintNode(node, true);
        _super.prototype.visitEnumDeclaration.call(this, node);
    };
    TrailingCommaWalker.prototype.visitFunctionType = function (node) {
        this.lintChildNodeWithIndex(node, 1);
        _super.prototype.visitFunctionType.call(this, node);
    };
    TrailingCommaWalker.prototype.visitFunctionDeclaration = function (node) {
        this.lintNode(node);
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    TrailingCommaWalker.prototype.visitFunctionExpression = function (node) {
        this.lintNode(node);
        _super.prototype.visitFunctionExpression.call(this, node);
    };
    TrailingCommaWalker.prototype.visitInterfaceDeclaration = function (node) {
        this.lintNode(node);
        _super.prototype.visitInterfaceDeclaration.call(this, node);
    };
    TrailingCommaWalker.prototype.visitMethodDeclaration = function (node) {
        this.lintNode(node);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    TrailingCommaWalker.prototype.visitMethodSignature = function (node) {
        this.lintNode(node);
        _super.prototype.visitMethodSignature.call(this, node);
    };
    TrailingCommaWalker.prototype.visitNamedImports = function (node) {
        this.lintChildNodeWithIndex(node, 1);
        _super.prototype.visitNamedImports.call(this, node);
    };
    TrailingCommaWalker.prototype.visitNewExpression = function (node) {
        this.lintNode(node);
        _super.prototype.visitNewExpression.call(this, node);
    };
    TrailingCommaWalker.prototype.visitObjectLiteralExpression = function (node) {
        this.lintChildNodeWithIndex(node, 1);
        _super.prototype.visitObjectLiteralExpression.call(this, node);
    };
    TrailingCommaWalker.prototype.visitSetAccessor = function (node) {
        this.lintNode(node);
        _super.prototype.visitSetAccessor.call(this, node);
    };
    TrailingCommaWalker.prototype.visitTupleType = function (node) {
        this.lintChildNodeWithIndex(node, 1);
        _super.prototype.visitTupleType.call(this, node);
    };
    TrailingCommaWalker.prototype.visitTypeLiteral = function (node) {
        this.lintNode(node);
        // object type literals need to be inspected separately because they
        // have a different syntax list wrapper token, and they can be semicolon delimited
        var children = node.getChildren();
        for (var i = 0; i < children.length - 2; i++) {
            if (children[i].kind === ts.SyntaxKind.OpenBraceToken &&
                children[i + 1].kind === ts.SyntaxKind.SyntaxList &&
                children[i + 2].kind === ts.SyntaxKind.CloseBraceToken) {
                var grandChildren = children[i + 1].getChildren();
                // the AST is different from the grammar spec. The semicolons are included as tokens as part of *Signature,
                // as opposed to optionals alongside it. So instead of children[i + 1] having
                // [ PropertySignature, Semicolon, PropertySignature, Semicolon ], the AST is
                // [ PropertySignature, PropertySignature], where the Semicolons are under PropertySignature
                var hasSemicolon = grandChildren.some(function (grandChild) {
                    return Lint.childOfKind(grandChild, ts.SyntaxKind.SemicolonToken) !== undefined;
                });
                if (!hasSemicolon) {
                    var endLineOfClosingElement = this.getSourceFile().getLineAndCharacterOfPosition(children[i + 2].getEnd()).line;
                    this.lintChildNodeWithIndex(children[i + 1], grandChildren.length - 1, endLineOfClosingElement);
                }
            }
        }
        _super.prototype.visitTypeLiteral.call(this, node);
    };
    TrailingCommaWalker.prototype.visitTypeReference = function (node) {
        this.lintNode(node);
        _super.prototype.visitTypeReference.call(this, node);
    };
    TrailingCommaWalker.prototype.lintNode = function (node, includeBraces) {
        var _this = this;
        var children = node.getChildren();
        var syntaxListWrapperTokens = (includeBraces === true) ?
            TrailingCommaWalker.SYNTAX_LIST_WRAPPER_TOKENS : TrailingCommaWalker.SYNTAX_LIST_WRAPPER_TOKENS.slice(1);
        var _loop_1 = function (i) {
            syntaxListWrapperTokens.forEach(function (_a) {
                var openToken = _a[0], closeToken = _a[1];
                if (children[i].kind === openToken &&
                    children[i + 1].kind === ts.SyntaxKind.SyntaxList &&
                    children[i + 2].kind === closeToken) {
                    _this.lintChildNodeWithIndex(node, i + 1);
                }
            });
        };
        for (var i = 0; i < children.length - 2; i++) {
            _loop_1(i);
        }
    };
    TrailingCommaWalker.prototype.lintChildNodeWithIndex = function (node, childNodeIndex, endLineOfClosingElement) {
        var child = node.getChildAt(childNodeIndex);
        if (child != null) {
            var grandChildren = child.getChildren();
            if (grandChildren.length > 0) {
                var lastGrandChild = grandChildren[grandChildren.length - 1];
                var hasTrailingComma = lastGrandChild.kind === ts.SyntaxKind.CommaToken;
                var endLineOfLastElement = this.getSourceFile().getLineAndCharacterOfPosition(lastGrandChild.getEnd()).line;
                if (endLineOfClosingElement === undefined) {
                    var closingElementNode = node.getChildAt(childNodeIndex + 1);
                    if (closingElementNode == null) {
                        closingElementNode = node;
                    }
                    endLineOfClosingElement = this.getSourceFile().getLineAndCharacterOfPosition(closingElementNode.getEnd()).line;
                }
                var isMultiline = endLineOfClosingElement !== endLineOfLastElement;
                var option = this.getOption(isMultiline ? "multiline" : "singleline");
                if (hasTrailingComma && option === "never") {
                    var failureStart = lastGrandChild.getStart();
                    var fix = this.createFix(this.deleteText(failureStart, 1));
                    this.addFailureAt(failureStart, 1, Rule.FAILURE_STRING_NEVER, fix);
                }
                else if (!hasTrailingComma && option === "always") {
                    var failureStart = lastGrandChild.getEnd();
                    var fix = this.createFix(this.appendText(failureStart, ","));
                    this.addFailureAt(failureStart - 1, 1, Rule.FAILURE_STRING_ALWAYS, fix);
                }
            }
        }
    };
    TrailingCommaWalker.prototype.getOption = function (option) {
        var allOptions = this.getOptions();
        if (allOptions == null || allOptions.length === 0) {
            return null;
        }
        return allOptions[0][option];
    };
    return TrailingCommaWalker;
}(Lint.RuleWalker));
TrailingCommaWalker.SYNTAX_LIST_WRAPPER_TOKENS = [
    [ts.SyntaxKind.OpenBraceToken, ts.SyntaxKind.CloseBraceToken],
    [ts.SyntaxKind.OpenBracketToken, ts.SyntaxKind.CloseBracketToken],
    [ts.SyntaxKind.OpenParenToken, ts.SyntaxKind.CloseParenToken],
];
var _a, _b;
