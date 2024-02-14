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
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoNamespaceWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-namespace",
    description: "Disallows use of internal \`module\`s and \`namespace\`s.",
    descriptionDetails: "This rule still allows the use of `declare module ... {}`",
    rationale: (_a = ["\n            ES6-style external modules are the standard way to modularize code.\n            Using `module {}` and `namespace {}` are outdated ways to organize TypeScript code."], _a.raw = ["\n            ES6-style external modules are the standard way to modularize code.\n            Using \\`module {}\\` and \\`namespace {}\\` are outdated ways to organize TypeScript code."], Lint.Utils.dedent(_a)),
    optionsDescription: (_b = ["\n            One argument may be optionally provided:\n\n            * `allow-declarations` allows `declare namespace ... {}` to describe external APIs."], _b.raw = ["\n            One argument may be optionally provided:\n\n            * \\`allow-declarations\\` allows \\`declare namespace ... {}\\` to describe external APIs."], Lint.Utils.dedent(_b)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: ["allow-declarations"],
        },
        minLength: 0,
        maxLength: 1,
    },
    optionExamples: ["true", '[true, "allow-declarations"]'],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "'namespace' and 'module' are disallowed";
exports.Rule = Rule;
var NoNamespaceWalker = (function (_super) {
    __extends(NoNamespaceWalker, _super);
    function NoNamespaceWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoNamespaceWalker.prototype.visitSourceFile = function (node) {
        // Ignore all .d.ts files by returning and not walking their ASTs.
        // .d.ts declarations do not have the Ambient flag set, but are still declarations.
        if (this.hasOption("allow-declarations") && node.isDeclarationFile) {
            return;
        }
        this.walkChildren(node);
    };
    NoNamespaceWalker.prototype.visitModuleDeclaration = function (decl) {
        // declare module 'foo' {} is an external module, not a namespace.
        if (decl.name.kind === ts.SyntaxKind.StringLiteral ||
            this.hasOption("allow-declarations") && Lint.hasModifier(decl.modifiers, ts.SyntaxKind.DeclareKeyword)) {
            return;
        }
        this.addFailureAtNode(decl, Rule.FAILURE_STRING);
    };
    return NoNamespaceWalker;
}(Lint.RuleWalker));
var _a, _b;
