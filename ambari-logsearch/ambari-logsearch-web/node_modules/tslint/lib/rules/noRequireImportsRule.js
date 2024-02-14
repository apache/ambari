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
        return this.applyWithWalker(new NoRequireImportsWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-require-imports",
    description: "Disallows invocation of `require()`.",
    rationale: "Prefer the newer ES6-style imports over `require()`.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "require() style import is forbidden";
exports.Rule = Rule;
var NoRequireImportsWalker = (function (_super) {
    __extends(NoRequireImportsWalker, _super);
    function NoRequireImportsWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoRequireImportsWalker.prototype.visitCallExpression = function (node) {
        if (node.arguments != null && node.expression != null) {
            var callExpressionText = node.expression.getText(this.getSourceFile());
            if (callExpressionText === "require") {
                this.addFailureAtNode(node, Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitCallExpression.call(this, node);
    };
    NoRequireImportsWalker.prototype.visitImportEqualsDeclaration = function (node) {
        var moduleReference = node.moduleReference;
        if (moduleReference.kind === ts.SyntaxKind.ExternalModuleReference) {
            this.addFailureAtNode(moduleReference, Rule.FAILURE_STRING);
        }
        _super.prototype.visitImportEqualsDeclaration.call(this, node);
    };
    return NoRequireImportsWalker;
}(Lint.RuleWalker));
