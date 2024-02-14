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
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.FAILURE_STRING = function (importName, exportName) {
        return "Expected import '" + importName + "' to match the default export '" + exportName + "'.";
    };
    Rule.prototype.applyWithProgram = function (sourceFile, langSvc) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "match-default-export-name",
    description: (_a = ["\n            Requires that a default import have the same name as the declaration it imports.\n            Does nothing for anonymous default exports."], _a.raw = ["\n            Requires that a default import have the same name as the declaration it imports.\n            Does nothing for anonymous default exports."], Lint.Utils.dedent(_a)),
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
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitSourceFile = function (node) {
        for (var _i = 0, _a = node.statements; _i < _a.length; _i++) {
            var statement = _a[_i];
            if (statement.kind !== ts.SyntaxKind.ImportDeclaration) {
                continue;
            }
            var importClause = statement.importClause;
            if (importClause && importClause.name) {
                this.checkDefaultImport(importClause.name);
            }
        }
    };
    Walker.prototype.checkDefaultImport = function (defaultImport) {
        var declarations = this.getTypeChecker().getAliasedSymbol(this.getTypeChecker().getSymbolAtLocation(defaultImport)).declarations;
        var name = declarations && declarations[0] && declarations[0].name;
        if (name && name.kind === ts.SyntaxKind.Identifier && defaultImport.text !== name.text) {
            this.addFailureAtNode(defaultImport, Rule.FAILURE_STRING(defaultImport.text, name.text));
        }
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
var _a;
