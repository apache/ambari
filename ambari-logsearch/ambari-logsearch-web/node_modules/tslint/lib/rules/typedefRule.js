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
        return this.applyWithWalker(new TypedefWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "typedef",
    description: "Requires type definitions to exist.",
    optionsDescription: (_a = ["\n            Seven arguments may be optionally provided:\n\n            * `\"call-signature\"` checks return type of functions.\n            * `\"arrow-call-signature\"` checks return type of arrow functions.\n            * `\"parameter\"` checks type specifier of function parameters for non-arrow functions.\n            * `\"arrow-parameter\"` checks type specifier of function parameters for arrow functions.\n            * `\"property-declaration\"` checks return types of interface properties.\n            * `\"variable-declaration\"` checks non-binding variable declarations.\n            * `\"member-variable-declaration\"` checks member variable declarations.\n            * `\"object-destructuring\"` checks object destructuring declarations.\n            * `\"array-destructuring\"` checks array destructuring declarations."], _a.raw = ["\n            Seven arguments may be optionally provided:\n\n            * \\`\"call-signature\"\\` checks return type of functions.\n            * \\`\"arrow-call-signature\"\\` checks return type of arrow functions.\n            * \\`\"parameter\"\\` checks type specifier of function parameters for non-arrow functions.\n            * \\`\"arrow-parameter\"\\` checks type specifier of function parameters for arrow functions.\n            * \\`\"property-declaration\"\\` checks return types of interface properties.\n            * \\`\"variable-declaration\"\\` checks non-binding variable declarations.\n            * \\`\"member-variable-declaration\"\\` checks member variable declarations.\n            * \\`\"object-destructuring\"\\` checks object destructuring declarations.\n            * \\`\"array-destructuring\"\\` checks array destructuring declarations."], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [
                "call-signature",
                "arrow-call-signature",
                "parameter",
                "arrow-parameter",
                "property-declaration",
                "variable-declaration",
                "member-variable-declaration",
                "object-destructuring",
                "array-destructuring",
            ],
        },
        minLength: 0,
        maxLength: 7,
    },
    optionExamples: ['[true, "call-signature", "parameter", "member-variable-declaration"]'],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "missing type declaration";
