/**
 * @license
 * Copyright 2014 Palantir Technologies, Inc.
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
        var requiresWalker = new NoVarRequiresWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(requiresWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-var-requires",
    description: "Disallows the use of require statements except in import statements.",
    descriptionDetails: (_a = ["\n            In other words, the use of forms such as `var module = require(\"module\")` are banned.\n            Instead use ES6 style imports or `import foo = require('foo')` imports."], _a.raw = ["\n            In other words, the use of forms such as \\`var module = require(\"module\")\\` are banned.\n            Instead use ES6 style imports or \\`import foo = require('foo')\\` imports."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "require statement not part of an import statement";
exports.Rule = Rule;
var NoVarRequiresWalker = (function (_super) {
    __extends(NoVarRequiresWalker, _super);
    function NoVarRequiresWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoVarRequiresWalker.prototype.createScope = function () {
        return {};
    };
    NoVarRequiresWalker.prototype.visitCallExpression = function (node) {
        var expression = node.expression;
        if (this.getCurrentDepth() <= 1 && expression.kind === ts.SyntaxKind.Identifier) {
            var identifierName = expression.text;
            if (identifierName === "require") {
                // if we're calling (invoking) require, then it's not part of an import statement
                this.addFailureAtNode(node, Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitCallExpression.call(this, node);
    };
    return NoVarRequiresWalker;
}(Lint.ScopeAwareRuleWalker));
var _a;
