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
var OPTION_REACT = "react";
var OPTION_CHECK_PARAMETERS = "check-parameters";
var REACT_MODULES = ["react", "react/addons"];
var REACT_NAMESPACE_IMPORT_NAME = "React";
var MODULE_SPECIFIER_MATCH = /^["'](.+)['"]$/;
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile, languageService) {
        return this.applyWithWalker(new NoUnusedVariablesWalker(sourceFile, this.getOptions(), languageService));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-unused-variable",
    description: (_a = ["Disallows unused imports, variables, functions and\n            private class members. Similar to tsc's --noUnusedParameters and --noUnusedLocals\n            options, but does not interrupt code compilation."], _a.raw = ["Disallows unused imports, variables, functions and\n            private class members. Similar to tsc's --noUnusedParameters and --noUnusedLocals\n            options, but does not interrupt code compilation."], Lint.Utils.dedent(_a)),
    hasFix: true,
    optionsDescription: (_b = ["\n            Three optional arguments may be optionally provided:\n\n            * `\"check-parameters\"` disallows unused function and constructor parameters.\n                * NOTE: this option is experimental and does not work with classes\n                that use abstract method declarations, among other things.\n            * `\"react\"` relaxes the rule for a namespace import named `React`\n            (from either the module `\"react\"` or `\"react/addons\"`).\n            Any JSX expression in the file will be treated as a usage of `React`\n            (because it expands to `React.createElement `).\n            * `{\"ignore-pattern\": \"pattern\"}` where pattern is a case-sensitive regexp.\n            Variable names that match the pattern will be ignored."], _b.raw = ["\n            Three optional arguments may be optionally provided:\n\n            * \\`\"check-parameters\"\\` disallows unused function and constructor parameters.\n                * NOTE: this option is experimental and does not work with classes\n                that use abstract method declarations, among other things.\n            * \\`\"react\"\\` relaxes the rule for a namespace import named \\`React\\`\n            (from either the module \\`\"react\"\\` or \\`\"react/addons\"\\`).\n            Any JSX expression in the file will be treated as a usage of \\`React\\`\n            (because it expands to \\`React.createElement \\`).\n            * \\`{\"ignore-pattern\": \"pattern\"}\\` where pattern is a case-sensitive regexp.\n            Variable names that match the pattern will be ignored."], Lint.Utils.dedent(_b)),
    options: {
        type: "array",
        items: {
            oneOf: [{
                    type: "string",
                    enum: ["check-parameters", "react"],
                }, {
                    type: "object",
                    properties: {
                        "ignore-pattern": { type: "string" },
                    },
                    additionalProperties: false,
                }],
        },
        minLength: 0,
        maxLength: 3,
    },
    optionExamples: ['[true, "react"]', '[true, {"ignore-pattern": "^_"}]'],
    type: "functionality",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_TYPE_FUNC = "function";
Rule.FAILURE_TYPE_IMPORT = "import";
Rule.FAILURE_TYPE_METHOD = "method";
Rule.FAILURE_TYPE_PARAM = "parameter";
Rule.FAILURE_TYPE_PROP = "property";
Rule.FAILURE_TYPE_VAR = "variable";
Rule.FAILURE_STRING_FACTORY = function (type, name) {
    return "Unused " + type + ": '" + name + "'";
};
exports.Rule = Rule;
var NoUnusedVariablesWalker = (function (_super) {
    __extends(NoUnusedVariablesWalker, _super);
    function NoUnusedVariablesWalker(sourceFile, options, languageService) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.languageService = languageService;
        _this.possibleFailures = [];
        _this.skipVariableDeclaration = false;
        _this.skipParameterDeclaration = false;
        _this.hasSeenJsxElement = false;
        _this.isReactUsed = false;
        var ignorePatternOption = _this.getOptions().filter(function (option) {
            return typeof option === "object" && option["ignore-pattern"] != null;
        })[0];
        if (ignorePatternOption != null) {
            _this.ignorePattern = new RegExp(ignorePatternOption["ignore-pattern"]);
        }
        return _this;
    }
    NoUnusedVariablesWalker.prototype.visitSourceFile = function (node) {
        var _this = this;
        _super.prototype.visitSourceFile.call(this, node);
        /*
         * After super.visitSourceFile() is completed, this.reactImport will be set to a NamespaceImport iff:
         *
         * - a react option has been provided to the rule and
         * - an import of a module that matches one of OPTION_REACT_MODULES is found, to a
         *   NamespaceImport named OPTION_REACT_NAMESPACE_IMPORT_NAME
         *
         * e.g.
         *
         * import * as React from "react/addons";
         *
         * If reactImport is defined when a walk is completed, we need to have:
         *
         * a) seen another usage of React and/or
         * b) seen a JSX identifier
         *
         * otherwise a a variable usage failure will will be reported
         */
        if (this.hasOption(OPTION_REACT)
            && this.reactImport != null
            && !this.isReactUsed
            && !this.hasSeenJsxElement) {
            var nameText = this.reactImport.name.getText();
            if (!this.isIgnored(nameText)) {
                var start = this.reactImport.name.getStart();
                var msg = Rule.FAILURE_STRING_FACTORY(Rule.FAILURE_TYPE_IMPORT, nameText);
                this.possibleFailures.push({ start: start, width: nameText.length, message: msg });
            }
        }
        var someFixBrokeIt = false;
        // Performance optimization: type-check the whole file before verifying individual fixes
        if (this.possibleFailures.some(function (f) { return f.fix !== undefined; })) {
            var newText = Lint.Fix.applyAll(this.getSourceFile().getFullText(), this.possibleFailures.map(function (f) { return f.fix; }).filter(function (f) { return f !== undefined; }));
            // If we have the program, we can verify that the fix doesn't introduce failures
            if (Lint.checkEdit(this.languageService, this.getSourceFile(), newText).length > 0) {
                console.error("Fixes caused errors in " + this.getSourceFile().fileName);
                someFixBrokeIt = true;
            }
        }
        this.possibleFailures.forEach(function (f) {
            if (!someFixBrokeIt || f.fix === undefined) {
                _this.addFailureAt(f.start, f.width, f.message, f.fix);
            }
            else {
                var newText = f.fix.apply(_this.getSourceFile().getFullText());
                if (Lint.checkEdit(_this.languageService, _this.getSourceFile(), newText).length === 0) {
                    _this.addFailureAt(f.start, f.width, f.message, f.fix);
                }
            }
        });
    };
    NoUnusedVariablesWalker.prototype.visitBindingElement = function (node) {
        var isSingleVariable = node.name.kind === ts.SyntaxKind.Identifier;
        if (isSingleVariable && !this.skipBindingElement) {
            var variableIdentifier = node.name;
            this.validateReferencesForVariable(Rule.FAILURE_TYPE_VAR, variableIdentifier.text, variableIdentifier.getStart());
        }
        _super.prototype.visitBindingElement.call(this, node);
    };
    NoUnusedVariablesWalker.prototype.visitCatchClause = function (node) {
        // don't visit the catch clause variable declaration, just visit the block
        // the catch clause variable declaration needs to be there but doesn't need to be used
        this.visitBlock(node.block);
    };
    // skip exported and declared functions
    NoUnusedVariablesWalker.prototype.visitFunctionDeclaration = function (node) {
        if (!Lint.hasModifier(node.modifiers, ts.SyntaxKind.ExportKeyword, ts.SyntaxKind.DeclareKeyword) && node.name !== undefined) {
            var variableName = node.name.text;
            this.validateReferencesForVariable(Rule.FAILURE_TYPE_FUNC, variableName, node.name.getStart());
        }
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    NoUnusedVariablesWalker.prototype.visitFunctionType = function (node) {
        this.skipParameterDeclaration = true;
        _super.prototype.visitFunctionType.call(this, node);
        this.skipParameterDeclaration = false;
    };
    NoUnusedVariablesWalker.prototype.visitImportDeclaration = function (node) {
        var _this = this;
        var importClause = node.importClause;
        // If the imports are exported, they may be used externally
        if (Lint.hasModifier(node.modifiers, ts.SyntaxKind.ExportKeyword) ||
            // importClause will be null for bare imports
            importClause == null) {
            _super.prototype.visitImportDeclaration.call(this, node);
            return;
        }
        // Two passes: first collect what's unused, then produce failures. This allows the fix to lookahead.
        var usesDefaultImport = false;
        var usedNamedImports = [];
        if (importClause.name != null) {
            var variableIdentifier = importClause.name;
            usesDefaultImport = this.isUsed(variableIdentifier.text, variableIdentifier.getStart());
        }
        if (importClause.namedBindings != null) {
            if (importClause.namedBindings.kind === ts.SyntaxKind.NamedImports && node.importClause !== undefined) {
                var imports = node.importClause.namedBindings;
                usedNamedImports = imports.elements.map(function (e) { return _this.isUsed(e.name.text, e.name.getStart()); });
            }
            // Avoid deleting the whole statement if there's an import * inside
            if (importClause.namedBindings.kind === ts.SyntaxKind.NamespaceImport) {
                usesDefaultImport = true;
            }
        }
        // Delete the entire import statement if named and default imports all unused
        if (!usesDefaultImport && usedNamedImports.every(function (e) { return !e; })) {
            this.fail(Rule.FAILURE_TYPE_IMPORT, node.getText(), node.getStart(), this.deleteImportStatement(node));
            _super.prototype.visitImportDeclaration.call(this, node);
            return;
        }
        // Delete the default import and trailing comma if unused
        if (importClause.name != null && !usesDefaultImport && importClause.namedBindings !== undefined) {
            // There must be some named imports or we would have been in case 1
            var end = importClause.namedBindings.getStart();
            this.fail(Rule.FAILURE_TYPE_IMPORT, importClause.name.text, importClause.name.getStart(), [
                this.deleteText(importClause.name.getStart(), end - importClause.name.getStart()),
            ]);
        }
        if (importClause.namedBindings != null &&
            importClause.namedBindings.kind === ts.SyntaxKind.NamedImports) {
            // Delete the entire named imports if all unused, including curly braces.
            if (usedNamedImports.every(function (e) { return !e; })) {
                var start = importClause.name != null ? importClause.name.getEnd() : importClause.namedBindings.getStart();
                this.fail(Rule.FAILURE_TYPE_IMPORT, importClause.namedBindings.getText(), importClause.namedBindings.getStart(), [
                    this.deleteText(start, importClause.namedBindings.getEnd() - start),
                ]);
            }
            else {
                var imports = importClause.namedBindings;
                var priorElementUsed = false;
                for (var idx = 0; idx < imports.elements.length; idx++) {
                    var namedImport = imports.elements[idx];
                    if (usedNamedImports[idx]) {
                        priorElementUsed = true;
                    }
                    else {
                        var isLast = idx === imports.elements.length - 1;
                        // Before the first used import, consume trailing commas.
                        // Afterward, consume leading commas instead.
                        var start = priorElementUsed ? imports.elements[idx - 1].getEnd() : namedImport.getStart();
                        var end = priorElementUsed || isLast ? namedImport.getEnd() : imports.elements[idx + 1].getStart();
                        this.fail(Rule.FAILURE_TYPE_IMPORT, namedImport.name.text, namedImport.name.getStart(), [
                            this.deleteText(start, end - start),
                        ]);
                    }
                }
            }
        }
        // import x = 'y' & import * as x from 'y' handled by other walker methods
        // because they only have one identifier that might be unused
        _super.prototype.visitImportDeclaration.call(this, node);
    };
    NoUnusedVariablesWalker.prototype.visitImportEqualsDeclaration = function (node) {
        if (!Lint.hasModifier(node.modifiers, ts.SyntaxKind.ExportKeyword)) {
            var name = node.name;
            this.validateReferencesForVariable(Rule.FAILURE_TYPE_IMPORT, name.text, name.getStart(), this.deleteImportStatement(node));
        }
        _super.prototype.visitImportEqualsDeclaration.call(this, node);
    };
    // skip parameters in index signatures (stuff like [key: string]: string)
    NoUnusedVariablesWalker.prototype.visitIndexSignatureDeclaration = function (node) {
        this.skipParameterDeclaration = true;
        _super.prototype.visitIndexSignatureDeclaration.call(this, node);
        this.skipParameterDeclaration = false;
    };
    // skip parameters in interfaces
    NoUnusedVariablesWalker.prototype.visitInterfaceDeclaration = function (node) {
        this.skipParameterDeclaration = true;
        _super.prototype.visitInterfaceDeclaration.call(this, node);
        this.skipParameterDeclaration = false;
    };
    NoUnusedVariablesWalker.prototype.visitJsxElement = function (node) {
        this.hasSeenJsxElement = true;
        _super.prototype.visitJsxElement.call(this, node);
    };
    NoUnusedVariablesWalker.prototype.visitJsxSelfClosingElement = function (node) {
        this.hasSeenJsxElement = true;
        _super.prototype.visitJsxSelfClosingElement.call(this, node);
    };
    // check private member functions
    NoUnusedVariablesWalker.prototype.visitMethodDeclaration = function (node) {
        if (node.name != null && node.name.kind === ts.SyntaxKind.Identifier) {
            var modifiers = node.modifiers;
            var variableName = node.name.text;
            if (Lint.hasModifier(modifiers, ts.SyntaxKind.PrivateKeyword)) {
                this.validateReferencesForVariable(Rule.FAILURE_TYPE_METHOD, variableName, node.name.getStart());
            }
        }
        // abstract methods can't have a body so their parameters are always unused
        if (Lint.hasModifier(node.modifiers, ts.SyntaxKind.AbstractKeyword)) {
            this.skipParameterDeclaration = true;
        }
        _super.prototype.visitMethodDeclaration.call(this, node);
        this.skipParameterDeclaration = false;
    };
    NoUnusedVariablesWalker.prototype.visitNamespaceImport = function (node) {
        if (node.parent !== undefined) {
            var importDeclaration = node.parent.parent;
            var moduleSpecifier = importDeclaration.moduleSpecifier.getText();
            // extract the unquoted module being imported
            var moduleNameMatch = moduleSpecifier.match(MODULE_SPECIFIER_MATCH);
            var isReactImport = (moduleNameMatch != null) && (REACT_MODULES.indexOf(moduleNameMatch[1]) !== -1);
            if (this.hasOption(OPTION_REACT) && isReactImport && node.name.text === REACT_NAMESPACE_IMPORT_NAME) {
                this.reactImport = node;
                var fileName = this.getSourceFile().fileName;
                var position = node.name.getStart();
                var highlights = this.languageService.getDocumentHighlights(fileName, position, [fileName]);
                if (highlights != null && highlights[0].highlightSpans.length > 1) {
                    this.isReactUsed = true;
                }
            }
            else {
                this.validateReferencesForVariable(Rule.FAILURE_TYPE_IMPORT, node.name.text, node.name.getStart(), this.deleteImportStatement(importDeclaration));
            }
        }
        _super.prototype.visitNamespaceImport.call(this, node);
    };
    NoUnusedVariablesWalker.prototype.visitParameterDeclaration = function (node) {
        var isSingleVariable = node.name.kind === ts.SyntaxKind.Identifier;
        var isPropertyParameter = Lint.hasModifier(node.modifiers, ts.SyntaxKind.PublicKeyword, ts.SyntaxKind.PrivateKeyword, ts.SyntaxKind.ProtectedKeyword);
        if (!isSingleVariable && isPropertyParameter) {
            // tsc error: a parameter property may not be a binding pattern
            this.skipBindingElement = true;
        }
        if (this.hasOption(OPTION_CHECK_PARAMETERS)
            && isSingleVariable
            && !this.skipParameterDeclaration
            && !Lint.hasModifier(node.modifiers, ts.SyntaxKind.PublicKeyword)) {
            var nameNode = node.name;
            this.validateReferencesForVariable(Rule.FAILURE_TYPE_PARAM, nameNode.text, node.name.getStart());
        }
        _super.prototype.visitParameterDeclaration.call(this, node);
        this.skipBindingElement = false;
    };
    // check private member variables
    NoUnusedVariablesWalker.prototype.visitPropertyDeclaration = function (node) {
        if (node.name != null && node.name.kind === ts.SyntaxKind.Identifier) {
            var modifiers = node.modifiers;
            var variableName = node.name.text;
            // check only if an explicit 'private' modifier is specified
            if (Lint.hasModifier(modifiers, ts.SyntaxKind.PrivateKeyword)) {
                this.validateReferencesForVariable(Rule.FAILURE_TYPE_PROP, variableName, node.name.getStart());
            }
        }
        _super.prototype.visitPropertyDeclaration.call(this, node);
    };
    NoUnusedVariablesWalker.prototype.visitVariableDeclaration = function (node) {
        var isSingleVariable = node.name.kind === ts.SyntaxKind.Identifier;
        if (isSingleVariable && !this.skipVariableDeclaration) {
            var variableIdentifier = node.name;
            this.validateReferencesForVariable(Rule.FAILURE_TYPE_VAR, variableIdentifier.text, variableIdentifier.getStart());
        }
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    // skip exported and declared variables
    NoUnusedVariablesWalker.prototype.visitVariableStatement = function (node) {
        if (Lint.hasModifier(node.modifiers, ts.SyntaxKind.ExportKeyword, ts.SyntaxKind.DeclareKeyword)) {
            this.skipBindingElement = true;
            this.skipVariableDeclaration = true;
        }
        _super.prototype.visitVariableStatement.call(this, node);
        this.skipBindingElement = false;
        this.skipVariableDeclaration = false;
    };
    /**
     * Delete the statement along with leading trivia.
     * BUT since imports are typically at the top of the file, the leading trivia is often a license.
     * So when the leading trivia includes a block comment, delete the statement without leading trivia instead.
     */
    NoUnusedVariablesWalker.prototype.deleteImportStatement = function (node) {
        if (node.getFullText().substr(0, node.getLeadingTriviaWidth()).indexOf("/*") >= 0) {
            return [this.deleteText(node.getStart(), node.getWidth())];
        }
        return [this.deleteText(node.getFullStart(), node.getFullWidth())];
    };
    NoUnusedVariablesWalker.prototype.validateReferencesForVariable = function (type, name, position, replacements) {
        if (!this.isUsed(name, position)) {
            this.fail(type, name, position, replacements);
        }
    };
    NoUnusedVariablesWalker.prototype.isUsed = function (name, position) {
        var fileName = this.getSourceFile().fileName;
        var highlights = this.languageService.getDocumentHighlights(fileName, position, [fileName]);
        return (highlights != null && highlights[0].highlightSpans.length > 1) || this.isIgnored(name);
    };
    NoUnusedVariablesWalker.prototype.fail = function (type, name, position, replacements) {
        var fix;
        if (replacements && replacements.length) {
            fix = new Lint.Fix(Rule.metadata.ruleName, replacements);
        }
        this.possibleFailures.push({ start: position, width: name.length, message: Rule.FAILURE_STRING_FACTORY(type, name), fix: fix });
    };
    NoUnusedVariablesWalker.prototype.isIgnored = function (name) {
        return this.ignorePattern != null && this.ignorePattern.test(name);
    };
    return NoUnusedVariablesWalker;
}(Lint.RuleWalker));
var _a, _b;