exports.Rule = Rule;
var TypedefWalker = (function (_super) {
    __extends(TypedefWalker, _super);
    function TypedefWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    TypedefWalker.prototype.visitFunctionDeclaration = function (node) {
        this.handleCallSignature(node);
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    TypedefWalker.prototype.visitFunctionExpression = function (node) {
        this.handleCallSignature(node);
        _super.prototype.visitFunctionExpression.call(this, node);
    };
    TypedefWalker.prototype.visitArrowFunction = function (node) {
        var location = (node.parameters != null) ? node.parameters.end : null;
        if (location != null
            && node.parent !== undefined
            && node.parent.kind !== ts.SyntaxKind.CallExpression
            && !isTypedPropertyDeclaration(node.parent)) {
            this.checkTypeAnnotation("arrow-call-signature", location, node.type, node.name);
        }
        _super.prototype.visitArrowFunction.call(this, node);
    };
    TypedefWalker.prototype.visitGetAccessor = function (node) {
        this.handleCallSignature(node);
        _super.prototype.visitGetAccessor.call(this, node);
    };
    TypedefWalker.prototype.visitMethodDeclaration = function (node) {
        this.handleCallSignature(node);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    TypedefWalker.prototype.visitMethodSignature = function (node) {
        this.handleCallSignature(node);
        _super.prototype.visitMethodSignature.call(this, node);
    };
    TypedefWalker.prototype.visitObjectLiteralExpression = function (node) {
        for (var _i = 0, _a = node.properties; _i < _a.length; _i++) {
            var property = _a[_i];
            switch (property.kind) {
                case ts.SyntaxKind.PropertyAssignment:
                    this.visitPropertyAssignment(property);
                    break;
                case ts.SyntaxKind.MethodDeclaration:
                    this.visitMethodDeclaration(property);
                    break;
                case ts.SyntaxKind.GetAccessor:
                    this.visitGetAccessor(property);
                    break;
                case ts.SyntaxKind.SetAccessor:
                    this.visitSetAccessor(property);
                    break;
                default:
                    break;
            }
        }
    };
    TypedefWalker.prototype.visitParameterDeclaration = function (node) {
        // a parameter's "type" could be a specific string value, for example `fn(option: "someOption", anotherOption: number)`
        if ((node.type == null || node.type.kind !== ts.SyntaxKind.StringLiteral)
            && node.parent !== undefined
            && node.parent.parent !== undefined) {
            var isArrowFunction = node.parent.kind === ts.SyntaxKind.ArrowFunction;
            var optionName = null;
            if (isArrowFunction && isTypedPropertyDeclaration(node.parent.parent)) {
                // leave optionName as null and don't perform check
            }
            else if (isArrowFunction && utils.isPropertyDeclaration(node.parent.parent)) {
                optionName = "member-variable-declaration";
            }
            else if (isArrowFunction) {
                optionName = "arrow-parameter";
            }
            else {
                optionName = "parameter";
            }
            if (optionName !== null) {
                this.checkTypeAnnotation(optionName, node.getEnd(), node.type, node.name);
            }
        }
        _super.prototype.visitParameterDeclaration.call(this, node);
    };
    TypedefWalker.prototype.visitPropertyAssignment = function (node) {
        switch (node.initializer.kind) {
            case ts.SyntaxKind.ArrowFunction:
            case ts.SyntaxKind.FunctionExpression:
                this.handleCallSignature(node.initializer);
                break;
            default:
                break;
        }
        _super.prototype.visitPropertyAssignment.call(this, node);
    };
    TypedefWalker.prototype.visitPropertyDeclaration = function (node) {
        var optionName = "member-variable-declaration";
        // If this is an arrow function, it doesn't need to have a typedef on the property declaration
        // as the typedefs can be on the function's parameters instead
        var performCheck = !(node.initializer != null && node.initializer.kind === ts.SyntaxKind.ArrowFunction && node.type == null);
        if (performCheck) {
            this.checkTypeAnnotation(optionName, node.name.getEnd(), node.type, node.name);
        }
        _super.prototype.visitPropertyDeclaration.call(this, node);
    };
    TypedefWalker.prototype.visitPropertySignature = function (node) {
        var optionName = "property-declaration";
        this.checkTypeAnnotation(optionName, node.name.getEnd(), node.type, node.name);
        _super.prototype.visitPropertySignature.call(this, node);
    };
    TypedefWalker.prototype.visitSetAccessor = function (node) {
        this.handleCallSignature(node);
        _super.prototype.visitSetAccessor.call(this, node);
    };
    TypedefWalker.prototype.visitVariableDeclaration = function (node) {
        // variable declarations should always have a grandparent, but check that to be on the safe side.
        // catch statements will be the parent of the variable declaration
        // for-in/for-of loops will be the gradparent of the variable declaration
        if (node.parent != null && node.parent.parent != null
            && node.parent.kind !== ts.SyntaxKind.CatchClause
            && node.parent.parent.kind !== ts.SyntaxKind.ForInStatement
            && node.parent.parent.kind !== ts.SyntaxKind.ForOfStatement) {
            var rule = void 0;
            switch (node.name.kind) {
                case ts.SyntaxKind.ObjectBindingPattern:
                    rule = "object-destructuring";
                    break;
                case ts.SyntaxKind.ArrayBindingPattern:
                    rule = "array-destructuring";
                    break;
                default:
                    rule = "variable-declaration";
                    break;
            }
            this.checkTypeAnnotation(rule, node.name.getEnd(), node.type, node.name);
        }
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    TypedefWalker.prototype.handleCallSignature = function (node) {
        var location = (node.parameters != null) ? node.parameters.end : null;
        // set accessors can't have a return type.
        if (location != null && node.kind !== ts.SyntaxKind.SetAccessor && node.kind !== ts.SyntaxKind.ArrowFunction) {
            this.checkTypeAnnotation("call-signature", location, node.type, node.name);
        }
    };
    TypedefWalker.prototype.checkTypeAnnotation = function (option, location, typeAnnotation, name) {
        if (this.hasOption(option) && typeAnnotation == null) {
            this.addFailureAt(location, 1, "expected " + option + getName(name, ": '", "'") + " to have a typedef");
        }
    };
    return TypedefWalker;
}(Lint.RuleWalker));
function getName(name, prefix, suffix) {
    var ns = "";
    if (name != null) {
        switch (name.kind) {
            case ts.SyntaxKind.Identifier:
                ns = name.text;
                break;
            case ts.SyntaxKind.BindingElement:
                ns = getName(name.name);
                break;
            case ts.SyntaxKind.ArrayBindingPattern:
                ns = "[ " + name.elements.map(function (n) { return getName(n); }).join(", ") + " ]";
                break;
            case ts.SyntaxKind.ObjectBindingPattern:
                ns = "{ " + name.elements.map(function (n) { return getName(n); }).join(", ") + " }";
                break;
            default:
                break;
        }
    }
    return ns ? "" + (prefix || "") + ns + (suffix || "") : "";
}
function isTypedPropertyDeclaration(node) {
    return utils.isPropertyDeclaration(node) && node.type != null;
}
var _a;
