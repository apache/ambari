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
var OPTION_FUNCTION_IN_METHOD = "check-function-in-method";
var DEPRECATED_OPTION_FUNCTION_IN_METHOD = "no-this-in-function-in-method";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoInvalidThisWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-invalid-this",
    description: "Disallows using the `this` keyword outside of classes.",
    rationale: "See [the rule's author's rationale here.](https://github.com/palantir/tslint/pull/1105#issue-147549402)",
    optionsDescription: (_a = ["\n            One argument may be optionally provided:\n\n            * `", "` disallows using the `this` keyword in functions within class methods."], _a.raw = ["\n            One argument may be optionally provided:\n\n            * \\`", "\\` disallows using the \\`this\\` keyword in functions within class methods."], Lint.Utils.dedent(_a, OPTION_FUNCTION_IN_METHOD)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [OPTION_FUNCTION_IN_METHOD],
        },
        minLength: 0,
        maxLength: 1,
    },
    optionExamples: ["true", "[true, \"" + OPTION_FUNCTION_IN_METHOD + "\"]"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_OUTSIDE = "the \"this\" keyword is disallowed outside of a class body";
Rule.FAILURE_STRING_INSIDE = "the \"this\" keyword is disallowed in function bodies inside class methods, " +
    "use arrow functions instead";
exports.Rule = Rule;
var NoInvalidThisWalker = (function (_super) {
    __extends(NoInvalidThisWalker, _super);
    function NoInvalidThisWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoInvalidThisWalker.prototype.createScope = function (node) {
        var isClassScope = node.kind === ts.SyntaxKind.ClassDeclaration || node.kind === ts.SyntaxKind.ClassExpression;
        var inFunction = node.kind === ts.SyntaxKind.FunctionDeclaration || node.kind === ts.SyntaxKind.FunctionExpression;
        return {
            inClass: isClassScope,
            inFunction: inFunction,
        };
    };
    NoInvalidThisWalker.prototype.validateThisKeyword = function (node) {
        var inClass = 0;
        var inFunction = 0;
        this.getAllScopes().forEach(function (scope, index) {
            inClass = scope.inClass ? index + 1 : inClass;
            inFunction = scope.inFunction ? index + 1 : inFunction;
        });
        if (inClass === 0) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING_OUTSIDE);
        }
        var checkFuncInMethod = this.hasOption(DEPRECATED_OPTION_FUNCTION_IN_METHOD) || this.hasOption(OPTION_FUNCTION_IN_METHOD);
        if (checkFuncInMethod && inClass > 0 && inFunction > inClass) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING_INSIDE);
        }
    };
    NoInvalidThisWalker.prototype.visitNode = function (node) {
        if (node.kind === ts.SyntaxKind.ThisKeyword) {
            this.validateThisKeyword(node);
        }
        _super.prototype.visitNode.call(this, node);
    };
    return NoInvalidThisWalker;
}(Lint.ScopeAwareRuleWalker));
var _a;
