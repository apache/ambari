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
        return this.applyWithWalker(new ObjectLiteralSortKeysWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "object-literal-sort-keys",
    description: "Requires keys in object literals to be sorted alphabetically",
    rationale: "Useful in preventing merge conflicts",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (name) {
    return "The key '" + name + "' is not sorted alphabetically";
};
exports.Rule = Rule;
var ObjectLiteralSortKeysWalker = (function (_super) {
    __extends(ObjectLiteralSortKeysWalker, _super);
    function ObjectLiteralSortKeysWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        // stacks are used to maintain state while recursing through nested object literals
        _this.lastSortedKeyStack = [];
        _this.multilineFlagStack = [];
        _this.sortedStateStack = [];
        return _this;
    }
    ObjectLiteralSortKeysWalker.prototype.visitObjectLiteralExpression = function (node) {
        // char code 0; every string should be >= to this
        this.lastSortedKeyStack.push("");
        // sorted state is always initially true
        this.sortedStateStack.push(true);
        this.multilineFlagStack.push(this.isMultilineListNode(node));
        _super.prototype.visitObjectLiteralExpression.call(this, node);
        this.multilineFlagStack.pop();
        this.lastSortedKeyStack.pop();
        this.sortedStateStack.pop();
    };
    ObjectLiteralSortKeysWalker.prototype.visitPropertyAssignment = function (node) {
        var sortedState = this.sortedStateStack[this.sortedStateStack.length - 1];
        var isMultiline = this.multilineFlagStack[this.multilineFlagStack.length - 1];
        // skip remainder of object literal scan if a previous key was found
        // in an unsorted position. This ensures only one error is thrown at
        // a time and keeps error output clean. Skip also single line objects.
        if (sortedState && isMultiline) {
            var lastSortedKey = this.lastSortedKeyStack[this.lastSortedKeyStack.length - 1];
            var keyNode = node.name;
            if (isIdentifierOrStringLiteral(keyNode)) {
                var key = keyNode.text;
                if (key < lastSortedKey) {
                    var failureString = Rule.FAILURE_STRING_FACTORY(key);
                    this.addFailureAtNode(keyNode, failureString);
                    this.sortedStateStack[this.sortedStateStack.length - 1] = false;
                }
                else {
                    this.lastSortedKeyStack[this.lastSortedKeyStack.length - 1] = key;
                }
            }
        }
        _super.prototype.visitPropertyAssignment.call(this, node);
    };
    ObjectLiteralSortKeysWalker.prototype.isMultilineListNode = function (node) {
        var startLineOfNode = this.getLineAndCharacterOfPosition(node.getStart()).line;
        var endLineOfNode = this.getLineAndCharacterOfPosition(node.getEnd()).line;
        return endLineOfNode !== startLineOfNode;
    };
    return ObjectLiteralSortKeysWalker;
}(Lint.RuleWalker));
function isIdentifierOrStringLiteral(node) {
    return node.kind === ts.SyntaxKind.Identifier || node.kind === ts.SyntaxKind.StringLiteral;
}
