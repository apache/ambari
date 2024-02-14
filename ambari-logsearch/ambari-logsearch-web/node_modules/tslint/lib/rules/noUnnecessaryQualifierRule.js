/**
 * @license
 * Copyright 2017 Palantir Technologies, Inc.
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
var utils = require("tsutils");
var ts = require("typescript");
var Lint = require("../index");
var utils_1 = require("../utils");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.FAILURE_STRING = function (name) {
        return "Qualifier is unnecessary since '" + name + "' is in scope.";
    };
    Rule.prototype.applyWithProgram = function (sourceFile, langSvc) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-unnecessary-qualifier",
    description: "Warns when a namespace qualifier (`A.x`) is unnecessary.",
    hasFix: true,
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.namespacesInScope = [];
        return _this;
    }
    Walker.prototype.visitModuleDeclaration = function (node) {
        this.namespacesInScope.push(node);
        _super.prototype.visitModuleDeclaration.call(this, node);
        this.namespacesInScope.pop();
    };
    Walker.prototype.visitEnumDeclaration = function (node) {
        this.namespacesInScope.push(node);
        _super.prototype.visitEnumDeclaration.call(this, node);
        this.namespacesInScope.pop();
    };
    Walker.prototype.visitNode = function (node) {
        switch (node.kind) {
            case ts.SyntaxKind.QualifiedName:
                var _a = node, left = _a.left, right = _a.right;
                this.visitNamespaceAccess(node, left, right);
                break;
            case ts.SyntaxKind.PropertyAccessExpression:
                var _b = node, expression = _b.expression, name = _b.name;
                if (utils.isEntityNameExpression(expression)) {
                    this.visitNamespaceAccess(node, expression, name);
                    break;
                }
            // fall through
            default:
                _super.prototype.visitNode.call(this, node);
        }
    };
    Walker.prototype.visitNamespaceAccess = function (node, qualifier, name) {
        if (this.qualifierIsUnnecessary(qualifier, name)) {
            var fix = this.createFix(this.deleteFromTo(qualifier.getStart(), name.getStart()));
            this.addFailureAtNode(qualifier, Rule.FAILURE_STRING(qualifier.getText()), fix);
        }
        else {
            // Only look for nested qualifier errors if we didn't already fail on the outer qualifier.
            _super.prototype.visitNode.call(this, node);
        }
    };
    Walker.prototype.qualifierIsUnnecessary = function (qualifier, name) {
        var namespaceSymbol = this.symbolAtLocation(qualifier);
        if (namespaceSymbol === undefined || !this.symbolIsNamespaceInScope(namespaceSymbol)) {
            return false;
        }
        var accessedSymbol = this.symbolAtLocation(name);
        if (accessedSymbol === undefined) {
            return false;
        }
        // If the symbol in scope is different, the qualifier is necessary.
        var fromScope = this.getSymbolInScope(qualifier, accessedSymbol.flags, name.text);
        return fromScope === undefined || symbolsAreEqual(fromScope, accessedSymbol);
    };
    Walker.prototype.getSymbolInScope = function (node, flags, name) {
        // TODO:PERF `getSymbolsInScope` gets a long list. Is there a better way?
        var scope = this.getTypeChecker().getSymbolsInScope(node, flags);
        return scope.find(function (scopeSymbol) { return scopeSymbol.name === name; });
    };
    Walker.prototype.symbolAtLocation = function (node) {
        return this.getTypeChecker().getSymbolAtLocation(node);
    };
    Walker.prototype.symbolIsNamespaceInScope = function (symbol) {
        var _this = this;
        if (symbol.getDeclarations().some(function (decl) { return _this.namespacesInScope.some(function (ns) { return nodesAreEqual(ns, decl); }); })) {
            return true;
        }
        var alias = this.tryGetAliasedSymbol(symbol);
        return alias !== undefined && this.symbolIsNamespaceInScope(alias);
    };
    Walker.prototype.tryGetAliasedSymbol = function (symbol) {
        return Lint.isSymbolFlagSet(symbol, ts.SymbolFlags.Alias) ? this.getTypeChecker().getAliasedSymbol(symbol) : undefined;
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
// TODO: Should just be `===`. See https://github.com/palantir/tslint/issues/1969
function nodesAreEqual(a, b) {
    return a.pos === b.pos;
}
// Only needed in global files. Likely due to https://github.com/palantir/tslint/issues/1969. See `test.global.ts.lint`.
function symbolsAreEqual(a, b) {
    return utils_1.arraysAreEqual(a.declarations, b.declarations, nodesAreEqual);
}
