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
var OPTION_MULTILINE = "multiline";
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
    ruleName: "arrow-return-shorthand",
    description: "Suggests to convert `() => { return x; }` to `() => x`.",
    hasFix: true,
    optionsDescription: (_a = ["\n            If `", "` is specified, then this will warn even if the function spans multiple lines."], _a.raw = ["\n            If \\`", "\\` is specified, then this will warn even if the function spans multiple lines."], Lint.Utils.dedent(_a, OPTION_MULTILINE)),
    options: {
        type: "string",
        enum: [OPTION_MULTILINE],
    },
    optionExamples: [
        "[true]",
        "[true, \"" + OPTION_MULTILINE + "\"]",
    ],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "This arrow function body can be simplified by omitting the curly braces and the keyword 'return'.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitArrowFunction = function (node) {
        if (node.body && node.body.kind === ts.SyntaxKind.Block) {
            var expr = getSimpleReturnExpression(node.body);
            if (expr !== undefined && (this.hasOption(OPTION_MULTILINE) || !this.isMultiline(node.body))) {
                this.addFailureAtNode(node.body, Rule.FAILURE_STRING, this.createArrowFunctionFix(node, node.body, expr));
            }
        }
        _super.prototype.visitArrowFunction.call(this, node);
    };
    Walker.prototype.isMultiline = function (node) {
        var _this = this;
        var getLine = function (position) { return _this.getLineAndCharacterOfPosition(position).line; };
        return getLine(node.getEnd()) > getLine(node.getStart());
    };
    Walker.prototype.createArrowFunctionFix = function (arrowFunction, body, expr) {
        var text = this.getSourceFile().text;
        var statement = expr.parent;
        var returnKeyword = Lint.childOfKind(statement, ts.SyntaxKind.ReturnKeyword);
        var arrow = Lint.childOfKind(arrowFunction, ts.SyntaxKind.EqualsGreaterThanToken);
        var openBrace = Lint.childOfKind(body, ts.SyntaxKind.OpenBraceToken);
        var closeBrace = Lint.childOfKind(body, ts.SyntaxKind.CloseBraceToken);
        var semicolon = Lint.childOfKind(statement, ts.SyntaxKind.SemicolonToken);
        var anyComments = hasComments(arrow) || hasComments(openBrace) || hasComments(statement) || hasComments(returnKeyword) ||
            hasComments(expr) || (semicolon && hasComments(semicolon)) || hasComments(closeBrace);
        return anyComments ? undefined : this.createFix.apply(this, (expr.kind === ts.SyntaxKind.ObjectLiteralExpression ? [
            this.appendText(expr.getStart(), "("),
            this.appendText(expr.getEnd(), ")"),
        ] : []).concat([
            // " {"
            this.deleteFromTo(arrow.end, openBrace.end),
            // "return "
            this.deleteFromTo(statement.getStart(), expr.getStart()),
            // " }" (may include semicolon)
            this.deleteFromTo(expr.end, closeBrace.end)]));
        function hasComments(node) {
            return ts.getTrailingCommentRanges(text, node.getEnd()) !== undefined;
        }
    };
    return Walker;
}(Lint.RuleWalker));
/** Given `{ return x; }`, return `x`. */
function getSimpleReturnExpression(block) {
    return block.statements.length === 1 && block.statements[0].kind === ts.SyntaxKind.ReturnStatement
        ? block.statements[0].expression
        : undefined;
}
var _a;
