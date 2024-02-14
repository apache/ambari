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
var ALWAYS_OR_NEVER = {
    enum: ["always", "never"],
    type: "string",
};
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new FunctionWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
Rule.metadata = {
    description: "Require or disallow a space before function parenthesis",
    hasFix: true,
    optionExamples: [
        "true",
        "[true, \"always\"]",
        "[true, \"never\"]",
        "[true, {\"anonymous\": \"always\", \"named\": \"never\", \"asyncArrow\": \"always\"}]",
    ],
    options: {
        properties: {
            anonymous: ALWAYS_OR_NEVER,
            asyncArrow: ALWAYS_OR_NEVER,
            constructor: ALWAYS_OR_NEVER,
            method: ALWAYS_OR_NEVER,
            named: ALWAYS_OR_NEVER,
        },
        type: "object",
    },
    optionsDescription: (_a = ["\n            One argument which is an object which may contain the keys `anonymous`, `named`, and `asyncArrow`\n            These should be set to either `\"always\"` or `\"never\"`.\n\n            * `\"anonymous\"` checks before the opening paren in anonymous functions\n            * `\"named\"` checks before the opening paren in named functions\n            * `\"asyncArrow\"` checks before the opening paren in async arrow functions\n            * `\"method\"` checks before the opening paren in class methods\n            * `\"constructor\"` checks before the opening paren in class constructors\n        "], _a.raw = ["\n            One argument which is an object which may contain the keys \\`anonymous\\`, \\`named\\`, and \\`asyncArrow\\`\n            These should be set to either \\`\"always\"\\` or \\`\"never\"\\`.\n\n            * \\`\"anonymous\"\\` checks before the opening paren in anonymous functions\n            * \\`\"named\"\\` checks before the opening paren in named functions\n            * \\`\"asyncArrow\"\\` checks before the opening paren in async arrow functions\n            * \\`\"method\"\\` checks before the opening paren in class methods\n            * \\`\"constructor\"\\` checks before the opening paren in class constructors\n        "], Lint.Utils.dedent(_a)),
    ruleName: "space-before-function-paren",
    type: "style",
    typescriptOnly: false,
};
Rule.INVALID_WHITESPACE_ERROR = "Spaces before function parens are disallowed";
Rule.MISSING_WHITESPACE_ERROR = "Missing whitespace before function parens";
exports.Rule = Rule;
var FunctionWalker = (function (_super) {
    __extends(FunctionWalker, _super);
    function FunctionWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        // assign constructor now to avoid typescript assuming its a function type
        _this.cachedOptions = { constructor: undefined };
        _this.scanner = ts.createScanner(ts.ScriptTarget.ES5, false, ts.LanguageVariant.Standard, sourceFile.text);
        _this.cacheOptions();
        return _this;
    }
    FunctionWalker.prototype.visitArrowFunction = function (node) {
        var option = this.getOption("asyncArrow");
        var syntaxList = Lint.childOfKind(node, ts.SyntaxKind.SyntaxList);
        var isAsyncArrow = syntaxList.getStart() === node.getStart() && syntaxList.getText() === "async";
        var openParen = isAsyncArrow ? Lint.childOfKind(node, ts.SyntaxKind.OpenParenToken) : undefined;
        this.evaluateRuleAt(openParen, option);
        _super.prototype.visitArrowFunction.call(this, node);
    };
    FunctionWalker.prototype.visitConstructorDeclaration = function (node) {
        var option = this.getOption("constructor");
        var openParen = Lint.childOfKind(node, ts.SyntaxKind.OpenParenToken);
        this.evaluateRuleAt(openParen, option);
        _super.prototype.visitConstructorDeclaration.call(this, node);
    };
    FunctionWalker.prototype.visitFunctionDeclaration = function (node) {
        this.visitFunction(node);
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    FunctionWalker.prototype.visitFunctionExpression = function (node) {
        this.visitFunction(node);
        _super.prototype.visitFunctionExpression.call(this, node);
    };
    FunctionWalker.prototype.visitMethodDeclaration = function (node) {
        this.visitMethod(node);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    FunctionWalker.prototype.visitMethodSignature = function (node) {
        this.visitMethod(node);
        _super.prototype.visitMethodSignature.call(this, node);
    };
    FunctionWalker.prototype.cacheOptions = function () {
        var _this = this;
        var allOptions = this.getOptions();
        var options = allOptions[0];
        var optionNames = ["anonymous", "asyncArrow", "constructor", "method", "named"];
        optionNames.forEach(function (optionName) {
            switch (options) {
                case undefined:
                case "always":
                    _this.cachedOptions[optionName] = "always";
                    break;
                case "never":
                    _this.cachedOptions[optionName] = "never";
                    break;
                default:
                    _this.cachedOptions[optionName] = options[optionName];
            }
        });
    };
    FunctionWalker.prototype.getOption = function (optionName) {
        return this.cachedOptions[optionName];
    };
    FunctionWalker.prototype.evaluateRuleAt = function (openParen, option) {
        if (openParen === undefined || option === undefined) {
            return;
        }
        var hasSpace = this.isSpaceAt(openParen.getStart() - 1);
        if (hasSpace && option === "never") {
            var pos = openParen.getStart() - 1;
            var fix = new Lint.Fix(Rule.metadata.ruleName, [this.deleteText(pos, 1)]);
            this.addFailureAt(pos, 1, Rule.INVALID_WHITESPACE_ERROR, fix);
        }
        else if (!hasSpace && option === "always") {
            var pos = openParen.getStart();
            var fix = new Lint.Fix(Rule.metadata.ruleName, [this.appendText(pos, " ")]);
            this.addFailureAt(pos, 1, Rule.MISSING_WHITESPACE_ERROR, fix);
        }
    };
    FunctionWalker.prototype.isSpaceAt = function (textPos) {
        this.scanner.setTextPos(textPos);
        var prevTokenKind = this.scanner.scan();
        return prevTokenKind === ts.SyntaxKind.WhitespaceTrivia;
    };
    FunctionWalker.prototype.visitFunction = function (node) {
        var identifier = Lint.childOfKind(node, ts.SyntaxKind.Identifier);
        var hasIdentifier = identifier !== undefined && (identifier.getEnd() !== identifier.getStart());
        var optionName = hasIdentifier ? "named" : "anonymous";
        var option = this.getOption(optionName);
        var openParen = Lint.childOfKind(node, ts.SyntaxKind.OpenParenToken);
        this.evaluateRuleAt(openParen, option);
    };
    FunctionWalker.prototype.visitMethod = function (node) {
        var option = this.getOption("method");
        var openParen = Lint.childOfKind(node, ts.SyntaxKind.OpenParenToken);
        this.evaluateRuleAt(openParen, option);
    };
    return FunctionWalker;
}(Lint.RuleWalker));
var _a;
