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
var OPTION_BRACE = "check-open-brace";
var OPTION_CATCH = "check-catch";
var OPTION_ELSE = "check-else";
var OPTION_FINALLY = "check-finally";
var OPTION_WHITESPACE = "check-whitespace";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var oneLineWalker = new OneLineWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(oneLineWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "one-line",
    description: "Requires the specified tokens to be on the same line as the expression preceding them.",
    optionsDescription: (_a = ["\n            Five arguments may be optionally provided:\n\n            * `\"check-catch\"` checks that `catch` is on the same line as the closing brace for `try`.\n            * `\"check-finally\"` checks that `finally` is on the same line as the closing brace for `catch`.\n            * `\"check-else\"` checks that `else` is on the same line as the closing brace for `if`.\n            * `\"check-open-brace\"` checks that an open brace falls on the same line as its preceding expression.\n            * `\"check-whitespace\"` checks preceding whitespace for the specified tokens."], _a.raw = ["\n            Five arguments may be optionally provided:\n\n            * \\`\"check-catch\"\\` checks that \\`catch\\` is on the same line as the closing brace for \\`try\\`.\n            * \\`\"check-finally\"\\` checks that \\`finally\\` is on the same line as the closing brace for \\`catch\\`.\n            * \\`\"check-else\"\\` checks that \\`else\\` is on the same line as the closing brace for \\`if\\`.\n            * \\`\"check-open-brace\"\\` checks that an open brace falls on the same line as its preceding expression.\n            * \\`\"check-whitespace\"\\` checks preceding whitespace for the specified tokens."], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: ["check-catch", "check-finally", "check-else", "check-open-brace", "check-whitespace"],
        },
        minLength: 0,
        maxLength: 5,
    },
    optionExamples: ['[true, "check-catch", "check-finally", "check-else"]'],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.BRACE_FAILURE_STRING = "misplaced opening brace";
