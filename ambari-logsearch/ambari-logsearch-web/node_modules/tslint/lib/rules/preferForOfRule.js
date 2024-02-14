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
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new PreferForOfWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "prefer-for-of",
    description: "Recommends a 'for-of' loop over a standard 'for' loop if the index is only used to access the array being iterated.",
    rationale: "A for(... of ...) loop is easier to implement and read when the index is not needed.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "typescript",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Expected a 'for-of' loop instead of a 'for' loop with this simple iteration";
exports.Rule = Rule;
var PreferForOfWalker = (function (_super) {
    __extends(PreferForOfWalker, _super);
    function PreferForOfWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PreferForOfWalker.prototype.createScope = function () { }; // tslint:disable-line:no-empty
    PreferForOfWalker.prototype.createBlockScope = function () {
        return new Map();
    };
    PreferForOfWalker.prototype.visitForStatement = function (node) {
        var arrayNodeInfo = this.getForLoopHeaderInfo(node);
        var currentBlockScope = this.getCurrentBlockScope();
        var indexVariableName;
        if (node.incrementor != null && arrayNodeInfo != null) {
            var indexVariable = arrayNodeInfo.indexVariable, arrayToken = arrayNodeInfo.arrayToken;
            indexVariableName = indexVariable.getText();
            // store `for` loop state
            currentBlockScope.set(indexVariableName, {
                arrayToken: arrayToken,
                forLoopEndPosition: node.incrementor.end + 1,
                onlyArrayReadAccess: true,
            });
        }
        _super.prototype.visitForStatement.call(this, node);
        if (indexVariableName != null) {
            var incrementorState = currentBlockScope.get(indexVariableName);
            if (incrementorState.onlyArrayReadAccess) {
                this.addFailureFromStartToEnd(node.getStart(), incrementorState.forLoopEndPosition, Rule.FAILURE_STRING);
            }
            // remove current `for` loop state
            currentBlockScope.delete(indexVariableName);
        }
    };
    PreferForOfWalker.prototype.visitIdentifier = function (node) {
        var incrementorScope = this.findBlockScope(function (scope) { return scope.has(node.text); });
        if (incrementorScope != null) {
            var incrementorState = incrementorScope.get(node.text);
            // check if the identifier is an iterator and is currently in the `for` loop body
            if (incrementorState != null && incrementorState.arrayToken != null && incrementorState.forLoopEndPosition < node.getStart()) {
                // check if iterator is used for something other than reading data from array
                if (node.parent.kind === ts.SyntaxKind.ElementAccessExpression) {
                    var elementAccess = node.parent;
                    var arrayIdentifier = utils_1.unwrapParentheses(elementAccess.expression);
                    if (incrementorState.arrayToken.getText() !== arrayIdentifier.getText()) {
                        // iterator used in array other than one iterated over
                        incrementorState.onlyArrayReadAccess = false;
                    }
                    else if (elementAccess.parent != null && utils_1.isAssignment(elementAccess.parent)) {
                        // array position is assigned a new value
                        incrementorState.onlyArrayReadAccess = false;
                    }
                }
                else {
                    incrementorState.onlyArrayReadAccess = false;
                }
            }
            _super.prototype.visitIdentifier.call(this, node);
        }
    };
    // returns the iterator and array of a `for` loop if the `for` loop is basic. Otherwise, `null`
    PreferForOfWalker.prototype.getForLoopHeaderInfo = function (forLoop) {
        var indexVariableName;
        var indexVariable;
        // assign `indexVariableName` if initializer is simple and starts at 0
        if (forLoop.initializer != null && forLoop.initializer.kind === ts.SyntaxKind.VariableDeclarationList) {
            var syntaxList = forLoop.initializer.getChildAt(1);
            if (syntaxList.kind === ts.SyntaxKind.SyntaxList && syntaxList.getChildCount() === 1) {
                var assignment = syntaxList.getChildAt(0);
                if (assignment.kind === ts.SyntaxKind.VariableDeclaration && assignment.getChildCount() === 3) {
                    var value = assignment.getChildAt(2).getText();
                    if (value === "0") {
                        indexVariable = assignment.getChildAt(0);
                        indexVariableName = indexVariable.getText();
                    }
                }
            }
        }
        // ensure `for` condition
        if (indexVariableName == null
            || forLoop.condition == null
            || forLoop.condition.kind !== ts.SyntaxKind.BinaryExpression
            || forLoop.condition.getChildAt(0).getText() !== indexVariableName
            || forLoop.condition.getChildAt(1).getText() !== "<") {
            return null;
        }
        if (forLoop.incrementor == null || !this.isIncremented(forLoop.incrementor, indexVariableName)) {
            return null;
        }
        // ensure that the condition checks a `length` property
        var conditionRight = forLoop.condition.getChildAt(2);
        if (conditionRight.kind === ts.SyntaxKind.PropertyAccessExpression) {
            var propertyAccess = conditionRight;
            if (indexVariable != null && propertyAccess.name.getText() === "length") {
                return { indexVariable: indexVariable, arrayToken: utils_1.unwrapParentheses(propertyAccess.expression) };
            }
        }
        return null;
    };
    PreferForOfWalker.prototype.isIncremented = function (node, indexVariableName) {
        if (node == null) {
            return false;
        }
        // ensure variable is incremented
        if (node.kind === ts.SyntaxKind.PrefixUnaryExpression) {
            var incrementor = node;
            if (incrementor.operator === ts.SyntaxKind.PlusPlusToken && incrementor.operand.getText() === indexVariableName) {
                // x++
                return true;
            }
        }
        else if (node.kind === ts.SyntaxKind.PostfixUnaryExpression) {
            var incrementor = node;
            if (incrementor.operator === ts.SyntaxKind.PlusPlusToken && incrementor.operand.getText() === indexVariableName) {
                // ++x
                return true;
            }
        }
        else if (node.kind === ts.SyntaxKind.BinaryExpression) {
            var binaryExpression = node;
            if (binaryExpression.operatorToken.getText() === "+="
                && binaryExpression.left.getText() === indexVariableName
                && binaryExpression.right.getText() === "1") {
                // x += 1
                return true;
            }
            if (binaryExpression.operatorToken.getText() === "="
                && binaryExpression.left.getText() === indexVariableName) {
                var addExpression = binaryExpression.right;
                if (addExpression.operatorToken.getText() === "+") {
                    if (addExpression.right.getText() === indexVariableName && addExpression.left.getText() === "1") {
                        // x = 1 + x
                        return true;
                    }
                    else if (addExpression.left.getText() === indexVariableName && addExpression.right.getText() === "1") {
                        // x = x + 1
                        return true;
                    }
                }
            }
        }
        return false;
    };
    return PreferForOfWalker;
}(Lint.BlockScopeAwareRuleWalker));
