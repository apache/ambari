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
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var alignWalker = new AlignWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(alignWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "align",
    description: "Enforces vertical alignment.",
    hasFix: true,
    rationale: "Helps maintain a readable, consistent style in your codebase.",
    optionsDescription: (_a = ["\n            Three arguments may be optionally provided:\n\n            * `\"parameters\"` checks alignment of function parameters.\n            * `\"arguments\"` checks alignment of function call arguments.\n            * `\"statements\"` checks alignment of statements."], _a.raw = ["\n            Three arguments may be optionally provided:\n\n            * \\`\"parameters\"\\` checks alignment of function parameters.\n            * \\`\"arguments\"\\` checks alignment of function call arguments.\n            * \\`\"statements\"\\` checks alignment of statements."], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: ["arguments", "parameters", "statements"],
        },
        minLength: 1,
        maxLength: 3,
    },
    optionExamples: ['[true, "parameters", "statements"]'],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.PARAMETERS_OPTION = "parameters";
Rule.ARGUMENTS_OPTION = "arguments";
Rule.STATEMENTS_OPTION = "statements";
Rule.FAILURE_STRING_SUFFIX = " are not aligned";
exports.Rule = Rule;
var AlignWalker = (function (_super) {
    __extends(AlignWalker, _super);
    function AlignWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    AlignWalker.prototype.visitConstructorDeclaration = function (node) {
        this.checkAlignment(Rule.PARAMETERS_OPTION, node.parameters);
        _super.prototype.visitConstructorDeclaration.call(this, node);
    };
    AlignWalker.prototype.visitFunctionDeclaration = function (node) {
        this.checkAlignment(Rule.PARAMETERS_OPTION, node.parameters);
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    AlignWalker.prototype.visitFunctionExpression = function (node) {
        this.checkAlignment(Rule.PARAMETERS_OPTION, node.parameters);
        _super.prototype.visitFunctionExpression.call(this, node);
    };
    AlignWalker.prototype.visitMethodDeclaration = function (node) {
        this.checkAlignment(Rule.PARAMETERS_OPTION, node.parameters);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    AlignWalker.prototype.visitCallExpression = function (node) {
        this.checkAlignment(Rule.ARGUMENTS_OPTION, node.arguments);
        _super.prototype.visitCallExpression.call(this, node);
    };
    AlignWalker.prototype.visitNewExpression = function (node) {
        this.checkAlignment(Rule.ARGUMENTS_OPTION, node.arguments);
        _super.prototype.visitNewExpression.call(this, node);
    };
    AlignWalker.prototype.visitBlock = function (node) {
        this.checkAlignment(Rule.STATEMENTS_OPTION, node.statements);
        _super.prototype.visitBlock.call(this, node);
    };
    AlignWalker.prototype.checkAlignment = function (kind, nodes) {
        if (nodes == null || nodes.length === 0 || !this.hasOption(kind)) {
            return;
        }
        var prevPos = this.getLineAndCharacterOfPosition(nodes[0].getStart());
        var alignToColumn = prevPos.character;
        // skip first node in list
        for (var _i = 0, _a = nodes.slice(1); _i < _a.length; _i++) {
            var node = _a[_i];
            var curPos = this.getLineAndCharacterOfPosition(node.getStart());
            if (curPos.line !== prevPos.line && curPos.character !== alignToColumn) {
                var diff = alignToColumn - curPos.character;
                var fix = void 0;
                if (0 < diff) {
                    fix = this.createFix(this.appendText(node.getStart(), " ".repeat(diff)));
                }
                else {
                    fix = this.createFix(this.deleteText(node.getStart() + diff, -diff));
                }
                this.addFailureAtNode(node, kind + Rule.FAILURE_STRING_SUFFIX, fix);
            }
            prevPos = curPos;
        }
    };
    return AlignWalker;
}(Lint.RuleWalker));
var _a;
