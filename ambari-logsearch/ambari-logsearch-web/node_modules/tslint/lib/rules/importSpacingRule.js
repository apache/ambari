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
var Lint = require("../index");
var LINE_BREAK_REGEX = /\n|\r\n/;
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var comparisonWalker = new ImportStatementWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(comparisonWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "import-spacing",
    description: "Ensures proper spacing between import statement keywords",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: false,
};
Rule.ADD_SPACE_AFTER_IMPORT = "Add space after 'import'";
Rule.TOO_MANY_SPACES_AFTER_IMPORT = "Too many spaces after 'import'";
Rule.ADD_SPACE_AFTER_STAR = "Add space after '*'";
Rule.TOO_MANY_SPACES_AFTER_STAR = "Too many spaces after '*'";
Rule.ADD_SPACE_AFTER_FROM = "Add space after 'from'";
Rule.TOO_MANY_SPACES_AFTER_FROM = "Too many spaces after 'from'";
Rule.ADD_SPACE_BEFORE_FROM = "Add space before 'from'";
Rule.TOO_MANY_SPACES_BEFORE_FROM = "Too many spaces before 'from'";
Rule.NO_LINE_BREAKS = "Line breaks are not allowed in import declaration";
exports.Rule = Rule;
var ImportStatementWalker = (function (_super) {
    __extends(ImportStatementWalker, _super);
    function ImportStatementWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ImportStatementWalker.prototype.visitImportDeclaration = function (node) {
        if (!node.importClause) {
            this.checkModuleWithSideEffect(node);
        }
        else {
            var nodeStart = node.getStart();
            var importKeywordEnd = node.getStart() + "import".length;
            var moduleSpecifierStart = node.moduleSpecifier.getStart();
            var importClauseEnd = node.importClause.getEnd();
            var importClauseStart = node.importClause.getStart();
            if (importKeywordEnd === importClauseStart) {
                this.addFailure(this.createFailure(nodeStart, "import".length, Rule.ADD_SPACE_AFTER_IMPORT));
            }
            else if (importClauseStart > (importKeywordEnd + 1)) {
                this.addFailure(this.createFailure(nodeStart, importClauseStart - nodeStart, Rule.TOO_MANY_SPACES_AFTER_IMPORT));
            }
            var fromString = node.getText().substring(importClauseEnd - nodeStart, moduleSpecifierStart - nodeStart);
            if (/from$/.test(fromString)) {
                this.addFailure(this.createFailure(importClauseEnd, fromString.length, Rule.ADD_SPACE_AFTER_FROM));
            }
            else if (/from\s{2,}$/.test(fromString)) {
                this.addFailure(this.createFailure(importClauseEnd, fromString.length, Rule.TOO_MANY_SPACES_AFTER_FROM));
            }
            if (/^\s{2,}from/.test(fromString)) {
                this.addFailure(this.createFailure(importClauseEnd, fromString.length, Rule.TOO_MANY_SPACES_BEFORE_FROM));
            }
            else if (/^from/.test(fromString)) {
                this.addFailure(this.createFailure(importClauseEnd, fromString.length, Rule.ADD_SPACE_BEFORE_FROM));
            }
            var text = node.getText();
            var beforeImportClauseText = text.substring(0, importClauseStart - nodeStart);
            var afterImportClauseText = text.substring(importClauseEnd - nodeStart);
            if (LINE_BREAK_REGEX.test(beforeImportClauseText)) {
                this.addFailure(this.createFailure(nodeStart, importClauseStart - nodeStart - 1, Rule.NO_LINE_BREAKS));
            }
            if (LINE_BREAK_REGEX.test(afterImportClauseText)) {
                this.addFailure(this.createFailure(importClauseEnd, node.getEnd() - importClauseEnd, Rule.NO_LINE_BREAKS));
            }
        }
        _super.prototype.visitImportDeclaration.call(this, node);
    };
    ImportStatementWalker.prototype.visitNamespaceImport = function (node) {
        var text = node.getText();
        if (text.indexOf("*as") > -1) {
            this.addFailure(this.createFailure(node.getStart(), node.getWidth(), Rule.ADD_SPACE_AFTER_STAR));
        }
        else if (/\*\s{2,}as/.test(text)) {
            this.addFailure(this.createFailure(node.getStart(), node.getWidth(), Rule.TOO_MANY_SPACES_AFTER_STAR));
        }
        else if (LINE_BREAK_REGEX.test(text)) {
            this.addFailure(this.createFailure(node.getStart(), node.getWidth(), Rule.NO_LINE_BREAKS));
        }
        _super.prototype.visitNamespaceImport.call(this, node);
    };
    ImportStatementWalker.prototype.checkModuleWithSideEffect = function (node) {
        var moduleSpecifierStart = node.moduleSpecifier.getStart();
        var nodeStart = node.getStart();
        if ((nodeStart + "import".length + 1) < moduleSpecifierStart) {
            this.addFailure(this.createFailure(nodeStart, moduleSpecifierStart - nodeStart, Rule.TOO_MANY_SPACES_AFTER_IMPORT));
        }
        else if ((nodeStart + "import".length) === moduleSpecifierStart) {
            this.addFailure(this.createFailure(nodeStart, "import".length, Rule.ADD_SPACE_AFTER_IMPORT));
        }
        if (LINE_BREAK_REGEX.test(node.getText())) {
            this.addFailure(this.createFailure(nodeStart, node.getWidth(), Rule.NO_LINE_BREAKS));
        }
    };
    return ImportStatementWalker;
}(Lint.RuleWalker));
