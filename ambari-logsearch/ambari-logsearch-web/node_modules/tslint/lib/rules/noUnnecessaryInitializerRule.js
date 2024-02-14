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
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-unnecessary-initializer",
    description: "Forbids a 'var'/'let' statement or destructuring initializer to be initialized to 'undefined'.",
    hasFix: true,
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Unnecessary initialization to 'undefined'.";
Rule.FAILURE_STRING_PARAMETER = "Use an optional parameter instead of initializing to 'undefined'. " +
    "Also, the type declaration does not need to include '| undefined'.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitVariableDeclaration = function (node) {
        if (utils.isBindingPattern(node.name)) {
            for (var _i = 0, _a = node.name.elements; _i < _a.length; _i++) {
                var elem = _a[_i];
                if (elem.kind === ts.SyntaxKind.BindingElement) {
                    this.checkInitializer(elem);
                }
            }
        }
        else if (!Lint.isNodeFlagSet(node.parent, ts.NodeFlags.Const)) {
            this.checkInitializer(node);
        }
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    Walker.prototype.visitMethodDeclaration = function (node) {
        this.checkSignature(node);
        _super.prototype.visitMethodDeclaration.call(this, node);
    };
    Walker.prototype.visitFunctionDeclaration = function (node) {
        this.checkSignature(node);
        _super.prototype.visitFunctionDeclaration.call(this, node);
    };
    Walker.prototype.visitConstructorDeclaration = function (node) {
        this.checkSignature(node);
        _super.prototype.visitConstructorDeclaration.call(this, node);
    };
    Walker.prototype.checkSignature = function (_a) {
        var _this = this;
        var parameters = _a.parameters;
        parameters.forEach(function (parameter, i) {
            if (isUndefined(parameter.initializer)) {
                if (parametersAllOptionalAfter(parameters, i)) {
                    // No fix since they may want to remove '| undefined' from the type.
                    _this.addFailureAtNode(parameter, Rule.FAILURE_STRING_PARAMETER);
                }
                else {
                    _this.failWithFix(parameter);
                }
            }
        });
    };
    Walker.prototype.checkInitializer = function (node) {
        if (isUndefined(node.initializer)) {
            this.failWithFix(node);
        }
    };
    Walker.prototype.failWithFix = function (node) {
        var fix = this.createFix(this.deleteFromTo(Lint.childOfKind(node, ts.SyntaxKind.EqualsToken).pos, node.end));
        this.addFailureAtNode(node, Rule.FAILURE_STRING, fix);
    };
    return Walker;
}(Lint.RuleWalker));
function parametersAllOptionalAfter(parameters, idx) {
    for (var i = idx + 1; i < parameters.length; i++) {
        if (parameters[i].questionToken) {
            return true;
        }
        if (!parameters[i].initializer) {
            return false;
        }
    }
    return true;
}
function isUndefined(node) {
    return node !== undefined &&
        node.kind === ts.SyntaxKind.Identifier &&
        node.originalKeywordKind === ts.SyntaxKind.UndefinedKeyword;
}
