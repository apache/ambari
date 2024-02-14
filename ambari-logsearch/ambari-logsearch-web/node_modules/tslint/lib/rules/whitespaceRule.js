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
var utils = require("tsutils");
var ts = require("typescript");
var Lint = require("../index");
var OPTION_BRANCH = "check-branch";
var OPTION_DECL = "check-decl";
var OPTION_OPERATOR = "check-operator";
var OPTION_MODULE = "check-module";
var OPTION_SEPARATOR = "check-separator";
var OPTION_TYPE = "check-type";
var OPTION_TYPECAST = "check-typecast";
var OPTION_PREBLOCK = "check-preblock";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new WhitespaceWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "whitespace",
    description: "Enforces whitespace style conventions.",
    rationale: "Helps maintain a readable, consistent style in your codebase.",
    optionsDescription: (_a = ["\n            Eight arguments may be optionally provided:\n\n            * `\"check-branch\"` checks branching statements (`if`/`else`/`for`/`while`) are followed by whitespace.\n            * `\"check-decl\"`checks that variable declarations have whitespace around the equals token.\n            * `\"check-operator\"` checks for whitespace around operator tokens.\n            * `\"check-module\"` checks for whitespace in import & export statements.\n            * `\"check-separator\"` checks for whitespace after separator tokens (`,`/`;`).\n            * `\"check-type\"` checks for whitespace before a variable type specification.\n            * `\"check-typecast\"` checks for whitespace between a typecast and its target.\n            * `\"check-preblock\"` checks for whitespace before the opening brace of a block"], _a.raw = ["\n            Eight arguments may be optionally provided:\n\n            * \\`\"check-branch\"\\` checks branching statements (\\`if\\`/\\`else\\`/\\`for\\`/\\`while\\`) are followed by whitespace.\n            * \\`\"check-decl\"\\`checks that variable declarations have whitespace around the equals token.\n            * \\`\"check-operator\"\\` checks for whitespace around operator tokens.\n            * \\`\"check-module\"\\` checks for whitespace in import & export statements.\n            * \\`\"check-separator\"\\` checks for whitespace after separator tokens (\\`,\\`/\\`;\\`).\n            * \\`\"check-type\"\\` checks for whitespace before a variable type specification.\n            * \\`\"check-typecast\"\\` checks for whitespace between a typecast and its target.\n            * \\`\"check-preblock\"\\` checks for whitespace before the opening brace of a block"], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: ["check-branch", "check-decl", "check-operator", "check-module",
                "check-separator", "check-type", "check-typecast", "check-preblock"],
        },
        minLength: 0,
        maxLength: 7,
    },
    optionExamples: ['[true, "check-branch", "check-operator", "check-typecast"]'],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "missing whitespace";
