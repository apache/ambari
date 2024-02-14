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
    Rule.prototype.applyWithProgram = function (srcFile, langSvc) {
        return this.applyWithWalker(new NoInferredEmptyObjectTypeRule(srcFile, this.getOptions(), langSvc.getProgram()));
    };
    return Rule;
}(Lint.Rules.TypedRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-inferred-empty-object-type",
    description: "Disallow type inference of {} (empty object type) at function and constructor call sites",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: true,
    requiresTypeInfo: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.EMPTY_INTERFACE_INSTANCE = "Explicit type parameter needs to be provided to the constructor";
Rule.EMPTY_INTERFACE_FUNCTION = "Explicit type parameter needs to be provided to the function call";
exports.Rule = Rule;
var NoInferredEmptyObjectTypeRule = (function (_super) {
    __extends(NoInferredEmptyObjectTypeRule, _super);
    function NoInferredEmptyObjectTypeRule(srcFile, lintOptions, program) {
        var _this = _super.call(this, srcFile, lintOptions, program) || this;
        _this.checker = _this.getTypeChecker();
        return _this;
    }
    NoInferredEmptyObjectTypeRule.prototype.visitNewExpression = function (node) {
        var _this = this;
        var nodeTypeArgs = node.typeArguments;
        var isObjectReference;
        if (ts.TypeFlags.Reference != null) {
            // typescript 2.0.x specific code
            isObjectReference = function (o) { return utils_1.isTypeFlagSet(o, ts.TypeFlags.Reference); };
        }
        else {
            isObjectReference = function (o) { return utils_1.isTypeFlagSet(o, ts.TypeFlags.Object); };
        }
        if (nodeTypeArgs === undefined) {
            var objType = this.checker.getTypeAtLocation(node);
            if (isObjectReference(objType) && objType.typeArguments !== undefined) {
                var typeArgs = objType.typeArguments;
                typeArgs.forEach(function (a) {
                    if (_this.isEmptyObjectInterface(a)) {
                        _this.addFailureAtNode(node, Rule.EMPTY_INTERFACE_INSTANCE);
                    }
                });
            }
        }
        _super.prototype.visitNewExpression.call(this, node);
    };
    NoInferredEmptyObjectTypeRule.prototype.visitCallExpression = function (node) {
        if (node.typeArguments === undefined) {
            var callSig = this.checker.getResolvedSignature(node);
            var retType = this.checker.getReturnTypeOfSignature(callSig);
            if (this.isEmptyObjectInterface(retType)) {
                this.addFailureAtNode(node, Rule.EMPTY_INTERFACE_FUNCTION);
            }
        }
        _super.prototype.visitCallExpression.call(this, node);
    };
    NoInferredEmptyObjectTypeRule.prototype.isEmptyObjectInterface = function (objType) {
        var _this = this;
        var isAnonymous;
        if (ts.ObjectFlags == null) {
            // typescript 2.0.x specific code
            isAnonymous = utils_1.isTypeFlagSet(objType, ts.TypeFlags.Anonymous);
        }
        else {
            isAnonymous = utils_1.isObjectFlagSet(objType, ts.ObjectFlags.Anonymous);
        }
        var hasProblematicCallSignatures = false;
        var hasProperties = (objType.getProperties() !== undefined && objType.getProperties().length > 0);
        var hasNumberIndexType = objType.getNumberIndexType() !== undefined;
        var hasStringIndexType = objType.getStringIndexType() !== undefined;
        var callSig = objType.getCallSignatures();
        if (callSig !== undefined && callSig.length > 0) {
            var isClean = callSig.every(function (sig) {
                var csigRetType = _this.checker.getReturnTypeOfSignature(sig);
                return _this.isEmptyObjectInterface(csigRetType);
            });
            if (!isClean) {
                hasProblematicCallSignatures = true;
            }
        }
        return (isAnonymous && !hasProblematicCallSignatures && !hasProperties && !hasNumberIndexType && !hasStringIndexType);
    };
    return NoInferredEmptyObjectTypeRule;
}(Lint.ProgramAwareRuleWalker));
