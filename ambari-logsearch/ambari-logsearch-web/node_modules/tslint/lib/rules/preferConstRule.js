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
var utils_1 = require("../language/utils");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var preferConstWalker = new PreferConstWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(preferConstWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "prefer-const",
    description: "Requires that variable declarations use `const` instead of `let` if possible.",
    descriptionDetails: (_a = ["\n            If a variable is only assigned to once when it is declared, it should be declared using 'const'"], _a.raw = ["\n            If a variable is only assigned to once when it is declared, it should be declared using 'const'"], Lint.Utils.dedent(_a)),
    hasFix: true,
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (identifier) {
    return "Identifier '" + identifier + "' is never reassigned; use 'const' instead of 'let'.";
};
exports.Rule = Rule;
var PreferConstWalker = (function (_super) {
    __extends(PreferConstWalker, _super);
    function PreferConstWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PreferConstWalker.collect = function (statements, scopeInfo) {
        for (var _i = 0, statements_1 = statements; _i < statements_1.length; _i++) {
            var s = statements_1[_i];
            if (s.kind === ts.SyntaxKind.VariableStatement) {
                PreferConstWalker.collectInVariableDeclarationList(s.declarationList, scopeInfo);
            }
        }
    };
    PreferConstWalker.collectInVariableDeclarationList = function (node, scopeInfo) {
        var allowConst;
        if (ts.getCombinedModifierFlags === undefined) {
            // for back-compat, TypeScript < 2.1
            allowConst = utils_1.isCombinedNodeFlagSet(node, ts.NodeFlags.Let)
                && !Lint.hasModifier(node.parent.modifiers, ts.SyntaxKind.ExportKeyword);
        }
        else {
            allowConst = utils_1.isCombinedNodeFlagSet(node, ts.NodeFlags.Let) && !utils_1.isCombinedModifierFlagSet(node, ts.ModifierFlags.Export);
        }
        if (allowConst) {
            for (var _i = 0, _a = node.declarations; _i < _a.length; _i++) {
                var decl = _a[_i];
                PreferConstWalker.addDeclarationName(decl.name, node, scopeInfo);
            }
        }
    };
    PreferConstWalker.addDeclarationName = function (node, container, scopeInfo) {
        if (node.kind === ts.SyntaxKind.Identifier) {
            scopeInfo.addVariable(node, container);
        }
        else {
            for (var _i = 0, _a = node.elements; _i < _a.length; _i++) {
                var el = _a[_i];
                if (el.kind === ts.SyntaxKind.BindingElement) {
                    PreferConstWalker.addDeclarationName(el.name, container, scopeInfo);
                }
            }
        }
    };
    PreferConstWalker.prototype.createScope = function () {
        return {};
    };
    PreferConstWalker.prototype.createBlockScope = function (node) {
        var scopeInfo = new ScopeInfo();
        switch (node.kind) {
            case ts.SyntaxKind.SourceFile:
                PreferConstWalker.collect(node.statements, scopeInfo);
                break;
            case ts.SyntaxKind.Block:
                PreferConstWalker.collect(node.statements, scopeInfo);
                break;
            case ts.SyntaxKind.ModuleDeclaration:
                var body = node.body;
                if (body && body.kind === ts.SyntaxKind.ModuleBlock) {
                    PreferConstWalker.collect(body.statements, scopeInfo);
                }
                break;
            case ts.SyntaxKind.ForStatement:
            case ts.SyntaxKind.ForOfStatement:
            case ts.SyntaxKind.ForInStatement:
                var initializer = node.initializer;
                if (initializer && initializer.kind === ts.SyntaxKind.VariableDeclarationList) {
                    PreferConstWalker.collectInVariableDeclarationList(initializer, scopeInfo);
                }
                break;
            case ts.SyntaxKind.SwitchStatement:
                for (var _i = 0, _a = node.caseBlock.clauses; _i < _a.length; _i++) {
                    var caseClause = _a[_i];
                    PreferConstWalker.collect(caseClause.statements, scopeInfo);
                }
                break;
            default:
                break;
        }
        return scopeInfo;
    };
    PreferConstWalker.prototype.onBlockScopeEnd = function () {
        var seenLetStatements = new Set();
        for (var _i = 0, _a = this.getCurrentBlockScope().getConstCandiates(); _i < _a.length; _i++) {
            var usage = _a[_i];
            var fix = void 0;
            if (!usage.reassignedSibling && !seenLetStatements.has(usage.letStatement)) {
                // only fix if all variables in the `let` statement can use `const`
                fix = this.createFix(this.createReplacement(usage.letStatement.getStart(), "let".length, "const"));
                seenLetStatements.add(usage.letStatement);
            }
            this.addFailureAtNode(usage.identifier, Rule.FAILURE_STRING_FACTORY(usage.identifier.text), fix);
        }
    };
    PreferConstWalker.prototype.visitBinaryExpression = function (node) {
        if (utils_1.isAssignment(node)) {
            this.handleLHSExpression(node.left);
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    PreferConstWalker.prototype.visitPrefixUnaryExpression = function (node) {
        this.handleUnaryExpression(node);
        _super.prototype.visitPrefixUnaryExpression.call(this, node);
    };
    PreferConstWalker.prototype.visitPostfixUnaryExpression = function (node) {
        this.handleUnaryExpression(node);
        _super.prototype.visitPostfixUnaryExpression.call(this, node);
    };
    PreferConstWalker.prototype.handleLHSExpression = function (node) {
        var _this = this;
        node = utils_1.unwrapParentheses(node);
        if (node.kind === ts.SyntaxKind.Identifier) {
            this.markAssignment(node);
        }
        else if (node.kind === ts.SyntaxKind.ArrayLiteralExpression) {
            var deconstructionArray = node;
            deconstructionArray.elements.forEach(function (child) {
                // recursively unwrap destructuring arrays
                _this.handleLHSExpression(child);
            });
        }
        else if (node.kind === ts.SyntaxKind.ObjectLiteralExpression) {
            for (var _i = 0, _a = node.properties; _i < _a.length; _i++) {
                var prop = _a[_i];
                switch (prop.kind) {
                    case ts.SyntaxKind.PropertyAssignment:
                        this.handleLHSExpression(prop.initializer);
                        break;
                    case ts.SyntaxKind.ShorthandPropertyAssignment:
                        this.handleLHSExpression(prop.name);
                        break;
                    case ts.SyntaxKind.SpreadAssignment:
                        this.handleLHSExpression(prop.expression);
                        break;
                    default:
                        break;
                }
            }
        }
    };
    PreferConstWalker.prototype.handleUnaryExpression = function (node) {
        if (node.operator === ts.SyntaxKind.PlusPlusToken || node.operator === ts.SyntaxKind.MinusMinusToken) {
            this.handleLHSExpression(node.operand);
        }
    };
    PreferConstWalker.prototype.markAssignment = function (identifier) {
        var allBlockScopes = this.getAllBlockScopes();
        // look through block scopes from local -> global
        for (var i = allBlockScopes.length - 1; i >= 0; i--) {
            if (allBlockScopes[i].incrementVariableUsage(identifier.text)) {
                break;
            }
        }
    };
    return PreferConstWalker;
}(Lint.BlockScopeAwareRuleWalker));
var ScopeInfo = (function () {
    function ScopeInfo() {
        this.identifierUsages = new Map();
        // variable names grouped by common `let` statements
        this.sharedLetSets = new Map();
    }
    ScopeInfo.prototype.addVariable = function (identifier, letStatement) {
        this.identifierUsages.set(identifier.text, { letStatement: letStatement, identifier: identifier, usageCount: 0 });
        var shared = this.sharedLetSets.get(letStatement);
        if (shared === undefined) {
            shared = [];
            this.sharedLetSets.set(letStatement, shared);
        }
        shared.push(identifier.text);
    };
    ScopeInfo.prototype.getConstCandiates = function () {
        var _this = this;
        var constCandidates = [];
        this.sharedLetSets.forEach(function (variableNames) {
            var anyReassigned = variableNames.some(function (key) { return _this.identifierUsages.get(key).usageCount > 0; });
            for (var _i = 0, variableNames_1 = variableNames; _i < variableNames_1.length; _i++) {
                var variableName = variableNames_1[_i];
                var usage = _this.identifierUsages.get(variableName);
                if (usage.usageCount === 0) {
                    constCandidates.push({
                        identifier: usage.identifier,
                        letStatement: usage.letStatement,
                        reassignedSibling: anyReassigned,
                    });
                }
            }
        });
        return constCandidates;
    };
    ScopeInfo.prototype.incrementVariableUsage = function (varName) {
        var usages = this.identifierUsages.get(varName);
        if (usages !== undefined) {
            usages.usageCount++;
            return true;
        }
        return false;
    };
    return ScopeInfo;
}());
var _a;
