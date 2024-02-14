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
        var radixWalker = new RadixWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(radixWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "radix",
    description: "Requires the radix parameter to be specified when calling `parseInt`.",
    rationale: (_a = ["\n            From [MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/parseInt):\n            > Always specify this parameter to eliminate reader confusion and to guarantee predictable behavior.\n            > Different implementations produce different results when a radix is not specified, usually defaulting the value to 10."], _a.raw = ["\n            From [MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/parseInt):\n            > Always specify this parameter to eliminate reader confusion and to guarantee predictable behavior.\n            > Different implementations produce different results when a radix is not specified, usually defaulting the value to 10."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Missing radix parameter";
exports.Rule = Rule;
var RadixWalker = (function (_super) {
    __extends(RadixWalker, _super);
    function RadixWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    RadixWalker.prototype.visitCallExpression = function (node) {
        var expression = node.expression;
        if (expression.kind === ts.SyntaxKind.Identifier
            && node.getFirstToken().getText() === "parseInt"
            && node.arguments.length < 2) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING);
        }
        _super.prototype.visitCallExpression.call(this, node);
    };
    return RadixWalker;
}(Lint.RuleWalker));
var _a;
