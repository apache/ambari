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
        var allowedNumbers = this.ruleArguments.length > 0 ? this.ruleArguments : Rule.DEFAULT_ALLOWED;
        return this.applyWithWalker(new NoMagicNumbersWalker(sourceFile, this.ruleName, new Set(allowedNumbers.map(String))));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-magic-numbers",
    description: (_a = ["\n            Disallows the use constant number values outside of variable assignments.\n            When no list of allowed values is specified, -1, 0 and 1 are allowed by default."], _a.raw = ["\n            Disallows the use constant number values outside of variable assignments.\n            When no list of allowed values is specified, -1, 0 and 1 are allowed by default."], Lint.Utils.dedent(_a)),
    rationale: (_b = ["\n            Magic numbers should be avoided as they often lack documentation, forcing\n            them to be stored in variables gives them implicit documentation."], _b.raw = ["\n            Magic numbers should be avoided as they often lack documentation, forcing\n            them to be stored in variables gives them implicit documentation."], Lint.Utils.dedent(_b)),
    optionsDescription: "A list of allowed numbers.",
    options: {
        type: "array",
        items: {
            type: "number",
        },
        minLength: 1,
    },
    optionExamples: ["true", "[true, 1, 2, 3]"],
    type: "typescript",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "'magic numbers' are not allowed";
Rule.ALLOWED_NODES = new Set([
    ts.SyntaxKind.ExportAssignment,
    ts.SyntaxKind.FirstAssignment,
    ts.SyntaxKind.LastAssignment,
    ts.SyntaxKind.PropertyAssignment,
    ts.SyntaxKind.ShorthandPropertyAssignment,
    ts.SyntaxKind.VariableDeclaration,
    ts.SyntaxKind.VariableDeclarationList,
    ts.SyntaxKind.EnumMember,
    ts.SyntaxKind.PropertyDeclaration,
    ts.SyntaxKind.Parameter,
]);
Rule.DEFAULT_ALLOWED = [-1, 0, 1];
exports.Rule = Rule;
var NoMagicNumbersWalker = (function (_super) {
    __extends(NoMagicNumbersWalker, _super);
    function NoMagicNumbersWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoMagicNumbersWalker.prototype.walk = function (sourceFile) {
        var _this = this;
        var cb = function (node) {
            if (node.kind === ts.SyntaxKind.NumericLiteral) {
                _this.checkNumericLiteral(node, node.text);
            }
            else if (node.kind === ts.SyntaxKind.PrefixUnaryExpression &&
                node.operator === ts.SyntaxKind.MinusToken) {
                _this.checkNumericLiteral(node, "-" + node.operand.text);
            }
            else {
                ts.forEachChild(node, cb);
            }
        };
        return ts.forEachChild(sourceFile, cb);
    };
    NoMagicNumbersWalker.prototype.checkNumericLiteral = function (node, num) {
        if (!Rule.ALLOWED_NODES.has(node.parent.kind) && !this.options.has(num)) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING);
        }
    };
    return NoMagicNumbersWalker;
}(Lint.AbstractWalker));
var _a, _b;
