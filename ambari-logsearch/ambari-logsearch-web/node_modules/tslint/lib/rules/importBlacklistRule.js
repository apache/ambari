/**
 * @license
 * Copyright 2016 Palantir Technologies, Inc.
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
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.isEnabled = function () {
        var ruleArguments = this.getOptions().ruleArguments;
        return _super.prototype.isEnabled.call(this) && ruleArguments.length > 0;
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new ImportBlacklistWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "import-blacklist",
    description: (_a = ["\n            Disallows importing the specified modules directly via `import` and `require`.\n            Instead only sub modules may be imported from that module."], _a.raw = ["\n            Disallows importing the specified modules directly via \\`import\\` and \\`require\\`.\n            Instead only sub modules may be imported from that module."], Lint.Utils.dedent(_a)),
    rationale: (_b = ["\n            Some libraries allow importing their submodules instead of the entire module.\n            This is good practise as it avoids loading unused modules."], _b.raw = ["\n            Some libraries allow importing their submodules instead of the entire module.\n            This is good practise as it avoids loading unused modules."], Lint.Utils.dedent(_b)),
    optionsDescription: "A list of blacklisted modules.",
    options: {
        type: "array",
        items: {
            type: "string",
        },
        minLength: 1,
    },
    optionExamples: ["true", '[true, "rxjs", "lodash"]'],
    type: "functionality",
    typescriptOnly: false,
};
Rule.FAILURE_STRING = "This import is blacklisted, import a submodule instead";
exports.Rule = Rule;
var ImportBlacklistWalker = (function (_super) {
    __extends(ImportBlacklistWalker, _super);
    function ImportBlacklistWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ImportBlacklistWalker.prototype.visitCallExpression = function (node) {
        if (node.expression.kind === ts.SyntaxKind.Identifier &&
            node.expression.text === "require" &&
            node.arguments !== undefined &&
            node.arguments.length === 1) {
            this.checkForBannedImport(node.arguments[0]);
        }
        _super.prototype.visitCallExpression.call(this, node);
    };
    ImportBlacklistWalker.prototype.visitImportEqualsDeclaration = function (node) {
        if (utils.isExternalModuleReference(node.moduleReference) &&
            node.moduleReference.expression !== undefined) {
            // If it's an import require and not an import alias
            this.checkForBannedImport(node.moduleReference.expression);
        }
        _super.prototype.visitImportEqualsDeclaration.call(this, node);
    };
    ImportBlacklistWalker.prototype.visitImportDeclaration = function (node) {
        this.checkForBannedImport(node.moduleSpecifier);
        _super.prototype.visitImportDeclaration.call(this, node);
    };
    ImportBlacklistWalker.prototype.checkForBannedImport = function (expression) {
        if (utils.isTextualLiteral(expression) && this.hasOption(expression.text)) {
            this.addFailureFromStartToEnd(expression.getStart(this.getSourceFile()) + 1, expression.getEnd() - 1, Rule.FAILURE_STRING);
        }
    };
    return ImportBlacklistWalker;
}(Lint.RuleWalker));
var _a, _b;
