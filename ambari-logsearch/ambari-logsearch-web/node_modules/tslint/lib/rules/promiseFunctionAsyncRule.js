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
    Rule.prototype.applyWithProgram = function (sourceFile, langSvc) {
        return this.applyWithWalker(new PromiseAsyncWalker(sourceFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "promise-function-async",
    description: "Requires any function or method that returns a promise to be marked async.",
    rationale: (_a = ["\n            Ensures that each function is only capable of 1) returning a rejected promise, or 2)\n            throwing an Error object. In contrast, non-`async` `Promise`-returning functions\n            are technically capable of either. This practice removes a requirement for consuming\n            code to handle both cases.\n        "], _a.raw = ["\n            Ensures that each function is only capable of 1) returning a rejected promise, or 2)\n            throwing an Error object. In contrast, non-\\`async\\` \\`Promise\\`-returning functions\n            are technically capable of either. This practice removes a requirement for consuming\n            code to handle both cases.\n        "], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "typescript",
    typescriptOnly: false,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "functions that return promises must be async";
exports.Rule = Rule;
var PromiseAsyncWalker = (function (_super) {
    __extends(PromiseAsyncWalker, _super);
    function PromiseAsyncWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PromiseAsyncWalker.prototype.visitArrowFunction = function (node) {
        this.handleDeclaration(node);
        _super.prototype.visitArrowFunction.call(this, node);
    };
    PromiseAsyncWalker.prototype.visitFunctionDeclaration = function (node) {
        this.handleDeclaration(node);
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    PromiseAsyncWalker.prototype.visitFunctionExpression = function (node) {
        this.handleDeclaration(node);
        _super.prototype.visitFunctionExpression.call(this, node);
    };
    PromiseAsyncWalker.prototype.visitMethodDeclaration = function (node) {
        this.handleDeclaration(node);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    PromiseAsyncWalker.prototype.handleDeclaration = function (node) {
        var tc = this.getTypeChecker();
        var signature = tc.getTypeAtLocation(node).getCallSignatures()[0];
        var returnType = tc.typeToString(tc.getReturnTypeOfSignature(signature));
        var isAsync = Lint.hasModifier(node.modifiers, ts.SyntaxKind.AsyncKeyword);
        var isPromise = returnType.indexOf("Promise<") === 0;
        var signatureEnd = node.body != null
            ? node.body.getStart() - node.getStart() - 1
            : node.getWidth();
        if (isPromise && !isAsync) {
            this.addFailureAt(node.getStart(), signatureEnd, Rule.FAILURE_STRING);
        }
    };
    return PromiseAsyncWalker;
}(Lint.ProgramAwareRuleWalker));
var _a;
