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
var utils = require("tsutils");
var ts = require("typescript");
var Lint = require("../index");
var utils_1 = require("../utils");
var adjacentOverloadSignaturesRule_1 = require("./adjacentOverloadSignaturesRule");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.FAILURE_STRING_SINGLE_PARAMETER_DIFFERENCE = function (type1, type2) {
        return "These overloads can be combined into one signature taking `" + type1 + " | " + type2 + "`.";
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "unified-signatures",
    description: "Warns for any two overloads that could be unified into one by using a union or an optional/rest parameter.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_OMITTING_SINGLE_PARAMETER = "These overloads can be combined into one signature with an optional parameter.";
Rule.FAILURE_STRING_OMITTING_REST_PARAMETER = "These overloads can be combined into one signature with a rest parameter.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitSourceFile = function (node) {
        this.checkStatements(node.statements);
        _super.prototype.visitSourceFile.call(this, node);
    };
    Walker.prototype.visitModuleDeclaration = function (node) {
        var body = node.body;
        if (body && body.kind === ts.SyntaxKind.ModuleBlock) {
            this.checkStatements(body.statements);
        }
        _super.prototype.visitModuleDeclaration.call(this, node);
    };
    Walker.prototype.visitInterfaceDeclaration = function (node) {
        this.checkMembers(node.members, node.typeParameters);
        _super.prototype.visitInterfaceDeclaration.call(this, node);
    };
    Walker.prototype.visitClassDeclaration = function (node) {
        this.checkMembers(node.members, node.typeParameters);
        _super.prototype.visitClassDeclaration.call(this, node);
    };
    Walker.prototype.visitTypeLiteral = function (node) {
        this.checkMembers(node.members);
        _super.prototype.visitTypeLiteral.call(this, node);
    };
    Walker.prototype.checkStatements = function (statements) {
        this.checkOverloads(statements, function (statement) {
            if (statement.kind === ts.SyntaxKind.FunctionDeclaration) {
                var fn = statement;
                if (fn.body) {
                    return undefined;
                }
                return fn.name && { signature: fn, key: fn.name.text };
            }
            else {
                return undefined;
            }
        });
    };
    Walker.prototype.checkMembers = function (members, typeParameters) {
        this.checkOverloads(members, getOverloadName, typeParameters);
        function getOverloadName(member) {
            if (!utils.isSignatureDeclaration(member) || member.body) {
                return undefined;
            }
            var key = adjacentOverloadSignaturesRule_1.getOverloadKey(member);
            return key === undefined ? undefined : { signature: member, key: key };
        }
    };
    Walker.prototype.checkOverloads = function (signatures, getOverload, typeParameters) {
        var _this = this;
        var isTypeParameter = getIsTypeParameter(typeParameters);
        for (var _i = 0, _a = collectOverloads(signatures, getOverload); _i < _a.length; _i++) {
            var overloads = _a[_i];
            forEachPair(overloads, function (a, b) {
                _this.compareSignatures(a, b, isTypeParameter);
            });
        }
    };
    Walker.prototype.compareSignatures = function (a, b, isTypeParameter) {
        if (!signaturesCanBeUnified(a, b, isTypeParameter)) {
            return;
        }
        if (a.parameters.length === b.parameters.length) {
            var params = signaturesDifferBySingleParameter(a.parameters, b.parameters);
            if (params) {
                var p0 = params[0], p1 = params[1];
                this.addFailureAtNode(p1, Rule.FAILURE_STRING_SINGLE_PARAMETER_DIFFERENCE(typeText(p0), typeText(p1)));
            }
        }
        else {
            var extraParameter = signaturesDifferByOptionalOrRestParameter(a.parameters, b.parameters);
            if (extraParameter) {
                this.addFailureAtNode(extraParameter, extraParameter.dotDotDotToken
                    ? Rule.FAILURE_STRING_OMITTING_REST_PARAMETER
                    : Rule.FAILURE_STRING_OMITTING_SINGLE_PARAMETER);
            }
        }
    };
    return Walker;
}(Lint.RuleWalker));
function typeText(_a) {
    var type = _a.type;
    return type === undefined ? "any" : type.getText();
}
function signaturesCanBeUnified(a, b, isTypeParameter) {
    // Must return the same type.
    return typesAreEqual(a.type, b.type) &&
        // Must take the same type parameters.
        utils_1.arraysAreEqual(a.typeParameters, b.typeParameters, typeParametersAreEqual) &&
        // If one uses a type parameter (from outside) and the other doesn't, they shouldn't be joined.
        signatureUsesTypeParameter(a, isTypeParameter) === signatureUsesTypeParameter(b, isTypeParameter);
}
/** Detect `a(x: number, y: number, z: number)` and `a(x: number, y: string, z: number)`. */
function signaturesDifferBySingleParameter(types1, types2) {
    var index = getIndexOfFirstDifference(types1, types2, parametersAreEqual);
    if (index === undefined) {
        return undefined;
    }
    // If remaining arrays are equal, the signatures differ by just one parameter type
    if (!utils_1.arraysAreEqual(types1.slice(index + 1), types2.slice(index + 1), parametersAreEqual)) {
        return undefined;
    }
    var a = types1[index];
    var b = types2[index];
    return parametersHaveEqualSigils(a, b) ? [a, b] : undefined;
}
/**
 * Detect `a(): void` and `a(x: number): void`.
 * Returns the parameter declaration (`x: number` in this example) that should be optional/rest.
 */
