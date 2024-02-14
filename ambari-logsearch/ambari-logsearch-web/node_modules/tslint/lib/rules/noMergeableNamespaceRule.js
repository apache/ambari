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
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.failureStringFactory = function (name, seenBeforeLine) {
        return "Mergeable namespace '" + name + "' found. Merge its contents with the namespace on line " + seenBeforeLine + ".";
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-mergeable-namespace",
    description: "Disallows mergeable namespaces in the same file.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "maintainability",
    typescriptOnly: true,
};
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitSourceFile = function (node) {
        this.checkStatements(node.statements);
        // All tree-walking handled by 'checkStatements'
    };
    Walker.prototype.checkStatements = function (statements) {
        var seen = new Map();
        for (var _i = 0, statements_1 = statements; _i < statements_1.length; _i++) {
            var statement = statements_1[_i];
            if (statement.kind !== ts.SyntaxKind.ModuleDeclaration) {
                continue;
            }
            var name = statement.name;
            if (name.kind === ts.SyntaxKind.Identifier) {
                var text = name.text;
                var prev = seen.get(text);
                if (prev) {
                    this.addFailureAtNode(name, Rule.failureStringFactory(text, this.getLineOfNode(prev)));
                }
                seen.set(text, statement);
            }
            // Recursively check in all module declarations
            this.checkModuleDeclaration(statement);
        }
    };
    Walker.prototype.checkModuleDeclaration = function (decl) {
        var body = decl.body;
        if (!body) {
            return;
        }
        switch (body.kind) {
            case ts.SyntaxKind.ModuleBlock:
                this.checkStatements(body.statements);
                break;
            case ts.SyntaxKind.ModuleDeclaration:
                this.checkModuleDeclaration(body);
                break;
            default:
                break;
        }
    };
    Walker.prototype.getLineOfNode = function (node) {
        return this.getLineAndCharacterOfPosition(node.pos).line;
    };
    return Walker;
}(Lint.RuleWalker));
