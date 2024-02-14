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
        return this.applyWithWalker(new NoShadowedVariableWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-shadowed-variable",
    description: "Disallows shadowing variable declarations.",
    rationale: "Shadowing a variable masks access to it and obscures to what value an identifier actually refers.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (name) {
    return "Shadowed variable: '" + name + "'";
};
exports.Rule = Rule;
var NoShadowedVariableWalker = (function (_super) {
    __extends(NoShadowedVariableWalker, _super);
    function NoShadowedVariableWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoShadowedVariableWalker.prototype.createScope = function () {
        return new Set();
    };
    NoShadowedVariableWalker.prototype.createBlockScope = function () {
        return new Set();
    };
    NoShadowedVariableWalker.prototype.visitBindingElement = function (node) {
        var isSingleVariable = node.name.kind === ts.SyntaxKind.Identifier;
        if (isSingleVariable) {
            var name = node.name;
            var variableDeclaration = Lint.getBindingElementVariableDeclaration(node);
            var isBlockScopedVariable = variableDeclaration !== null && Lint.isBlockScopedVariable(variableDeclaration);
            this.handleSingleVariableIdentifier(name, isBlockScopedVariable);
        }
        _super.prototype.visitBindingElement.call(this, node);
    };
    NoShadowedVariableWalker.prototype.visitCatchClause = function (node) {
        // don't visit the catch clause variable declaration, just visit the block
        // the catch clause variable declaration has its own special scoping rules
        this.visitBlock(node.block);
    };
    NoShadowedVariableWalker.prototype.visitCallSignature = function (_node) {
        // don't call super, we don't need to check parameter names in call signatures
    };
    NoShadowedVariableWalker.prototype.visitFunctionType = function (_node) {
        // don't call super, we don't need to check names in function types
    };
    NoShadowedVariableWalker.prototype.visitConstructorType = function (_node) {
        // don't call super, we don't need to check names in constructor types
    };
    NoShadowedVariableWalker.prototype.visitIndexSignatureDeclaration = function (_node) {
        // don't call super, we don't want to walk index signatures
    };
    NoShadowedVariableWalker.prototype.visitMethodSignature = function (_node) {
        // don't call super, we don't want to walk method signatures either
    };
    NoShadowedVariableWalker.prototype.visitParameterDeclaration = function (node) {
        var isSingleParameter = node.name.kind === ts.SyntaxKind.Identifier;
        if (isSingleParameter) {
            this.handleSingleVariableIdentifier(node.name, false);
        }
        _super.prototype.visitParameterDeclaration.call(this, node);
    };
    NoShadowedVariableWalker.prototype.visitTypeLiteral = function (_node) {
        // don't call super, we don't want to walk the inside of type nodes
    };
    NoShadowedVariableWalker.prototype.visitVariableDeclaration = function (node) {
        var isSingleVariable = node.name.kind === ts.SyntaxKind.Identifier;
        if (isSingleVariable) {
            this.handleSingleVariableIdentifier(node.name, Lint.isBlockScopedVariable(node));
        }
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    NoShadowedVariableWalker.prototype.handleSingleVariableIdentifier = function (variableIdentifier, isBlockScoped) {
        var variableName = variableIdentifier.text;
        if (this.isVarInCurrentScope(variableName) && !this.inCurrentBlockScope(variableName)) {
            // shadowing if there's already a `var` of the same name in the scope AND
            // it's not in the current block (handled by the 'no-duplicate-variable' rule)
            this.addFailureOnIdentifier(variableIdentifier);
        }
        else if (this.inPreviousBlockScope(variableName)) {
            // shadowing if there is a `var`, `let`, 'const`, or parameter in a previous block scope
            this.addFailureOnIdentifier(variableIdentifier);
        }
        if (!isBlockScoped) {
            // `var` variables go on the scope
            this.getCurrentScope().add(variableName);
        }
        // all variables go on block scope, including `var`
        this.getCurrentBlockScope().add(variableName);
    };
    NoShadowedVariableWalker.prototype.isVarInCurrentScope = function (varName) {
        return this.getCurrentScope().has(varName);
    };
    NoShadowedVariableWalker.prototype.inCurrentBlockScope = function (varName) {
        return this.getCurrentBlockScope().has(varName);
    };
    NoShadowedVariableWalker.prototype.inPreviousBlockScope = function (varName) {
        var _this = this;
        return this.getAllBlockScopes().some(function (scopeInfo) {
            return scopeInfo !== _this.getCurrentBlockScope() && scopeInfo.has(varName);
        });
    };
    NoShadowedVariableWalker.prototype.addFailureOnIdentifier = function (ident) {
        var failureString = Rule.FAILURE_STRING_FACTORY(ident.text);
        this.addFailureAtNode(ident, failureString);
    };
    return NoShadowedVariableWalker;
}(Lint.BlockScopeAwareRuleWalker));
