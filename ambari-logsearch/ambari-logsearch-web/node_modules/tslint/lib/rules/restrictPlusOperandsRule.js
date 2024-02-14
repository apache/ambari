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
var utils_1 = require("../language/utils");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.applyWithProgram = function (sourceFile, langSvc) {
        return this.applyWithWalker(new RestrictPlusOperandsWalker(sourceFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "restrict-plus-operands",
    description: "When adding two variables, operands must both be of type number or of type string.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.INVALID_TYPES_ERROR = "Operands of '+' operation must either be both strings or both numbers";
exports.Rule = Rule;
var RestrictPlusOperandsWalker = (function (_super) {
    __extends(RestrictPlusOperandsWalker, _super);
    function RestrictPlusOperandsWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    RestrictPlusOperandsWalker.prototype.visitBinaryExpression = function (node) {
        if (node.operatorToken.kind === ts.SyntaxKind.PlusToken) {
            var tc = this.getTypeChecker();
            var leftType = getBaseTypeOfLiteralType(tc.getTypeAtLocation(node.left));
            var rightType = getBaseTypeOfLiteralType(tc.getTypeAtLocation(node.right));
            var width = node.getWidth();
            var position = node.getStart();
            if (leftType === "invalid" || rightType === "invalid" || leftType !== rightType) {
                this.addFailureAt(position, width, Rule.INVALID_TYPES_ERROR);
            }
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    return RestrictPlusOperandsWalker;
}(Lint.ProgramAwareRuleWalker));
function getBaseTypeOfLiteralType(type) {
    if (utils_1.isTypeFlagSet(type, ts.TypeFlags.StringLiteral) || utils_1.isTypeFlagSet(type, ts.TypeFlags.String)) {
        return "string";
    }
    else if (utils_1.isTypeFlagSet(type, ts.TypeFlags.NumberLiteral) || utils_1.isTypeFlagSet(type, ts.TypeFlags.Number)) {
        return "number";
    }
    else if (isUnionType(type) && !utils_1.isTypeFlagSet(type, ts.TypeFlags.Enum)) {
        var types = type.types.map(getBaseTypeOfLiteralType);
        return allSame(types) ? types[0] : "invalid";
    }
    else if (utils_1.isTypeFlagSet(type, ts.TypeFlags.EnumLiteral)) {
        return getBaseTypeOfLiteralType(type.baseType);
    }
    return "invalid";
}
function allSame(array) {
    return array.every(function (value) { return value === array[0]; });
}
function isUnionType(type) {
    return Lint.isTypeFlagSet(type, ts.TypeFlags.Union);
}
