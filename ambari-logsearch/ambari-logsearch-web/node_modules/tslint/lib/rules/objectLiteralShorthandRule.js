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
        var objectLiteralShorthandWalker = new ObjectLiteralShorthandWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(objectLiteralShorthandWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "object-literal-shorthand",
    description: "Enforces use of ES6 object literal shorthand when possible.",
    hasFix: true,
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.LONGHAND_PROPERTY = "Expected property shorthand in object literal ";
Rule.LONGHAND_METHOD = "Expected method shorthand in object literal ";
exports.Rule = Rule;
var ObjectLiteralShorthandWalker = (function (_super) {
    __extends(ObjectLiteralShorthandWalker, _super);
    function ObjectLiteralShorthandWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ObjectLiteralShorthandWalker.prototype.visitPropertyAssignment = function (node) {
        var name = node.name;
        var value = node.initializer;
        if (name.kind === ts.SyntaxKind.Identifier &&
            value.kind === ts.SyntaxKind.Identifier &&
            name.getText() === value.getText()) {
            // Delete from name start up to value to include the ':'.
            var lengthToValueStart = value.getStart() - name.getStart();
            var fix = this.createFix(this.deleteText(name.getStart(), lengthToValueStart));
            this.addFailureAtNode(node, Rule.LONGHAND_PROPERTY + ("('{" + name.getText() + "}')."), fix);
        }
        if (value.kind === ts.SyntaxKind.FunctionExpression) {
            var fnNode = value;
            if (fnNode.name) {
                return; // named function expressions are OK.
            }
            var star = fnNode.asteriskToken ? fnNode.asteriskToken.getText() : "";
            this.addFailureAtNode(node, Rule.LONGHAND_METHOD + ("('{" + name.getText() + star + "() {...}}')."));
        }
        _super.prototype.visitPropertyAssignment.call(this, node);
    };
    return ObjectLiteralShorthandWalker;
}(Lint.RuleWalker));
