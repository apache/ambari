/**
 * @license
 * Copyright 2015 Palantir Technologies, Inc.
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
        var walker = new NoConditionalAssignmentWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(walker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-conditional-assignment",
    description: "Disallows any type of assignment in conditionals.",
    descriptionDetails: "This applies to `do-while`, `for`, `if`, and `while` statements.",
    rationale: (_a = ["\n            Assignments in conditionals are often typos:\n            for example `if (var1 = var2)` instead of `if (var1 == var2)`.\n            They also can be an indicator of overly clever code which decreases maintainability."], _a.raw = ["\n            Assignments in conditionals are often typos:\n            for example \\`if (var1 = var2)\\` instead of \\`if (var1 == var2)\\`.\n            They also can be an indicator of overly clever code which decreases maintainability."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Assignments in conditional expressions are forbidden";
exports.Rule = Rule;
var NoConditionalAssignmentWalker = (function (_super) {
    __extends(NoConditionalAssignmentWalker, _super);
    function NoConditionalAssignmentWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.isInConditional = false;
        return _this;
    }
    NoConditionalAssignmentWalker.prototype.visitIfStatement = function (node) {
        this.validateConditionalExpression(node.expression);
        _super.prototype.visitIfStatement.call(this, node);
    };
    NoConditionalAssignmentWalker.prototype.visitWhileStatement = function (node) {
        this.validateConditionalExpression(node.expression);
        _super.prototype.visitWhileStatement.call(this, node);
    };
    NoConditionalAssignmentWalker.prototype.visitDoStatement = function (node) {
        this.validateConditionalExpression(node.expression);
        _super.prototype.visitDoStatement.call(this, node);
    };
    NoConditionalAssignmentWalker.prototype.visitForStatement = function (node) {
        if (node.condition != null) {
            this.validateConditionalExpression(node.condition);
        }
        _super.prototype.visitForStatement.call(this, node);
    };
    NoConditionalAssignmentWalker.prototype.visitBinaryExpression = function (expression) {
        if (this.isInConditional) {
            this.checkForAssignment(expression);
        }
        _super.prototype.visitBinaryExpression.call(this, expression);
    };
    NoConditionalAssignmentWalker.prototype.validateConditionalExpression = function (expression) {
        this.isInConditional = true;
        if (expression.kind === ts.SyntaxKind.BinaryExpression) {
            // check for simple assignment in a conditional, like `if (a = 1) {`
            this.checkForAssignment(expression);
        }
        // walk the children of the conditional expression for nested assignments, like `if ((a = 1) && (b == 1)) {`
        this.walkChildren(expression);
        this.isInConditional = false;
    };
    NoConditionalAssignmentWalker.prototype.checkForAssignment = function (expression) {
        if (isAssignmentToken(expression.operatorToken)) {
            this.addFailureAtNode(expression, Rule.FAILURE_STRING);
        }
    };
    return NoConditionalAssignmentWalker;
}(Lint.RuleWalker));
function isAssignmentToken(token) {
    return token.kind >= ts.SyntaxKind.FirstAssignment && token.kind <= ts.SyntaxKind.LastAssignment;
}
var _a;
