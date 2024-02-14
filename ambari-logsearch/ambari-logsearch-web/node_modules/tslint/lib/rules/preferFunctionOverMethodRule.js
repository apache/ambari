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
var OPTION_ALLOW_PUBLIC = "allow-public";
var OPTION_ALLOW_PROTECTED = "allow-protected";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new PreferFunctionOverMethodWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "prefer-function-over-method",
    description: "Warns for class methods that do not use 'this'.",
    optionsDescription: (_a = ["\n            \"", "\" excludes checking of public methods.\n            \"", "\" excludes checking of protected methods."], _a.raw = ["\n            \"", "\" excludes checking of public methods.\n            \"", "\" excludes checking of protected methods."], Lint.Utils.dedent(_a, OPTION_ALLOW_PUBLIC, OPTION_ALLOW_PROTECTED)),
    options: {
        type: "string",
        enum: [OPTION_ALLOW_PUBLIC, OPTION_ALLOW_PROTECTED],
    },
    optionExamples: [
        "true",
        "[true, \"" + OPTION_ALLOW_PUBLIC + "\", \"" + OPTION_ALLOW_PROTECTED + "\"]",
    ],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Class method does not use 'this'. Use a function instead.";
exports.Rule = Rule;
var PreferFunctionOverMethodWalker = (function (_super) {
    __extends(PreferFunctionOverMethodWalker, _super);
    function PreferFunctionOverMethodWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.allowPublic = _this.hasOption(OPTION_ALLOW_PUBLIC);
        _this.allowProtected = _this.hasOption(OPTION_ALLOW_PROTECTED);
        _this.stack = [];
        return _this;
    }
    PreferFunctionOverMethodWalker.prototype.visitNode = function (node) {
        var _this = this;
        switch (node.kind) {
            case ts.SyntaxKind.ThisKeyword:
            case ts.SyntaxKind.SuperKeyword:
                this.setThisUsed(node);
                break;
            case ts.SyntaxKind.MethodDeclaration:
                var name = node.name;
                var usesThis = this.withThisScope(name.kind === ts.SyntaxKind.Identifier ? name.text : undefined, function () { return _super.prototype.visitNode.call(_this, node); });
                if (!usesThis
                    && node.parent.kind !== ts.SyntaxKind.ObjectLiteralExpression
                    && this.shouldWarnForModifiers(node)) {
                    this.addFailureAtNode(node.name, Rule.FAILURE_STRING);
                }
                break;
            case ts.SyntaxKind.FunctionDeclaration:
            case ts.SyntaxKind.FunctionExpression:
                this.withThisScope(undefined, function () { return _super.prototype.visitNode.call(_this, node); });
                break;
            default:
                _super.prototype.visitNode.call(this, node);
        }
    };
    PreferFunctionOverMethodWalker.prototype.setThisUsed = function (node) {
        var cur = this.stack[this.stack.length - 1];
        if (cur && !isRecursiveCall(node, cur)) {
            cur.isThisUsed = true;
        }
    };
    PreferFunctionOverMethodWalker.prototype.withThisScope = function (name, recur) {
        this.stack.push({ name: name, isThisUsed: false });
        recur();
        return this.stack.pop().isThisUsed;
    };
    PreferFunctionOverMethodWalker.prototype.shouldWarnForModifiers = function (node) {
        if (Lint.hasModifier(node.modifiers, ts.SyntaxKind.StaticKeyword)) {
            return false;
        }
        // TODO: Also return false if it's marked "override" (https://github.com/palantir/tslint/pull/2037)
        switch (methodVisibility(node)) {
            case 0 /* Public */:
                return !this.allowPublic;
            case 1 /* Protected */:
                return !this.allowProtected;
            default:
                return true;
        }
    };
    return PreferFunctionOverMethodWalker;
}(Lint.RuleWalker));
function isRecursiveCall(thisOrSuper, cur) {
    var parent = thisOrSuper.parent;
    return thisOrSuper.kind === ts.SyntaxKind.ThisKeyword
        && parent.kind === ts.SyntaxKind.PropertyAccessExpression
        && parent.name.text === cur.name;
}
function methodVisibility(node) {
    if (Lint.hasModifier(node.modifiers, ts.SyntaxKind.PrivateKeyword)) {
        return 2 /* Private */;
    }
    else if (Lint.hasModifier(node.modifiers, ts.SyntaxKind.ProtectedKeyword)) {
        return 1 /* Protected */;
    }
    else {
        return 0 /* Public */;
    }
}
var _a;
