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
var OPTION_ALLOW_DECLARATIONS = "allow-declarations";
var OPTION_ALLOW_NAMED_FUNCTIONS = "allow-named-functions";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new OnlyArrowFunctionsWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "only-arrow-functions",
    description: "Disallows traditional (non-arrow) function expressions.",
    rationale: "Traditional functions don't bind lexical scope, which can lead to unexpected behavior when accessing 'this'.",
    optionsDescription: (_a = ["\n            Two arguments may be optionally provided:\n\n            * `\"", "\"` allows standalone function declarations.\n            * `\"", "\"` allows the expression `function foo() {}` but not `function() {}`.\n        "], _a.raw = ["\n            Two arguments may be optionally provided:\n\n            * \\`\"", "\"\\` allows standalone function declarations.\n            * \\`\"", "\"\\` allows the expression \\`function foo() {}\\` but not \\`function() {}\\`.\n        "], Lint.Utils.dedent(_a, OPTION_ALLOW_DECLARATIONS, OPTION_ALLOW_NAMED_FUNCTIONS)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [OPTION_ALLOW_DECLARATIONS, OPTION_ALLOW_NAMED_FUNCTIONS],
        },
        minLength: 0,
        maxLength: 1,
    },
    optionExamples: ["true", "[true, \"" + OPTION_ALLOW_DECLARATIONS + "\", \"" + OPTION_ALLOW_NAMED_FUNCTIONS + "\"]"],
    type: "typescript",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "non-arrow functions are forbidden";
exports.Rule = Rule;
var OnlyArrowFunctionsWalker = (function (_super) {
    __extends(OnlyArrowFunctionsWalker, _super);
    function OnlyArrowFunctionsWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.allowDeclarations = _this.hasOption(OPTION_ALLOW_DECLARATIONS);
        _this.allowNamedFunctions = _this.hasOption(OPTION_ALLOW_NAMED_FUNCTIONS);
        return _this;
    }
    OnlyArrowFunctionsWalker.prototype.visitFunctionDeclaration = function (node) {
        if (!this.allowDeclarations && !this.allowNamedFunctions) {
            this.failUnlessExempt(node);
        }
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    OnlyArrowFunctionsWalker.prototype.visitFunctionExpression = function (node) {
        if (node.name === undefined || !this.allowNamedFunctions) {
            this.failUnlessExempt(node);
        }
        _super.prototype.visitFunctionExpression.call(this, node);
    };
    OnlyArrowFunctionsWalker.prototype.failUnlessExempt = function (node) {
        if (!functionIsExempt(node)) {
            this.addFailureAtNode(Lint.childOfKind(node, ts.SyntaxKind.FunctionKeyword), Rule.FAILURE_STRING);
        }
    };
    return OnlyArrowFunctionsWalker;
}(Lint.RuleWalker));
/** Generator functions and functions explicitly declaring `this` are allowed. */
function functionIsExempt(node) {
    return node.asteriskToken || hasThisParameter(node);
}
function hasThisParameter(node) {
    var first = node.parameters[0];
    return first && first.name.kind === ts.SyntaxKind.Identifier &&
        first.name.originalKeywordKind === ts.SyntaxKind.ThisKeyword;
}
var _a;
