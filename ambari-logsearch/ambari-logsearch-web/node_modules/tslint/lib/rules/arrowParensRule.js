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
var BAN_SINGLE_ARG_PARENS = "ban-single-arg-parens";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var newParensWalker = new ArrowParensWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(newParensWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "arrow-parens",
    description: "Requires parentheses around the parameters of arrow function definitions.",
    hasFix: true,
    rationale: "Maintains stylistic consistency with other arrow function definitions.",
    optionsDescription: (_a = ["\n            If `", "` is specified, then arrow functions with one parameter\n            must not have parentheses if removing them is allowed by TypeScript."], _a.raw = ["\n            If \\`", "\\` is specified, then arrow functions with one parameter\n            must not have parentheses if removing them is allowed by TypeScript."], Lint.Utils.dedent(_a, BAN_SINGLE_ARG_PARENS)),
    options: {
        type: "string",
        enum: [BAN_SINGLE_ARG_PARENS],
    },
    optionExamples: ["true", "[true, \"" + BAN_SINGLE_ARG_PARENS + "\"]"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_MISSING = "Parentheses are required around the parameters of an arrow function definition";
Rule.FAILURE_STRING_EXISTS = "Parentheses are prohibited around the parameter in this single parameter arrow function";
exports.Rule = Rule;
var ArrowParensWalker = (function (_super) {
    __extends(ArrowParensWalker, _super);
    function ArrowParensWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.avoidOnSingleParameter = _this.hasOption(BAN_SINGLE_ARG_PARENS);
        return _this;
    }
    ArrowParensWalker.prototype.visitArrowFunction = function (node) {
        if (node.parameters.length === 1 && node.typeParameters === undefined) {
            var parameter = node.parameters[0];
            var openParen = node.getFirstToken();
            var openParenIndex = 0;
            if (openParen.kind === ts.SyntaxKind.AsyncKeyword) {
                openParen = node.getChildAt(1);
                openParenIndex = 1;
            }
            var hasParens = openParen.kind === ts.SyntaxKind.OpenParenToken;
            if (!hasParens && !this.avoidOnSingleParameter) {
                var fix = this.createFix(this.appendText(parameter.getStart(), "("), this.appendText(parameter.getEnd(), ")"));
                this.addFailureAtNode(parameter, Rule.FAILURE_STRING_MISSING, fix);
            }
            else if (hasParens && this.avoidOnSingleParameter && isSimpleParameter(parameter)) {
                // Skip over the parameter to get the closing parenthesis
                var closeParen = node.getChildAt(openParenIndex + 2);
                var fix = this.createFix(this.deleteText(openParen.getStart(), 1), this.deleteText(closeParen.getStart(), 1));
                this.addFailureAtNode(parameter, Rule.FAILURE_STRING_EXISTS, fix);
            }
        }
        _super.prototype.visitArrowFunction.call(this, node);
    };
    return ArrowParensWalker;
}(Lint.RuleWalker));
function isSimpleParameter(parameter) {
    return parameter.name.kind === ts.SyntaxKind.Identifier
        && parameter.dotDotDotToken === undefined
        && parameter.initializer === undefined
        && parameter.questionToken === undefined
        && parameter.type === undefined;
}
var _a;
