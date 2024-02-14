/**
 * @license
 * Copyright 2017 Palantir Technologies, Inc.
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
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "prefer-method-signature",
    description: "Prefer `foo(): void` over `foo: () => void` in interfaces and types.",
    hasFix: true,
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Use a method signature instead of a property signature of function type.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitPropertySignature = function (node) {
        var type = node.type;
        if (type !== undefined && type.kind === ts.SyntaxKind.FunctionType) {
            this.addFailureAtNode(node.name, Rule.FAILURE_STRING, this.createMethodSignatureFix(node, type));
        }
        _super.prototype.visitPropertySignature.call(this, node);
    };
    Walker.prototype.createMethodSignatureFix = function (node, type) {
        return type.type && this.createFix(this.deleteFromTo(Lint.childOfKind(node, ts.SyntaxKind.ColonToken).getStart(), type.getStart()), this.deleteFromTo(Lint.childOfKind(type, ts.SyntaxKind.EqualsGreaterThanToken).getStart(), type.type.getStart()), this.appendText(Lint.childOfKind(type, ts.SyntaxKind.CloseParenToken).end, ":"));
    };
    return Walker;
}(Lint.RuleWalker));
