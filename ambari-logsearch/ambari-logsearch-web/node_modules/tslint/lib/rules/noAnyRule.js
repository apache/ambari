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
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoAnyWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-any",
    description: "Disallows usages of `any` as a type declaration.",
    hasFix: true,
    rationale: "Using `any` as a type declaration nullifies the compile-time benefits of the type system.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Type declaration of 'any' loses type-safety. " +
    "Consider replacing it with a more precise type, the empty type ('{}'), " +
    "or suppress this occurrence.";
exports.Rule = Rule;
var NoAnyWalker = (function (_super) {
    __extends(NoAnyWalker, _super);
    function NoAnyWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoAnyWalker.prototype.visitAnyKeyword = function (node) {
        var fix = this.createFix(this.createReplacement(node.getStart(), node.getWidth(), "{}"));
        this.addFailureAtNode(node, Rule.FAILURE_STRING, fix);
        _super.prototype.visitAnyKeyword.call(this, node);
    };
    return NoAnyWalker;
}(Lint.RuleWalker));
