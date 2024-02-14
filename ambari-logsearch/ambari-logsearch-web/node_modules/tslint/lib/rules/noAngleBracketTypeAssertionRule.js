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
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoAngleBracketTypeAssertionWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-angle-bracket-type-assertion",
    description: "Requires the use of `as Type` for type assertions instead of `<Type>`.",
    hasFix: true,
    rationale: (_a = ["\n            Both formats of type assertions have the same effect, but only `as` type assertions\n            work in `.tsx` files. This rule ensures that you have a consistent type assertion style\n            across your codebase."], _a.raw = ["\n            Both formats of type assertions have the same effect, but only \\`as\\` type assertions\n            work in \\`.tsx\\` files. This rule ensures that you have a consistent type assertion style\n            across your codebase."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Type assertion using the '<>' syntax is forbidden. Use the 'as' syntax instead.";
exports.Rule = Rule;
var NoAngleBracketTypeAssertionWalker = (function (_super) {
    __extends(NoAngleBracketTypeAssertionWalker, _super);
    function NoAngleBracketTypeAssertionWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoAngleBracketTypeAssertionWalker.prototype.visitTypeAssertionExpression = function (node) {
        var expression = node.expression, type = node.type;
        var fix = this.createFix(
        // add 'as' syntax at end
        this.createReplacement(node.getEnd(), 0, " as " + type.getText()), 
        // delete the angle bracket assertion
        this.createReplacement(node.getStart(), expression.getStart() - node.getStart(), ""));
        this.addFailureAtNode(node, Rule.FAILURE_STRING, fix);
        _super.prototype.visitTypeAssertionExpression.call(this, node);
    };
    return NoAngleBracketTypeAssertionWalker;
}(Lint.RuleWalker));
var _a;
