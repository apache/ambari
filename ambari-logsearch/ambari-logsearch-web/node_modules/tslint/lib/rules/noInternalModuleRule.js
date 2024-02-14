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
        return this.applyWithWalker(new NoInternalModuleWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-internal-module",
    description: "Disallows internal `module`",
    rationale: "Using `module` leads to a confusion of concepts with external modules. Use the newer `namespace` keyword instead.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "The internal 'module' syntax is deprecated, use the 'namespace' keyword instead.";
exports.Rule = Rule;
var NoInternalModuleWalker = (function (_super) {
    __extends(NoInternalModuleWalker, _super);
    function NoInternalModuleWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoInternalModuleWalker.prototype.visitModuleDeclaration = function (node) {
        if (this.isInternalModuleDeclaration(node)) {
            var start = this.getStartBeforeModule(node);
            this.addFailureAt(node.getStart() + start, "module".length, Rule.FAILURE_STRING);
        }
        _super.prototype.visitModuleDeclaration.call(this, node);
    };
    NoInternalModuleWalker.prototype.isInternalModuleDeclaration = function (node) {
        // an internal module declaration is not a namespace or a nested declaration
        // for external modules, node.name.kind will be a LiteralExpression instead of Identifier
        return !Lint.isNodeFlagSet(node, ts.NodeFlags.Namespace)
            && !Lint.isNestedModuleDeclaration(node)
            && node.name.kind === ts.SyntaxKind.Identifier
            && !isGlobalAugmentation(node);
    };
    NoInternalModuleWalker.prototype.getStartBeforeModule = function (node) {
        return node.getText().indexOf("module");
    };
    return NoInternalModuleWalker;
}(Lint.RuleWalker));
function isGlobalAugmentation(node) {
    // augmenting global uses a special syntax that is allowed
    // see https://github.com/Microsoft/TypeScript/pull/6213
    return node.name.kind === ts.SyntaxKind.Identifier && node.name.text === "global";
}