function signaturesDifferByOptionalOrRestParameter(types1, types2) {
    var minLength = Math.min(types1.length, types2.length);
    var longer = types1.length < types2.length ? types2 : types1;
    // If one is has 2+ parameters more than the other, they must all be optional/rest.
    // Differ by optional parameters: f() and f(x), f() and f(x, ?y, ...z)
    // Not allowed: f() and f(x, y)
    for (var i = minLength + 1; i < longer.length; i++) {
        if (!parameterMayBeMissing(longer[i])) {
            return undefined;
        }
    }
    for (var i = 0; i < minLength; i++) {
        if (!typesAreEqual(types1[i].type, types2[i].type)) {
            return undefined;
        }
    }
    return longer[longer.length - 1];
}
/** Given type parameters, returns a function to test whether a type is one of those parameters. */
function getIsTypeParameter(typeParameters) {
    if (!typeParameters) {
        return function () { return false; };
    }
    var set = new Set();
    for (var _i = 0, typeParameters_1 = typeParameters; _i < typeParameters_1.length; _i++) {
        var t = typeParameters_1[_i];
        set.add(t.getText());
    }
    return function (typeName) { return set.has(typeName); };
}
/** True if any of the outer type parameters are used in a signature. */
function signatureUsesTypeParameter(sig, isTypeParameter) {
    return sig.parameters.some(function (p) { return p.type !== undefined && typeContainsTypeParameter(p.type); });
    function typeContainsTypeParameter(type) {
        if (type.kind === ts.SyntaxKind.TypeReference) {
            var name = type.typeName;
            if (name.kind === ts.SyntaxKind.Identifier && isTypeParameter(name.text)) {
                return true;
            }
        }
        return !!ts.forEachChild(type, typeContainsTypeParameter);
    }
}
/**
 * Given all signatures, collects an array of arrays of signatures which are all overloads.
 * Does not rely on overloads being adjacent. This is similar to code in adjacentOverloadSignaturesRule.ts, but not the same.
 */
function collectOverloads(nodes, getOverload) {
    var map = new Map();
    for (var _i = 0, nodes_1 = nodes; _i < nodes_1.length; _i++) {
        var sig = nodes_1[_i];
        var overload = getOverload(sig);
        if (!overload) {
            continue;
        }
        var signature = overload.signature, key = overload.key;
        var overloads = map.get(key);
        if (overloads) {
            overloads.push(signature);
        }
        else {
            map.set(key, [signature]);
        }
    }
    return Array.from(map.values());
}
function parametersAreEqual(a, b) {
    return parametersHaveEqualSigils(a, b) && typesAreEqual(a.type, b.type);
}
/** True for optional/rest parameters. */
function parameterMayBeMissing(p) {
    return !!p.dotDotDotToken || !!p.questionToken;
}
/** False if one is optional and the other isn't, or one is a rest parameter and the other isn't. */
function parametersHaveEqualSigils(a, b) {
    return !!a.dotDotDotToken === !!b.dotDotDotToken && !!a.questionToken === !!b.questionToken;
}
function typeParametersAreEqual(a, b) {
    return a.name.text === b.name.text && typesAreEqual(a.constraint, b.constraint);
}
function typesAreEqual(a, b) {
    // TODO: Could traverse AST so that formatting differences don't affect this.
    return a === b || !!a && !!b && a.getText() === b.getText();
}
/** Returns the first index where `a` and `b` differ. */
function getIndexOfFirstDifference(a, b, equal) {
    for (var i = 0; i < a.length && i < b.length; i++) {
        if (!equal(a[i], b[i])) {
            return i;
        }
    }
    return undefined;
}
/** Calls `action` for every pair of values in `values`. */
function forEachPair(values, action) {
    for (var i = 0; i < values.length; i++) {
        for (var j = i + 1; j < values.length; j++) {
            action(values[i], values[j]);
        }
    }
}
