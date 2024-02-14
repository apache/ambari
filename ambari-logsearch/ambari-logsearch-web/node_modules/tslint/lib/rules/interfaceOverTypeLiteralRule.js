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
        return this.applyWithWalker(new InterfaceOverTypeLiteralWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "interface-over-type-literal",
    description: "Prefer an interface declaration over a type literal (`type T = { ... }`)",
    rationale: "Interfaces are generally preferred over type literals because interfaces can be implemented, extended and merged.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Use an interface instead of a type literal.";
exports.Rule = Rule;
var InterfaceOverTypeLiteralWalker = (function (_super) {
    __extends(InterfaceOverTypeLiteralWalker, _super);
    function InterfaceOverTypeLiteralWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    InterfaceOverTypeLiteralWalker.prototype.visitTypeAliasDeclaration = function (node) {
        if (node.type.kind === ts.SyntaxKind.TypeLiteral) {
            this.addFailureAtNode(node.name, Rule.FAILURE_STRING);
        }
        _super.prototype.visitTypeAliasDeclaration.call(this, node);
    };
    return InterfaceOverTypeLiteralWalker;
}(Lint.RuleWalker));
