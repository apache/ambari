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
var OPTION_ARRAY = "array";
var OPTION_GENERIC = "generic";
var OPTION_ARRAY_SIMPLE = "array-simple";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var alignWalker = new ArrayTypeWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(alignWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "array-type",
    description: "Requires using either 'T[]' or 'Array<T>' for arrays.",
    hasFix: true,
    optionsDescription: (_a = ["\n            One of the following arguments must be provided:\n\n            * `\"", "\"` enforces use of `T[]` for all types T.\n            * `\"", "\"` enforces use of `Array<T>` for all types T.\n            * `\"", "\"` enforces use of `T[]` if `T` is a simple type (primitive or type reference)."], _a.raw = ["\n            One of the following arguments must be provided:\n\n            * \\`\"", "\"\\` enforces use of \\`T[]\\` for all types T.\n            * \\`\"", "\"\\` enforces use of \\`Array<T>\\` for all types T.\n            * \\`\"", "\"\\` enforces use of \\`T[]\\` if \\`T\\` is a simple type (primitive or type reference)."], Lint.Utils.dedent(_a, OPTION_ARRAY, OPTION_GENERIC, OPTION_ARRAY_SIMPLE)),
    options: {
        type: "string",
        enum: [OPTION_ARRAY, OPTION_GENERIC, OPTION_ARRAY_SIMPLE],
    },
    optionExamples: ["[true, \"" + OPTION_ARRAY + "\"]", "[true, \"" + OPTION_GENERIC + "\"]", "[true, \"" + OPTION_ARRAY_SIMPLE + "\"]"],
    type: "style",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_ARRAY = "Array type using 'Array<T>' is forbidden. Use 'T[]' instead.";
Rule.FAILURE_STRING_GENERIC = "Array type using 'T[]' is forbidden. Use 'Array<T>' instead.";
Rule.FAILURE_STRING_ARRAY_SIMPLE = "Array type using 'Array<T>' is forbidden for simple types. Use 'T[]' instead.";
Rule.FAILURE_STRING_GENERIC_SIMPLE = "Array type using 'T[]' is forbidden for non-simple types. Use 'Array<T>' instead.";
exports.Rule = Rule;
var ArrayTypeWalker = (function (_super) {
    __extends(ArrayTypeWalker, _super);
    function ArrayTypeWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ArrayTypeWalker.prototype.visitArrayType = function (node) {
        var typeName = node.elementType;
        if (this.hasOption(OPTION_GENERIC) || this.hasOption(OPTION_ARRAY_SIMPLE) && !this.isSimpleType(typeName)) {
            var failureString = this.hasOption(OPTION_GENERIC) ? Rule.FAILURE_STRING_GENERIC : Rule.FAILURE_STRING_GENERIC_SIMPLE;
            var parens = typeName.kind === ts.SyntaxKind.ParenthesizedType ? 1 : 0;
            // Add a space if the type is preceded by 'as' and the node has no leading whitespace
            var space = !parens && node.parent.kind === ts.SyntaxKind.AsExpression &&
                node.getStart() === node.getFullStart() ? " " : "";
            var fix = this.createFix(this.createReplacement(typeName.getStart(), parens, space + "Array<"), 
            // Delete the square brackets and replace with an angle bracket
            this.createReplacement(typeName.getEnd() - parens, node.getEnd() - typeName.getEnd() + parens, ">"));
            this.addFailureAtNode(node, failureString, fix);
        }
        _super.prototype.visitArrayType.call(this, node);
    };
    ArrayTypeWalker.prototype.visitTypeReference = function (node) {
        var typeName = node.typeName.getText();
        if (typeName === "Array" && (this.hasOption(OPTION_ARRAY) || this.hasOption(OPTION_ARRAY_SIMPLE))) {
            var failureString = this.hasOption(OPTION_ARRAY) ? Rule.FAILURE_STRING_ARRAY : Rule.FAILURE_STRING_ARRAY_SIMPLE;
            var typeArgs = node.typeArguments;
            if (!typeArgs || typeArgs.length === 0) {
                // Create an 'any' array
                var fix = this.createFix(this.createReplacement(node.getStart(), node.getWidth(), "any[]"));
                this.addFailureAtNode(node, failureString, fix);
            }
            else if (typeArgs && typeArgs.length === 1 && (!this.hasOption(OPTION_ARRAY_SIMPLE) || this.isSimpleType(typeArgs[0]))) {
                var type = typeArgs[0];
                var typeStart = type.getStart();
                var typeEnd = type.getEnd();
                var parens = type.kind === ts.SyntaxKind.UnionType ||
                    type.kind === ts.SyntaxKind.FunctionType || type.kind === ts.SyntaxKind.IntersectionType;
                var fix = this.createFix(
                // Delete Array and the first angle bracket
                this.createReplacement(node.getStart(), typeStart - node.getStart(), parens ? "(" : ""), 
                // Delete the last angle bracket and replace with square brackets
                this.createReplacement(typeEnd, node.getEnd() - typeEnd, (parens ? ")" : "") + "[]"));
                this.addFailureAtNode(node, failureString, fix);
            }
        }
        _super.prototype.visitTypeReference.call(this, node);
    };
    ArrayTypeWalker.prototype.isSimpleType = function (nodeType) {
        switch (nodeType.kind) {
            case ts.SyntaxKind.AnyKeyword:
            case ts.SyntaxKind.ArrayType:
            case ts.SyntaxKind.BooleanKeyword:
            case ts.SyntaxKind.NullKeyword:
            case ts.SyntaxKind.NumberKeyword:
            case ts.SyntaxKind.StringKeyword:
            case ts.SyntaxKind.SymbolKeyword:
            case ts.SyntaxKind.VoidKeyword:
                return true;
            case ts.SyntaxKind.TypeReference:
                // TypeReferences must be non-generic or be another Array with a simple type
                var node = nodeType;
                var typeArgs = node.typeArguments;
                if (!typeArgs || typeArgs.length === 0 || node.typeName.getText() === "Array" && this.isSimpleType(typeArgs[0])) {
                    return true;
                }
                else {
                    return false;
                }
            default:
                return false;
        }
    };
    return ArrayTypeWalker;
}(Lint.RuleWalker));
var _a;