exports.Rule = Rule;
var WhitespaceWalker = (function (_super) {
    __extends(WhitespaceWalker, _super);
    function WhitespaceWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.scanner = ts.createScanner(ts.ScriptTarget.ES5, false, ts.LanguageVariant.Standard, sourceFile.text);
        return _this;
    }
    WhitespaceWalker.prototype.visitSourceFile = function (node) {
        var _this = this;
        _super.prototype.visitSourceFile.call(this, node);
        var prevTokenShouldBeFollowedByWhitespace = false;
        utils.forEachTokenWithTrivia(node, function (_text, tokenKind, range, parent) {
            if (tokenKind === ts.SyntaxKind.WhitespaceTrivia ||
                tokenKind === ts.SyntaxKind.NewLineTrivia ||
                tokenKind === ts.SyntaxKind.EndOfFileToken) {
                prevTokenShouldBeFollowedByWhitespace = false;
                return;
            }
            else if (prevTokenShouldBeFollowedByWhitespace) {
                _this.addMissingWhitespaceErrorAt(range.pos);
                prevTokenShouldBeFollowedByWhitespace = false;
            }
            // check for trailing space after the given tokens
            switch (tokenKind) {
                case ts.SyntaxKind.CatchKeyword:
                case ts.SyntaxKind.ForKeyword:
                case ts.SyntaxKind.IfKeyword:
                case ts.SyntaxKind.SwitchKeyword:
                case ts.SyntaxKind.WhileKeyword:
                case ts.SyntaxKind.WithKeyword:
                    if (_this.hasOption(OPTION_BRANCH)) {
                        prevTokenShouldBeFollowedByWhitespace = true;
                    }
                    break;
                case ts.SyntaxKind.CommaToken:
                case ts.SyntaxKind.SemicolonToken:
                    if (_this.hasOption(OPTION_SEPARATOR)) {
                        prevTokenShouldBeFollowedByWhitespace = true;
                    }
                    break;
                case ts.SyntaxKind.EqualsToken:
                    if (_this.hasOption(OPTION_DECL) && parent.kind !== ts.SyntaxKind.JsxAttribute) {
                        prevTokenShouldBeFollowedByWhitespace = true;
                    }
                    break;
                case ts.SyntaxKind.ColonToken:
                    if (_this.hasOption(OPTION_TYPE)) {
                        prevTokenShouldBeFollowedByWhitespace = true;
                    }
                    break;
                case ts.SyntaxKind.ImportKeyword:
                case ts.SyntaxKind.ExportKeyword:
                case ts.SyntaxKind.FromKeyword:
                    if (_this.hasOption(OPTION_MODULE)) {
                        prevTokenShouldBeFollowedByWhitespace = true;
                    }
                    break;
                default:
                    break;
            }
        });
    };
    WhitespaceWalker.prototype.visitArrowFunction = function (node) {
        this.checkEqualsGreaterThanTokenInNode(node);
        _super.prototype.visitArrowFunction.call(this, node);
    };
    // check for spaces between the operator symbol (except in the case of comma statements)
    WhitespaceWalker.prototype.visitBinaryExpression = function (node) {
        if (this.hasOption(OPTION_OPERATOR) && node.operatorToken.kind !== ts.SyntaxKind.CommaToken) {
            this.checkForTrailingWhitespace(node.left.getEnd());
            this.checkForTrailingWhitespace(node.right.getFullStart());
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    WhitespaceWalker.prototype.visitBlock = function (block) {
        if (this.hasOption(OPTION_PREBLOCK)) {
            this.checkForTrailingWhitespace(block.getFullStart());
        }
        _super.prototype.visitBlock.call(this, block);
    };
    // check for spaces between ternary operator symbols
    WhitespaceWalker.prototype.visitConditionalExpression = function (node) {
        if (this.hasOption(OPTION_OPERATOR)) {
            this.checkForTrailingWhitespace(node.condition.getEnd());
            this.checkForTrailingWhitespace(node.whenTrue.getFullStart());
            this.checkForTrailingWhitespace(node.whenTrue.getEnd());
        }
        _super.prototype.visitConditionalExpression.call(this, node);
    };
    WhitespaceWalker.prototype.visitConstructorType = function (node) {
        this.checkEqualsGreaterThanTokenInNode(node);
        _super.prototype.visitConstructorType.call(this, node);
    };
    WhitespaceWalker.prototype.visitExportAssignment = function (node) {
        if (this.hasOption(OPTION_MODULE)) {
            var exportKeyword = node.getChildAt(0);
            var position = exportKeyword.getEnd();
            this.checkForTrailingWhitespace(position);
        }
        _super.prototype.visitExportAssignment.call(this, node);
    };
    WhitespaceWalker.prototype.visitFunctionType = function (node) {
        this.checkEqualsGreaterThanTokenInNode(node);
        _super.prototype.visitFunctionType.call(this, node);
    };
    WhitespaceWalker.prototype.visitImportDeclaration = function (node) {
        var importClause = node.importClause;
        if (this.hasOption(OPTION_MODULE) && importClause != null) {
            // an import clause can have _both_ named bindings and a name (the latter for the default import)
            // but the named bindings always come last, so we only need to check that for whitespace
            var position = void 0;
            if (importClause.namedBindings !== undefined) {
                position = importClause.namedBindings.getEnd();
            }
            else if (importClause.name !== undefined) {
                position = importClause.name.getEnd();
            }
            if (position !== undefined) {
                this.checkForTrailingWhitespace(position);
            }
        }
        _super.prototype.visitImportDeclaration.call(this, node);
    };
    WhitespaceWalker.prototype.visitImportEqualsDeclaration = function (node) {
        if (this.hasOption(OPTION_MODULE)) {
            var position = node.name.getEnd();
            this.checkForTrailingWhitespace(position);
        }
        _super.prototype.visitImportEqualsDeclaration.call(this, node);
    };
    WhitespaceWalker.prototype.visitTypeAssertionExpression = function (node) {
        if (this.hasOption(OPTION_TYPECAST)) {
            var position = node.expression.getFullStart();
            this.checkForTrailingWhitespace(position);
        }
        _super.prototype.visitTypeAssertionExpression.call(this, node);
    };
    WhitespaceWalker.prototype.visitVariableDeclaration = function (node) {
        if (this.hasOption(OPTION_DECL) && node.initializer != null) {
            if (node.type != null) {
                this.checkForTrailingWhitespace(node.type.getEnd());
            }
            else {
                this.checkForTrailingWhitespace(node.name.getEnd());
            }
        }
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    WhitespaceWalker.prototype.checkEqualsGreaterThanTokenInNode = function (node) {
        if (!this.hasOption(OPTION_OPERATOR)) {
            return;
        }
        var equalsGreaterThanToken = Lint.childOfKind(node, ts.SyntaxKind.EqualsGreaterThanToken);
        // condition so we don't crash if the arrow is somehow missing
        if (equalsGreaterThanToken === undefined) {
            return;
        }
        this.checkForTrailingWhitespace(equalsGreaterThanToken.getFullStart());
        this.checkForTrailingWhitespace(equalsGreaterThanToken.getEnd());
    };
    WhitespaceWalker.prototype.checkForTrailingWhitespace = function (position) {
        this.scanner.setTextPos(position);
        var nextTokenType = this.scanner.scan();
        if (nextTokenType !== ts.SyntaxKind.WhitespaceTrivia
            && nextTokenType !== ts.SyntaxKind.NewLineTrivia
            && nextTokenType !== ts.SyntaxKind.EndOfFileToken) {
            this.addMissingWhitespaceErrorAt(position);
        }
    };
    WhitespaceWalker.prototype.addMissingWhitespaceErrorAt = function (position) {
        var fix = this.createFix(this.appendText(position, " "));
        this.addFailureAt(position, 1, Rule.FAILURE_STRING, fix);
    };
    return WhitespaceWalker;
}(Lint.RuleWalker));
var _a;
