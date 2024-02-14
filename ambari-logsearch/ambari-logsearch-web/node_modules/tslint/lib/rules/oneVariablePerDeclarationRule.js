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
var OPTION_IGNORE_FOR_LOOP = "ignore-for-loop";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var oneVarWalker = new OneVariablePerDeclarationWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(oneVarWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "one-variable-per-declaration",
    description: "Disallows multiple variable definitions in the same declaration statement.",
    optionsDescription: (_a = ["\n            One argument may be optionally provided:\n\n            * `", "` allows multiple variable definitions in a for loop declaration."], _a.raw = ["\n            One argument may be optionally provided:\n\n            * \\`", "\\` allows multiple variable definitions in a for loop declaration."], Lint.Utils.dedent(_a, OPTION_IGNORE_FOR_LOOP)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [OPTION_IGNORE_FOR_LOOP],
        },
        minLength: 0,
        maxLength: 1,
    },
    optionExamples: ["true", "[true, \"" + OPTION_IGNORE_FOR_LOOP + "\"]"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Multiple variable declarations in the same statement are forbidden";
exports.Rule = Rule;
var OneVariablePerDeclarationWalker = (function (_super) {
    __extends(OneVariablePerDeclarationWalker, _super);
    function OneVariablePerDeclarationWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    OneVariablePerDeclarationWalker.prototype.visitVariableStatement = function (node) {
        var declarationList = node.declarationList;
        if (declarationList.declarations.length > 1) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING);
        }
        _super.prototype.visitVariableStatement.call(this, node);
    };
    OneVariablePerDeclarationWalker.prototype.visitForStatement = function (node) {
        var initializer = node.initializer;
        var shouldIgnoreForLoop = this.hasOption(OPTION_IGNORE_FOR_LOOP);
        if (!shouldIgnoreForLoop
            && initializer != null
            && initializer.kind === ts.SyntaxKind.VariableDeclarationList
            && initializer.declarations.length > 1) {
            this.addFailureAtNode(initializer, Rule.FAILURE_STRING);
        }
        _super.prototype.visitForStatement.call(this, node);
    };
    return OneVariablePerDeclarationWalker;
}(Lint.RuleWalker));
var _a;
