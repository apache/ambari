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
var utils = require("tsutils");
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.FAILURE_STRING_FACTORY = function () {
        return "Missing blank line before return";
    };
    ;
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NewlineBeforeReturnWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "newline-before-return",
    description: "Enforces blank line before return when not the only line in the block.",
    rationale: "Helps maintain a readable style in your codebase.",
    optionsDescription: "Not configurable.",
    options: {},
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: false,
};
exports.Rule = Rule;
var NewlineBeforeReturnWalker = (function (_super) {
    __extends(NewlineBeforeReturnWalker, _super);
    function NewlineBeforeReturnWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NewlineBeforeReturnWalker.prototype.visitReturnStatement = function (node) {
        _super.prototype.visitReturnStatement.call(this, node);
        var parent = node.parent;
        if (!utils.isBlockLike(parent)) {
            // `node` is the only statement within this "block scope". No need to do any further validation.
            return;
        }
        var index = parent.statements.indexOf(node);
        if (index === 0) {
            // First element in the block so no need to check for the blank line.
            return;
        }
        var start = node.getStart();
        var comments = ts.getLeadingCommentRanges(this.getSourceFile().text, node.getFullStart());
        if (comments) {
            // In case the return statement is preceded by a comment, we use comments start as the starting position
            start = comments[0].pos;
        }
        var lc = this.getLineAndCharacterOfPosition(start);
        var prev = parent.statements[index - 1];
        var prevLine = this.getLineAndCharacterOfPosition(prev.getEnd()).line;
        if (prevLine >= lc.line - 1) {
            // Previous statement is on the same or previous line
            this.addFailureFromStartToEnd(start, start, Rule.FAILURE_STRING_FACTORY());
        }
    };
    return NewlineBeforeReturnWalker;
}(Lint.RuleWalker));
