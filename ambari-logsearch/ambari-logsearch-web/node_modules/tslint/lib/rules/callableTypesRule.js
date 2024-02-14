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
    /* tslint:enable:object-literal-sort-keys */
    Rule.failureStringForInterface = function (name, sigSuggestion) {
        return "Interface has only a call signature \u2014 use `type " + name + " = " + sigSuggestion + "` instead.";
    };
    Rule.failureStringForTypeLiteral = function (sigSuggestion) {
        return "Type literal has only a call signature \u2014 use `" + sigSuggestion + "` instead.";
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "callable-types",
    description: "An interface or literal type with just a call signature can be written as a function type.",
    rationale: "style",
    optionsDescription: "Not configurable.",
    options: null,
    type: "style",
    typescriptOnly: true,
};
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitInterfaceDeclaration = function (node) {
        if (noSupertype(node.heritageClauses)) {
            this.check(node);
        }
        _super.prototype.visitInterfaceDeclaration.call(this, node);
    };
    Walker.prototype.visitTypeLiteral = function (node) {
        this.check(node);
        _super.prototype.visitTypeLiteral.call(this, node);
    };
    Walker.prototype.check = function (node) {
        if (node.members.length === 1 && node.members[0].kind === ts.SyntaxKind.CallSignature) {
            var call = node.members[0];
            if (!call.type) {
                // Bad parse
                return;
            }
            var suggestion = renderSuggestion(call);
            if (node.kind === ts.SyntaxKind.InterfaceDeclaration) {
                this.addFailureAtNode(node.name, Rule.failureStringForInterface(node.name.getText(), suggestion));
            }
            else {
                this.addFailureAtNode(call, Rule.failureStringForTypeLiteral(suggestion));
            }
        }
    };
    return Walker;
}(Lint.RuleWalker));
/** True if there is no supertype or if the supertype is `Function`. */
function noSupertype(heritageClauses) {
    if (!heritageClauses) {
        return true;
    }
    if (heritageClauses.length === 1) {
        var expr = heritageClauses[0].types[0].expression;
        if (expr.kind === ts.SyntaxKind.Identifier && expr.text === "Function") {
            return true;
        }
    }
    return false;
}
function renderSuggestion(call) {
    var typeParameters = call.typeParameters && call.typeParameters.map(function (p) { return p.getText(); }).join(", ");
    var parameters = call.parameters.map(function (p) { return p.getText(); }).join(", ");
    var returnType = call.type === undefined ? "void" : call.type.getText();
    var res = "(" + parameters + ") => " + returnType;
    if (typeParameters) {
        res = "<" + typeParameters + ">" + res;
    }
    return res;
}
