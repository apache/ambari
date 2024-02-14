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
var OPTION_ALLOW_NULL_UNION = "allow-null-union";
var OPTION_ALLOW_UNDEFINED_UNION = "allow-undefined-union";
var OPTION_ALLOW_STRING = "allow-string";
var OPTION_ALLOW_NUMBER = "allow-number";
var OPTION_ALLOW_MIX = "allow-mix";
// tslint:disable:switch-default
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.FAILURE_STRING = function (locationDescription, ty, isUnionType, expectedTypes) {
        var expected = expectedTypes.length === 1
            ? "Only " + expectedTypes[0] + "s are allowed"
            : "Allowed types are " + stringOr(expectedTypes);
        return "This type is not allowed in the " + locationDescription + " because it " + this.tyFailure(ty, isUnionType) + ". " + expected + ".";
    };
    Rule.tyFailure = function (ty, isUnionType) {
        var is = isUnionType ? "could be" : "is";
        switch (ty) {
            case 0 /* AlwaysTruthy */: return "is always truthy";
            case 1 /* AlwaysFalsy */: return "is always falsy";
            case 2 /* String */: return is + " a string";
            case 3 /* Number */: return is + " a number";
            case 4 /* Null */: return is + " null";
            case 5 /* Undefined */: return is + " undefined";
            case 6 /* Enum */: return is + " an enum";
            case 7 /* Mixes */: return "unions more than one truthy/falsy type";
        }
    };
    Rule.prototype.applyWithProgram = function (srcFile, langSvc) {
        return this.applyWithWalker(new Walker(srcFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "strict-boolean-expressions",
    description: (_a = ["\n            Restricts the types allowed in boolean expressions. By default only booleans are allowed.\n\n            The following nodes are checked:\n\n            * Arguments to the `!`, `&&`, and `||` operators\n            * The condition in a conditional expression (`cond ? x : y`)\n            * Conditions for `if`, `for`, `while`, and `do-while` statements."], _a.raw = ["\n            Restricts the types allowed in boolean expressions. By default only booleans are allowed.\n\n            The following nodes are checked:\n\n            * Arguments to the \\`!\\`, \\`&&\\`, and \\`||\\` operators\n            * The condition in a conditional expression (\\`cond ? x : y\\`)\n            * Conditions for \\`if\\`, \\`for\\`, \\`while\\`, and \\`do-while\\` statements."], Lint.Utils.dedent(_a)),
    optionsDescription: (_b = ["\n            These options may be provided:\n\n            * `", "` allows union types containing `null`.\n              - It does *not* allow `null` itself.\n            * `", "` allows union types containing `undefined`.\n              - It does *not* allow `undefined` itself.\n            * `", "` allows strings.\n              - It does *not* allow unions containing `string`.\n              - It does *not* allow string literal types.\n            * `", "` allows numbers.\n              - It does *not* allow unions containing `number`.\n              - It does *not* allow enums or number literal types.\n            * `", "` allow multiple of the above to appear together.\n              - For example, `string | number` or `RegExp | null | undefined` would normally not be allowed.\n              - A type like `\"foo\" | \"bar\" | undefined` is always allowed, because it has only one way to be false."], _b.raw = ["\n            These options may be provided:\n\n            * \\`", "\\` allows union types containing \\`null\\`.\n              - It does *not* allow \\`null\\` itself.\n            * \\`", "\\` allows union types containing \\`undefined\\`.\n              - It does *not* allow \\`undefined\\` itself.\n            * \\`", "\\` allows strings.\n              - It does *not* allow unions containing \\`string\\`.\n              - It does *not* allow string literal types.\n            * \\`", "\\` allows numbers.\n              - It does *not* allow unions containing \\`number\\`.\n              - It does *not* allow enums or number literal types.\n            * \\`", "\\` allow multiple of the above to appear together.\n              - For example, \\`string | number\\` or \\`RegExp | null | undefined\\` would normally not be allowed.\n              - A type like \\`\"foo\" | \"bar\" | undefined\\` is always allowed, because it has only one way to be false."], Lint.Utils.dedent(_b, OPTION_ALLOW_NULL_UNION, OPTION_ALLOW_UNDEFINED_UNION, OPTION_ALLOW_STRING, OPTION_ALLOW_NUMBER, OPTION_ALLOW_MIX)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [OPTION_ALLOW_NULL_UNION, OPTION_ALLOW_UNDEFINED_UNION, OPTION_ALLOW_STRING, OPTION_ALLOW_NUMBER],
        },
        minLength: 0,
        maxLength: 5,
    },
    optionExamples: [
        "true",
        "[true, " + OPTION_ALLOW_NULL_UNION + ", " + OPTION_ALLOW_UNDEFINED_UNION + ", " + OPTION_ALLOW_STRING + ", " + OPTION_ALLOW_NUMBER + "]",
    ],
    type: "functionality",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.allowNullUnion = _this.hasOption(OPTION_ALLOW_NULL_UNION);
        _this.allowUndefinedUnion = _this.hasOption(OPTION_ALLOW_UNDEFINED_UNION);
        _this.allowString = _this.hasOption(OPTION_ALLOW_STRING);
        _this.allowNumber = _this.hasOption(OPTION_ALLOW_NUMBER);
        _this.allowMix = _this.hasOption(OPTION_ALLOW_MIX);
        return _this;
    }
    Walker.prototype.visitBinaryExpression = function (node) {
        var _this = this;
        var op = binaryBooleanExpressionKind(node);
        if (op !== undefined) {
            var checkHalf = function (expr) {
                // If it's another boolean binary expression, we'll check it when recursing.
                if (!isBooleanBinaryExpression(expr)) {
                    _this.checkExpression(expr, "operand for the '" + op + "' operator");
                }
            };
            checkHalf(node.left);
            checkHalf(node.right);
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    Walker.prototype.visitPrefixUnaryExpression = function (node) {
        if (node.operator === ts.SyntaxKind.ExclamationToken) {
            this.checkExpression(node.operand, "operand for the '!' operator");
        }
        _super.prototype.visitPrefixUnaryExpression.call(this, node);
    };
    Walker.prototype.visitIfStatement = function (node) {
        this.checkStatement(node, "'if' condition");
        _super.prototype.visitIfStatement.call(this, node);
    };
    Walker.prototype.visitWhileStatement = function (node) {
        this.checkStatement(node, "'while' condition");
        _super.prototype.visitWhileStatement.call(this, node);
    };
    Walker.prototype.visitDoStatement = function (node) {
        this.checkStatement(node, "'do-while' condition");
        _super.prototype.visitDoStatement.call(this, node);
    };
    Walker.prototype.visitConditionalExpression = function (node) {
        this.checkExpression(node.condition, "condition");
        _super.prototype.visitConditionalExpression.call(this, node);
    };
    Walker.prototype.visitForStatement = function (node) {
        if (node.condition !== undefined) {
            this.checkExpression(node.condition, "'for' condition");
        }
        _super.prototype.visitForStatement.call(this, node);
    };
    Walker.prototype.checkStatement = function (node, locationDescription) {
        // If it's a boolean binary expression, we'll check it when recursing.
        if (!isBooleanBinaryExpression(node.expression)) {
            this.checkExpression(node.expression, locationDescription);
        }
    };
    Walker.prototype.checkExpression = function (node, locationDescription) {
        var type = this.getTypeChecker().getTypeAtLocation(node);
        var failure = this.getTypeFailure(type);
        if (failure !== undefined) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING(locationDescription, failure, isUnionType(type), this.expectedTypes()));
        }
    };
    Walker.prototype.getTypeFailure = function (type) {
        if (isUnionType(type)) {
            return this.handleUnion(type);
        }
        var kind = getKind(type);
        var failure = this.failureForKind(kind, /*isInUnion*/ false);
        if (failure !== undefined) {
            return failure;
        }
        switch (triState(kind)) {
            case true:
                return 0 /* AlwaysTruthy */;
            case false:
                return 1 /* AlwaysFalsy */;
            case undefined:
                return undefined;
        }
    };
    /** Fails if a kind of falsiness is not allowed. */
    Walker.prototype.failureForKind = function (kind, isInUnion) {
        switch (kind) {
            case 0 /* String */:
            case 1 /* FalseStringLiteral */:
                return this.allowString ? undefined : 2 /* String */;
            case 2 /* Number */:
            case 3 /* FalseNumberLiteral */:
                return this.allowNumber ? undefined : 3 /* Number */;
            case 8 /* Enum */:
                return 6 /* Enum */;
            case 6 /* Null */:
                return isInUnion && !this.allowNullUnion ? 4 /* Null */ : undefined;
            case 7 /* Undefined */:
                return isInUnion && !this.allowUndefinedUnion ? 5 /* Undefined */ : undefined;
            default:
                return undefined;
        }
    };
    Walker.prototype.handleUnion = function (type) {
        // Tracks whether it's possibly truthy.
        var anyTruthy = false;
        // Counts falsy kinds to see if there's a mix. Also tracks whether it's possibly falsy.
        var seenFalsy = 0;
        for (var _i = 0, _a = type.types; _i < _a.length; _i++) {
            var ty = _a[_i];
            var kind = getKind(ty);
            var failure = this.failureForKind(kind, /*isInUnion*/ true);
            if (failure !== undefined) {
                return failure;
            }
            switch (triState(kind)) {
                case true:
                    anyTruthy = true;
                    break;
                case false:
                    seenFalsy++;
                    break;
                default:
                    anyTruthy = true;
                    seenFalsy++;
            }
        }
        return seenFalsy === 0 ? 0 /* AlwaysTruthy */
            : !anyTruthy ? 1 /* AlwaysFalsy */
                : !this.allowMix && seenFalsy > 1 ? 7 /* Mixes */ : undefined;
    };
    Walker.prototype.expectedTypes = function () {
        var parts = ["boolean"];
        if (this.allowNullUnion) {
            parts.push("null-union");
        }
        if (this.allowUndefinedUnion) {
            parts.push("undefined-union");
        }
        if (this.allowString) {
            parts.push("string");
        }
        if (this.allowNumber) {
            parts.push("number");
        }
        return parts;
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
/** Divides a type into always true, always false, or unknown. */
function triState(kind) {
    switch (kind) {
        case 0 /* String */:
        case 2 /* Number */:
        case 4 /* Boolean */:
        case 8 /* Enum */:
            return undefined;
        case 6 /* Null */:
        case 7 /* Undefined */:
        case 3 /* FalseNumberLiteral */:
        case 1 /* FalseStringLiteral */:
        case 5 /* FalseBooleanLiteral */:
            return false;
        case 9 /* AlwaysTruthy */:
            return true;
    }
}
function getKind(type) {
    return is(ts.TypeFlags.String) ? 0 /* String */
        : is(ts.TypeFlags.Number) ? 2 /* Number */
            : is(ts.TypeFlags.Boolean) ? 4 /* Boolean */
                : is(ts.TypeFlags.Null) ? 6 /* Null */
                    : is(ts.TypeFlags.Undefined | ts.TypeFlags.Void) ? 7 /* Undefined */ // tslint:disable-line:no-bitwise
                        : is(ts.TypeFlags.EnumLike) ? 8 /* Enum */
                            : is(ts.TypeFlags.NumberLiteral) ?
                                (type.text === "0" ? 3 /* FalseNumberLiteral */ : 9 /* AlwaysTruthy */)
                                : is(ts.TypeFlags.StringLiteral) ?
                                    (type.text === "" ? 1 /* FalseStringLiteral */ : 9 /* AlwaysTruthy */)
                                    : is(ts.TypeFlags.BooleanLiteral) ?
                                        (type.intrinsicName === "true" ? 9 /* AlwaysTruthy */ : 5 /* FalseBooleanLiteral */)
                                        : 9 /* AlwaysTruthy */;
    function is(flags) {
        return Lint.isTypeFlagSet(type, flags);
    }
}
/** Matches `&&` and `||` operators. */
function isBooleanBinaryExpression(node) {
    return node.kind === ts.SyntaxKind.BinaryExpression && binaryBooleanExpressionKind(node) !== undefined;
}
function binaryBooleanExpressionKind(node) {
    switch (node.operatorToken.kind) {
        case ts.SyntaxKind.AmpersandAmpersandToken:
            return "&&";
        case ts.SyntaxKind.BarBarToken:
            return "||";
        default:
            return undefined;
    }
}
function stringOr(parts) {
    switch (parts.length) {
        case 1:
            return parts[0];
        case 2:
            return parts[0] + " or " + parts[1];
        default:
            var res = "";
            for (var i = 0; i < parts.length - 1; i++) {
                res += parts[i] + ", ";
            }
            return res + "or " + parts[parts.length - 1];
    }
}
function isUnionType(type) {
    return Lint.isTypeFlagSet(type, ts.TypeFlags.Union) && !Lint.isTypeFlagSet(type, ts.TypeFlags.Enum);
}
var _a, _b;
