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
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new CyclomaticComplexityWalker(sourceFile, this.getOptions(), this.threshold));
    };
    Rule.prototype.isEnabled = function () {
        // Disable the rule if the option is provided but non-numeric or less than the minimum.
        var isThresholdValid = typeof this.threshold === "number" && this.threshold >= Rule.MINIMUM_THRESHOLD;
        return _super.prototype.isEnabled.call(this) && isThresholdValid;
    };
    Object.defineProperty(Rule.prototype, "threshold", {
        get: function () {
            var ruleArguments = this.getOptions().ruleArguments;
            if (ruleArguments[0] !== undefined) {
                return ruleArguments[0];
            }
            return Rule.DEFAULT_THRESHOLD;
        },
        enumerable: true,
        configurable: true
    });
    return Rule;
}(Lint.Rules.AbstractRule));
Rule.DEFAULT_THRESHOLD = 20;
Rule.MINIMUM_THRESHOLD = 2;
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "cyclomatic-complexity",
    description: "Enforces a threshold of cyclomatic complexity.",
    descriptionDetails: (_a = ["\n            Cyclomatic complexity is assessed for each function of any type. A starting value of 20\n            is assigned and this value is then incremented for every statement which can branch the\n            control flow within the function. The following statements and expressions contribute\n            to cyclomatic complexity:\n            * `catch`\n            * `if` and `? :`\n            * `||` and `&&` due to short-circuit evaluation\n            * `for`, `for in` and `for of` loops\n            * `while` and `do while` loops"], _a.raw = ["\n            Cyclomatic complexity is assessed for each function of any type. A starting value of 20\n            is assigned and this value is then incremented for every statement which can branch the\n            control flow within the function. The following statements and expressions contribute\n            to cyclomatic complexity:\n            * \\`catch\\`\n            * \\`if\\` and \\`? :\\`\n            * \\`||\\` and \\`&&\\` due to short-circuit evaluation\n            * \\`for\\`, \\`for in\\` and \\`for of\\` loops\n            * \\`while\\` and \\`do while\\` loops"], Lint.Utils.dedent(_a)),
    rationale: (_b = ["\n            Cyclomatic complexity is a code metric which indicates the level of complexity in a\n            function. High cyclomatic complexity indicates confusing code which may be prone to\n            errors or difficult to modify."], _b.raw = ["\n            Cyclomatic complexity is a code metric which indicates the level of complexity in a\n            function. High cyclomatic complexity indicates confusing code which may be prone to\n            errors or difficult to modify."], Lint.Utils.dedent(_b)),
    optionsDescription: (_c = ["\n            An optional upper limit for cyclomatic complexity can be specified. If no limit option\n            is provided a default value of $(Rule.DEFAULT_THRESHOLD) will be used."], _c.raw = ["\n            An optional upper limit for cyclomatic complexity can be specified. If no limit option\n            is provided a default value of $(Rule.DEFAULT_THRESHOLD) will be used."], Lint.Utils.dedent(_c)),
    options: {
        type: "number",
        minimum: "$(Rule.MINIMUM_THRESHOLD)",
    },
    optionExamples: ["true", "[true, 20]"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.ANONYMOUS_FAILURE_STRING = function (expected, actual) {
    return "The function has a cyclomatic complexity of " + actual + " which is higher than the threshold of " + expected;
};
Rule.NAMED_FAILURE_STRING = function (expected, actual, name) {
    return "The function " + name + " has a cyclomatic complexity of " + actual + " which is higher than the threshold of " + expected;
};
exports.Rule = Rule;
var CyclomaticComplexityWalker = (function (_super) {
    __extends(CyclomaticComplexityWalker, _super);
    function CyclomaticComplexityWalker(sourceFile, options, threshold) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.threshold = threshold;
        _this.functions = [];
        return _this;
    }
    CyclomaticComplexityWalker.prototype.visitArrowFunction = function (node) {
        this.startFunction();
        _super.prototype.visitArrowFunction.call(this, node);
        this.endFunction(node);
    };
    CyclomaticComplexityWalker.prototype.visitBinaryExpression = function (node) {
        switch (node.operatorToken.kind) {
            case ts.SyntaxKind.BarBarToken:
            case ts.SyntaxKind.AmpersandAmpersandToken:
                this.incrementComplexity();
                break;
            default:
                break;
        }
        _super.prototype.visitBinaryExpression.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitCaseClause = function (node) {
        this.incrementComplexity();
        _super.prototype.visitCaseClause.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitCatchClause = function (node) {
        this.incrementComplexity();
        _super.prototype.visitCatchClause.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitConditionalExpression = function (node) {
        this.incrementComplexity();
        _super.prototype.visitConditionalExpression.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitConstructorDeclaration = function (node) {
        this.startFunction();
        _super.prototype.visitConstructorDeclaration.call(this, node);
        this.endFunction(node);
    };
    CyclomaticComplexityWalker.prototype.visitDoStatement = function (node) {
        this.incrementComplexity();
        _super.prototype.visitDoStatement.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitForStatement = function (node) {
        this.incrementComplexity();
        _super.prototype.visitForStatement.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitForInStatement = function (node) {
        this.incrementComplexity();
        _super.prototype.visitForInStatement.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitForOfStatement = function (node) {
        this.incrementComplexity();
        _super.prototype.visitForOfStatement.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitFunctionDeclaration = function (node) {
        this.startFunction();
        _super.prototype.visitFunctionDeclaration.call(this, node);
        this.endFunction(node);
    };
    CyclomaticComplexityWalker.prototype.visitFunctionExpression = function (node) {
        this.startFunction();
        _super.prototype.visitFunctionExpression.call(this, node);
        this.endFunction(node);
    };
    CyclomaticComplexityWalker.prototype.visitGetAccessor = function (node) {
        this.startFunction();
        _super.prototype.visitGetAccessor.call(this, node);
        this.endFunction(node);
    };
    CyclomaticComplexityWalker.prototype.visitIfStatement = function (node) {
        this.incrementComplexity();
        _super.prototype.visitIfStatement.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.visitMethodDeclaration = function (node) {
        this.startFunction();
        _super.prototype.visitMethodDeclaration.call(this, node);
        this.endFunction(node);
    };
    CyclomaticComplexityWalker.prototype.visitSetAccessor = function (node) {
        this.startFunction();
        _super.prototype.visitSetAccessor.call(this, node);
        this.endFunction(node);
    };
    CyclomaticComplexityWalker.prototype.visitWhileStatement = function (node) {
        this.incrementComplexity();
        _super.prototype.visitWhileStatement.call(this, node);
    };
    CyclomaticComplexityWalker.prototype.startFunction = function () {
        // Push an initial complexity value to the stack for the new function.
        this.functions.push(1);
    };
    CyclomaticComplexityWalker.prototype.endFunction = function (node) {
        var complexity = this.functions.pop();
        // Check for a violation.
        if (complexity !== undefined && complexity > this.threshold) {
            var failureString = void 0;
            // Attempt to find a name for the function.
            if (node.name && node.name.kind === ts.SyntaxKind.Identifier) {
                failureString = Rule.NAMED_FAILURE_STRING(this.threshold, complexity, node.name.text);
            }
            else {
                failureString = Rule.ANONYMOUS_FAILURE_STRING(this.threshold, complexity);
            }
            this.addFailureAtNode(node, failureString);
        }
    };
    CyclomaticComplexityWalker.prototype.incrementComplexity = function () {
        if (this.functions.length) {
            this.functions[this.functions.length - 1]++;
        }
    };
    return CyclomaticComplexityWalker;
}(Lint.RuleWalker));
var _a, _b, _c;
