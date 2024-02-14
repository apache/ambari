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
        return this.applyWithWalker(new NoEvalWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-eval",
    description: "Disallows `eval` function invocations.",
    rationale: (_a = ["\n            `eval()` is dangerous as it allows arbitrary code execution with full privileges. There are\n            [alternatives](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval)\n            for most of the use cases for `eval()`."], _a.raw = ["\n            \\`eval()\\` is dangerous as it allows arbitrary code execution with full privileges. There are\n            [alternatives](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval)\n            for most of the use cases for \\`eval()\\`."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "forbidden eval";
exports.Rule = Rule;
var NoEvalWalker = (function (_super) {
    __extends(NoEvalWalker, _super);
    function NoEvalWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoEvalWalker.prototype.visitCallExpression = function (node) {
        var expression = node.expression;
        if (expression.kind === ts.SyntaxKind.Identifier) {
            var expressionName = expression.text;
            if (expressionName === "eval") {
                this.addFailureAtNode(expression, Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitCallExpression.call(this, node);
    };
    return NoEvalWalker;
}(Lint.RuleWalker));
var _a;
