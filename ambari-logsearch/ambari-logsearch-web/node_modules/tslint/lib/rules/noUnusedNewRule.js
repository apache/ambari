/**
 * @license
 * Copyright 2014 Palantir Technologies, Inc.
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
var noUnusedExpressionRule_1 = require("./noUnusedExpressionRule");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoUnusedNewWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-unused-new",
    description: "Disallows unused 'new' expression statements.",
    descriptionDetails: (_a = ["\n            Unused 'new' expressions indicate that a constructor is being invoked solely for its side effects."], _a.raw = ["\n            Unused 'new' expressions indicate that a constructor is being invoked solely for its side effects."], Lint.Utils.dedent(_a)),
    rationale: (_b = ["\n            Detects constructs such as `new SomeClass()`, where a constructor is used solely for its side effects, which is considered\n            poor style."], _b.raw = ["\n            Detects constructs such as \\`new SomeClass()\\`, where a constructor is used solely for its side effects, which is considered\n            poor style."], Lint.Utils.dedent(_b)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "do not use 'new' for side effects";
exports.Rule = Rule;
var NoUnusedNewWalker = (function (_super) {
    __extends(NoUnusedNewWalker, _super);
    function NoUnusedNewWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.expressionContainsNew = false;
        return _this;
    }
    NoUnusedNewWalker.prototype.visitExpressionStatement = function (node) {
        this.expressionContainsNew = false;
        _super.prototype.visitExpressionStatement.call(this, node);
    };
    NoUnusedNewWalker.prototype.visitNewExpression = function (node) {
        _super.prototype.visitNewExpression.call(this, node);
        this.expressionIsUnused = true;
        this.expressionContainsNew = true;
    };
    NoUnusedNewWalker.prototype.checkExpressionUsage = function (node) {
        if (this.expressionIsUnused && this.expressionContainsNew) {
            var expression = node.expression;
            var kind = expression.kind;
            var isValidStandaloneExpression = kind === ts.SyntaxKind.DeleteExpression
                || kind === ts.SyntaxKind.YieldExpression
                || kind === ts.SyntaxKind.AwaitExpression;
            if (!isValidStandaloneExpression && !noUnusedExpressionRule_1.NoUnusedExpressionWalker.isDirective(node)) {
                this.addFailureAtNode(node, Rule.FAILURE_STRING);
            }
        }
    };
    return NoUnusedNewWalker;
}(noUnusedExpressionRule_1.NoUnusedExpressionWalker));
var _a, _b;
