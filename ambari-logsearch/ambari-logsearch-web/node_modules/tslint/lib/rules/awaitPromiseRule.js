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
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.applyWithProgram = function (srcFile, langSvc) {
        return this.applyWithWalker(new Walker(srcFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "await-promise",
    description: "Warns for an awaited value that is not a Promise.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "'await' of non-Promise.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Walker.prototype.visitNode = function (node) {
        if (node.kind === ts.SyntaxKind.AwaitExpression &&
            !couldBePromise(this.getTypeChecker().getTypeAtLocation(node.expression))) {
            this.addFailureAtNode(node, Rule.FAILURE_STRING);
        }
        _super.prototype.visitNode.call(this, node);
    };
    return Walker;
}(Lint.ProgramAwareRuleWalker));
function couldBePromise(type) {
    if (Lint.isTypeFlagSet(type, ts.TypeFlags.Any) || isPromiseType(type)) {
        return true;
    }
    if (isUnionType(type)) {
        return type.types.some(isPromiseType);
    }
    var bases = type.getBaseTypes();
    return bases !== undefined && bases.some(couldBePromise);
}
function isPromiseType(type) {
    var target = type.target;
    var symbol = target && target.symbol;
    return !!symbol && symbol.name === "Promise";
}
function isUnionType(type) {
    return Lint.isTypeFlagSet(type, ts.TypeFlags.Union);
}
