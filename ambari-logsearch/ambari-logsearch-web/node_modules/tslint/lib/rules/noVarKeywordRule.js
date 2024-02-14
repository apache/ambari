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
        var noVarKeywordWalker = new NoVarKeywordWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(noVarKeywordWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-var-keyword",
    description: "Disallows usage of the `var` keyword.",
    descriptionDetails: "Use `let` or `const` instead.",
    hasFix: true,
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Forbidden 'var' keyword, use 'let' or 'const' instead";
exports.Rule = Rule;
var NoVarKeywordWalker = (function (_super) {
    __extends(NoVarKeywordWalker, _super);
    function NoVarKeywordWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoVarKeywordWalker.prototype.visitVariableStatement = function (node) {
        if (!Lint.hasModifier(node.modifiers, ts.SyntaxKind.DeclareKeyword)
            && !Lint.isBlockScopedVariable(node)) {
            this.reportFailure(node.declarationList);
        }
        _super.prototype.visitVariableStatement.call(this, node);
    };
    NoVarKeywordWalker.prototype.visitForStatement = function (node) {
        this.handleInitializerNode(node.initializer);
        _super.prototype.visitForStatement.call(this, node);
    };
    NoVarKeywordWalker.prototype.visitForInStatement = function (node) {
        this.handleInitializerNode(node.initializer);
        _super.prototype.visitForInStatement.call(this, node);
    };
    NoVarKeywordWalker.prototype.visitForOfStatement = function (node) {
        this.handleInitializerNode(node.initializer);
        _super.prototype.visitForOfStatement.call(this, node);
    };
    NoVarKeywordWalker.prototype.handleInitializerNode = function (node) {
        if (node && node.kind === ts.SyntaxKind.VariableDeclarationList &&
            !(Lint.isNodeFlagSet(node, ts.NodeFlags.Let) || Lint.isNodeFlagSet(node, ts.NodeFlags.Const))) {
            this.reportFailure(node);
        }
    };
    NoVarKeywordWalker.prototype.reportFailure = function (node) {
        var nodeStart = node.getStart(this.getSourceFile());
        this.addFailureAt(nodeStart, "var".length, Rule.FAILURE_STRING, this.createFix(this.createReplacement(nodeStart, "var".length, "let")));
    };
    return NoVarKeywordWalker;
}(Lint.RuleWalker));
