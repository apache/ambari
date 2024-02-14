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
        return this.applyWithWalker(new LabelPositionWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "label-position",
    description: "Only allows labels in sensible locations.",
    descriptionDetails: "This rule only allows labels to be on `do/for/while/switch` statements.",
    rationale: (_a = ["\n            Labels in JavaScript only can be used in conjunction with `break` or `continue`,\n            constructs meant to be used for loop flow control. While you can theoretically use\n            labels on any block statement in JS, it is considered poor code structure to do so."], _a.raw = ["\n            Labels in JavaScript only can be used in conjunction with \\`break\\` or \\`continue\\`,\n            constructs meant to be used for loop flow control. While you can theoretically use\n            labels on any block statement in JS, it is considered poor code structure to do so."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "unexpected label on statement";
exports.Rule = Rule;
var LabelPositionWalker = (function (_super) {
    __extends(LabelPositionWalker, _super);
    function LabelPositionWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    LabelPositionWalker.prototype.visitLabeledStatement = function (node) {
        var statement = node.statement;
        if (statement.kind !== ts.SyntaxKind.DoStatement
            && statement.kind !== ts.SyntaxKind.ForStatement
            && statement.kind !== ts.SyntaxKind.ForInStatement
            && statement.kind !== ts.SyntaxKind.ForOfStatement
            && statement.kind !== ts.SyntaxKind.WhileStatement
            && statement.kind !== ts.SyntaxKind.SwitchStatement) {
            this.addFailureAtNode(node.label, Rule.FAILURE_STRING);
        }
        _super.prototype.visitLabeledStatement.call(this, node);
    };
    return LabelPositionWalker;
}(Lint.RuleWalker));
var _a;
