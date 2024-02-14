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
        return this.applyWithWalker(new MemberAccessWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "member-access",
    description: "Requires explicit visibility declarations for class members.",
    rationale: "Explicit visibility declarations can make code more readable and accessible for those new to TS.",
    optionsDescription: (_a = ["\n            Two arguments may be optionally provided:\n\n            * `\"check-accessor\"` enforces explicit visibility on get/set accessors (can only be public)\n            * `\"check-constructor\"`  enforces explicit visibility on constructors (can only be public)"], _a.raw = ["\n            Two arguments may be optionally provided:\n\n            * \\`\"check-accessor\"\\` enforces explicit visibility on get/set accessors (can only be public)\n            * \\`\"check-constructor\"\\`  enforces explicit visibility on constructors (can only be public)"], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: ["check-accessor", "check-constructor"],
        },
        minLength: 0,
        maxLength: 2,
    },
    optionExamples: ["true", '[true, "check-accessor"]'],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (memberType, memberName, publicOnly) {
    memberName = memberName === undefined ? "" : " '" + memberName + "'";
    if (publicOnly) {
        return "The " + memberType + memberName + " must be marked as 'public'";
    }
    return "The " + memberType + memberName + " must be marked either 'private', 'public', or 'protected'";
};
exports.Rule = Rule;
var MemberAccessWalker = (function (_super) {
    __extends(MemberAccessWalker, _super);
    function MemberAccessWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    MemberAccessWalker.prototype.visitConstructorDeclaration = function (node) {
        if (this.hasOption("check-constructor")) {
            // constructor is only allowed to have public or nothing, but the compiler will catch this
            this.validateVisibilityModifiers(node);
        }
        _super.prototype.visitConstructorDeclaration.call(this, node);
    };
    MemberAccessWalker.prototype.visitMethodDeclaration = function (node) {
        this.validateVisibilityModifiers(node);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    MemberAccessWalker.prototype.visitPropertyDeclaration = function (node) {
        this.validateVisibilityModifiers(node);
        _super.prototype.visitPropertyDeclaration.call(this, node);
    };
    MemberAccessWalker.prototype.visitGetAccessor = function (node) {
        if (this.hasOption("check-accessor")) {
            this.validateVisibilityModifiers(node);
        }
        _super.prototype.visitGetAccessor.call(this, node);
    };
    MemberAccessWalker.prototype.visitSetAccessor = function (node) {
        if (this.hasOption("check-accessor")) {
            this.validateVisibilityModifiers(node);
        }
        _super.prototype.visitSetAccessor.call(this, node);
    };
    MemberAccessWalker.prototype.validateVisibilityModifiers = function (node) {
        if (node.parent.kind === ts.SyntaxKind.ObjectLiteralExpression) {
            return;
        }
        var hasAnyVisibilityModifiers = Lint.hasModifier(node.modifiers, ts.SyntaxKind.PublicKeyword, ts.SyntaxKind.PrivateKeyword, ts.SyntaxKind.ProtectedKeyword);
        if (!hasAnyVisibilityModifiers) {
            var memberType = void 0;
            var publicOnly = false;
            var end = void 0;
            if (node.kind === ts.SyntaxKind.MethodDeclaration) {
                memberType = "class method";
                end = node.name.getEnd();
            }
            else if (node.kind === ts.SyntaxKind.PropertyDeclaration) {
                memberType = "class property";
                end = node.name.getEnd();
            }
            else if (node.kind === ts.SyntaxKind.Constructor) {
                memberType = "class constructor";
                publicOnly = true;
                end = Lint.childOfKind(node, ts.SyntaxKind.ConstructorKeyword).getEnd();
            }
            else if (node.kind === ts.SyntaxKind.GetAccessor) {
                memberType = "get property accessor";
                end = node.name.getEnd();
            }
            else if (node.kind === ts.SyntaxKind.SetAccessor) {
                memberType = "set property accessor";
                end = node.name.getEnd();
            }
            else {
                throw new Error("unhandled node type");
            }
            var memberName = void 0;
            // look for the identifier and get its text
            if (node.name !== undefined && node.name.kind === ts.SyntaxKind.Identifier) {
                memberName = node.name.text;
            }
            var failureString = Rule.FAILURE_STRING_FACTORY(memberType, memberName, publicOnly);
            this.addFailureFromStartToEnd(node.getStart(), end, failureString);
        }
    };
    return MemberAccessWalker;
}(Lint.RuleWalker));
exports.MemberAccessWalker = MemberAccessWalker;
var _a;
