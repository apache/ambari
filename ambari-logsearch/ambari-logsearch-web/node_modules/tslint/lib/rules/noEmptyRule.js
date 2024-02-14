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
        return this.applyWithWalker(new BlockWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-empty",
    description: "Disallows empty blocks.",
    descriptionDetails: "Blocks with a comment inside are not considered empty.",
    rationale: "Empty blocks are often indicators of missing code.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "block is empty";
exports.Rule = Rule;
var BlockWalker = (function (_super) {
    __extends(BlockWalker, _super);
    function BlockWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    BlockWalker.prototype.visitBlock = function (node) {
        if (node.statements.length === 0 && !isExcludedConstructor(node.parent)) {
            var sourceFile = this.getSourceFile();
            var start = node.getStart(sourceFile);
            // Block always starts with open brace. Adding 1 to its start gives us the end of the brace,
            // which can be used to conveniently check for comments between braces
            if (!Lint.hasCommentAfterPosition(sourceFile.text, start + 1)) {
                this.addFailureFromStartToEnd(start, node.getEnd(), Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitBlock.call(this, node);
    };
    return BlockWalker;
}(Lint.RuleWalker));
function isExcludedConstructor(node) {
    if (node.kind === ts.SyntaxKind.Constructor) {
        if (Lint.hasModifier(node.modifiers, ts.SyntaxKind.PrivateKeyword, ts.SyntaxKind.ProtectedKeyword)) {
            /* If constructor is private or protected, the block is allowed to be empty.
               The constructor is there on purpose to disallow instantiation from outside the class */
            /* The public modifier does not serve a purpose here. It can only be used to allow instantiation of a base class where
               the super constructor is protected. But then the block would not be empty, because of the call to super() */
            return true;
        }
        for (var _i = 0, _a = node.parameters; _i < _a.length; _i++) {
            var parameter = _a[_i];
            if (Lint.hasModifier(parameter.modifiers, ts.SyntaxKind.PrivateKeyword, ts.SyntaxKind.ProtectedKeyword, ts.SyntaxKind.PublicKeyword, ts.SyntaxKind.ReadonlyKeyword)) {
                return true;
            }
        }
    }
    return false;
}
