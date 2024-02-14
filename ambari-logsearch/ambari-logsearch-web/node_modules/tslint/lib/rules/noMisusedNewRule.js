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
    ruleName: "no-misused-new",
    description: "Warns on apparent attempts to define constructors for interfaces or `new` for classes.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_INTERFACE = "Interfaces cannot be constructed, only classes. Did you mean `declare class`?";
Rule.FAILURE_STRING_CLASS = '`new` in a class is a method named "new". Did you mean `constructor`?';
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitMethodSignature = function (node) {
        if (nameIs(node.name, "constructor")) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING_INTERFACE);
        }
    };
    Walker.prototype.visitMethodDeclaration = function (node) {
        if (node.body === undefined && nameIs(node.name, "new") &&
            returnTypeMatchesParent(node.parent, node)) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING_CLASS);
        }
    };
    Walker.prototype.visitConstructSignature = function (node) {
        if (returnTypeMatchesParent(node.parent, node)) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING_INTERFACE);
        }
    };
    return Walker;
}(Lint.RuleWalker));
function nameIs(name, text) {
    return name.kind === ts.SyntaxKind.Identifier && name.text === text;
}
function returnTypeMatchesParent(parent, decl) {
    if (parent.name === undefined) {
        return false;
    }
    var name = parent.name.text;
    var type = decl.type;
    if (type === undefined || type.kind !== ts.SyntaxKind.TypeReference) {
        return false;
    }
    var typeName = type.typeName;
    return typeName.kind === ts.SyntaxKind.Identifier && typeName.text === name;
}
