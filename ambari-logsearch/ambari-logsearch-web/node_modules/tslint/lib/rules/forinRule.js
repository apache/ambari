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
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new ForInWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "forin",
    description: "Requires a `for ... in` statement to be filtered with an `if` statement.",
    rationale: (_a = ["\n            ```ts\n            for (let key in someObject) {\n                if (someObject.hasOwnProperty(key)) {\n                    // code here\n                }\n            }\n            ```\n            Prevents accidental iteration over properties inherited from an object's prototype.\n            See [MDN's `for...in`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/for...in)\n            documentation for more information about `for...in` loops."], _a.raw = ["\n            \\`\\`\\`ts\n            for (let key in someObject) {\n                if (someObject.hasOwnProperty(key)) {\n                    // code here\n                }\n            }\n            \\`\\`\\`\n            Prevents accidental iteration over properties inherited from an object's prototype.\n            See [MDN's \\`for...in\\`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/for...in)\n            documentation for more information about \\`for...in\\` loops."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "for (... in ...) statements must be filtered with an if statement";
exports.Rule = Rule;
var ForInWalker = (function (_super) {
    __extends(ForInWalker, _super);
    function ForInWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ForInWalker.prototype.visitForInStatement = function (node) {
        this.handleForInStatement(node);
        _super.prototype.visitForInStatement.call(this, node);
    };
    ForInWalker.prototype.handleForInStatement = function (node) {
        var statement = node.statement;
        var statementKind = node.statement.kind;
        // a direct if statement under a for...in is valid
        if (statementKind === ts.SyntaxKind.IfStatement) {
            return;
        }
        // if there is a block, verify that it has a single if statement or starts with if (..) continue;
        if (statementKind === ts.SyntaxKind.Block) {
            var blockNode = statement;
            var blockStatements = blockNode.statements;
            if (blockStatements.length >= 1) {
                var firstBlockStatement = blockStatements[0];
                if (firstBlockStatement.kind === ts.SyntaxKind.IfStatement) {
                    // if this "if" statement is the only statement within the block
                    if (blockStatements.length === 1) {
                        return;
                    }
                    // if this "if" statement has a single continue block
                    var ifStatement = firstBlockStatement.thenStatement;
                    if (nodeIsContinue(ifStatement)) {
                        return;
                    }
                }
            }
        }
        this.addFailureAtNode(node, Rule.FAILURE_STRING);
    };
    return ForInWalker;
}(Lint.RuleWalker));
function nodeIsContinue(node) {
    var kind = node.kind;
    if (kind === ts.SyntaxKind.ContinueStatement) {
        return true;
    }
    if (kind === ts.SyntaxKind.Block) {
        var blockStatements = node.statements;
        if (blockStatements.length === 1 && blockStatements[0].kind === ts.SyntaxKind.ContinueStatement) {
            return true;
        }
    }
    return false;
}
var _a;
