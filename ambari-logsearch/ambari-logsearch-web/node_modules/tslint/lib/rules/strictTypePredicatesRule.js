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
// tslint:disable:no-bitwise
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.FAILURE_STRING = function (value) {
        return "Expression is always " + value + ".";
    };
    Rule.FAILURE_STRICT_PREFER_STRICT_EQUALS = function (value, isPositive) {
        return "Use '" + (isPositive ? "===" : "!==") + " " + value + "' instead.";
    };
    Rule.prototype.applyWithProgram = function (srcFile, langSvc) {
        return this.applyWithWalker(new Walker(srcFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "strict-type-predicates",
    description: (_a = ["\n            Warns for type predicates that are always true or always false.\n            Works for 'typeof' comparisons to constants (e.g. 'typeof foo === \"string\"'), and equality comparison to 'null'/'undefined'.\n            (TypeScript won't let you compare '1 === 2', but it has an exception for '1 === undefined'.)\n            Does not yet work for 'instanceof'.\n            Does *not* warn for 'if (x.y)' where 'x.y' is always truthy. For that, see strict-boolean-expressions."], _a.raw = ["\n            Warns for type predicates that are always true or always false.\n            Works for 'typeof' comparisons to constants (e.g. 'typeof foo === \"string\"'), and equality comparison to 'null'/'undefined'.\n            (TypeScript won't let you compare '1 === 2', but it has an exception for '1 === undefined'.)\n            Does not yet work for 'instanceof'.\n            Does *not* warn for 'if (x.y)' where 'x.y' is always truthy. For that, see strict-boolean-expressions."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_BAD_TYPEOF = "Bad comparison for 'typeof'.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitBinaryExpression = function (node) {
        var equals = Lint.getEqualsKind(node.operatorToken);
        if (equals) {
            this.checkEquals(node, equals);
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    Walker.prototype.checkEquals = function (node, _a) {
        var _this = this;
        var isStrict = _a.isStrict, isPositive = _a.isPositive;
        var exprPred = getTypePredicate(node, isStrict);
        if (!exprPred) {
            return;
        }
        var fail = function (failure) { return _this.addFailureAtNode(node, failure); };
        if (exprPred.kind === 2 /* TypeofTypo */) {
            fail(Rule.FAILURE_STRING_BAD_TYPEOF);
            return;
        }
        var checker = this.getTypeChecker();
        var exprType = checker.getTypeAtLocation(exprPred.expression);
        // TODO: could use checker.getBaseConstraintOfType to help with type parameters, but it's not publicly exposed.
        if (Lint.isTypeFlagSet(exprType, ts.TypeFlags.Any | ts.TypeFlags.TypeParameter)) {
            return;
        }
        switch (exprPred.kind) {
            case 0 /* Plain */:
                var predicate = exprPred.predicate, isNullOrUndefined = exprPred.isNullOrUndefined;
                var value = getConstantBoolean(exprType, predicate);
                // 'null'/'undefined' are the only two values *not* assignable to '{}'.
                if (value !== undefined && (isNullOrUndefined || !isEmptyType(checker, exprType))) {
                    fail(Rule.FAILURE_STRING(value === isPositive));
                }
                break;
            case 1 /* NonStructNullUndefined */:
                var result = testNonStrictNullUndefined(exprType);
                switch (typeof result) {
                    case "boolean":
                        fail(Rule.FAILURE_STRING(result === isPositive));
                        break;
                    case "string":
                        fail(Rule.FAILURE_STRICT_PREFER_STRICT_EQUALS(result, isPositive));
                        break;
                    default:
                }
        }
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
/** Detects a type predicate given `left === right`. */
function getTypePredicate(node, isStrictEquals) {
    var left = node.left, right = node.right;
    return getTypePredicateOneWay(left, right, isStrictEquals) || getTypePredicateOneWay(right, left, isStrictEquals);
}
/** Only gets the type predicate if the expression is on the left. */
function getTypePredicateOneWay(left, right, isStrictEquals) {
    switch (right.kind) {
        case ts.SyntaxKind.TypeOfExpression:
            var expression = right.expression;
            var kind = left.kind === ts.SyntaxKind.StringLiteral ? left.text : "";
            var predicate = getTypePredicateForKind(kind);
            return predicate === undefined
                ? { kind: 2 /* TypeofTypo */ }
                : { kind: 0 /* Plain */, expression: expression, predicate: predicate, isNullOrUndefined: kind === "undefined" };
        case ts.SyntaxKind.NullKeyword:
            return nullOrUndefined(ts.TypeFlags.Null);
        case ts.SyntaxKind.Identifier:
            if (right.originalKeywordKind === ts.SyntaxKind.UndefinedKeyword) {
                return nullOrUndefined(undefinedFlags);
            }
        default:
            return undefined;
    }
    function nullOrUndefined(flags) {
        return isStrictEquals
            ? { kind: 0 /* Plain */, expression: left, predicate: flagPredicate(flags), isNullOrUndefined: true }
            : { kind: 1 /* NonStructNullUndefined */, expression: left };
    }
}
function isEmptyType(checker, type) {
    return checker.typeToString(type) === "{}";
}
var undefinedFlags = ts.TypeFlags.Undefined | ts.TypeFlags.Void;
function getTypePredicateForKind(kind) {
    switch (kind) {
        case "undefined":
            return flagPredicate(undefinedFlags);
        case "boolean":
            return flagPredicate(ts.TypeFlags.BooleanLike);
        case "number":
            return flagPredicate(ts.TypeFlags.NumberLike);
        case "string":
            return flagPredicate(ts.TypeFlags.StringLike);
        case "symbol":
            return flagPredicate(ts.TypeFlags.ESSymbol);
        case "function":
            return isFunction;
        case "object":
            // It's an object if it's not any of the above.
            var allFlags_1 = ts.TypeFlags.Undefined | ts.TypeFlags.Void | ts.TypeFlags.BooleanLike |
                ts.TypeFlags.NumberLike | ts.TypeFlags.StringLike | ts.TypeFlags.ESSymbol;
            return function (type) { return !Lint.isTypeFlagSet(type, allFlags_1) && !isFunction(type); };
        default:
            return undefined;
    }
}
function flagPredicate(testedFlag) {
    return function (type) { return Lint.isTypeFlagSet(type, testedFlag); };
}
function isFunction(t) {
    if (t.getCallSignatures().length !== 0) {
        return true;
    }
    var symbol = t.getSymbol();
    return (symbol && symbol.getName()) === "Function";
}
/** Returns a boolean value if that should always be the result of a type predicate. */
function getConstantBoolean(type, predicate) {
    var anyTrue = false;
    var anyFalse = false;
    for (var _i = 0, _a = unionParts(type); _i < _a.length; _i++) {
        var ty = _a[_i];
        if (predicate(ty)) {
            anyTrue = true;
        }
        else {
            anyFalse = true;
        }
        if (anyTrue && anyFalse) {
            return undefined;
        }
    }
    return anyTrue;
}
/** Returns bool for always/never true, or a string to recommend strict equality. */
function testNonStrictNullUndefined(type) {
    var anyNull = false;
    var anyUndefined = false;
    var anyOther = false;
    for (var _i = 0, _a = unionParts(type); _i < _a.length; _i++) {
        var ty = _a[_i];
        if (Lint.isTypeFlagSet(ty, ts.TypeFlags.Null)) {
            anyNull = true;
        }
        else if (Lint.isTypeFlagSet(ty, undefinedFlags)) {
            anyUndefined = true;
        }
        else {
            anyOther = true;
        }
    }
    return !anyOther ? true
        : anyNull && anyUndefined ? undefined
            : anyNull ? "null"
                : anyUndefined ? "undefined"
                    : false;
}
function unionParts(type) {
    return isUnionType(type) ? type.types : [type];
}
/** Type predicate to test for a union type. */
function isUnionType(type) {
    return Lint.isTypeFlagSet(type, ts.TypeFlags.Union);
}
var _a;
