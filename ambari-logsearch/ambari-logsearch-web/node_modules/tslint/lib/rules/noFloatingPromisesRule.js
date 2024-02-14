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
        var walker = new NoFloatingPromisesWalker(sourceFile, this.getOptions(), langSvc.getProgram());
        for (var _i = 0, _a = this.getOptions().ruleArguments; _i < _a.length; _i++) {
            var className = _a[_i];
            walker.addPromiseClass(className);
        }
        return this.applyWithWalker(walker);
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-floating-promises",
    description: "Promises returned by functions must be handled appropriately.",
    optionsDescription: (_a = ["\n            A list of 'string' names of any additional classes that should also be handled as Promises.\n        "], _a.raw = ["\n            A list of \\'string\\' names of any additional classes that should also be handled as Promises.\n        "], Lint.Utils.dedent(_a)),
    options: {
        type: "list",
        listType: {
            type: "array",
            items: { type: "string" },
        },
    },
    optionExamples: ["true", "[true, \"JQueryPromise\"]"],
    rationale: "Unhandled Promises can cause unexpected behavior, such as resolving at unexpected times.",
    type: "functionality",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Promises must be handled appropriately";
exports.Rule = Rule;
var NoFloatingPromisesWalker = (function (_super) {
    __extends(NoFloatingPromisesWalker, _super);
    function NoFloatingPromisesWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.promiseClasses = ["Promise"];
        return _this;
    }
    NoFloatingPromisesWalker.prototype.addPromiseClass = function (className) {
        this.promiseClasses.push(className);
    };
    NoFloatingPromisesWalker.prototype.visitCallExpression = function (node) {
        this.checkNode(node);
        _super.prototype.visitCallExpression.call(this, node);
    };
    NoFloatingPromisesWalker.prototype.visitExpressionStatement = function (node) {
        this.checkNode(node);
        _super.prototype.visitExpressionStatement.call(this, node);
    };
    NoFloatingPromisesWalker.prototype.checkNode = function (node) {
        if (node.parent && this.kindCanContainPromise(node.parent.kind)) {
            return;
        }
        var typeChecker = this.getTypeChecker();
        var type = typeChecker.getTypeAtLocation(node);
        if (this.symbolIsPromise(type.symbol)) {
            this.addFailure(this.createFailure(node.getStart(), node.getWidth(), Rule.FAILURE_STRING));
        }
    };
    NoFloatingPromisesWalker.prototype.symbolIsPromise = function (symbol) {
        if (!symbol) {
            return false;
        }
        return this.promiseClasses.indexOf(symbol.name) !== -1;
    };
    NoFloatingPromisesWalker.prototype.kindCanContainPromise = function (kind) {
        return !NoFloatingPromisesWalker.barredParentKinds[kind];
    };
    return NoFloatingPromisesWalker;
}(Lint.ProgramAwareRuleWalker));
NoFloatingPromisesWalker.barredParentKinds = (_b = {},
    _b[ts.SyntaxKind.Block] = true,
    _b[ts.SyntaxKind.ExpressionStatement] = true,
    _b[ts.SyntaxKind.SourceFile] = true,
    _b);
var _a, _b;
