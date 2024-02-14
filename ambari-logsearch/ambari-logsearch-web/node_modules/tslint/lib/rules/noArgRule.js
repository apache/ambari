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
        return this.applyWithWalker(new NoArgWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-arg",
    description: "Disallows use of `arguments.callee`.",
    rationale: (_a = ["\n            Using `arguments.callee` makes various performance optimizations impossible.\n            See [MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/arguments/callee)\n            for more details on why to avoid `arguments.callee`."], _a.raw = ["\n            Using \\`arguments.callee\\` makes various performance optimizations impossible.\n            See [MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/arguments/callee)\n            for more details on why to avoid \\`arguments.callee\\`."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Access to arguments.callee is forbidden";
exports.Rule = Rule;
var NoArgWalker = (function (_super) {
    __extends(NoArgWalker, _super);
    function NoArgWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoArgWalker.prototype.visitPropertyAccessExpression = function (node) {
        var expression = node.expression;
        var name = node.name;
        if (expression.kind === ts.SyntaxKind.Identifier && name.text === "callee") {
            var identifierExpression = expression;
            if (identifierExpression.text === "arguments") {
                this.addFailureAtNode(expression, Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitPropertyAccessExpression.call(this, node);
    };
    return NoArgWalker;
}(Lint.RuleWalker));
var _a;