Rule.CATCH_FAILURE_STRING = "misplaced 'catch'";
Rule.ELSE_FAILURE_STRING = "misplaced 'else'";
Rule.FINALLY_FAILURE_STRING = "misplaced 'finally'";
Rule.WHITESPACE_FAILURE_STRING = "missing whitespace";
exports.Rule = Rule;
var OneLineWalker = (function (_super) {
    __extends(OneLineWalker, _super);
    function OneLineWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    OneLineWalker.prototype.visitIfStatement = function (node) {
        var thenStatement = node.thenStatement;
        var thenIsBlock = thenStatement.kind === ts.SyntaxKind.Block;
        if (thenIsBlock) {
            var expressionCloseParen = node.getChildAt(3);
            var thenOpeningBrace = thenStatement.getChildAt(0);
            this.handleOpeningBrace(expressionCloseParen, thenOpeningBrace);
        }
        var elseStatement = node.elseStatement;
        if (elseStatement != null) {
            // find the else keyword
            var elseKeyword = Lint.childOfKind(node, ts.SyntaxKind.ElseKeyword);
            if (elseStatement.kind === ts.SyntaxKind.Block) {
                var elseOpeningBrace = elseStatement.getChildAt(0);
                this.handleOpeningBrace(elseKeyword, elseOpeningBrace);
            }
            if (thenIsBlock && this.hasOption(OPTION_ELSE)) {
                var thenStatementEndLine = this.getLineAndCharacterOfPosition(thenStatement.getEnd()).line;
                var elseKeywordLine = this.getLineAndCharacterOfPosition(elseKeyword.getStart()).line;
                if (thenStatementEndLine !== elseKeywordLine) {
                    this.addFailureAtNode(elseKeyword, Rule.ELSE_FAILURE_STRING);
                }
            }
        }
        _super.prototype.visitIfStatement.call(this, node);
    };
    OneLineWalker.prototype.visitCatchClause = function (node) {
        var catchClosingParen = Lint.childOfKind(node, ts.SyntaxKind.CloseParenToken);
        var catchOpeningBrace = node.block.getChildAt(0);
        this.handleOpeningBrace(catchClosingParen, catchOpeningBrace);
        _super.prototype.visitCatchClause.call(this, node);
    };
    OneLineWalker.prototype.visitTryStatement = function (node) {
        var catchClause = node.catchClause;
        var finallyBlock = node.finallyBlock;
        var finallyKeyword = Lint.childOfKind(node, ts.SyntaxKind.FinallyKeyword);
        // "visit" try block
        var tryKeyword = node.getChildAt(0);
        var tryBlock = node.tryBlock;
        var tryOpeningBrace = tryBlock.getChildAt(0);
        this.handleOpeningBrace(tryKeyword, tryOpeningBrace);
        if (this.hasOption(OPTION_CATCH) && catchClause != null) {
            var tryClosingBrace = node.tryBlock.getChildAt(node.tryBlock.getChildCount() - 1);
            var catchKeyword = catchClause.getChildAt(0);
            var tryClosingBraceLine = this.getLineAndCharacterOfPosition(tryClosingBrace.getEnd()).line;
            var catchKeywordLine = this.getLineAndCharacterOfPosition(catchKeyword.getStart()).line;
            if (tryClosingBraceLine !== catchKeywordLine) {
                this.addFailureAtNode(catchKeyword, Rule.CATCH_FAILURE_STRING);
            }
        }
        if (finallyBlock != null && finallyKeyword != null) {
            var finallyOpeningBrace = finallyBlock.getChildAt(0);
            this.handleOpeningBrace(finallyKeyword, finallyOpeningBrace);
            if (this.hasOption(OPTION_FINALLY)) {
                var previousBlock = catchClause != null ? catchClause.block : node.tryBlock;
                var closingBrace = previousBlock.getChildAt(previousBlock.getChildCount() - 1);
                var closingBraceLine = this.getLineAndCharacterOfPosition(closingBrace.getEnd()).line;
                var finallyKeywordLine = this.getLineAndCharacterOfPosition(finallyKeyword.getStart()).line;
                if (closingBraceLine !== finallyKeywordLine) {
                    this.addFailureAtNode(finallyKeyword, Rule.FINALLY_FAILURE_STRING);
                }
            }
        }
        _super.prototype.visitTryStatement.call(this, node);
    };
    OneLineWalker.prototype.visitForStatement = function (node) {
        this.handleIterationStatement(node);
        _super.prototype.visitForStatement.call(this, node);
    };
    OneLineWalker.prototype.visitForInStatement = function (node) {
        this.handleIterationStatement(node);
        _super.prototype.visitForInStatement.call(this, node);
    };
    OneLineWalker.prototype.visitWhileStatement = function (node) {
        this.handleIterationStatement(node);
        _super.prototype.visitWhileStatement.call(this, node);
    };
    OneLineWalker.prototype.visitBinaryExpression = function (node) {
        var rightkind = node.right.kind;
        var opkind = node.operatorToken.kind;
        if (opkind === ts.SyntaxKind.EqualsToken && rightkind === ts.SyntaxKind.ObjectLiteralExpression) {
            var equalsToken = node.getChildAt(1);
            var openBraceToken = node.right.getChildAt(0);
            this.handleOpeningBrace(equalsToken, openBraceToken);
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    OneLineWalker.prototype.visitVariableDeclaration = function (node) {
        var initializer = node.initializer;
        if (initializer != null && initializer.kind === ts.SyntaxKind.ObjectLiteralExpression) {
            var equalsToken = Lint.childOfKind(node, ts.SyntaxKind.EqualsToken);
            var openBraceToken = initializer.getChildAt(0);
            this.handleOpeningBrace(equalsToken, openBraceToken);
        }
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitDoStatement = function (node) {
        var doKeyword = node.getChildAt(0);
        var statement = node.statement;
        if (statement.kind === ts.SyntaxKind.Block) {
            var openBraceToken = statement.getChildAt(0);
            this.handleOpeningBrace(doKeyword, openBraceToken);
        }
        _super.prototype.visitDoStatement.call(this, node);
    };
    OneLineWalker.prototype.visitModuleDeclaration = function (node) {
        var nameNode = node.name;
        var body = node.body;
        if (body != null && body.kind === ts.SyntaxKind.ModuleBlock) {
            var openBraceToken = body.getChildAt(0);
            this.handleOpeningBrace(nameNode, openBraceToken);
        }
        _super.prototype.visitModuleDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitEnumDeclaration = function (node) {
        var nameNode = node.name;
        var openBraceToken = Lint.childOfKind(node, ts.SyntaxKind.OpenBraceToken);
        this.handleOpeningBrace(nameNode, openBraceToken);
        _super.prototype.visitEnumDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitSwitchStatement = function (node) {
        var closeParenToken = node.getChildAt(3);
        var openBraceToken = node.caseBlock.getChildAt(0);
        this.handleOpeningBrace(closeParenToken, openBraceToken);
        _super.prototype.visitSwitchStatement.call(this, node);
    };
    OneLineWalker.prototype.visitInterfaceDeclaration = function (node) {
        this.handleClassLikeDeclaration(node);
        _super.prototype.visitInterfaceDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitClassDeclaration = function (node) {
        this.handleClassLikeDeclaration(node);
        _super.prototype.visitClassDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitFunctionDeclaration = function (node) {
        this.handleFunctionLikeDeclaration(node);
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitMethodDeclaration = function (node) {
        this.handleFunctionLikeDeclaration(node);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitConstructorDeclaration = function (node) {
        this.handleFunctionLikeDeclaration(node);
        _super.prototype.visitConstructorDeclaration.call(this, node);
    };
    OneLineWalker.prototype.visitArrowFunction = function (node) {
        var body = node.body;
        if (body != null && body.kind === ts.SyntaxKind.Block) {
            var arrowToken = Lint.childOfKind(node, ts.SyntaxKind.EqualsGreaterThanToken);
            var openBraceToken = body.getChildAt(0);
            this.handleOpeningBrace(arrowToken, openBraceToken);
        }
        _super.prototype.visitArrowFunction.call(this, node);
    };
    OneLineWalker.prototype.handleFunctionLikeDeclaration = function (node) {
        var body = node.body;
        if (body != null && body.kind === ts.SyntaxKind.Block) {
            var openBraceToken = body.getChildAt(0);
            if (node.type != null) {
                this.handleOpeningBrace(node.type, openBraceToken);
            }
            else {
                var closeParenToken = Lint.childOfKind(node, ts.SyntaxKind.CloseParenToken);
                this.handleOpeningBrace(closeParenToken, openBraceToken);
            }
        }
    };
    OneLineWalker.prototype.handleClassLikeDeclaration = function (node) {
        var lastNodeOfDeclaration = node.name;
        var openBraceToken = Lint.childOfKind(node, ts.SyntaxKind.OpenBraceToken);
        if (node.heritageClauses != null) {
            lastNodeOfDeclaration = node.heritageClauses[node.heritageClauses.length - 1];
        }
        else if (node.typeParameters != null) {
            lastNodeOfDeclaration = node.typeParameters[node.typeParameters.length - 1];
        }
        this.handleOpeningBrace(lastNodeOfDeclaration, openBraceToken);
    };
    OneLineWalker.prototype.handleIterationStatement = function (node) {
        // last child is the statement, second to last child is the close paren
        var closeParenToken = node.getChildAt(node.getChildCount() - 2);
        var statement = node.statement;
        if (statement.kind === ts.SyntaxKind.Block) {
            var openBraceToken = statement.getChildAt(0);
            this.handleOpeningBrace(closeParenToken, openBraceToken);
        }
    };
    OneLineWalker.prototype.handleOpeningBrace = function (previousNode, openBraceToken) {
        if (previousNode == null || openBraceToken == null) {
            return;
        }
        var previousNodeLine = this.getLineAndCharacterOfPosition(previousNode.getEnd()).line;
        var openBraceLine = this.getLineAndCharacterOfPosition(openBraceToken.getStart()).line;
        var failure;
        if (this.hasOption(OPTION_BRACE) && previousNodeLine !== openBraceLine) {
            failure = Rule.BRACE_FAILURE_STRING;
        }
        else if (this.hasOption(OPTION_WHITESPACE) && previousNode.getEnd() === openBraceToken.getStart()) {
            failure = Rule.WHITESPACE_FAILURE_STRING;
        }
        if (failure) {
            this.addFailureAtNode(openBraceToken, failure);
        }
    };
    return OneLineWalker;
}(Lint.RuleWalker));
var _a;
