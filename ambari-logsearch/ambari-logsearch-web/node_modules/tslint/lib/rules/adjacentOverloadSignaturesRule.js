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
var utils = require("tsutils");
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new AdjacentOverloadSignaturesWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "adjacent-overload-signatures",
    description: "Enforces function overloads to be consecutive.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    rationale: "Improves readability and organization by grouping naturally related items together.",
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (name) {
    return "All '" + name + "' signatures should be adjacent";
};
exports.Rule = Rule;
var AdjacentOverloadSignaturesWalker = (function (_super) {
    __extends(AdjacentOverloadSignaturesWalker, _super);
    function AdjacentOverloadSignaturesWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    AdjacentOverloadSignaturesWalker.prototype.visitSourceFile = function (node) {
        this.visitStatements(node.statements);
        _super.prototype.visitSourceFile.call(this, node);
    };
    AdjacentOverloadSignaturesWalker.prototype.visitModuleDeclaration = function (node) {
        var body = node.body;
        if (body && body.kind === ts.SyntaxKind.ModuleBlock) {
            this.visitStatements(body.statements);
        }
        _super.prototype.visitModuleDeclaration.call(this, node);
    };
    AdjacentOverloadSignaturesWalker.prototype.visitInterfaceDeclaration = function (node) {
        this.checkOverloadsAdjacent(node.members, getOverloadIfSignature);
        _super.prototype.visitInterfaceDeclaration.call(this, node);
    };
    AdjacentOverloadSignaturesWalker.prototype.visitClassDeclaration = function (node) {
        this.visitMembers(node.members);
        _super.prototype.visitClassDeclaration.call(this, node);
    };
    AdjacentOverloadSignaturesWalker.prototype.visitTypeLiteral = function (node) {
        this.visitMembers(node.members);
        _super.prototype.visitTypeLiteral.call(this, node);
    };
    AdjacentOverloadSignaturesWalker.prototype.visitStatements = function (statements) {
        this.checkOverloadsAdjacent(statements, function (statement) {
            if (statement.kind === ts.SyntaxKind.FunctionDeclaration) {
                var name = statement.name;
                return name && { name: name.text, key: name.text };
            }
            else {
                return undefined;
            }
        });
    };
    AdjacentOverloadSignaturesWalker.prototype.visitMembers = function (members) {
        this.checkOverloadsAdjacent(members, getOverloadIfSignature);
    };
    /** 'getOverloadName' may return undefined for nodes that cannot be overloads, e.g. a `const` declaration. */
    AdjacentOverloadSignaturesWalker.prototype.checkOverloadsAdjacent = function (overloads, getOverload) {
        var lastKey;
        var seen = new Set();
        for (var _i = 0, overloads_1 = overloads; _i < overloads_1.length; _i++) {
            var node = overloads_1[_i];
            var overload = getOverload(node);
            if (overload) {
                var name = overload.name, key = overload.key;
                if (seen.has(key) && lastKey !== key) {
                    this.addFailureAtNode(node, Rule.FAILURE_STRING_FACTORY(name));
                }
                seen.add(key);
                lastKey = key;
            }
            else {
                lastKey = undefined;
            }
        }
    };
    return AdjacentOverloadSignaturesWalker;
}(Lint.RuleWalker));
function getOverloadKey(node) {
    var o = getOverload(node);
    return o && o.key;
}
exports.getOverloadKey = getOverloadKey;
function getOverloadIfSignature(node) {
    return utils.isSignatureDeclaration(node) ? getOverload(node) : undefined;
}
function getOverload(node) {
    switch (node.kind) {
        case ts.SyntaxKind.ConstructSignature:
        case ts.SyntaxKind.Constructor:
            return { name: "constructor", key: "constructor" };
        case ts.SyntaxKind.CallSignature:
            return { name: "()", key: "()" };
        default:
    }
    if (node.name === undefined) {
        return undefined;
    }
    var propertyInfo = getPropertyInfo(node.name);
    if (!propertyInfo) {
        return undefined;
    }
    var name = propertyInfo.name, computed = propertyInfo.computed;
    var isStatic = Lint.hasModifier(node.modifiers, ts.SyntaxKind.StaticKeyword);
    var key = (computed ? "0" : "1") + (isStatic ? "0" : "1") + name;
    return { name: name, key: key };
}
function getPropertyInfo(name) {
    switch (name.kind) {
        case ts.SyntaxKind.Identifier:
            return { name: name.text };
        case ts.SyntaxKind.ComputedPropertyName:
            var expression = name.expression;
            return utils.isLiteralExpression(expression) ? { name: expression.text } : { name: expression.getText(), computed: true };
        default:
            return utils.isLiteralExpression(name) ? { name: name.text } : undefined;
    }
}
