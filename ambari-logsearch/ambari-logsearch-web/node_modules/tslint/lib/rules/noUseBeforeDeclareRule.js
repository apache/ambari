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
    Rule.prototype.apply = function (sourceFile, languageService) {
        return this.applyWithWalker(new NoUseBeforeDeclareWalker(sourceFile, this.getOptions(), languageService));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-use-before-declare",
    description: "Disallows usage of variables before their declaration.",
    descriptionDetails: (_a = ["\n            This rule is primarily useful when using the `var` keyword -\n            the compiler will detect if a `let` and `const` variable is used before it is declared."], _a.raw = ["\n            This rule is primarily useful when using the \\`var\\` keyword -\n            the compiler will detect if a \\`let\\` and \\`const\\` variable is used before it is declared."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_PREFIX = "variable '";
Rule.FAILURE_STRING_POSTFIX = "' used before declaration";
exports.Rule = Rule;
var NoUseBeforeDeclareWalker = (function (_super) {
    __extends(NoUseBeforeDeclareWalker, _super);
    function NoUseBeforeDeclareWalker(sourceFile, options, languageService) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.languageService = languageService;
        _this.importedPropertiesPositions = [];
        return _this;
    }
    NoUseBeforeDeclareWalker.prototype.createScope = function () {
        return new Set();
    };
    NoUseBeforeDeclareWalker.prototype.visitBindingElement = function (node) {
        var isSingleVariable = node.name.kind === ts.SyntaxKind.Identifier;
        var isBlockScoped = Lint.isBlockScopedBindingElement(node);
        // use-before-declare errors for block-scoped vars are caught by tsc
        if (isSingleVariable && !isBlockScoped) {
            var variableName = node.name.text;
            this.validateUsageForVariable(variableName, node.name.getStart());
        }
        _super.prototype.visitBindingElement.call(this, node);
    };
    NoUseBeforeDeclareWalker.prototype.visitImportDeclaration = function (node) {
        var importClause = node.importClause;
        // named imports & namespace imports handled by other walker methods
        // importClause will be null for bare imports
        if (importClause != null && importClause.name != null) {
            var variableIdentifier = importClause.name;
            this.validateUsageForVariable(variableIdentifier.text, variableIdentifier.getStart());
        }
        _super.prototype.visitImportDeclaration.call(this, node);
    };
    NoUseBeforeDeclareWalker.prototype.visitImportEqualsDeclaration = function (node) {
        var name = node.name;
        this.validateUsageForVariable(name.text, name.getStart());
        _super.prototype.visitImportEqualsDeclaration.call(this, node);
    };
    NoUseBeforeDeclareWalker.prototype.visitNamedImports = function (node) {
        for (var _i = 0, _a = node.elements; _i < _a.length; _i++) {
            var namedImport = _a[_i];
            if (namedImport.propertyName != null) {
                this.saveImportedPropertiesPositions(namedImport.propertyName.getStart());
            }
            this.validateUsageForVariable(namedImport.name.text, namedImport.name.getStart());
        }
        _super.prototype.visitNamedImports.call(this, node);
    };
    NoUseBeforeDeclareWalker.prototype.visitNamespaceImport = function (node) {
        this.validateUsageForVariable(node.name.text, node.name.getStart());
        _super.prototype.visitNamespaceImport.call(this, node);
    };
    NoUseBeforeDeclareWalker.prototype.visitVariableDeclaration = function (node) {
        var isSingleVariable = node.name.kind === ts.SyntaxKind.Identifier;
        var variableName = node.name.text;
        var currentScope = this.getCurrentScope();
        // only validate on the first variable declaration within the current scope
        if (isSingleVariable && !currentScope.has(variableName)) {
            this.validateUsageForVariable(variableName, node.getStart());
        }
        currentScope.add(variableName);
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    NoUseBeforeDeclareWalker.prototype.validateUsageForVariable = function (name, position) {
        var fileName = this.getSourceFile().fileName;
        var highlights = this.languageService.getDocumentHighlights(fileName, position, [fileName]);
        if (highlights != null) {
            for (var _i = 0, highlights_1 = highlights; _i < highlights_1.length; _i++) {
                var highlight = highlights_1[_i];
                for (var _a = 0, _b = highlight.highlightSpans; _a < _b.length; _a++) {
                    var highlightSpan = _b[_a];
                    var referencePosition = highlightSpan.textSpan.start;
                    if (referencePosition < position && !this.isImportedPropertyName(referencePosition)) {
                        var failureString = Rule.FAILURE_STRING_PREFIX + name + Rule.FAILURE_STRING_POSTFIX;
                        this.addFailureAt(referencePosition, name.length, failureString);
                    }
                }
            }
        }
    };
    NoUseBeforeDeclareWalker.prototype.saveImportedPropertiesPositions = function (position) {
        this.importedPropertiesPositions.push(position);
    };
    NoUseBeforeDeclareWalker.prototype.isImportedPropertyName = function (position) {
        return this.importedPropertiesPositions.indexOf(position) !== -1;
    };
    return NoUseBeforeDeclareWalker;
}(Lint.ScopeAwareRuleWalker));
var _a;
