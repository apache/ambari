/**
 * @license
 * Copyright 2015 Palantir Technologies, Inc.
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
var OPTION_IGNORE_PARMS = "ignore-params";
var OPTION_IGNORE_PROPERTIES = "ignore-properties";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoInferrableTypesWalker(sourceFile, this.ruleName, {
            ignoreParameters: this.ruleArguments.indexOf(OPTION_IGNORE_PARMS) !== -1,
            ignoreProperties: this.ruleArguments.indexOf(OPTION_IGNORE_PROPERTIES) !== -1,
        }));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-inferrable-types",
    description: "Disallows explicit type declarations for variables or parameters initialized to a number, string, or boolean.",
    rationale: "Explicit types where they can be easily inferred by the compiler make code more verbose.",
    optionsDescription: (_a = ["\n            Two arguments may be optionally provided:\n\n            * `", "` allows specifying an inferrable type annotation for function params.\n            This can be useful when combining with the `typedef` rule.\n            * `", "` allows specifying an inferrable type annotation for class properties."], _a.raw = ["\n            Two arguments may be optionally provided:\n\n            * \\`", "\\` allows specifying an inferrable type annotation for function params.\n            This can be useful when combining with the \\`typedef\\` rule.\n            * \\`", "\\` allows specifying an inferrable type annotation for class properties."], Lint.Utils.dedent(_a, OPTION_IGNORE_PARMS, OPTION_IGNORE_PROPERTIES)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [OPTION_IGNORE_PARMS, OPTION_IGNORE_PROPERTIES],
        },
        minLength: 0,
        maxLength: 2,
    },
    hasFix: true,
    optionExamples: [
        "true",
        "[true, \"" + OPTION_IGNORE_PARMS + "\"]",
        "[true, \"" + OPTION_IGNORE_PARMS + "\", \"" + OPTION_IGNORE_PROPERTIES + "\"]",
    ],
    type: "typescript",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (type) {
    return "Type " + type + " trivially inferred from a " + type + " literal, remove type annotation";
};
exports.Rule = Rule;
var NoInferrableTypesWalker = (function (_super) {
    __extends(NoInferrableTypesWalker, _super);
    function NoInferrableTypesWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoInferrableTypesWalker.prototype.walk = function (sourceFile) {
        var _this = this;
        var cb = function (node) {
            switch (node.kind) {
                case ts.SyntaxKind.Parameter:
                    if (!_this.options.ignoreParameters) {
                        _this.checkDeclaration(node);
                    }
                    break;
                case ts.SyntaxKind.PropertyDeclaration:
                    if (_this.options.ignoreProperties) {
                        break;
                    }
                /* falls through*/
                case ts.SyntaxKind.VariableDeclaration:
                    _this.checkDeclaration(node);
                default:
            }
            return ts.forEachChild(node, cb);
        };
        return ts.forEachChild(sourceFile, cb);
    };
    NoInferrableTypesWalker.prototype.checkDeclaration = function (node) {
        if (node.type != null && node.initializer != null) {
            var failure = null;
            switch (node.type.kind) {
                case ts.SyntaxKind.BooleanKeyword:
                    if (node.initializer.kind === ts.SyntaxKind.TrueKeyword || node.initializer.kind === ts.SyntaxKind.FalseKeyword) {
                        failure = "boolean";
                    }
                    break;
                case ts.SyntaxKind.NumberKeyword:
                    if (node.initializer.kind === ts.SyntaxKind.NumericLiteral) {
                        failure = "number";
                    }
                    break;
                case ts.SyntaxKind.StringKeyword:
                    switch (node.initializer.kind) {
                        case ts.SyntaxKind.StringLiteral:
                        case ts.SyntaxKind.NoSubstitutionTemplateLiteral:
                        case ts.SyntaxKind.TemplateExpression:
                            failure = "string";
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
            if (failure != null) {
                this.addFailureAtNode(node.type, Rule.FAILURE_STRING_FACTORY(failure), this.createFix(Lint.Replacement.deleteFromTo(node.name.end, node.type.end)));
            }
        }
    };
    return NoInferrableTypesWalker;
}(Lint.AbstractWalker));
var _a;
